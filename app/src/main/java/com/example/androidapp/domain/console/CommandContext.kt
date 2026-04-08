package com.example.androidapp.domain.console

import com.example.androidapp.domain.model.User

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
 * domain layer and infrastructure wrappers. The actual implementations
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
 *   in the current session (oldest first).
 */
data class CommandContext(
    val currentUser: User,
    val repositories: RepositoryBundle,
    val services: ServiceBundle,
    val pipeInput: List<String>? = null,
    val aliases: Map<String, String> = emptyMap(),
    val commandHistory: List<String> = emptyList()
)

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
    val poolRepository: com.example.androidapp.domain.repository.PoolRepository
)

/**
 * Bundle of infrastructure services available to commands.
 *
 * These are data-layer or framework-level services that commands may need
 * for operations like triggering sync, checking connectivity, reading
 * settings, or querying the log buffer.
 *
 * The types here are concrete classes from the data layer. This is an
 * accepted trade-off: [CommandContext] itself lives in the domain layer
 * but acts as a bridge to infrastructure. The [Command] interface remains
 * pure — it only sees [CommandContext], not the individual service types.
 */
data class ServiceBundle(
    val syncManager: com.example.androidapp.data.sync.SyncManager,
    val networkMonitor: com.example.androidapp.data.network.NetworkMonitor,
    val settingsPreferences: com.example.androidapp.data.preferences.SettingsPreferences,
    val logCollector: com.example.androidapp.data.logging.LogCollector
)
