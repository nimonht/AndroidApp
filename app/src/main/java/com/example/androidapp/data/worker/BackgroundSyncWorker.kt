package com.example.androidapp.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.androidapp.QuizzezApplication
import kotlinx.coroutines.flow.first

/**
 * Periodic background worker that synchronises local Room data with Firestore.
 *
 * The worker is scheduled via WorkManager with a 15-minute repeat interval and
 * a network connectivity constraint. On each run it:
 * 1. Checks whether sync is currently allowed (network + user preferences).
 * 2. Retrieves the authenticated user; skips silently when no user is signed in.
 * 3. Delegates to [com.example.androidapp.data.sync.SyncManager.performFullSync]
 *    which uploads pending local changes and then downloads remote updates.
 *
 * Returns [Result.success] when the sync completes (or is skipped) and
 * [Result.retry] on transient failures so WorkManager can back off and retry.
 */
class BackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as QuizzezApplication).appContainer
        val syncManager = container.syncManager
        val authRepository = container.authRepository

        return try {
            if (!syncManager.isSyncAllowed()) {
                Log.d(TAG, "Sync not allowed at this time, skipping.")
                return Result.success()
            }

            val user = authRepository.currentUser.first()
            if (user == null) {
                Log.d(TAG, "No authenticated user, skipping sync.")
                return Result.success()
            }

            Log.d(TAG, "Starting full sync for user ${user.id}...")
            syncManager.performFullSync(user.id)
            Log.d(TAG, "Full sync completed successfully.")

            Result.success()
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
