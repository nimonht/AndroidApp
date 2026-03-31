package com.example.androidapp

import android.app.Application
import com.example.androidapp.data.error.GlobalErrorHandler
import com.example.androidapp.di.AppContainer
import com.example.androidapp.di.AppContainerImpl

/**
 * Main Application class for Quizzez.
 * Manages the application-wide dependency injection container.
 */
class QuizzezApplication : Application() {

    /**
     * Application-wide dependency injection container.
     * Initialized once when the app starts.
     */
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        // Setup Global Error Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(GlobalErrorHandler(applicationContext, defaultHandler))

        // Initialize manual dependency injection container
        appContainer = AppContainerImpl(this)
    }
}
