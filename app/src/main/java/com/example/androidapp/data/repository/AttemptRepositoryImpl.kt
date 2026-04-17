package com.example.androidapp.data.repository

import android.util.Log
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.entity.SyncEntityType
import com.example.androidapp.data.local.entity.SyncOperation
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.sync.SyncManager
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.UUID

/**
 * Local-first implementation of [AttemptRepository].
 * Saves attempts to Room first, then syncs to Firestore in the background.
 */
class AttemptRepositoryImpl(
    private val attemptDao: AttemptDao,
    private val syncManager: SyncManager
) : AttemptRepository {

    private companion object {
        const val TAG = "AttemptRepositoryImpl"
    }

    override fun getAttemptsByUser(userId: String): Flow<List<Attempt>> {
        return attemptDao.getAttemptsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }.onStart {
            // Refresh attempts from Firebase in the background so that
            // history is available when logging in on a new device.
            try {
                syncManager.downloadAttempts(userId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download attempts from Firestore", e)
            }
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
        return safeCall {
            val attemptId = attempt.id.ifBlank { UUID.randomUUID().toString() }
            val finalAttempt = attempt.copy(id = attemptId)

            // Write to Room first
            attemptDao.insertAttempt(finalAttempt.toEntity())

            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.ATTEMPT,
                attemptId,
                SyncOperation.CREATE
            )

            attemptId
        }
    }

    override suspend fun updateAttempt(attempt: Attempt): Result<Unit> {
        return safeCall {
            // Write to Room first
            attemptDao.updateAttempt(attempt.toEntity())

            // Enqueue sync operation synchronously to ensure durability
            syncManager.enqueueSync(
                SyncEntityType.ATTEMPT,
                attempt.id,
                SyncOperation.UPDATE
            )

            Unit
        }
    }

    override suspend fun linkGuestAttempts(guestId: String, userId: String): Result<Int> {
        return safeCall {
            attemptDao.updateUserId(guestId, userId)
        }
    }

    // ==================== Paginated query implementations ====================

    override fun getAttemptsByUserLimited(userId: String, limit: Int): Flow<List<Attempt>> {
        return attemptDao.getAttemptsByUserLimited(userId, limit).map { entities ->
            entities.map { it.toDomain() }
        }.onStart {
            try {
                syncManager.downloadAttempts(userId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download attempts from Firestore", e)
            }
        }
    }

    override suspend fun getAttemptCountByUser(userId: String): Int {
        return attemptDao.getAttemptCountByUserOnce(userId)
    }
}
