package com.example.androidapp.data.sync

import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.entity.PendingSyncEntity
import com.example.androidapp.data.local.entity.PendingSyncStatus
import com.example.androidapp.data.local.entity.SyncEntityType
import com.example.androidapp.data.local.entity.SyncOperation
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.data.remote.firebase.AttemptRemoteDataSource
import com.example.androidapp.data.remote.firebase.QuestionRemoteDataSource
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.data.remote.toDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SyncState {
    IDLE,
    SYNCING,
    PENDING,
    ERROR
}

class SyncManager(
    private val pendingSyncDao: PendingSyncDao,
    private val quizDao: QuizDao,
    private val questionDao: QuestionDao,
    private val choiceDao: ChoiceDao,
    private val attemptDao: AttemptDao,
    private val quizRemoteDataSource: QuizRemoteDataSource,
    private val questionRemoteDataSource: QuestionRemoteDataSource,
    private val attemptRemoteDataSource: AttemptRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    processPendingOperations()
                }
            }
        }
    }

    suspend fun processPendingOperations() {
        val pending = pendingSyncDao.getPendingOperations()
        if (pending.isEmpty()) {
            _syncState.value = SyncState.IDLE
            return
        }

        _syncState.value = SyncState.SYNCING

        var hasErrors = false
        for (operation in pending) {
            try {
                pendingSyncDao.updateStatus(operation.id, PendingSyncStatus.IN_PROGRESS.name)
                executeOperation(operation)
                pendingSyncDao.updateStatus(operation.id, PendingSyncStatus.COMPLETED.name)
            } catch (e: Exception) {
                hasErrors = true
                pendingSyncDao.incrementRetryCount(
                    operation.id,
                    e.message ?: "Unknown error"
                )
            }
        }

        // Remove successfully completed operations so the table does not grow unbounded.
        pendingSyncDao.deleteCompletedOperations()

        _syncState.value = if (hasErrors) SyncState.ERROR else SyncState.IDLE
    }

    suspend fun enqueueSync(
        entityType: SyncEntityType,
        entityId: String,
        operation: SyncOperation,
        payload: String = ""
    ) {
        pendingSyncDao.insertOperation(
            PendingSyncEntity(
                entityType = entityType.name,
                entityId = entityId,
                operation = operation.name,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
        _syncState.value = SyncState.PENDING

        if (networkMonitor.isOnline.value) {
            scope.launch { processPendingOperations() }
        }
    }

    suspend fun getPendingCount(): Int {
        return pendingSyncDao.getPendingCount()
    }

    suspend fun retryFailedOperations() {
        pendingSyncDao.resetFailedToPending()
        if (networkMonitor.isOnline.value) {
            scope.launch { processPendingOperations() }
        }
    }

    private suspend fun executeOperation(operation: PendingSyncEntity) {
        when (operation.entityType) {
            SyncEntityType.QUIZ.name -> executeQuizSync(operation)
            SyncEntityType.QUESTION.name -> executeQuestionSync(operation)
            SyncEntityType.CHOICE.name -> executeChoiceSync(operation)
            SyncEntityType.ATTEMPT.name -> executeAttemptSync(operation)
            else -> throw IllegalArgumentException(
                "Unsupported entityType '${operation.entityType}' for operation ${operation.id}"
            )
        }
    }

    private suspend fun executeQuizSync(operation: PendingSyncEntity) {
        when (operation.operation) {
            SyncOperation.CREATE.name, SyncOperation.UPDATE.name -> {
                val quizEntity = quizDao.getQuizById(operation.entityId)
                    ?: throw IllegalStateException(
                        "Quiz '${operation.entityId}' not found locally; cannot sync."
                    )
                val quiz = quizEntity.toDomain()

                // Load questions (with choices) from Room to sync the full quiz graph.
                val questionDtos = questionDao.getQuestionsByQuizIdOnce(operation.entityId)
                    .map { qEntity ->
                        val choices = choiceDao.getChoicesByQuestionIdOnce(qEntity.id)
                        qEntity.toDomain(choices.map { it.toDomain() }).toDto()
                    }

                quizRemoteDataSource.saveQuiz(
                    operation.entityId,
                    quiz.toDto(),
                    questionDtos
                )
                quizDao.updateSyncStatus(operation.entityId, "SYNCED")
            }
            SyncOperation.DELETE.name -> {
                quizRemoteDataSource.permanentlyDeleteQuiz(operation.entityId)
            }
        }
    }

    private suspend fun executeQuestionSync(operation: PendingSyncEntity) {
        when (operation.operation) {
            SyncOperation.CREATE.name, SyncOperation.UPDATE.name -> {
                val questionEntity = questionDao.getQuestionById(operation.entityId)
                    ?: throw IllegalStateException(
                        "Question '${operation.entityId}' not found locally; cannot sync."
                    )

                // Load choices for this question before converting to domain
                val choices = choiceDao.getChoicesByQuestionIdOnce(operation.entityId)
                val question = questionEntity.toDomain(choices.map { it.toDomain() })

                questionRemoteDataSource.saveQuestion(
                    question.quizId,
                    question.toDto(),
                    question.choices.map { it.toDto() }
                )
            }
            SyncOperation.DELETE.name -> {
                // Extract quizId from payload, or fallback to fetching from DB
                val quizId = if (operation.payload.isNotBlank()) {
                    operation.payload
                } else {
                    questionDao.getQuestionById(operation.entityId)?.quizId
                }

                if (quizId != null) {
                    questionRemoteDataSource.deleteQuestion(quizId, operation.entityId)
                }
            }
        }
    }

    private suspend fun executeChoiceSync(operation: PendingSyncEntity) {
        // Choices are synced as part of their parent question
        // This operation type exists for completeness but is handled by question sync
        when (operation.operation) {
            SyncOperation.CREATE.name, SyncOperation.UPDATE.name -> {
                // Find parent question and sync entire question with all choices
                val choiceEntity = choiceDao.getChoiceById(operation.entityId)
                if (choiceEntity != null) {
                    val questionEntity = questionDao.getQuestionById(choiceEntity.questionId)
                    if (questionEntity != null) {
                        // Load all choices for the question before converting to domain
                        val choices = choiceDao.getChoicesByQuestionIdOnce(choiceEntity.questionId)
                        val question = questionEntity.toDomain(choices.map { it.toDomain() })

                        questionRemoteDataSource.saveQuestion(
                            question.quizId,
                            question.toDto(),
                            question.choices.map { it.toDto() }
                        )
                    }
                }
            }
            SyncOperation.DELETE.name -> {
                // Choice deletion is handled by parent question sync
                // Individual choice deletes require re-syncing the entire question
            }
        }
    }

    private suspend fun executeAttemptSync(operation: PendingSyncEntity) {
        when (operation.operation) {
            SyncOperation.CREATE.name, SyncOperation.UPDATE.name -> {
                val attemptEntity = attemptDao.getAttemptById(operation.entityId)
                    ?: throw IllegalStateException(
                        "Attempt '${operation.entityId}' not found locally; cannot sync."
                    )
                val attempt = attemptEntity.toDomain()

                attemptRemoteDataSource.saveAttempt(attempt.toDto())
            }
            SyncOperation.DELETE.name -> {
                // Attempts are not typically deleted, but if needed:
                // Firebase doesn't have a delete method in AttemptRemoteDataSource yet
                // This would need to be implemented if attempt deletion is required
            }
        }
    }

    /**
     * Download (pull) quizzes from Firebase and save to Room.
     * This implements the Firebase → Room sync direction.
     */
    suspend fun downloadQuizzes(userId: String) {
        try {
            _syncState.value = SyncState.SYNCING

            // Fetch user's quizzes from Firebase
            val quizDtos = quizRemoteDataSource.getQuizzesByOwner(userId).first()

            quizDtos.forEach { quizDto ->
                val quiz = quizDto.toDomain()

                // Check if local version exists and compare timestamps
                val localQuiz = quizDao.getQuizById(quiz.id)

                if (localQuiz == null || localQuiz.updatedAt < quiz.updatedAt) {
                    // Firebase version is newer or doesn't exist locally - download it
                    quizDao.insertQuiz(quiz.toEntity())

                    // Also download associated questions and choices
                    val questionDtos = questionRemoteDataSource.getQuestionsForQuiz(quiz.id)
                    questionDtos.forEach { questionDto ->
                        // Fetch choices from subcollection for this question
                        val choiceDtos = questionRemoteDataSource.getChoicesForQuestion(quiz.id, questionDto.id)

                        // Convert to domain with proper quizId and choices
                        val question = questionDto.toDomain().copy(quizId = quiz.id)
                        questionDao.insertQuestion(question.toEntity())

                        // Insert choices
                        choiceDtos.forEach { choiceDto ->
                            val choice = choiceDto.toDomain()
                            choiceDao.insertChoice(choice.toEntity(question.id))
                        }
                    }
                }
            }

            _syncState.value = SyncState.IDLE
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
        }
    }

    /**
     * Download (pull) public quizzes from Firebase and save to Room cache.
     */
    suspend fun downloadPublicQuizzes() {
        try {
            val quizDtos = quizRemoteDataSource.getPublicQuizzes().first()

            quizDtos.forEach { quizDto ->
                val quiz = quizDto.toDomain()
                quizDao.insertQuiz(quiz.toEntity())
            }
        } catch (_: Exception) {
            // Silently fail for public quiz refresh
        }
    }

    /**
     * Download (pull) attempts from Firebase and save to Room.
     */
    suspend fun downloadAttempts(userId: String) {
        try {
            val attemptDtos = attemptRemoteDataSource.getAttemptsByUser(userId)

            attemptDtos.forEach { attemptDto ->
                val attempt = attemptDto.toDomain()

                // Check if local version exists
                val localAttempt = attemptDao.getAttemptById(attempt.id)

                if (localAttempt == null || localAttempt.finishedAt == null) {
                    // Download new or incomplete attempts
                    attemptDao.insertAttempt(attempt.toEntity())
                }
            }
        } catch (_: Exception) {
            // Silently fail
        }
    }

    /**
     * Full bi-directional sync - upload pending changes then download updates.
     * Only downloads if uploads succeed or have no pending operations.
     */
    suspend fun performFullSync(userId: String) {
        // First, upload any pending local changes
        processPendingOperations()

        // Only download if sync state is not ERROR (i.e., uploads succeeded or no pending ops)
        if (_syncState.value != SyncState.ERROR) {
            // Then, download updates from Firebase
            downloadQuizzes(userId)
            downloadAttempts(userId)
            downloadPublicQuizzes()
        }
    }
}
