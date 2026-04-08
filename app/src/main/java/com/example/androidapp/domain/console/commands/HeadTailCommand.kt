package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.UserRole

/**
 * Pipe command that extracts lines from the beginning or end of piped input.
 *
 * When [isHead] is `true`, behaves as `head` (takes the first N lines).
 * When [isHead] is `false`, behaves as `tail` (takes the last N lines).
 *
 * Both variants share the same implementation; the only difference is which
 * end of the input the lines are drawn from. The line count defaults to 10
 * and can be overridden via the first positional argument or the `--lines`
 * (`-n`) flag.
 *
 * Requires piped input — returns an error when invoked without a pipe.
 *
 * @property isHead `true` for head behaviour, `false` for tail behaviour.
 */
class HeadTailCommand(private val isHead: Boolean) : Command {

    /** Primary command name — either `"head"` or `"tail"`. */
    override val name: String = if (isHead) "head" else "tail"

    override val aliases: List<String> = emptyList()

    override val description: String =
        if (isHead) "Lay N dong dau tien tu dau vao pipe"
        else "Lay N dong cuoi cung tu dau vao pipe"

    override val usage: String =
        if (isHead) "head [<so_dong>] [--lines/-n <N>] [--skip <N>] [--numbered]"
        else "tail [<so_dong>] [--lines/-n <N>] [--skip <N>] [--numbered]"

    override val minimumRole: UserRole = UserRole.USER

    override val category: String = "pipe"

    override val examples: List<Pair<String, String>>
        get() = if (isHead) {
            listOf(
                "ls -q | head" to "Lay 10 quiz dau tien",
                "ls -q | head 5" to "Lay 5 dong dau tien",
                "ls -q | head -n 3 --numbered" to "Lay 3 dong dau tien kem so thu tu",
                "ls -q | head --skip 2 -n 5" to "Bo qua 2 dong dau, lay 5 dong tiep theo"
            )
        } else {
            listOf(
                "ls -q | tail" to "Lay 10 dong cuoi cung",
                "ls -q | tail 5" to "Lay 5 dong cuoi cung",
                "ls -q | tail -n 3 --numbered" to "Lay 3 dong cuoi kem so thu tu",
                "ls -q | tail --skip 2 -n 5" to "Bo qua 2 dong cuoi, lay 5 dong tiep theo"
            )
        }

    /**
     * Provides autocomplete suggestions for flags and arguments.
     *
     * @param args Positional arguments entered so far.
     * @param flags Flags entered so far.
     * @param context Runtime context.
     * @return Ordered list of completion suggestions.
     */
    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        if (args.isEmpty()) {
            suggestions.add(
                CompletionSuggestion(
                    text = "10",
                    displayText = "10",
                    description = "So dong mac dinh",
                    type = SuggestionType.ARGUMENT
                )
            )
        }

        val availableFlags = listOf(
            Triple("--lines", "-n", "So dong can lay"),
            Triple("--skip", null, "Bo qua N dong truoc khi lay"),
            Triple("--numbered", null, "Hien thi so thu tu dong")
        )

        for ((long, short, desc) in availableFlags) {
            if (long !in flags && (short == null || short !in flags)) {
                suggestions.add(
                    CompletionSuggestion(
                        text = long,
                        displayText = if (short != null) "$long/$short" else long,
                        description = desc,
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        return suggestions
    }

    /**
     * Executes the head/tail command against piped input lines.
     *
     * @param args Positional arguments. `args[0]` is optionally the line count.
     * @param flags Parsed flags — `--lines`/`-n`, `--skip`, `--numbered`.
     * @param context Runtime context carrying [CommandContext.pipeInput].
     * @return [CommandResult] containing the selected output lines.
     */
    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val pipeInput = context.pipeInput
            ?: return CommandResult.error(
                "Lenh '$name' yeu cau dau vao pipe. Su dung: <lenh> | $name"
            )

        if (pipeInput.isEmpty()) {
            return CommandResult.success("(khong co dong nao)")
        }

        // --- Parse line count --------------------------------------------------
        val lineCount = resolveLineCount(args, flags)
            ?: return CommandResult.error(
                "So dong khong hop le. Vui long nhap so nguyen duong."
            )

        // --- Parse skip count --------------------------------------------------
        val skipCount = resolveSkipCount(flags)
            ?: return CommandResult.error(
                "Gia tri --skip khong hop le. Vui long nhap so nguyen khong am."
            )

        val numbered = "numbered" in flags

        // --- Apply skip then take ----------------------------------------------
        val selected = applySelection(pipeInput, lineCount, skipCount)

        if (selected.isEmpty()) {
            return CommandResult.success("(khong co dong nao sau khi loc)")
        }

        // --- Build output with optional line numbers ---------------------------
        val baseOffset = computeBaseOffset(pipeInput.size, lineCount, skipCount)
        val outputLines = buildOutputLines(selected, numbered, baseOffset)

        return CommandResult.success(outputLines)
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves the number of lines to take.
     *
     * Priority: `--lines` / `-n` flag > first positional arg > default (10).
     *
     * @return The positive line count, or `null` if the value is invalid.
     */
    private fun resolveLineCount(
        args: List<String>,
        flags: Map<String, String?>
    ): Int? {
        val raw = flags["lines"] ?: flags["n"] ?: args.firstOrNull()
        if (raw == null) return DEFAULT_LINE_COUNT

        val parsed = raw.toIntOrNull() ?: return null
        return if (parsed > 0) parsed else null
    }

    /**
     * Resolves the number of lines to skip before taking.
     *
     * @return The non-negative skip count, or `null` if the value is invalid.
     */
    private fun resolveSkipCount(flags: Map<String, String?>): Int? {
        val raw = flags["skip"] ?: return 0
        val parsed = raw.toIntOrNull() ?: return null
        return if (parsed >= 0) parsed else null
    }

    /**
     * Selects lines from the input according to head/tail semantics with skip.
     *
     * For **head**: skip the first [skipCount] lines, then take [lineCount].
     * For **tail**: skip the last [skipCount] lines, then take [lineCount] from the end.
     */
    private fun applySelection(
        input: List<String>,
        lineCount: Int,
        skipCount: Int
    ): List<String> {
        return if (isHead) {
            input.drop(skipCount).take(lineCount)
        } else {
            val withoutSkipped = if (skipCount > 0) input.dropLast(skipCount) else input
            withoutSkipped.takeLast(lineCount)
        }
    }

    /**
     * Computes the 1-based line number of the first selected line within the
     * original input. Used when `--numbered` is active.
     */
    private fun computeBaseOffset(
        totalLines: Int,
        lineCount: Int,
        skipCount: Int
    ): Int {
        return if (isHead) {
            skipCount + 1
        } else {
            val afterSkip = totalLines - skipCount
            val taken = minOf(lineCount, maxOf(afterSkip, 0))
            maxOf(afterSkip - taken + 1, 1)
        }
    }

    /**
     * Formats selected lines, optionally prepending 1-based line numbers.
     *
     * @param lines The selected lines.
     * @param numbered Whether to prepend line numbers.
     * @param baseOffset The 1-based index of the first line in the original input.
     */
    private fun buildOutputLines(
        lines: List<String>,
        numbered: Boolean,
        baseOffset: Int
    ): List<OutputLine> {
        if (!numbered) {
            return lines.map { OutputLine(it) }
        }

        val maxNumber = baseOffset + lines.size - 1
        val padWidth = maxNumber.toString().length

        return lines.mapIndexed { index, line ->
            val lineNumber = (baseOffset + index).toString().padStart(padWidth)
            OutputLine("$lineNumber  $line")
        }
    }

    private companion object {
        /** Default number of lines when no count is specified. */
        const val DEFAULT_LINE_COUNT = 10
    }
}

/**
 * Pre-configured `head` command instance.
 *
 * Takes lines from the **beginning** of piped input.
 * Default line count: 10. Supports `--skip` and `--numbered`.
 */
val HeadCommand: Command = HeadTailCommand(isHead = true)

/**
 * Pre-configured `tail` command instance.
 *
 * Takes lines from the **end** of piped input.
 * Default line count: 10. Supports `--skip` and `--numbered`.
 */
val TailCommand: Command = HeadTailCommand(isHead = false)
