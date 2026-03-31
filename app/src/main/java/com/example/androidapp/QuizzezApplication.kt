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
import com.example.androidapp.di.AppContainer
import com.example.androidapp.di.AppContainerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

        scheduleBackendMaintenance()
    }

    /**
     * Schedules the [BackendMaintenanceWorker].
     *
     * **Testing mode**: Triggers an immediate one-time run and then re-triggers
     * every [DEBUG_REPEAT_INTERVAL_MS] milliseconds using a coroutine loop.
     * WorkManager's minimum periodic interval is 15 minutes, which is too long
     * for interactive testing, so the coroutine loop supplements it.
     *
     * **Production mode**: Change [DEBUG_REPEAT_INTERVAL_MS] to 0 and rely
     * solely on the periodic WorkManager request (1 day interval).
     */
    private fun scheduleBackendMaintenance() {
        applicationScope.launch {
            val wifiOnly = try {
                appContainer.settingsPreferences.wifiOnlySync.first()
            } catch (_: Exception) {
                false
            }

            val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            // Immediate one-time run for testing
            val immediateRequest = OneTimeWorkRequestBuilder<BackendMaintenanceWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(this@QuizzezApplication)
                .enqueue(immediateRequest)

            // Periodic schedule (15 min minimum for WorkManager)
            val periodicRequest = PeriodicWorkRequestBuilder<BackendMaintenanceWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this@QuizzezApplication).enqueueUniquePeriodicWork(
                BackendMaintenanceWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )

            // Testing loop: re-trigger every 30 seconds for rapid iteration.
            // Set DEBUG_REPEAT_INTERVAL_MS to 0 to disable this loop in production.
            if (DEBUG_REPEAT_INTERVAL_MS > 0) {
                Log.d(TAG, "Debug maintenance loop active (${DEBUG_REPEAT_INTERVAL_MS}ms interval)")
                while (true) {
                    delay(DEBUG_REPEAT_INTERVAL_MS)
                    val oneShot = OneTimeWorkRequestBuilder<BackendMaintenanceWorker>()
                        .setConstraints(constraints)
                        .build()
                    WorkManager.getInstance(this@QuizzezApplication).enqueue(oneShot)
                    Log.d(TAG, "Debug: enqueued maintenance one-shot")
                }
            }
        }
    }

    companion object {
        private const val TAG = "QuizzezApp"

        /**
         * Interval in milliseconds between debug maintenance runs.
         * Set to 30_000 (30 s) for testing; set to 0 to disable the debug loop
         * and rely solely on the periodic WorkManager schedule.
         */
        private const val DEBUG_REPEAT_INTERVAL_MS = 30_000L
    }
}
