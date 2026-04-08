package com.example.androidapp.domain.console

import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.service.LogService
import com.example.androidapp.domain.service.NetworkService
import com.example.androidapp.domain.service.SettingsService
import com.example.androidapp.domain.service.SyncService

/**
 * Runtime context injected into every command during execution.
 *
 * Provides access to the current user, all repository interfaces, and
 * infrastructure services needed by commands. This is the single point
 * of dependency injection for the command engine.
 *
 * Commands receive a fresh [CommandContext] for each execution. The
 * [pipeInput] field is populated only when the command is on the
 * receiving end of a pipe (`|`) operator.
 *
 * **Design note**: This class references repository *interfaces* from the
 * domain layer and domain-layer service interfaces. The actual implementations
 * are resolved by the DI container and injected via [CommandExecutor]'s
 * context provider lambda.
 *
 * @property currentUser The authenticated user executing the command.
 * @property repositories Bag of repository interfaces for data access.
 * @property services Infrastructure services (sync, network, settings, logs).
 * @property pipeInput Lines piped from the previous command's output, or null
 *   if this command is not receiving piped input.
 * @property aliases Currently registered command aliases (name -> expansion).
 * @property commandHistory List of previously executed raw command strings
 *   in the current session (oldest first). Capped at [MAX_HISTORY_SIZE]
 *   entries to prevent unbounded memory growth.
 */
data class CommandContext(
    val currentUser: User,
    val repositories: RepositoryBundle,
    val services: ServiceBundle,
    val pipeInput: List<String>? = null,
    val aliases: Map<String, String> = emptyMap(),
    val commandHistory: List<String> = emptyList()
) {
    companion object {
        /**
         * Maximum number of command history entries retained in the context.
         * Older entries are dropped when this limit is exceeded.
         */
        const val MAX_HISTORY_SIZE = 500
    }
}

/**
 * Bundle of all repository interfaces available to commands.
 *
 * Grouped into a separate class to keep [CommandContext] readable and to
 * make it easy to add new repositories without changing the context signature.
 *
 * All properties are typed as domain-layer interfaces — no data-layer
 * implementation types leak into the console engine.
 */
data class RepositoryBundle(
    val adminRepository: com.example.androidapp.domain.repository.AdminRepository,
    val authRepository: com.example.androidapp.domain.repository.AuthRepository,
    val quizRepository: com.example.androidapp.domain.repository.QuizRepository,
    val attemptRepository: com.example.androidapp.domain.repository.AttemptRepository,
    val shareCodeRepository: com.example.androidapp.domain.repository.ShareCodeRepository,
    val poolRepository: com.example.androidapp.domain.repository.PoolRepository,
    val searchRepository: com.example.androidapp.domain.repository.SearchRepository
)

/**
 * Bundle of infrastructure services available to commands.
 *
 * All properties are typed as domain-layer service interfaces, keeping the
 * [CommandContext] and all [Command] implementations free of data-layer or
 * Android-framework imports.
 *
 * @property syncService Sync management operations (trigger, retry, observe state).
 * @property networkService Network connectivity status (online, WiFi).
 * @property settingsService Application settings (theme, sync preferences).
 * @property logService Application log buffer access (read, clear).
 */
data class ServiceBundle(
    val syncService: SyncService,
    val networkService: NetworkService,
    val settingsService: SettingsService,
    val logService: LogService
)
