package com.example.androidapp.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.androidapp.QuizzezApplication

/**
 * Extension function to access the AppContainer from composables.
 * Usage: val container = LocalAppContainer.current
 */
val LocalAppContainer: AppContainer
    @Composable
    get() = (LocalContext.current.applicationContext as QuizzezApplication).appContainer
