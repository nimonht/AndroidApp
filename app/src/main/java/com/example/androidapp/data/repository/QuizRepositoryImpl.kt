package com.example.androidapp.data.repository

import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
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
    private val remoteDataSource: QuizRemoteDataSource
) : QuizRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun getHomeQuizzes(userId: String): Flow<HomeQuizzes> {
        val myQuizzesFlow = quizDao.getQuizzesByOwner(userId).map { entities ->
            entities.map { it.toDomain() }
        }
        val publicQuizzesFlow = quizDao.getPublicQuizzes().map { entities ->
            entities.map { it.toDomain() }
        }
        // Refresh from Firestore in background when flow starts
        return combine(myQuizzesFlow, publicQuizzesFlow) { mine, public ->
            HomeQuizzes(
                recentAttemptQuizzes = emptyList(),
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

            // Write to Room first with PENDING status
            quizDao.insertQuiz(finalQuiz.toEntity(syncStatus = "PENDING"))
            questions.forEachIndexed { idx, question ->
                val qId = question.id.ifBlank { UUID.randomUUID().toString() }
                val finalQuestion = question.copy(id = qId, quizId = quizId, position = idx)
                questionDao.insertQuestion(finalQuestion.toEntity())
                finalQuestion.choices.forEachIndexed { cIdx, choice ->
                    val cId = choice.id.ifBlank { UUID.randomUUID().toString() }
                    choiceDao.insertChoice(choice.copy(id = cId).toEntity(qId))
                }
            }

            // Sync to Firestore in background
            ioScope.launch {
                try {
                    val questionDtos = questions.mapIndexed { idx, q ->
                        val qId = q.id.ifBlank { quizId + "_q$idx" }
                        q.copy(id = qId, quizId = quizId, position = idx).toDto()
                    }
                    remoteDataSource.saveQuiz(quizId, finalQuiz.toDto(), questionDtos)
                    quizDao.updateSyncStatus(quizId, "SYNCED")
                } catch (_: Exception) {
                    quizDao.updateSyncStatus(quizId, "FAILED")
                }
            }

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
            ioScope.launch {
                try {
                    remoteDataSource.softDeleteQuiz(quizId, Timestamp(Date(deletedAt)))
                } catch (_: Exception) { }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreQuiz(quizId: String): Result<Unit> {
        return try {
            quizDao.restoreQuiz(quizId)
            ioScope.launch {
                try {
                    remoteDataSource.restoreQuiz(quizId)
                } catch (_: Exception) { }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun permanentlyDeleteQuiz(quizId: String): Result<Unit> {
        return try {
            val entity = quizDao.getQuizById(quizId)
            if (entity != null) quizDao.deleteQuiz(entity)
            ioScope.launch {
                try {
                    remoteDataSource.permanentlyDeleteQuiz(quizId)
                } catch (_: Exception) { }
            }
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
                    remoteDataSource.incrementAttemptCount(quizId)
                } catch (_: Exception) { }
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

    // ==================== Background refresh helpers ====================

    private fun refreshFromFirestore(userId: String) {
        ioScope.launch {
            refreshMyQuizzes(userId)
            refreshPublicQuizzes()
        }
    }

    private suspend fun refreshMyQuizzes(userId: String) {
        try {
            val dtos = remoteDataSource.getQuizzesByOwner(userId).first()
            dtos.forEach { dto ->
                quizDao.insertQuiz(dto.toDomain().toEntity())
            }
        } catch (_: Exception) { }
    }

    private suspend fun refreshPublicQuizzes() {
        try {
            val dtos = remoteDataSource.getPublicQuizzes().first()
            dtos.forEach { dto ->
                quizDao.insertQuiz(dto.toDomain().toEntity())
            }
        } catch (_: Exception) { }
    }
}



