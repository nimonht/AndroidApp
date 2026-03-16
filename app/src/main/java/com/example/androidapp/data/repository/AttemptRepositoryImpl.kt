package com.example.androidapp.data.repository

import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.remote.firebase.AttemptRemoteDataSource
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.repository.AttemptRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.androidapp.data.remote.firebase.UserRemoteDataSource

/**
 * Local-first implementation of [AttemptRepository].
 * Saves attempts to Room first, then syncs to Firestore in the background.
 */
class AttemptRepositoryImpl(
    private val attemptDao: AttemptDao,
    private val remoteDataSource: AttemptRemoteDataSource
) : AttemptRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun getAttemptsByUser(userId: String): Flow<List<Attempt>> {
        return attemptDao.getAttemptsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAttemptsByQuiz(quizId: String): Flow<List<Attempt>> {
        return attemptDao.getAttemptsByQuiz(quizId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAttemptById(attemptId: String): Attempt? {
        return attemptDao.getAttemptById(attemptId)?.toDomain()
    }

    override suspend fun getLatestAttempt(userId: String, quizId: String): Attempt? {
        return attemptDao.getLatestAttempt(userId, quizId)?.toDomain()
    }

    override suspend fun saveAttempt(attempt: Attempt): Result<String> {
        return try {
            val attemptId = attempt.id.ifBlank { UUID.randomUUID().toString() }
            val finalAttempt = attempt.copy(id = attemptId)
            attemptDao.insertAttempt(finalAttempt.toEntity())

            // Sync to Firestore in background
            ioScope.launch {
                try {
                    remoteDataSource.saveAttempt(finalAttempt.toDto())
                } catch (_: Exception) { }
            }

            Result.success(attemptId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAttempt(attempt: Attempt): Result<Unit> {
        return try {
            attemptDao.updateAttempt(attempt.toEntity())
            ioScope.launch {
                try {
                    remoteDataSource.updateAttempt(attempt.toDto())
                } catch (_: Exception) { }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun updateUserProfile(
        displayName: String,
        avatarUrl: String?
    ) {

        userRemoteDataSource.updateUserProfile(
            displayName = displayName,
            avatarUrl = avatarUrl
        )
    }
    override fun getAttemptsByUser(userId: String): Flow<List<Attempt>> {
        return attemptDao.getAttemptsByUser(userId)
            .map { list -> list.map { it.toDomain() } }
    }
}

