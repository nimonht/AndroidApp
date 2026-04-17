package com.example.androidapp.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.androidapp.QuizzezApplication

/**
 * Top-level composable property that retrieves the [AppContainer] from the
 * current [android.app.Application] instance.
 *
 * Usage: `val container = LocalAppContainer`
 *
 * Note: Despite the `Local` naming prefix, this is a plain computed property,
 * not a [androidx.compose.runtime.CompositionLocal]. It resolves the container
 * via [androidx.compose.ui.platform.LocalContext].
 */
val LocalAppContainer: AppContainer
    @Composable
    get() = (LocalContext.current.applicationContext as QuizzezApplication).appContainer
