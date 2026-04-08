package com.example.androidapp.domain.console

import com.example.androidapp.domain.model.UserRole

/**
 * Central registry for all console commands.
 *
 * Commands are registered at application startup (via the DI container) and
 * looked up by name or alias during command resolution. The registry also
 * supports role-filtered listing, prefix-based autocomplete for the
 * console input field, and aggregation of command-declared value flags
 * for the parser.
 *
 * Thread safety: registration is expected to happen once at startup on the
 * main thread. After that, [resolve], [allCommands], [commandsForRole],
 * [autocompleteCommand], [allValueFlags], and [allShortValueFlags] are
 * read-only and safe to call from any thread.
 */
class CommandRegistry {

    /**
     * Primary map: command name (lowercase) -> [Command] instance.
     */
    private val commands = mutableMapOf<String, Command>()

    /**
     * Alias map: alias (lowercase) -> primary command name (lowercase).
     * Kept in sync with [commands] during [register].
     */
    private val aliasMap = mutableMapOf<String, String>()

    /**
     * Cached aggregation of all [Command.valueFlags] from registered commands.
     * Invalidated on each [register] call.
     */
    private var cachedValueFlags: Set<String>? = null

    /**
     * Cached aggregation of all [Command.shortValueFlags] from registered commands.
     * Invalidated on each [register] call.
     */
    private var cachedShortValueFlags: Set<String>? = null

    /**
     * Cached list of all commands sorted by name.
     * Invalidated on each [register] call.
     */
    private var cachedAllCommands: List<Command>? = null

    /**
     * Registers a [Command] in the registry.
     *
     * The command's [Command.name] and all [Command.aliases] are indexed
     * (case-insensitively). If a name or alias collides with an existing
     * entry, a warning is printed to stderr and the new registration
     * overwrites the old one.
     *
     * @param command The command instance to register.
     */
    fun register(command: Command) {
        val key = command.name.lowercase()

        // Warn on primary name collision
        val existing = commands[key]
        if (existing != null && existing !== command) {
            System.err.println(
                "CommandRegistry: command name '$key' is being overwritten " +
                        "(old: ${existing::class.simpleName}, new: ${command::class.simpleName})"
            )
        }

        commands[key] = command

        for (alias in command.aliases) {
            val aliasKey = alias.lowercase()
            val existingAlias = aliasMap[aliasKey]
            if (existingAlias != null && existingAlias != key) {
                System.err.println(
                    "CommandRegistry: alias '$aliasKey' is being reassigned " +
                            "(old target: $existingAlias, new target: $key)"
                )
            }
            aliasMap[aliasKey] = key
        }

        // Invalidate caches
        cachedValueFlags = null
        cachedShortValueFlags = null
        cachedAllCommands = null
    }

    /**
     * Registers multiple commands at once.
     *
     * @param cmds The command instances to register.
     */
    fun registerAll(vararg cmds: Command) {
        for (cmd in cmds) {
            register(cmd)
        }
    }

    /**
     * Resolves a command by name or alias.
     *
     * @param name The command name or alias (case-insensitive).
     * @return The matching [Command], or `null` if not found.
     */
    fun resolve(name: String): Command? {
        val key = name.lowercase()
        return commands[key] ?: aliasMap[key]?.let { commands[it] }
    }

    /**
     * Returns all registered commands (one instance per primary name, no
     * duplicates from aliases), sorted alphabetically by name.
     *
     * Results are cached and invalidated when new commands are registered.
     *
     * @return An alphabetically sorted list of all registered [Command]s.
     */
    fun allCommands(): List<Command> {
        return cachedAllCommands ?: commands.values.sortedBy { it.name }.also {
            cachedAllCommands = it
        }
    }

    /**
     * Returns commands visible to the given [role].
     *
     * A command is visible if the user's role meets or exceeds the command's
     * [Command.minimumRole] requirement.
     *
     * @param role The user's current [UserRole].
     * @return An alphabetically sorted list of commands accessible to the role.
     */
    fun commandsForRole(role: UserRole): List<Command> {
        return commands.values
            .filter { role >= it.minimumRole }
            .sortedBy { it.name }
    }

    /**
     * Returns commands in a specific [category] that are visible to [role].
     *
     * @param category The category string to filter by (case-insensitive).
     * @param role The user's current [UserRole].
     * @return Filtered and sorted list of commands.
     */
    fun commandsForCategory(category: String, role: UserRole): List<Command> {
        val cat = category.lowercase()
        return commandsForRole(role).filter { it.category.lowercase() == cat }
    }

    /**
     * Returns all unique category names from commands visible to the given [role].
     *
     * @param role The user's current [UserRole].
     * @return Sorted list of category names.
     */
    fun categoriesForRole(role: UserRole): List<String> {
        return commandsForRole(role)
            .map { it.category }
            .distinct()
            .sorted()
    }

    /**
     * Returns the aggregated union of all [Command.valueFlags] declared by
     * registered commands.
     *
     * The result is cached and invalidated on new registrations. This set
     * is passed to [CommandParser] so that flag-value association is driven
     * by the commands themselves rather than a hardcoded central list.
     *
     * @return The merged set of long value-bearing flag names.
     */
    fun allValueFlags(): Set<String> {
        return cachedValueFlags ?: commands.values
            .flatMap { it.valueFlags }
            .toSet()
            .also { cachedValueFlags = it }
    }

    /**
     * Returns the aggregated union of all [Command.shortValueFlags] declared
     * by registered commands.
     *
     * @return The merged set of short value-bearing flag names.
     */
    fun allShortValueFlags(): Set<String> {
        return cachedShortValueFlags ?: commands.values
            .flatMap { it.shortValueFlags }
            .toSet()
            .also { cachedShortValueFlags = it }
    }

    /**
     * Produces autocomplete suggestions for a partially typed command name.
     *
     * Matches both primary names and aliases. Results are filtered to only
     * include commands the given [role] is allowed to see.
     *
     * @param prefix The current text the user has typed (case-insensitive).
     * @param role The user's current [UserRole].
     * @return A list of [CompletionSuggestion]s matching the prefix, sorted by
     *   relevance (exact prefix matches first, then alphabetically).
     */
    fun autocompleteCommand(prefix: String, role: UserRole): List<CompletionSuggestion> {
        val lowerPrefix = prefix.lowercase()
        val suggestions = mutableListOf<CompletionSuggestion>()
        val seen = mutableSetOf<String>()

        // Search primary names
        for ((name, cmd) in commands) {
            if (role < cmd.minimumRole) continue
            if (name.startsWith(lowerPrefix) && seen.add(name)) {
                suggestions.add(
                    CompletionSuggestion(
                        text = cmd.name,
                        displayText = cmd.name,
                        description = cmd.description,
                        type = SuggestionType.COMMAND
                    )
                )
            }
        }

        // Search aliases
        for ((alias, primaryKey) in aliasMap) {
            val cmd = commands[primaryKey] ?: continue
            if (role < cmd.minimumRole) continue
            if (alias.startsWith(lowerPrefix) && seen.add(alias)) {
                suggestions.add(
                    CompletionSuggestion(
                        text = alias,
                        displayText = "$alias (${cmd.name})",
                        description = cmd.description,
                        type = SuggestionType.COMMAND
                    )
                )
            }
        }

        // Sort: exact prefix match on primary name first, then alphabetically
        return suggestions.sortedWith(
            compareByDescending<CompletionSuggestion> { it.text.lowercase() == lowerPrefix }
                .thenBy { it.text.lowercase() }
        )
    }

    /**
     * Searches all commands (visible to [role]) whose name, description,
     * or aliases contain the given [query] (case-insensitive).
     *
     * @param query The search text.
     * @param role The user's current [UserRole].
     * @return Matching commands sorted by name.
     */
    fun searchCommands(query: String, role: UserRole): List<Command> {
        val lowerQuery = query.lowercase()
        return commandsForRole(role).filter { cmd ->
            cmd.name.lowercase().contains(lowerQuery) ||
                    cmd.description.lowercase().contains(lowerQuery) ||
                    cmd.aliases.any { it.lowercase().contains(lowerQuery) } ||
                    cmd.usage.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Returns the total number of registered commands (primary names only,
     * aliases are not counted separately).
     */
    val size: Int get() = commands.size
}
