package com.example.androidapp.domain.console

/**
 * Represents a fully parsed command line, potentially containing multiple
 * segments separated by pipe operators `|` or semicolons `;`.
 *
 * @property chains List of pipeline chains separated by semicolons.
 *   Each chain is a list of [CommandSegment]s connected by pipes.
 */
data class ParsedCommand(
    val chains: List<List<CommandSegment>>
)

/**
 * A single command invocation within a parsed command line.
 *
 * @property command The primary command name (e.g. "ls", "ban", "my").
 * @property subCommand Optional sub-command (e.g. "quizzes" in "my quizzes").
 * @property args Positional arguments that are not the command/sub-command.
 * @property flags Parsed flags: flag name (without dashes) mapped to an optional value.
 *   Boolean flags (e.g. `--confirm`) map to `null`. Value flags (e.g. `--role=admin`
 *   or `--role admin`) map to their string value.
 * @property rawInput The original raw text for this segment (for display/logging).
 */
data class CommandSegment(
    val command: String,
    val subCommand: String? = null,
    val args: List<String> = emptyList(),
    val flags: Map<String, String?> = emptyMap(),
    val rawInput: String = ""
)

/**
 * Parses a list of [CommandToken]s produced by [CommandLexer] into a
 * structured [ParsedCommand].
 *
 * The parser handles:
 * - Pipe `|` operators: splits tokens into pipeline segments.
 * - Semicolon `;` operators: splits tokens into sequential chains.
 * - Flag grouping: associates a flag with the following token as its value
 *   when the flag expects one (heuristic: next token is not itself a flag).
 * - Sub-command detection: the second keyword-like token (if not a flag)
 *   is treated as a sub-command.
 *
 * Flags that use the `--key=value` syntax are already captured as
 * [CommandToken.FlagValue] by the lexer. For `--key value` syntax, the
 * parser peeks at the next token: if it is a [CommandToken.Keyword],
 * [CommandToken.Argument], or [CommandToken.StringLiteral], it is consumed
 * as the flag's value. Otherwise the flag is treated as boolean (value = null).
 *
 * Known value-bearing flags that always consume the next token:
 * `role`, `format`, `sort`, `limit`, `offset`, `page`, `fields`, `timeout`,
 * `output`, `level`, `tag`, `search`, `owner`, `user`, `quiz`, `service`,
 * `style`, `repeat`, `count`, `since`, `after`, `before`, `period`,
 * `category`, `score-above`, `score-below`, `min-attempts`, `max-attempts`,
 * `min-questions`, `max-questions`, `min-usage`, `max-usage`, `inactive-days`,
 * `active-days`, `email-domain`, `permission`, `contributor`, `source-quiz`,
 * `reason`, `from`, `to`, `compare-period`, `trend`, `type`, `exclude`,
 * `older-than`, `context`, `before-context`, `after-context`, `max-count`,
 * `delimiter`, `field`, `by-field`, `lines`, `skip`, `key`, `banned-before`,
 * `banned-after`, `created-after`, `created-before`, `updated-after`,
 * `updated-before`, `duration-above`, `duration-below`, `last`,
 * `n`, `l`, `t`, `k`, `d`, `s`, `f`, `o`, `p`, `c`, `r`, `m`, `C`, `B`, `A`.
 */
object CommandParser {

    /**
     * Set of long flag names that are known to take a value argument.
     */
    private val VALUE_FLAGS: Set<String> = setOf(
        "role", "format", "sort", "limit", "offset", "page", "fields", "timeout",
        "output", "level", "tag", "search", "owner", "user", "quiz", "service",
        "style", "repeat", "count", "since", "after", "before", "period",
        "category", "score-above", "score-below", "score-between",
        "min-attempts", "max-attempts", "min-questions", "max-questions",
        "min-usage", "max-usage", "inactive-days", "active-days",
        "email-domain", "permission", "contributor", "source-quiz",
        "reason", "from", "to", "compare-period", "trend", "type", "exclude",
        "older-than", "context", "before-context", "after-context", "max-count",
        "delimiter", "field", "by-field", "lines", "skip", "key",
        "banned-before", "banned-after",
        "created-after", "created-before", "created-between",
        "updated-after", "updated-before", "updated-between",
        "duration-above", "duration-below", "last",
        "between", "breakdown", "remove"
    )

    /**
     * Set of short flag characters that are known to take a value argument.
     */
    private val SHORT_VALUE_FLAGS: Set<String> = setOf(
        "n", "l", "t", "k", "d", "s", "f", "o", "p", "c", "r", "m",
        "C", "B", "A"
    )

    /**
     * Parses a token list into a [ParsedCommand].
     *
     * @param tokens The token list produced by [CommandLexer.tokenize].
     * @param rawInput The original raw input string (for reconstructing segment raw text).
     * @return A [ParsedCommand] with chains of piped segments.
     */
    fun parse(tokens: List<CommandToken>, rawInput: String = ""): ParsedCommand {
        if (tokens.isEmpty()) {
            return ParsedCommand(chains = emptyList())
        }

        val chains = mutableListOf<List<CommandSegment>>()
        var currentPipeline = mutableListOf<List<CommandToken>>()
        var currentSegmentTokens = mutableListOf<CommandToken>()

        for (token in tokens) {
            when (token) {
                is CommandToken.Pipe -> {
                    if (currentSegmentTokens.isNotEmpty()) {
                        currentPipeline.add(currentSegmentTokens.toList())
                        currentSegmentTokens = mutableListOf()
                    }
                }
                is CommandToken.Semicolon -> {
                    if (currentSegmentTokens.isNotEmpty()) {
                        currentPipeline.add(currentSegmentTokens.toList())
                        currentSegmentTokens = mutableListOf()
                    }
                    if (currentPipeline.isNotEmpty()) {
                        chains.add(currentPipeline.map { parseSegment(it) })
                        currentPipeline = mutableListOf()
                    }
                }
                else -> {
                    currentSegmentTokens.add(token)
                }
            }
        }

        // Flush remaining tokens
        if (currentSegmentTokens.isNotEmpty()) {
            currentPipeline.add(currentSegmentTokens.toList())
        }
        if (currentPipeline.isNotEmpty()) {
            chains.add(currentPipeline.map { parseSegment(it) })
        }

        return ParsedCommand(chains = chains)
    }

    /**
     * Parses a single segment's token list into a [CommandSegment].
     *
     * The first keyword/argument/string-literal token becomes the command name.
     * The second keyword-like token (if any) becomes the sub-command, unless
     * the command is known to not have sub-commands.
     *
     * @param tokens Tokens for this segment (no pipes or semicolons).
     * @return A fully parsed [CommandSegment].
     */
    private fun parseSegment(tokens: List<CommandToken>): CommandSegment {
        if (tokens.isEmpty()) {
            return CommandSegment(command = "", rawInput = "")
        }

        var command = ""
        var subCommand: String? = null
        val args = mutableListOf<String>()
        val flags = mutableMapOf<String, String?>()
        val rawParts = mutableListOf<String>()
        var commandFound = false
        var subCommandFound = false

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when (token) {
                is CommandToken.Keyword -> {
                    rawParts.add(token.value)
                    if (!commandFound) {
                        command = token.value.lowercase()
                        commandFound = true
                    } else if (!subCommandFound && !looksLikeValue(token.value)) {
                        subCommand = token.value.lowercase()
                        subCommandFound = true
                    } else {
                        args.add(token.value)
                    }
                }

                is CommandToken.Argument -> {
                    rawParts.add(token.value)
                    if (!commandFound) {
                        command = token.value.lowercase()
                        commandFound = true
                    } else {
                        args.add(token.value)
                    }
                }

                is CommandToken.StringLiteral -> {
                    rawParts.add("\"${token.value}\"")
                    if (!commandFound) {
                        command = token.value
                        commandFound = true
                    } else {
                        args.add(token.value)
                    }
                }

                is CommandToken.Flag -> {
                    rawParts.add(if (token.name.length == 1) "-${token.name}" else "--${token.name}")
                    // Check if this flag is known to take a value
                    if (flagExpectsValue(token.name) && i + 1 < tokens.size) {
                        val nextToken = tokens[i + 1]
                        val nextValue = extractTokenValue(nextToken)
                        if (nextValue != null) {
                            // If the flag already exists, we may have repeated flags
                            // For repeated flags like --tag, --level, append with comma
                            val existing = flags[token.name]
                            if (existing != null) {
                                flags[token.name] = "$existing,$nextValue"
                            } else {
                                flags[token.name] = nextValue
                            }
                            i++ // consume the next token
                            rawParts.add(nextValue)
                        } else {
                            flags.putIfAbsent(token.name, null)
                        }
                    } else {
                        flags.putIfAbsent(token.name, null)
                    }
                }

                is CommandToken.FlagValue -> {
                    rawParts.add("--${token.name}=${token.value}")
                    val existing = flags[token.name]
                    if (existing != null) {
                        flags[token.name] = "$existing,${token.value}"
                    } else {
                        flags[token.name] = token.value
                    }
                }

                // Pipe and Semicolon should not appear here (already split)
                is CommandToken.Pipe -> { /* skip */ }
                is CommandToken.Semicolon -> { /* skip */ }
            }
            i++
        }

        return CommandSegment(
            command = command,
            subCommand = subCommand,
            args = args,
            flags = flags,
            rawInput = rawParts.joinToString(" ")
        )
    }

    /**
     * Determines whether a flag name is known to take a value argument.
     */
    private fun flagExpectsValue(flagName: String): Boolean {
        return flagName in VALUE_FLAGS || flagName in SHORT_VALUE_FLAGS
    }

    /**
     * Extracts a usable string value from a token, if the token is a
     * keyword, argument, or string literal. Returns null for flags, pipes,
     * and semicolons (which cannot be consumed as flag values).
     */
    private fun extractTokenValue(token: CommandToken): String? = when (token) {
        is CommandToken.Keyword -> token.value
        is CommandToken.Argument -> token.value
        is CommandToken.StringLiteral -> token.value
        else -> null
    }

    /**
     * Heuristic check: returns true if the value looks like it could be
     * a flag value rather than a sub-command (e.g. contains `@`, digits only,
     * or starts with `/`).
     */
    private fun looksLikeValue(value: String): Boolean {
        return value.contains("@") ||
                value.all { it.isDigit() } ||
                value.startsWith("/")
    }
}
