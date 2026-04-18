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
 * Pipe utility command that counts lines, words, characters, unique values,
 * non-empty lines, or field occurrences from piped input.
 *
 * Behaves similarly to the Unix `wc` command but operates on the in-app
 * console pipe stream. Requires piped input to function.
 *
 * Supported flags:
 * - `--lines` / `-l` : Count lines (default when no flag is specified).
 * - `--words` / `-w` : Count words.
 * - `--chars` / `-c` : Count characters.
 * - `--unique`       : Count distinct lines.
 * - `--non-empty`    : Count non-empty (non-blank) lines.
 * - `--by-field N`   : Count occurrences of each distinct value in the Nth field
 *                      (1-based, whitespace-delimited) and display a frequency table.
 *
 * Multiple counting flags can be combined in a single invocation; each
 * requested metric is reported on its own output line. When `--by-field` is
 * used it takes precedence and produces a table instead.
 *
 * Examples:
 * ```
 * ls -u | count
 * ls -u | count --words --chars
 * ls -u | count --by-field 2
 * ```
 */
class CountCommand : Command {

    /** @inheritDoc */
    override val name: String = "count"

    /** @inheritDoc */
    override val aliases: List<String> = listOf("wc")

    /** @inheritDoc */
    override val description: String = "Dem dong, tu, ky tu hoac gia tri duy nhat tu dau vao pipe"

    /** @inheritDoc */
    override val usage: String = "count [--lines|-l] [--words|-w] [--chars|-c] [--unique] [--non-empty] [--by-field N]"

    /** @inheritDoc */
    override val minimumRole: UserRole = UserRole.USER

    /** @inheritDoc */
    override val category: String = "pipe"

    /** @inheritDoc */
    override val examples: List<Pair<String, String>> = listOf(
        "ls -u | count" to "Dem so dong trong danh sach",
        "ls -u | count -w -c" to "Dem so tu va ky tu",
        "ls -u | count --unique" to "Dem so dong duy nhat (khong trung lap)",
        "ls -u | count --non-empty" to "Dem so dong khong trong",
        "ls -u | count --by-field 2" to "Thong ke tan suat theo truong thu 2"
    )

    /** @inheritDoc */
    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val available = mutableListOf<CompletionSuggestion>()

        val definedFlags = mapOf(
            "lines" to ("--lines" to "Dem so dong"),
            "words" to ("--words" to "Dem so tu"),
            "chars" to ("--chars" to "Dem so ky tu"),
            "unique" to ("--unique" to "Dem so dong duy nhat"),
            "non-empty" to ("--non-empty" to "Dem so dong khong trong"),
            "by-field" to ("--by-field" to "Thong ke tan suat theo truong N")
        )

        val shortAliases = mapOf(
            "l" to "lines",
            "w" to "words",
            "c" to "chars"
        )

        val usedFlags = flags.keys
        val usedNormalizedFlags = usedFlags + shortAliases.filterKeys { it in usedFlags }.values

        for ((normalizedFlag, suggestion) in definedFlags) {
            if (normalizedFlag !in usedNormalizedFlags) {
                available.add(
                    CompletionSuggestion(
                        text = suggestion.first,
                        description = suggestion.second,
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        return available
    }

    /** @inheritDoc */
    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val lines = context.pipeInput
            ?: return CommandResult.error("count: Lenh nay yeu cau dau vao pipe. Su dung voi dau '|', vi du: ls | count")

        // Normalise short flags to their long equivalents
        val hasLines = "lines" in flags || "l" in flags
        val hasWords = "words" in flags || "w" in flags
        val hasChars = "chars" in flags || "c" in flags
        val hasUnique = "unique" in flags
        val hasNonEmpty = "non-empty" in flags
        val byFieldRaw = flags["by-field"]

        // --by-field mode: frequency table
        if (byFieldRaw != null) {
            return executeByField(lines, byFieldRaw, flags)
        }

        // If no specific flag was given, default to --lines
        val noFlagSpecified = !hasLines && !hasWords && !hasChars && !hasUnique && !hasNonEmpty
        val effectiveLines = hasLines || noFlagSpecified

        val output = mutableListOf<OutputLine>()

        if (effectiveLines) {
            output.add(OutputLine("Dong: ${lines.size}", OutputStyle.INFO))
        }
        if (hasWords) {
            val wordCount = lines.sumOf { line ->
                line.trim().split(WHITESPACE_REGEX).count { it.isNotEmpty() }
            }
            output.add(OutputLine("Tu: $wordCount", OutputStyle.INFO))
        }
        if (hasChars) {
            val charCount = lines.sumOf { it.length }
            output.add(OutputLine("Ky tu: $charCount", OutputStyle.INFO))
        }
        if (hasUnique) {
            val uniqueCount = lines.toSet().size
            output.add(OutputLine("Dong duy nhat: $uniqueCount", OutputStyle.INFO))
        }
        if (hasNonEmpty) {
            val nonEmptyCount = lines.count { it.isNotBlank() }
            output.add(OutputLine("Dong khong trong: $nonEmptyCount", OutputStyle.INFO))
        }

        return CommandResult.success(output)
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Handles `--by-field N`: splits each line by whitespace, extracts the
     * Nth field (1-based), tallies frequencies, and returns a sorted table.
     */
    private fun executeByField(
        lines: List<String>,
        fieldValue: String,
        flags: Map<String, String?>
    ): CommandResult {
        val fieldIndex = fieldValue.toIntOrNull()
        if (fieldIndex == null || fieldIndex < 1) {
            return CommandResult.error("count: Gia tri --by-field phai la so nguyen duong (bat dau tu 1)")
        }

        val delimiter = flags["delimiter"] ?: flags["d"]
        val delimRegex = if (delimiter != null) Regex(Regex.escape(delimiter)) else WHITESPACE_REGEX

        val frequency = mutableMapOf<String, Int>()
        var missingFieldCount = 0

        for (line in lines) {
            val fields = line.split(delimRegex).filter { it.isNotEmpty() }
            if (fieldIndex > fields.size) {
                missingFieldCount++
                continue
            }
            val value = fields[fieldIndex - 1]
            frequency[value] = (frequency[value] ?: 0) + 1
        }

        if (frequency.isEmpty()) {
            return CommandResult.error(
                "count: Khong tim thay truong $fieldIndex trong bat ky dong nao"
            )
        }

        val sorted = frequency.entries.sortedByDescending { it.value }

        val output = mutableListOf<OutputLine>()
        output.add(
            OutputLine(
                "Tan suat theo truong $fieldIndex (${sorted.size} gia tri):",
                OutputStyle.HEADER
            )
        )
        output.add(
            OutputLine(
                String.format(java.util.Locale.ROOT, "  %-30s %s", "GIA TRI", "SO LUONG"),
                OutputStyle.TABLE_HEADER
            )
        )

        for ((value, count) in sorted) {
            output.add(
                OutputLine(
                    String.format(java.util.Locale.ROOT, "  %-30s %d", value, count),
                    OutputStyle.TABLE_ROW
                )
            )
        }

        if (missingFieldCount > 0) {
            output.add(
                OutputLine(
                    "  ($missingFieldCount dong khong co truong $fieldIndex)",
                    OutputStyle.MUTED
                )
            )
        }

        output.add(OutputLine("Tong: ${lines.size} dong", OutputStyle.INFO))

        return CommandResult.success(output)
    }

    private companion object {
        /** Reusable whitespace-splitting regex. */
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
