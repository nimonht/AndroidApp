package com.example.androidapp

import android.app.Application
import com.example.androidapp.di.AppContainer
import com.example.androidapp.di.AppContainerImpl

/**
 * Main Application class for QuizCode.
 * Manages the application-wide dependency injection container.
 */
class QuizCodeApplication : Application() {

    /**
     * Application-wide dependency injection container.
     * Initialized once when the app starts.
     */
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        // Initialize manual dependency injection container
        appContainer = AppContainerImpl(this)
    }
}
