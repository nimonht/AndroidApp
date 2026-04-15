package com.example.androidapp.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.androidapp.QuizzezApplication
import kotlinx.coroutines.flow.first

/**
 * Periodic background worker that performs a full bi-directional sync
 * followed by an incremental tombstone-based invalidation check.
 *
 * Scheduled via WorkManager with a 15-minute repeat interval and a
 * network connectivity constraint. The worker:
 *
 * 1. Guards on sync-allowed settings and authenticated user.
 * 2. Delegates to [SyncManager.performFullSync] for upload + download.
 * 3. Runs [QuizInvalidationManager.checkForDeletedQuizzes] to purge
 *    locally-cached quizzes that were permanently deleted on Firestore
 *    by other users or maintenance tasks. This is a lightweight query
 *    against the `quizDeletions` tombstone collection -- far cheaper
 *    than re-fetching all quizzes.
 *
 * Returns [Result.retry] on failure so WorkManager applies exponential
 * backoff automatically.
 */
class BackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as QuizzezApplication).appContainer
        val syncManager = container.syncManager
        val authRepository = container.authRepository
        val quizInvalidationManager = container.quizInvalidationManager

        return try {
            if (!syncManager.isSyncAllowed()) {
                Log.d(TAG, "Sync not allowed at this time, skipping.")
                Result.success()
            } else {
                val user = authRepository.currentUser.first()
                if (user == null) {
                    Log.d(TAG, "No authenticated user, skipping sync.")
                    Result.success()
                } else {
                    Log.d(TAG, "Starting full sync for authenticated user...")
                    syncManager.performFullSync(user.id)

                    // Run lightweight tombstone-based invalidation to purge
                    // locally-cached quizzes that were permanently deleted on
                    // Firestore since the last check. This is an incremental
                    // query (typically returning 0-5 documents) and is much
                    // cheaper than the full stale-cleanup pass.
                    val purgedCount = quizInvalidationManager.checkForDeletedQuizzes()
                    if (purgedCount > 0) {
                        Log.d(TAG, "Invalidation sweep purged $purgedCount stale local quizzes.")
                    }

                    Log.d(TAG, "Full sync completed successfully.")

                    // After sync deposits quizzes with null embeddings into Room,
                    // trigger the embedding worker to recompute them.
                    EmbeddingIndexWorker.enqueueFullIndex(
                        WorkManager.getInstance(applicationContext)
                    )

                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed, will retry.", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BackgroundSync"
        const val WORK_NAME = "background_sync"
    }
}
