package com.example.androidapp.ui.screens.advanced.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.androidapp.domain.console.CommandExecutor
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.service.NetworkService

/**
 * [ViewModelProvider.Factory] for creating [ConsoleViewModel] instances
 * with manually injected dependencies.
 *
 * Used by [ConsoleScreen] to resolve all required dependencies from
 * the application's DI container ([com.example.androidapp.di.AppContainer])
 * and pass them into the ViewModel constructor.
 *
 * @param commandExecutor The engine that lexes, parses, and executes console commands.
 * @param authRepository Repository for observing the current user.
 * @param networkMonitor Connectivity state provider (domain-layer interface).
 */
class ConsoleViewModelFactory(
    private val commandExecutor: CommandExecutor,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkService
) : ViewModelProvider.Factory {

    /**
     * Creates a new [ConsoleViewModel] instance.
     *
     * @param modelClass The class of the ViewModel to create.
     * @return A new [ConsoleViewModel] with all dependencies injected.
     * @throws IllegalArgumentException if [modelClass] is not [ConsoleViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ConsoleViewModel::class.java)) {
            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}. Expected ConsoleViewModel."
            )
        }
        return ConsoleViewModel(
            commandExecutor = commandExecutor,
            authRepository = authRepository,
            networkMonitor = networkMonitor
        ) as T
    }
}
