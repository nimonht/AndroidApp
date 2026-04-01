package com.example.androidapp.data.repository

import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.entity.SyncEntityType
import com.example.androidapp.data.local.entity.SyncOperation
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.remote.firebase.QuestionRemoteDataSource
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.data.sync.SyncManager
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.repository.QuestionRepository
import com.example.androidapp.domain.util.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Room-backed implementation of [QuestionRepository] with Firestore sync.
 *
 * Follows the local-first pattern: all writes are persisted to Room immediately,
 * then [SyncManager.enqueueSync] is called to schedule background Firestore sync.
 * Reads always return Room data; Firestore refresh happens externally.
 *
 * @property questionDao Room DAO for question CRUD operations.
 * @property choiceDao Room DAO for choice CRUD operations.
 * @property remoteDataSource Firestore data source for question operations.
 * @property syncManager Coordinator for background sync scheduling.
 */
class QuestionRepositoryImpl(
    private val questionDao: QuestionDao,
    private val choiceDao: ChoiceDao,
    private val remoteDataSource: QuestionRemoteDataSource,
    private val syncManager: SyncManager
) : QuestionRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)

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

    override suspend fun addQuestion(quizId: String, question: Question): Result<String> {
        return safeCall {
            val questionId = question.id.ifBlank { UUID.randomUUID().toString() }
            // Normalize choices once — assign stable IDs + positions here
            val normalizedChoices = question.choices.mapIndexed { idx, choice ->
                choice.copy(id = choice.id.ifBlank { UUID.randomUUID().toString() }, position = idx)
            }
            val finalQuestion = question.copy(id = questionId, quizId = quizId, choices = normalizedChoices)

            // Write to Room first
            questionDao.insertQuestion(finalQuestion.toEntity())
            normalizedChoices.forEach { choice ->
                choiceDao.insertChoice(choice.toEntity(questionId))
            }

            // Enqueue sync operation instead of direct async call
            ioScope.launch {
                try {
                    syncManager.enqueueSync(
                        SyncEntityType.QUESTION,
                        questionId,
                        SyncOperation.CREATE
                    )
                } catch (_: Exception) {
                    // Sync will retry automatically when online
                }
            }

            questionId
        }
    }

    override suspend fun updateQuestion(question: Question): Result<Unit> {
        return safeCall {
            // Normalize choices once — assign stable IDs + positions here
            val normalizedChoices = question.choices.mapIndexed { idx, choice ->
                choice.copy(id = choice.id.ifBlank { UUID.randomUUID().toString() }, position = idx)
            }
            val normalizedQuestion = question.copy(choices = normalizedChoices)

            // Write to Room first
            questionDao.insertQuestion(normalizedQuestion.toEntity())
            choiceDao.deleteChoicesByQuestionId(question.id)
            normalizedChoices.forEach { choice ->
                choiceDao.insertChoice(choice.toEntity(question.id))
            }

            // Enqueue sync operation
            ioScope.launch {
                try {
                    syncManager.enqueueSync(
                        SyncEntityType.QUESTION,
                        question.id,
                        SyncOperation.UPDATE
                    )
                } catch (_: Exception) {
                    // Sync will retry automatically when online
                }
            }
        }
    }

    override suspend fun deleteQuestion(quizId: String, questionId: String): Result<Unit> {
        return safeCall {
            // Fetch entity to verify it exists and belongs to this quiz
            val entity = questionDao.getQuestionById(questionId)
            if (entity != null && entity.quizId == quizId) {
                // Delete from local database first
                questionDao.deleteQuestion(entity)

                // Enqueue sync operation with quizId in payload
                ioScope.launch {
                    try {
                        syncManager.enqueueSync(
                            SyncEntityType.QUESTION,
                            questionId,
                            SyncOperation.DELETE,
                            payload = quizId  // Store quizId in payload for later sync
                        )
                    } catch (_: Exception) {
                        // Sync will retry automatically when online
                    }
                }
            }
        }
    }

    override suspend fun reorderQuestions(
        quizId: String,
        questionIds: List<String>
    ): Result<Unit> {
        return safeCall {
            questionIds.forEachIndexed { index, questionId ->
                questionDao.updatePosition(questionId, index)
            }

            ioScope.launch {
                try {
                    val positionMap = questionIds.mapIndexed { index, id -> id to index }.toMap()
                    remoteDataSource.updateQuestionPositions(quizId, positionMap)
                } catch (_: Exception) { }
            }
        }
    }
}
