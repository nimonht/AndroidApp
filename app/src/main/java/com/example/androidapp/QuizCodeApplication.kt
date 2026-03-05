package com.example.androidapp

import android.app.Application
import com.example.androidapp.di.AppContainer
import com.example.androidapp.di.DefaultAppContainer

class QuizCodeApplication : Application() {

    // Khai báo một biến chứa nhà kho
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        // Khi app vừa mở lên là xây nhà kho liền
        appContainer = DefaultAppContainer()
    }
}