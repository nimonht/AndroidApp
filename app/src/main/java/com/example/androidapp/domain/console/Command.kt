package com.example.androidapp.domain.console

import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.UserRole

/**
 * Core command interface for the in-app console.
 *
 * Every command implementation must provide metadata (name, description, usage,
 * permission requirements) and two operations: [autocomplete] for fish-style
 * ghost-text suggestions and [execute] for running the command.
 *
 * Commands are registered in [CommandRegistry] and resolved/executed by
 * [CommandExecutor].
 */
interface Command {

    /** Primary command name (e.g. "help", "ban", "ls"). */
    val name: String

    /** Alternative names that also resolve to this command (e.g. "list" for "ls"). */
    val aliases: List<String> get() = emptyList()

    /** Short, single-line Vietnamese description shown in help listings. */
    val description: String

    /**
     * Usage pattern string shown in detailed help output.
     * Example: `"ls [-u|-q|-a|-p] [--role <role>] [--sort <field>]"`
     */
    val usage: String

    /**
     * The [AdminPermission] required to run this command, or `null` if no
     * specific admin permission is needed (i.e. the command is gated only
     * by [minimumRole]).
     */
    val requiredPermission: AdminPermission? get() = null

    /**
     * Whether this command performs an irreversible/destructive operation.
     * Destructive commands require a `--confirm` flag or interactive
     * confirmation before execution.
     */
    val isDestructive: Boolean get() = false

    /**
     * Minimum [UserRole] required to see and execute this command.
     * The executor checks `currentUser.role >= minimumRole` before running.
     *
     * Defaults to [UserRole.USER] (accessible to all logged-in users).
     */
    val minimumRole: UserRole get() = UserRole.USER

    /**
     * Optional category for grouping commands in help output.
     * Examples: "user", "quiz", "system", "util", "pipe".
     */
    val category: String get() = "general"

    /**
     * Example usages shown in `help <command> --examples`.
     * Each pair is (command string, description).
     */
    val examples: List<Pair<String, String>> get() = emptyList()

    /**
     * Produce autocomplete suggestions for the current input state.
     *
     * @param args Positional arguments entered so far.
     * @param flags Flags entered so far (name -> optional value).
     * @param context Runtime context with repositories and current user.
     * @return Ordered list of completion suggestions.
     */
    fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion>

    /**
     * Execute the command with the given arguments and flags.
     *
     * @param args Positional arguments.
     * @param flags Parsed flags (name -> optional value; boolean flags map to null).
     * @param context Runtime context with repositories, current user, and optional pipe input.
     * @return The [CommandResult] containing styled output lines and success/failure status.
     */
    suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult
}

/**
 * A single autocomplete suggestion displayed in the console dropdown.
 *
 * @property text The text that will be inserted when the suggestion is accepted.
 * @property displayText The text shown in the suggestion dropdown (defaults to [text]).
 * @property description Optional short description shown alongside the suggestion.
 * @property type The category of suggestion, used for icon/color differentiation.
 */
data class CompletionSuggestion(
    val text: String,
    val displayText: String = text,
    val description: String = "",
    val type: SuggestionType = SuggestionType.ARGUMENT
)

/**
 * Categories for autocomplete suggestions, used to determine the icon and
 * color shown in the suggestion dropdown.
 */
enum class SuggestionType {
    /** A top-level command name. */
    COMMAND,

    /** A sub-command (e.g. "quizzes" in "my quizzes"). */
    SUBCOMMAND,

    /** A flag (e.g. "--verbose", "-q"). */
    FLAG,

    /** A generic positional argument. */
    ARGUMENT,

    /** A user identifier (email or user ID). */
    USER,

    /** A quiz identifier. */
    QUIZ,

    /** A tag value. */
    TAG
}
