package com.example.androidapp.domain.console

/**
 * Token types produced by [CommandLexer] during lexical analysis of raw console input.
 *
 * The lexer splits user input into a stream of these tokens, which are then
 * consumed by [CommandParser] to build a [ParsedCommand] structure.
 */
sealed class CommandToken {

    /**
     * A command keyword or bare word (e.g. `help`, `ls`, `ban`).
     *
     * @property value The keyword text.
     */
    data class Keyword(val value: String) : CommandToken()

    /**
     * A flag without an inline value (e.g. `-q`, `--confirm`, `--force`).
     *
     * @property name The flag name without leading dashes (e.g. `q`, `confirm`).
     */
    data class Flag(val name: String) : CommandToken()

    /**
     * A flag with an inline value joined by `=` (e.g. `--role=admin`, `--format=json`).
     *
     * @property name The flag name without leading dashes.
     * @property value The value after the `=` sign.
     */
    data class FlagValue(val name: String, val value: String) : CommandToken()

    /**
     * A positional argument that is not a flag or keyword
     * (e.g. `user@email.com`, `quizId123`).
     *
     * @property value The argument text.
     */
    data class Argument(val value: String) : CommandToken()

    /**
     * The pipe operator `|`, used to chain command output as input to the next command.
     *
     * @property value Always `"|"`.
     */
    data class Pipe(val value: String = "|") : CommandToken()

    /**
     * A quoted string literal (e.g. `"hello world"`, `'single quotes'`).
     * The surrounding quotes are stripped; only the inner content is kept.
     *
     * @property value The string content without surrounding quotes.
     */
    data class StringLiteral(val value: String) : CommandToken()

    /**
     * The semicolon operator `;`, used for sequential command chaining.
     */
    data object Semicolon : CommandToken()
}
