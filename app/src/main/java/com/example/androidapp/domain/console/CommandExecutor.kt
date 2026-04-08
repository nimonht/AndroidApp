package com.example.androidapp.domain.console

import com.example.androidapp.domain.model.UserRole

/**
 * Orchestrates the full lifecycle of console command execution:
 * lexing, parsing, resolution, role/permission checks, destructive
 * confirmation checks, and finally execution with pipe/chain support.
 *
 * The executor is the single entry point used by the console ViewModel.
 * It delegates to [CommandLexer], [CommandParser], and [CommandRegistry]
 * internally.
 *
 * @property registry The command registry used to resolve command names.
 * @property contextProvider Lambda that produces a fresh [CommandContext]
 *   for each execution. Called once per top-level [execute] invocation;
 *   pipe stages share the same context (with updated [CommandContext.pipeInput]).
 */
class CommandExecutor(
    val registry: CommandRegistry,
    private val contextProvider: () -> CommandContext
) {

    /**
     * Execute a raw console input string and return the combined result.
     *
     * Processing pipeline:
     * 1. **Alias expansion** — if the first word matches a registered alias,
     *    the alias body replaces it before lexing.
     * 2. **Lex** — [CommandLexer.tokenize] splits the input into tokens.
     * 3. **Parse** — [CommandParser.parse] builds a [ParsedCommand] with
     *    semicolon-separated chains of piped segments.
     * 4. For each chain (semicolon-separated):
     *    a. For each segment in the pipeline:
     *       - **Resolve** the command name via [CommandRegistry.resolve].
     *       - **Role check** — current user's role must be >= command's [Command.minimumRole].
     *       - **Permission check** — if the command declares [Command.requiredPermission],
     *         the user must have it (superusers pass automatically).
     *       - **Destructive check** — if [Command.isDestructive] and the `--confirm`
     *         flag is absent, return a confirmation prompt instead of executing.
     *       - **Execute** — call [Command.execute] with args, flags, and context
     *         (including pipe input from the previous stage, if any).
     *    b. The output of each stage becomes the pipe input for the next stage.
     * 5. Outputs from all chains are concatenated.
     *
     * @param rawInput The raw text entered by the user in the console.
     * @return A [CommandResult] containing all output lines and a combined success status.
     */
    suspend fun execute(rawInput: String): CommandResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return CommandResult.empty()
        }

        val context = contextProvider()

        // Alias expansion: replace the first word if it matches an alias
        val expanded = expandAliases(trimmed, context.aliases)

        // Lex and parse
        val tokens = CommandLexer.tokenize(expanded)
        val parsed = CommandParser.parse(tokens, expanded)

        if (parsed.chains.isEmpty()) {
            return CommandResult.empty()
        }

        val allOutput = mutableListOf<OutputLine>()
        var allSuccess = true

        for (chain in parsed.chains) {
            val chainResult = executeChain(chain, context)
            allOutput.addAll(chainResult.output)
            if (!chainResult.isSuccess) {
                allSuccess = false
            }
        }

        return CommandResult(
            output = allOutput,
            isSuccess = allSuccess,
            exitCode = if (allSuccess) 0 else 1
        )
    }

    /**
     * Produce autocomplete suggestions for the current input at the given cursor position.
     *
     * If the cursor is on the first token (or the input is empty), command-name
     * completions are returned (filtered by the user's role). Otherwise, the
     * resolved command's [Command.autocomplete] method is called.
     *
     * @param rawInput The current text in the console input field.
     * @param cursorPosition The cursor index within [rawInput].
     * @return An ordered list of [CompletionSuggestion]s.
     */
    fun autocomplete(rawInput: String, cursorPosition: Int): List<CompletionSuggestion> {
        val context = try {
            contextProvider()
        } catch (_: Exception) {
            return emptyList()
        }

        val textUpToCursor = rawInput.take(cursorPosition)
        val tokens = CommandLexer.tokenize(textUpToCursor)

        // Find the last segment (after the last pipe or semicolon)
        val segmentTokens = getLastSegmentTokens(tokens)

        if (segmentTokens.isEmpty()) {
            // No tokens yet — suggest all available commands
            return registry.commandsForRole(context.currentUser.role).map { cmd ->
                CompletionSuggestion(
                    text = cmd.name,
                    displayText = cmd.name,
                    description = cmd.description,
                    type = SuggestionType.COMMAND
                )
            }
        }

        val firstToken = segmentTokens.firstOrNull()
        val commandName = when (firstToken) {
            is CommandToken.Keyword -> firstToken.value.lowercase()
            is CommandToken.Argument -> firstToken.value.lowercase()
            else -> ""
        }

        // If we only have one token and the input doesn't end with a space,
        // suggest commands matching the partial input
        val endsWithSpace = textUpToCursor.isNotEmpty() && textUpToCursor.last() == ' '
        if (segmentTokens.size == 1 && !endsWithSpace) {
            return registry.autocompleteCommand(commandName, context.currentUser.role)
        }

        // Resolve the command and delegate to its autocomplete
        val command = registry.resolve(commandName) ?: return emptyList()

        // Check role — don't suggest for commands the user can't run
        if (context.currentUser.role < command.minimumRole) {
            return emptyList()
        }

        // Parse the segment to extract args and flags
        val parsed = CommandParser.parse(segmentTokens, "")
        val segment = parsed.chains.firstOrNull()?.firstOrNull()
            ?: return emptyList()

        return command.autocomplete(
            args = buildList {
                if (segment.subCommand != null) add(segment.subCommand!!)
                addAll(segment.args)
            },
            flags = segment.flags,
            context = context
        )
    }

    // -- Internal helpers -------------------------------------------------------

    /**
     * Executes a single pipeline chain (list of piped [CommandSegment]s).
     * The output of each segment is fed as pipe input to the next.
     */
    private suspend fun executeChain(
        segments: List<CommandSegment>,
        baseContext: CommandContext
    ): CommandResult {
        var pipeInput: List<String>? = null
        val allOutput = mutableListOf<OutputLine>()
        var lastSuccess = true

        for ((index, segment) in segments.withIndex()) {
            val isLastSegment = index == segments.size - 1

            // Handle --help flag on any command
            if (segment.flags.containsKey("help") || segment.flags.containsKey("h")) {
                val command = registry.resolve(segment.command)
                if (command != null) {
                    val helpResult = buildCommandHelp(command)
                    allOutput.addAll(helpResult.output)
                    return CommandResult(output = allOutput, isSuccess = true)
                }
            }

            val result = executeSingleSegment(
                segment = segment,
                context = baseContext.copy(pipeInput = pipeInput)
            )

            if (!result.isSuccess) {
                allOutput.addAll(result.output)
                return CommandResult(
                    output = allOutput,
                    isSuccess = false,
                    exitCode = result.exitCode
                )
            }

            if (isLastSegment) {
                allOutput.addAll(result.output)
                lastSuccess = result.isSuccess
            } else {
                // Feed output as pipe input to the next segment
                pipeInput = result.output.map { it.text }
            }
        }

        return CommandResult(
            output = allOutput,
            isSuccess = lastSuccess,
            exitCode = if (lastSuccess) 0 else 1
        )
    }

    /**
     * Executes a single [CommandSegment] after performing all pre-flight checks.
     */
    private suspend fun executeSingleSegment(
        segment: CommandSegment,
        context: CommandContext
    ): CommandResult {
        val commandName = segment.command

        if (commandName.isEmpty()) {
            return CommandResult.empty()
        }

        // Resolve command
        val command = registry.resolve(commandName)
            ?: return CommandResult.error(
                "Khong tim thay lenh: '$commandName'. Go 'help' de xem danh sach lenh."
            )

        // Role check
        if (context.currentUser.role < command.minimumRole) {
            return CommandResult.error(
                "Quyen truy cap khong du. Lenh '$commandName' yeu cau vai tro ${command.minimumRole.name} tro len."
            )
        }

        // Permission check
        val requiredPerm = command.requiredPermission
        if (requiredPerm != null && !context.currentUser.hasPermission(requiredPerm)) {
            return CommandResult.error(
                "Quyen han khong du. Lenh '$commandName' yeu cau quyen ${requiredPerm.name}."
            )
        }

        // Destructive confirmation check
        if (command.isDestructive) {
            val hasConfirm = segment.flags.containsKey("confirm")
            val isDryRun = segment.flags.containsKey("dry-run")
            if (!hasConfirm && !isDryRun) {
                return CommandResult(
                    output = listOf(
                        OutputLine(
                            "Canh bao: Lenh nay thuc hien thao tac khong the hoan tac!",
                            OutputStyle.WARNING
                        ),
                        OutputLine(
                            "Them flag --confirm de xac nhan, hoac --dry-run de xem truoc.",
                            OutputStyle.INFO
                        )
                    ),
                    isSuccess = false,
                    exitCode = 2
                )
            }
        }

        // Build args list: include subCommand as first arg if present
        val args = buildList {
            if (segment.subCommand != null) add(segment.subCommand!!)
            addAll(segment.args)
        }

        // Execute
        return try {
            command.execute(args, segment.flags, context)
        } catch (e: Exception) {
            CommandResult.error(
                "Loi khi thuc thi lenh '$commandName': ${e.message ?: "Loi khong xac dinh"}"
            )
        }
    }

    /**
     * Performs alias expansion on the raw input.
     *
     * If the first whitespace-delimited word of [input] matches an alias key,
     * it is replaced with the alias value. Expansion is performed only once
     * (no recursive alias resolution) to prevent infinite loops.
     */
    private fun expandAliases(input: String, aliases: Map<String, String>): String {
        if (aliases.isEmpty()) return input
        val firstSpace = input.indexOf(' ')
        val firstWord = if (firstSpace == -1) input else input.substring(0, firstSpace)
        val aliasExpansion = aliases[firstWord] ?: return input
        return if (firstSpace == -1) {
            aliasExpansion
        } else {
            "$aliasExpansion${input.substring(firstSpace)}"
        }
    }

    /**
     * Extracts tokens belonging to the last command segment (after the last
     * pipe or semicolon) from a full token list. Used for autocomplete scoping.
     */
    private fun getLastSegmentTokens(tokens: List<CommandToken>): List<CommandToken> {
        val lastSeparatorIndex = tokens.indexOfLast {
            it is CommandToken.Pipe || it is CommandToken.Semicolon
        }
        return if (lastSeparatorIndex == -1) {
            tokens
        } else {
            tokens.subList(lastSeparatorIndex + 1, tokens.size)
        }
    }

    /**
     * Builds a help output for a single command, shown when the user
     * appends `--help` or `-h` to any command.
     */
    private fun buildCommandHelp(command: Command): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine(command.name.uppercase(), OutputStyle.HEADER))
        lines.add(OutputLine(command.description, OutputStyle.NORMAL))
        lines.add(OutputLine("", OutputStyle.NORMAL))
        lines.add(OutputLine("Cach dung:", OutputStyle.INFO))
        lines.add(OutputLine("  ${command.usage}", OutputStyle.CODE))

        if (command.aliases.isNotEmpty()) {
            lines.add(OutputLine("", OutputStyle.NORMAL))
            lines.add(OutputLine("Bi danh: ${command.aliases.joinToString(", ")}", OutputStyle.MUTED))
        }

        if (command.examples.isNotEmpty()) {
            lines.add(OutputLine("", OutputStyle.NORMAL))
            lines.add(OutputLine("Vi du:", OutputStyle.INFO))
            for ((example, desc) in command.examples) {
                lines.add(OutputLine("  $example", OutputStyle.CODE))
                lines.add(OutputLine("    $desc", OutputStyle.MUTED))
            }
        }

        if (command.requiredPermission != null) {
            lines.add(OutputLine("", OutputStyle.NORMAL))
            lines.add(OutputLine(
                "Quyen yeu cau: ${command.requiredPermission!!.name}",
                OutputStyle.WARNING
            ))
        }

        if (command.minimumRole != UserRole.USER) {
            lines.add(OutputLine(
                "Vai tro toi thieu: ${command.minimumRole.name}",
                OutputStyle.WARNING
            ))
        }

        if (command.isDestructive) {
            lines.add(OutputLine(
                "Canh bao: Lenh nay thuc hien thao tac khong the hoan tac!",
                OutputStyle.WARNING
            ))
        }

        return CommandResult.success(lines)
    }
}
