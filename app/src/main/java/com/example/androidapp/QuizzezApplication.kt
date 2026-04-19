package com.example.androidapp

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.androidapp.data.worker.BackendMaintenanceWorker
import com.example.androidapp.data.worker.BackgroundSyncWorker
import com.example.androidapp.data.worker.EmbeddingIndexWorker
import com.example.androidapp.di.AppContainer
import com.example.androidapp.di.AppContainerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Main Application class for Quizzez.
 * Manages the application-wide dependency injection container
 * and schedules periodic background maintenance work.
 */
class QuizzezApplication : Application() {

    /**
     * Application-wide dependency injection container.
     * Initialized once when the app starts.
     */
    lateinit var appContainer: AppContainer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Initialize manual dependency injection container
        appContainer = AppContainerImpl(this)
        appContainer.logCollector.install()

        scheduleBackendMaintenance()
        scheduleBackgroundSync()
        scheduleEmbeddingIndex()
        setupGlobalErrorHandler()
    }

    /**
     * Schedules the [BackendMaintenanceWorker].
     *
     * Triggers an immediate one-time run at startup and registers a periodic
     * WorkManager request (15-minute minimum interval).
     */
    private fun scheduleBackendMaintenance() {
        applicationScope.launch {
            val networkType = resolvedNetworkType()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val workManager = WorkManager.getInstance(this@QuizzezApplication)

            // Immediate one-time run for testing
            val immediateRequest = OneTimeWorkRequestBuilder<BackendMaintenanceWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueue(immediateRequest)

            // Periodic schedule (15 min minimum for WorkManager)
            val periodicRequest = PeriodicWorkRequestBuilder<BackendMaintenanceWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                BackendMaintenanceWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        }
    }

    /**
     * Schedules the [BackgroundSyncWorker] to run every 15 minutes with a
     * network constraint that respects the user's Wi-Fi-only preference.
     * Only enqueues when auto-sync is enabled; cancels the worker otherwise.
     */
    private fun scheduleBackgroundSync() {
        applicationScope.launch {
            val autoSync = try {
                appContainer.settingsPreferences.autoSyncEnabled.first()
            } catch (_: Exception) {
                true
            }

            val workManager = WorkManager.getInstance(this@QuizzezApplication)

            if (!autoSync) {
                workManager.cancelUniqueWork(BackgroundSyncWorker.WORK_NAME)
                return@launch
            }

            val networkType = resolvedNetworkType()

            val syncConstraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(syncConstraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                BackgroundSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )
        }
    }

    /**
     * Enqueues the [EmbeddingIndexWorker] to generate text embeddings
     * for any quizzes that are missing up-to-date vectors.
     * Runs once at startup; incremental updates are triggered by
     * quiz create/update operations in [QuizRepository].
     */
    private fun scheduleEmbeddingIndex() {
        EmbeddingIndexWorker.enqueueFullIndex(
            WorkManager.getInstance(this)
        )
    }

    /**
     * Installs a global uncaught exception handler that logs crashes
     * before delegating to the default handler. If no default handler is
     * installed, the process is terminated to preserve crash semantics.
     */
    private fun setupGlobalErrorHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    /**
     * Resolves the required [NetworkType] based on user preferences.
     * Returns [NetworkType.UNMETERED] if wifi-only sync is enabled, otherwise [NetworkType.CONNECTED].
     */
    private suspend fun resolvedNetworkType(): NetworkType {
        val wifiOnly = try {
            appContainer.settingsPreferences.wifiOnlySync.first()
        } catch (_: Exception) {
            false
        }
        return if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
    }

    companion object {
        private const val TAG = "QuizzezApp"
    }
}
