package com.example.androidapp.ui.screens.advanced.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.androidapp.data.logging.LogCollector
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.data.preferences.SettingsPreferences
import com.example.androidapp.data.sync.SyncManager
import com.example.androidapp.domain.console.CommandExecutor
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.ShareCodeRepository

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
 * @param networkMonitor Connectivity state provider.
 * @param logCollector Application log buffer.
 * @param syncManager Sync infrastructure.
 * @param settingsPreferences App settings.
 * @param adminRepository Admin data access.
 * @param quizRepository Quiz data access.
 * @param attemptRepository Attempt data access.
 * @param shareCodeRepository Share code data access.
 * @param poolRepository Question pool data access.
 */
class ConsoleViewModelFactory(
    private val commandExecutor: CommandExecutor,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val logCollector: LogCollector,
    private val syncManager: SyncManager,
    private val settingsPreferences: SettingsPreferences,
    private val adminRepository: AdminRepository,
    private val quizRepository: QuizRepository,
    private val attemptRepository: AttemptRepository,
    private val shareCodeRepository: ShareCodeRepository,
    private val poolRepository: PoolRepository
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
            networkMonitor = networkMonitor,
            logCollector = logCollector,
            syncManager = syncManager,
            settingsPreferences = settingsPreferences,
            adminRepository = adminRepository,
            quizRepository = quizRepository,
            attemptRepository = attemptRepository,
            shareCodeRepository = shareCodeRepository,
            poolRepository = poolRepository
        ) as T
    }
}
