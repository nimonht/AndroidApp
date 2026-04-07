package com.example.androidapp.data.repository

import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.entity.SyncEntityType
import com.example.androidapp.data.remote.firebase.QuestionRemoteDataSource
import com.example.androidapp.data.local.entity.SyncOperation
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.data.sync.SyncManager
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    private val shareCodeRepository: ShareCodeRepository
) : QuizRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun getHomeQuizzes(userId: String): Flow<HomeQuizzes> {
        val recentQuizzesFlow = quizDao.getRecentAttemptQuizzes(userId).map { entities ->
            entities.map { it.toDomain() }
        }
        val myQuizzesFlow = quizDao.getQuizzesByOwner(userId).map { entities ->
            entities.map { it.toDomain() }
        }
        val publicQuizzesFlow = quizDao.getPublicQuizzes().map { entities ->
            entities.map { it.toDomain() }
        }
        // Refresh from Firestore in background when flow starts
        return combine(recentQuizzesFlow, myQuizzesFlow, publicQuizzesFlow) { recent, mine, public ->
            HomeQuizzes(
                recentAttemptQuizzes = recent,
                myQuizzes = mine,
                trendingQuizzes = public.sortedByDescending { it.attemptCount }.take(10)
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
        }.onStart { refreshMyQuizzes(userId) }
    }

    override fun getPublicQuizzes(): Flow<List<Quiz>> {
        return quizDao.getPublicQuizzes().map { entities ->
            entities.map { it.toDomain() }
        }.onStart { refreshPublicQuizzes() }
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
                quizDao.insertQuiz(quiz.toEntity())
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
            quizDao.insertQuiz(finalQuiz.toEntity(syncStatus = "PENDING").copy(checksum = checksum))
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
            if (entity != null) quizDao.deleteQuiz(entity)
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
            ioScope.launch {
                try {
                    // Direct call for increment - this is a simple atomic operation
                    remoteDataSource.incrementAttemptCount(quizId)
                } catch (_: Exception) {
                    // Failure is ignored; this bypasses SyncManager so there is no queued retry
                }
            }
            Unit
        }
    }

    override fun getTrendingQuizzes(): Flow<List<Quiz>> {
        return quizDao.getPublicQuizzes().map { entities ->
            entities.take(20).map { it.toDomain() }
        }.onStart { refreshPublicQuizzes() }
    }

    override suspend fun getAllTags(): List<String> {
        return try {
            quizDao.getAllQuizzes().first().map { it.toDomain() }.flatMap { it.tags }.distinct()
        } catch (e: Exception) {
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
                quizDao.deleteQuiz(entity)
                syncManager.enqueueSync(
                    SyncEntityType.QUIZ,
                    entity.id,
                    SyncOperation.DELETE
                )
            }
        }
    }

    override suspend fun refreshQuizFromRemote(quizId: String): Result<Quiz> {
        return safeCall {
            val quizDto = remoteDataSource.getQuizById(quizId)

            if (quizDto == null || quizDto.deletedAt != null) {
                // Quiz has been deleted or soft-deleted on remote -- purge the
                // local copy so the user does not interact with stale content.
                purgeLocalQuiz(quizId)
                throw Exception("Quiz no longer available on remote")
            }

            val quiz = quizDto.toDomain()
            quizDao.insertQuiz(quiz.toEntity())
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
        }
    }

    private suspend fun refreshMyQuizzes(userId: String) {
        if (!syncManager.isSyncAllowed()) return
        try {
            val dtos = remoteDataSource.getQuizzesByOwner(userId).first()
            val remoteQuizIds = dtos.map { it.id }.toSet()

            dtos.forEach { dto ->
                val quiz = dto.toDomain()
                quizDao.insertQuiz(quiz.toEntity())
                refreshQuestionsAndChoices(quiz.id)
            }

            // Mark owner's quizzes absent from Firestore instead of deleting
            // them outright. They may have been removed by an admin; the user
            // should be warned and allowed to delete them manually.
            val localMyQuizzes = quizDao.getQuizzesByOwnerOnce(userId)
            val staleQuizzes = localMyQuizzes.filter { local ->
                local.id !in remoteQuizIds && local.syncStatus != "PENDING"
            }
            staleQuizzes.forEach { staleQuiz ->
                if (!staleQuiz.isRemovedFromCloud) {
                    quizDao.markRemovedFromCloud(staleQuiz.id, true)
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun refreshPublicQuizzes(currentUserId: String? = null) {
        if (!syncManager.isSyncAllowed()) return
        try {
            val dtos = remoteDataSource.getPublicQuizzes().first()
            val remoteQuizIds = dtos.map { it.id }.toSet()

            dtos.forEach { dto ->
                val quiz = dto.toDomain()
                quizDao.insertQuiz(quiz.toEntity())
                refreshQuestionsAndChoices(quiz.id)
            }

            // Clean up stale local public quizzes that no longer exist on remote.
            // Only remove quizzes not owned by the current user to preserve their own data.
            if (currentUserId != null) {
                val localPublicQuizzes = quizDao.getPublicQuizzesOnce()
                val staleQuizzes = localPublicQuizzes.filter { local ->
                    local.id !in remoteQuizIds && local.ownerId != currentUserId
                }
                staleQuizzes.forEach { staleQuiz ->
                    // Clean up associated questions and choices (no FK cascade)
                    val questions = questionDao.getQuestionsByQuizIdOnce(staleQuiz.id)
                    questions.forEach { question ->
                        choiceDao.deleteChoicesByQuestionId(question.id)
                        questionDao.deleteQuestion(question)
                    }
                    quizDao.deleteQuiz(staleQuiz)
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Removes a quiz and all its associated questions and choices from
     * the local Room database. Attempts are intentionally preserved
     * because they reference the quiz by ID but have no foreign key
     * constraint -- the user's history remains intact.
     *
     * Called when a remote check reveals the quiz has been permanently
     * deleted or soft-deleted by its owner.
     *
     * @param quizId the ID of the quiz to purge.
     */
    private suspend fun purgeLocalQuiz(quizId: String) {
        val questions = questionDao.getQuestionsByQuizIdOnce(quizId)
        for (question in questions) {
            choiceDao.deleteChoicesByQuestionId(question.id)
            questionDao.deleteQuestion(question)
        }
        quizDao.deleteQuizById(quizId)
    }

    /**
     * Fetches questions and their choices from Firestore subcollections
     * and inserts them into the local Room database.
     * Deletes existing local questions/choices for the quiz first to remove stale data.
     */
    private suspend fun refreshQuestionsAndChoices(quizId: String) {
        if (!syncManager.isSyncAllowed()) return
        try {
            // Delete existing local questions (and their choices via cascade) to remove stale data
            val existingQuestions = questionDao.getQuestionsByQuizIdOnce(quizId)
            existingQuestions.forEach { questionEntity ->
                questionDao.deleteQuestion(questionEntity)
            }

            // Fetch and insert fresh questions and choices from Firestore
            val questionDtos = questionRemoteDataSource.getQuestionsForQuiz(quizId)
            questionDtos.forEach { questionDto ->
                val choiceDtos = questionRemoteDataSource.getChoicesForQuestion(quizId, questionDto.id)
                val question = questionDto.toDomain().copy(quizId = quizId)
                questionDao.insertQuestion(question.toEntity())
                choiceDtos.forEach { choiceDto ->
                    val choice = choiceDto.toDomain()
                    choiceDao.insertChoice(choice.toEntity(question.id))
                }
            }
        } catch (_: Exception) {
            // Failure to refresh questions should not block quiz metadata refresh
        }
    }
}
