package com.example.androidapp.data.repository

import android.util.Log
import com.example.androidapp.data.local.LocalQuizPurger
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.entity.SyncEntityType
import com.example.androidapp.data.local.entity.SyncStatus
import com.example.androidapp.data.remote.firebase.QuestionRemoteDataSource
import com.example.androidapp.data.local.entity.SyncOperation
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.data.sync.SyncManager
import kotlinx.coroutines.SupervisorJob
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.util.ChecksumUtil
import com.example.androidapp.domain.util.safeCall
import com.example.androidapp.domain.repository.HomeQuizzes
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.example.androidapp.domain.service.EmbeddingIndex
import com.example.androidapp.domain.service.EmbeddingService
import com.example.androidapp.domain.util.VectorSimilarity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Local-first implementation of [QuizRepository].
 * Room is the single source of truth; Firestore syncs happen in the background.
 */
class QuizRepositoryImpl(
    private val quizDao: QuizDao,
    private val questionDao: QuestionDao,
    private val choiceDao: ChoiceDao,
    private val remoteDataSource: QuizRemoteDataSource,
    private val questionRemoteDataSource: QuestionRemoteDataSource,
    private val syncManager: SyncManager,
    private val shareCodeRepository: ShareCodeRepository,
    private val embeddingService: EmbeddingService,
    private val embeddingIndex: EmbeddingIndex
) : QuizRepository {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private companion object {
        private const val TAG = "QuizRepositoryImpl"

        /** Maximum number of user-owned quizzes shown on the home screen. */
        const val HOME_MY_QUIZZES_LIMIT = 20

        /** Maximum number of trending (public) quizzes shown on the home screen. */
        const val HOME_TRENDING_LIMIT = 20
    }

    /**
     * Upserts a quiz from a Firestore sync operation while preserving any
     * existing locally-computed embedding. Tries UPDATE first; falls back
     * to INSERT if the quiz does not exist locally yet.
     */
    private suspend fun upsertQuizFromSync(quiz: Quiz, syncStatus: String = SyncStatus.SYNCED.name) {
        val entity = quiz.toEntity(syncStatus)
        val updated = quizDao.updateQuizMetadata(
            id = entity.id,
            ownerId = entity.ownerId,
            title = entity.title,
            description = entity.description,
            authorName = entity.authorName,
            thumbnailUrl = entity.thumbnailUrl,
            isPublic = entity.isPublic,
            isDraft = entity.isDraft,
            shareCode = entity.shareCode,
            tags = entity.tags,
            checksum = entity.checksum,
            questionCount = entity.questionCount,
            attemptCount = entity.attemptCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
            syncStatus = entity.syncStatus,
            isRemovedFromCloud = entity.isRemovedFromCloud
        )
        if (updated == 0) {
            // Quiz doesn't exist locally yet -- full insert (embedding will be null,
            // EmbeddingIndexWorker will compute it in the next batch).
            quizDao.insertQuiz(entity)
        }
    }

    /**
     * Builds an FTS MATCH query from raw user input.
     *
     * Escapes FTS special characters, wraps in double quotes for phrase
     * matching, and appends a wildcard for prefix search.
     *
     * @return The escaped FTS query string, or null if the input is
     *         empty/blank after escaping (caller should fall back to LIKE).
     */
    private fun buildFtsQuery(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        // Remove FTS special characters: quotes, wildcards, parentheses, operators
        val escaped = trimmed
            .replace("\"", "")
            .replace("*", "")
            .replace("(", "")
            .replace(")", "")
            .replace(":", "")
        if (escaped.isBlank()) return null
        return "\"$escaped\"*"
    }

    override fun getHomeQuizzes(userId: String): Flow<HomeQuizzes> {
        val recentQuizzesFlow = quizDao.getRecentAttemptQuizzes(userId).map { entities ->
            entities.map { it.toDomain() }
        }
        val myQuizzesFlow = quizDao.getQuizzesByOwnerLimited(userId, HOME_MY_QUIZZES_LIMIT).map { entities ->
            entities.map { it.toDomain() }
        }
        val publicQuizzesFlow = quizDao.getPublicQuizzesLimited(HOME_TRENDING_LIMIT).map { entities ->
            entities.map { it.toDomain() }
        }
        // Refresh from Firestore in background when flow starts
        return combine(recentQuizzesFlow, myQuizzesFlow, publicQuizzesFlow) { recent, mine, public ->
            HomeQuizzes(
                recentAttemptQuizzes = recent,
                myQuizzes = mine,
                // publicQuizzesFlow is already sorted by attempt_count DESC and limited
                trendingQuizzes = public
            )
        }.onStart {
            refreshFromFirestore(userId)
        }
    }

    override suspend fun refreshHomeData(userId: String) {
        if (!syncManager.isSyncAllowed()) return
        refreshMyQuizzes(userId)
        refreshPublicQuizzes(currentUserId = userId)
    }

    override fun getMyQuizzes(userId: String): Flow<List<Quiz>> {
        return quizDao.getQuizzesByOwner(userId).map { entities ->
            entities.map { it.toDomain() }
        }.onStart { ioScope.launch { refreshMyQuizzes(userId) } }
    }

    override fun getPublicQuizzes(): Flow<List<Quiz>> {
        return quizDao.getPublicQuizzes().map { entities ->
            entities.map { it.toDomain() }
        }.onStart { ioScope.launch { refreshPublicQuizzes() } }
    }

    override fun searchQuizzes(query: String): Flow<List<Quiz>> {
        return quizDao.searchQuizzes(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDeletedQuizzes(userId: String): Flow<List<Quiz>> {
        return quizDao.getDeletedQuizzes(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getQuizById(quizId: String): Quiz? {
        return quizDao.getQuizById(quizId)?.toDomain()
    }

    override suspend fun getQuizByShareCode(shareCode: String): Quiz? {
        val local = quizDao.getQuizByShareCode(shareCode)
        if (local != null) return local.toDomain()
        return try {
            val remote = remoteDataSource.getQuizByShareCode(shareCode)
            remote?.toDomain()?.also { quiz ->
                upsertQuizFromSync(quiz)
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun getQuestionsForQuiz(quizId: String): Flow<List<Question>> {
        return questionDao.getQuestionsByQuizId(quizId).map { questionEntities ->
            questionEntities.map { qEntity ->
                val choiceEntities = choiceDao.getChoicesByQuestionIdOnce(qEntity.id)
                qEntity.toDomain(choiceEntities.map { it.toDomain() })
            }
        }
    }

    override suspend fun getQuestionsForQuizOnce(quizId: String): List<Question> {
        return questionDao.getQuestionsByQuizIdOnce(quizId).map { qEntity ->
            val choiceEntities = choiceDao.getChoicesByQuestionIdOnce(qEntity.id)
            qEntity.toDomain(choiceEntities.map { it.toDomain() })
        }
    }

    override suspend fun saveQuiz(quiz: Quiz, questions: List<Question>): Result<Unit> {
        return safeCall {
            val quizId = quiz.id.ifBlank { UUID.randomUUID().toString() }
            val finalQuiz = quiz.copy(id = quizId, questionCount = questions.size)

            // Compute content checksum before persisting so change detection works offline.
            val checksum = ChecksumUtil.computeQuizChecksum(finalQuiz, questions)

            // Write to Room first with PENDING status
            quizDao.insertQuiz(finalQuiz.toEntity(syncStatus = SyncStatus.PENDING.name).copy(checksum = checksum))
            questions.forEachIndexed { idx, question ->
                val qId = question.id.ifBlank { UUID.randomUUID().toString() }
                val finalQuestion = question.copy(id = qId, quizId = quizId, position = idx)
                questionDao.insertQuestion(finalQuestion.toEntity())
                finalQuestion.choices.forEachIndexed { cIdx, choice ->
                    val cId = choice.id.ifBlank { UUID.randomUUID().toString() }
                    choiceDao.insertChoice(choice.copy(id = cId).toEntity(qId))
                }
            }

            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.QUIZ,
                quizId,
                SyncOperation.CREATE
            )
        }
    }

    override suspend fun updateQuiz(quiz: Quiz, questions: List<Question>): Result<Unit> {
        return saveQuiz(quiz, questions)
    }

    override suspend fun deleteQuiz(quizId: String): Result<Unit> {
        return safeCall {
            // Clean up share code to prevent access to the deleted quiz
            val quiz = quizDao.getQuizById(quizId)
            quiz?.toDomain()?.shareCode?.let { code ->
                try {
                    shareCodeRepository.deleteShareCode(code)
                } catch (_: Exception) {
                    // Share code cleanup failure should not block quiz deletion
                }
                quizDao.updateQuiz(quiz.copy(shareCode = null))
            }

            val deletedAt = System.currentTimeMillis()
            quizDao.softDeleteQuiz(quizId, deletedAt)
            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.QUIZ,
                quizId,
                SyncOperation.UPDATE  // Soft delete is an update operation
            )
        }
    }

    override suspend fun restoreQuiz(quizId: String): Result<Unit> {
        return safeCall {
            quizDao.restoreQuiz(quizId)
            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.QUIZ,
                quizId,
                SyncOperation.UPDATE  // Restore is an update operation
            )
        }
    }

    override suspend fun permanentlyDeleteQuiz(quizId: String): Result<Unit> {
        return safeCall {
            val entity = quizDao.getQuizById(quizId)
            // Clean up share code before permanent deletion
            entity?.toDomain()?.shareCode?.let { code ->
                try {
                    shareCodeRepository.deleteShareCode(code)
                } catch (_: Exception) {
                    // Share code cleanup failure should not block quiz deletion
                }
            }
            if (entity != null) {
                LocalQuizPurger.purgeLocalQuiz(quizId, quizDao, questionDao, choiceDao)
            }
            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.QUIZ,
                quizId,
                SyncOperation.DELETE
            )
        }
    }

    override suspend fun incrementAttemptCount(quizId: String): Result<Unit> {
        return safeCall {
            quizDao.incrementAttemptCount(quizId)
            syncManager.enqueueSync(
                SyncEntityType.QUIZ,
                quizId,
                SyncOperation.UPDATE
            )
        }
    }

    override fun getTrendingQuizzes(): Flow<List<Quiz>> {
        return quizDao.getPublicQuizzesLimited(20).map { entities ->
            entities.map { it.toDomain() }
        }.onStart { ioScope.launch { refreshPublicQuizzes() } }
    }

    override suspend fun getAllTags(): List<String> {
        return try {
            quizDao.getAllTagStrings()
                .flatMap { tagString ->
                    tagString.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }
                .distinct()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load tags", e)
            emptyList()
        }
    }

    override suspend fun emptyTrash(userId: String): Result<Unit> {
        return safeCall {
            // Get all deleted quizzes for the user locally
            val deletedQuizzes = quizDao.getDeletedQuizzes(userId).first()

            // Clean up share codes for all trashed quizzes
            deletedQuizzes.forEach { entity ->
                entity.toDomain().shareCode?.let { code ->
                    try {
                        shareCodeRepository.deleteShareCode(code)
                    } catch (_: Exception) {
                        // Share code cleanup failure should not block trash emptying
                    }
                }
            }

            // Delete from local Room DB and enqueue a Firestore sync for each
            // quiz so the deletion is retried automatically when connectivity
            // returns (instead of a fire-and-forget that silently drops offline).
            deletedQuizzes.forEach { entity ->
                LocalQuizPurger.purgeLocalQuiz(entity.id, quizDao, questionDao, choiceDao)
                syncManager.enqueueSync(
                    SyncEntityType.QUIZ,
                    entity.id,
                    SyncOperation.DELETE
                )
            }
        }
    }

    // ==================== Paginated query implementations ====================

    override fun getPublicQuizzesLimited(limit: Int): Flow<List<Quiz>> {
        return quizDao.getPublicQuizzesLimited(limit).map { entities ->
            entities.map { it.toDomain() }
        }.onStart { ioScope.launch { refreshPublicQuizzes() } }
    }

    override fun getMyQuizzesLimited(userId: String, limit: Int): Flow<List<Quiz>> {
        return quizDao.getQuizzesByOwnerLimited(userId, limit).map { entities ->
            entities.map { it.toDomain() }
        }.onStart { ioScope.launch { refreshMyQuizzes(userId) } }
    }

    override fun searchQuizzesLimited(query: String, limit: Int): Flow<List<Quiz>> {
        val ftsQuery = buildFtsQuery(query)
            ?: return quizDao.searchQuizzesLimited(query, limit).map { entities ->
                entities.map { it.toDomain() }
            }
        return try {
            quizDao.searchQuizzesFts(ftsQuery, limit).map { entities ->
                entities.map { it.toDomain() }
            }
        } catch (_: Exception) {
            quizDao.searchQuizzesLimited(query, limit).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getDeletedQuizzesLimited(userId: String, limit: Int): Flow<List<Quiz>> {
        return quizDao.getDeletedQuizzesLimited(userId, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPublicQuizzesCount(): Int {
        return quizDao.getPublicQuizzesCount()
    }

    override suspend fun getSearchResultsCount(query: String): Int {
        val ftsQuery = buildFtsQuery(query) ?: return quizDao.searchQuizzesCount(query)
        return try {
            quizDao.searchQuizzesFtsCount(ftsQuery)
        } catch (_: Exception) {
            quizDao.searchQuizzesCount(query)
        }
    }

    override suspend fun refreshQuizFromRemote(quizId: String): Result<Quiz> {
        return safeCall {
            val quizDto = remoteDataSource.getQuizById(quizId)

            if (quizDto == null || quizDto.deletedAt != null) {
                // Quiz has been deleted or soft-deleted on remote -- purge the
                // local copy so the user does not interact with stale content.
                LocalQuizPurger.purgeLocalQuiz(quizId, quizDao, questionDao, choiceDao)
                throw Exception("Quiz no longer available on remote")
            }

            val quiz = quizDto.toDomain()
            upsertQuizFromSync(quiz)
            refreshQuestionsAndChoices(quizId)

            quiz
        }
    }

    // ==================== Background refresh helpers ====================

    private fun refreshFromFirestore(userId: String) {
        ioScope.launch {
            if (!syncManager.isSyncAllowed()) return@launch
            refreshMyQuizzes(userId)
            refreshPublicQuizzes(currentUserId = userId)
            // Trigger embedding for newly-synced quizzes with null embeddings.
            embeddingIndex.requestFullReindex()
        }
    }

    private suspend fun refreshMyQuizzes(userId: String) {
        if (!syncManager.isSyncAllowed()) return
        try {
            // Use one-shot get() instead of opening a real-time snapshot listener
            val dtos = remoteDataSource.getQuizzesByOwnerOnce(userId)
            val remoteQuizIds = dtos.map { it.id }.toSet()

            // Insert quiz metadata in parallel across all quizzes
            coroutineScope {
                dtos.map { dto ->
                    async {
                        val quiz = dto.toDomain()
                        upsertQuizFromSync(quiz)
                        refreshQuestionsAndChoices(quiz.id)
                    }
                }.awaitAll()
            }

            // Mark owner's quizzes absent from Firestore instead of deleting them
            val localMyQuizzes = quizDao.getQuizzesByOwnerOnce(userId)
            val staleQuizzes = localMyQuizzes.filter { local ->
                local.id !in remoteQuizIds && local.syncStatus != SyncStatus.PENDING.name
            }
            staleQuizzes.forEach { staleQuiz ->
                if (!staleQuiz.isRemovedFromCloud) {
                    quizDao.markRemovedFromCloud(staleQuiz.id, true)
                }
            }
            embeddingIndex.requestFullReindex()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh user quizzes from Firestore", e)
        }
    }

    override suspend fun forceRefreshUserQuizzes(userId: String) {
        try {
            val dtos = remoteDataSource.getQuizzesByOwnerOnce(userId)
            val remoteQuizIds = dtos.map { it.id }.toSet()

            coroutineScope {
                dtos.map { dto ->
                    async {
                        val quiz = dto.toDomain()
                        upsertQuizFromSync(quiz)
                        refreshQuestionsAndChoices(quiz.id, skipSyncCheck = true)
                    }
                }.awaitAll()
            }

            val localMyQuizzes = quizDao.getQuizzesByOwnerOnce(userId)
            val staleQuizzes = localMyQuizzes.filter { local ->
                local.id !in remoteQuizIds && local.syncStatus != SyncStatus.PENDING.name
            }
            staleQuizzes.forEach { staleQuiz ->
                if (!staleQuiz.isRemovedFromCloud) {
                    quizDao.markRemovedFromCloud(staleQuiz.id, true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to force-refresh user quizzes", e)
        }
    }

    override suspend fun refreshPublicQuizzes(currentUserId: String?) {
        if (!syncManager.isSyncAllowed()) return
        try {
            // Use one-shot get() instead of opening a real-time snapshot listener
            val dtos = remoteDataSource.getPublicQuizzesOnce()
            val remoteQuizIds = dtos.map { it.id }.toSet()

            // Insert quiz metadata in parallel across all quizzes
            coroutineScope {
                dtos.map { dto ->
                    async {
                        val quiz = dto.toDomain()
                        upsertQuizFromSync(quiz)
                        refreshQuestionsAndChoices(quiz.id)
                    }
                }.awaitAll()
            }

            // Clean up stale local public quizzes that no longer exist on remote.
            if (currentUserId != null) {
                val localPublicQuizzes = quizDao.getPublicQuizzesOnce()
                val staleQuizzes = localPublicQuizzes.filter { local ->
                    local.id !in remoteQuizIds && local.ownerId != currentUserId
                }
                staleQuizzes.forEach { staleQuiz ->
                    LocalQuizPurger.purgeLocalQuiz(staleQuiz.id, quizDao, questionDao, choiceDao)
                }
            }
            embeddingIndex.requestFullReindex()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh public quizzes from Firestore", e)
        }
    }


    /**
     * Fetches questions and their choices from Firestore subcollections
     * and inserts them into the local Room database.
     * Deletes existing local questions/choices for the quiz first to remove stale data.
     */
    private suspend fun refreshQuestionsAndChoices(
        quizId: String,
        skipSyncCheck: Boolean = false
    ) {
        if (!skipSyncCheck && !syncManager.isSyncAllowed()) return
        try {
            // Delete existing local questions (and their choices via cascade) to remove stale data
            val existingQuestions = questionDao.getQuestionsByQuizIdOnce(quizId)
            existingQuestions.forEach { questionEntity ->
                questionDao.deleteQuestion(questionEntity)
            }

            // Fetch all questions first (single RPC), then fetch choices for all questions in parallel
            val questionDtos = questionRemoteDataSource.getQuestionsForQuiz(quizId)
            coroutineScope {
                questionDtos.map { questionDto ->
                    async {
                        val choiceDtos = questionRemoteDataSource.getChoicesForQuestion(quizId, questionDto.id)
                        val question = questionDto.toDomain().copy(quizId = quizId)
                        questionDao.insertQuestion(question.toEntity())
                        choiceDtos.forEach { choiceDto ->
                            val choice = choiceDto.toDomain()
                            choiceDao.insertChoice(choice.toEntity(question.id))
                        }
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh questions/choices for quiz $quizId", e)
        }
    }

    // ==================== Semantic search ====================

    override fun semanticSearchQuizzes(query: String, limit: Int): Flow<List<Quiz>> = flow {
        val queryEmbedding = embeddingService.embed(query)
        if (queryEmbedding == null) {
            // Model not ready -- fall back to FTS
            emitAll(searchQuizzesLimited(query, limit))
            return@flow
        }
        val corpus = embeddingIndex.snapshot()
        if (corpus.isEmpty()) {
            // Index not populated yet -- fall back to FTS until embeddings are computed
            emitAll(searchQuizzesLimited(query, limit))
            return@flow
        }
        val ranked = VectorSimilarity.rankBySimilarity(queryEmbedding, corpus)
            .take(limit)
            .map { it.first }
        if (ranked.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val quizMap = quizDao.getQuizzesByIds(ranked).associate { it.id to it.toDomain() }
        emit(ranked.mapNotNull { quizMap[it] })
    }

    override fun hybridSearchQuizzes(query: String, limit: Int): Flow<List<Quiz>> = flow {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        // FTS path
        val ftsQuery = buildFtsQuery(trimmed)
        val ftsEntities = if (ftsQuery != null) {
            try {
                quizDao.searchQuizzesFts(ftsQuery, limit * 2).first()
            } catch (_: Exception) {
                quizDao.searchQuizzesLimited(trimmed, limit * 2).first()
            }
        } else {
            quizDao.searchQuizzesLimited(trimmed, limit * 2).first()
        }
        val ftsIds = ftsEntities.map { it.id }

        // Semantic path
        val queryEmbedding = embeddingService.embed(trimmed)
        val semanticIds = if (queryEmbedding != null) {
            val corpus = embeddingIndex.snapshot()
            VectorSimilarity.rankBySimilarity(queryEmbedding, corpus, threshold = 0.15f)
                .take(limit * 3)
                .map { it.first }
        } else {
            emptyList()
        }

        // Merge via Reciprocal Rank Fusion
        val mergedIds = VectorSimilarity.reciprocalRankFusion(ftsIds, semanticIds, semanticWeight = 2.0)
            .take(limit)

        if (mergedIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        // Load full Quiz objects, preserving RRF order
        val quizMap = ftsEntities.associate { it.id to it.toDomain() }.toMutableMap()
        val missingIds = mergedIds.filter { it !in quizMap }
        if (missingIds.isNotEmpty()) {
            quizDao.getQuizzesByIds(missingIds).forEach { quizMap[it.id] = it.toDomain() }
        }
        emit(mergedIds.mapNotNull { quizMap[it] })
    }

    override fun findSimilarQuizzes(quizId: String, limit: Int): Flow<List<Quiz>> = flow {
        val sourceEmbedding = embeddingIndex[quizId]
        if (sourceEmbedding == null) {
            emit(emptyList())
            return@flow
        }
        val corpus = embeddingIndex.snapshot().filterKeys { it != quizId }
        val similarIds = VectorSimilarity.rankBySimilarity(sourceEmbedding, corpus, threshold = 0.4f)
            .take(limit)
            .map { it.first }
        if (similarIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val quizzes = quizDao.getQuizzesByIds(similarIds)
        val quizMap = quizzes.associate { it.id to it.toDomain() }
        emit(similarIds.mapNotNull { quizMap[it] })
    }
}
