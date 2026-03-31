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
import com.example.androidapp.domain.repository.HomeQuizzes
import com.example.androidapp.domain.repository.QuizRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.Date
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
    private val syncManager: SyncManager
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
        return try {
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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateQuiz(quiz: Quiz, questions: List<Question>): Result<Unit> {
        return saveQuiz(quiz, questions)
    }

    override suspend fun deleteQuiz(quizId: String): Result<Unit> {
        return try {
            val deletedAt = System.currentTimeMillis()
            quizDao.softDeleteQuiz(quizId, deletedAt)
            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.QUIZ,
                quizId,
                SyncOperation.UPDATE  // Soft delete is an update operation
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreQuiz(quizId: String): Result<Unit> {
        return try {
            quizDao.restoreQuiz(quizId)
            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.QUIZ,
                quizId,
                SyncOperation.UPDATE  // Restore is an update operation
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun permanentlyDeleteQuiz(quizId: String): Result<Unit> {
        return try {
            val entity = quizDao.getQuizById(quizId)
            if (entity != null) quizDao.deleteQuiz(entity)
            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.QUIZ,
                quizId,
                SyncOperation.DELETE
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun incrementAttemptCount(quizId: String): Result<Unit> {
        return try {
            quizDao.incrementAttemptCount(quizId)
            ioScope.launch {
                try {
                    // Direct call for increment - this is a simple atomic operation
                    remoteDataSource.incrementAttemptCount(quizId)
                } catch (_: Exception) {
                    // Failure is ignored; this bypasses SyncManager so there is no queued retry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
        return try {
            // Get all deleted quizzes for the user locally
            val deletedQuizzes = quizDao.getDeletedQuizzes(userId).first()

            // Delete them from local Room DB
            deletedQuizzes.forEach { entity ->
                quizDao.deleteQuiz(entity)
            }

            // Sync deletion to Firestore in background
            ioScope.launch {
                try {
                    remoteDataSource.emptyTrash(userId)
                } catch (_: Exception) {
                    // Failures here are swallowed as this is background sync
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshQuizFromRemote(quizId: String): Result<Quiz> {
        return try {
            val quizDto = remoteDataSource.getQuizById(quizId)
                ?: return Result.failure(Exception("Quiz not found on remote"))

            val quiz = quizDto.toDomain()
            quizDao.insertQuiz(quiz.toEntity())
            refreshQuestionsAndChoices(quizId)

            Result.success(quiz)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Background refresh helpers ====================

    private fun refreshFromFirestore(userId: String) {
        ioScope.launch {
            if (!syncManager.isSyncAllowed()) return@launch
            refreshMyQuizzes(userId)
            refreshPublicQuizzes()
        }
    }

    private suspend fun refreshMyQuizzes(userId: String) {
        if (!syncManager.isSyncAllowed()) return
        try {
            val dtos = remoteDataSource.getQuizzesByOwner(userId).first()
            dtos.forEach { dto ->
                val quiz = dto.toDomain()
                quizDao.insertQuiz(quiz.toEntity())
                refreshQuestionsAndChoices(quiz.id)
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun refreshPublicQuizzes() {
        if (!syncManager.isSyncAllowed()) return
        try {
            val dtos = remoteDataSource.getPublicQuizzes().first()
            dtos.forEach { dto ->
                val quiz = dto.toDomain()
                quizDao.insertQuiz(quiz.toEntity())
                refreshQuestionsAndChoices(quiz.id)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Fetches questions and their choices from Firestore subcollections
     * and inserts them into the local Room database.
     */
    private suspend fun refreshQuestionsAndChoices(quizId: String) {
        if (!syncManager.isSyncAllowed()) return
        try {
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
