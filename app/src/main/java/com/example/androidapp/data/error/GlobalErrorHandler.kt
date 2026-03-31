package com.example.androidapp.data.error

import android.content.Context
import android.util.Log

/**
 * Global uncaught exception handler.
 * Wraps the default handler, logs the exception, persists a crash flag for
 * the next launch, then delegates to the default handler for actual termination.
 */
class GlobalErrorHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e("GlobalErrorHandler", "Uncaught exception in thread ${thread.name}", throwable)
        
        // Save crash flag to shared preferences
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("crashed_previously", true).commit() // commit to ensure synchronous write

        // Delegate to default handler to actually crash the app
        defaultHandler?.uncaughtException(thread, throwable)
    }
}
