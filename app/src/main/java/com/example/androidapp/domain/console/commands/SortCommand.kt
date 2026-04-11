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
 * Pipe command that sorts input lines with various ordering strategies.
 *
 * Supports alphabetical (default), numeric, field-based, case-insensitive,
 * unique-filtering, reverse, stable, and random sorting modes. Operates
 * exclusively on piped input — returns an error when invoked without a pipe.
 *
 * Usage examples:
 * ```
 * ls -u | sort
 * ls -u | sort --numeric --reverse
 * ls -u | sort --field 2 --delimiter ","
 * ls -u | sort --unique --ignore-case
 * ls -u | sort --random
 * ```
 */
class SortCommand : Command {

    override val name: String = "sort"

    override val description: String = "Sap xep cac dong dau vao theo thu tu"

    override val usage: String = "sort [--reverse|-r] [--numeric|-n] [--field|-k N] " +
            "[--delimiter|-d SEP] [--unique|-u] [--ignore-case|-f] [--stable] [--random]"

    override val minimumRole: UserRole = UserRole.USER

    override val category: String = "pipe"

    override val examples: List<Pair<String, String>> = listOf(
        "ls -u | sort" to "Sap xep danh sach theo thu tu alphabet",
        "ls -u | sort -r" to "Sap xep nguoc",
        "ls -u | sort -n" to "Sap xep theo gia tri so",
        "ls -u | sort -k 2 -d \",\"" to "Sap xep theo truong thu 2, phan cach bang dau phay",
        "ls -u | sort -u" to "Sap xep va loai bo cac dong trung lap",
        "ls -u | sort --random" to "Xao tron ngau nhien cac dong",
        "ls -u | sort -f" to "Sap xep khong phan biet hoa thuong"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val available = mutableListOf<CompletionSuggestion>()

        if ("reverse" !in flags && "r" !in flags) {
            available.add(
                CompletionSuggestion("--reverse", "--reverse", "Sap xep nguoc", SuggestionType.FLAG)
            )
        }
        if ("numeric" !in flags && "n" !in flags) {
            available.add(
                CompletionSuggestion("--numeric", "--numeric", "Sap xep theo so", SuggestionType.FLAG)
            )
        }
        if ("field" !in flags && "k" !in flags) {
            available.add(
                CompletionSuggestion("--field", "--field", "Sap xep theo truong N", SuggestionType.FLAG)
            )
        }
        if ("delimiter" !in flags && "d" !in flags) {
            available.add(
                CompletionSuggestion("--delimiter", "--delimiter", "Ky tu phan cach truong", SuggestionType.FLAG)
            )
        }
        if ("unique" !in flags && "u" !in flags) {
            available.add(
                CompletionSuggestion("--unique", "--unique", "Loai bo dong trung lap", SuggestionType.FLAG)
            )
        }
        if ("ignore-case" !in flags && "f" !in flags) {
            available.add(
                CompletionSuggestion(
                    "--ignore-case",
                    "--ignore-case",
                    "Khong phan biet hoa thuong",
                    SuggestionType.FLAG
                )
            )
        }
        if ("stable" !in flags) {
            available.add(
                CompletionSuggestion("--stable", "--stable", "Giu nguyen thu tu dong bang nhau", SuggestionType.FLAG)
            )
        }
        if ("random" !in flags) {
            available.add(
                CompletionSuggestion("--random", "--random", "Xao tron ngau nhien", SuggestionType.FLAG)
            )
        }

        return available
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val input = context.pipeInput
            ?: return CommandResult.error("sort: Lenh nay yeu cau dau vao pipe. Su dung: <lenh> | sort")

        if (input.isEmpty()) {
            return CommandResult.empty()
        }

        val reverse = "reverse" in flags || "r" in flags
        val numeric = "numeric" in flags || "n" in flags
        val unique = "unique" in flags || "u" in flags
        val ignoreCase = "ignore-case" in flags || "f" in flags
        val stable = "stable" in flags
        val random = "random" in flags
        val delimiter = flags["delimiter"] ?: flags["d"] ?: "\\s+"
        val fieldIndex = parseFieldIndex(flags)

        if (fieldIndex != null && fieldIndex < 1) {
            return CommandResult.error("sort: Chi so truong phai >= 1 (da nhan: $fieldIndex)")
        }

        var lines = input.toMutableList()

        // Apply unique filter before sorting if requested
        if (unique) {
            lines = if (ignoreCase) {
                lines.distinctBy { it.lowercase() }.toMutableList()
            } else {
                lines.distinct().toMutableList()
            }
        }

        // Random shuffle overrides all other sorting
        if (random) {
            lines.shuffle()
            return buildOutput(lines)
        }

        // Build comparator
        val comparator = buildComparator(
            numeric = numeric,
            ignoreCase = ignoreCase,
            fieldIndex = fieldIndex,
            delimiter = delimiter
        )

        // Sort: stable preserves input order for equal keys; non-stable applies
        // a deterministic tie-breaker instead of relying on insertion order.
        if (stable) {
            lines.sortWith(comparator)
        } else {
            lines = lines.withIndex()
                .sortedWith { a, b ->
                    val primary = comparator.compare(a.value, b.value)
                    if (primary != 0) {
                        primary
                    } else {
                        // When primary keys are equal, use case-sensitive
                        // lexicographic order as tie-breaker, then fall
                        // back to original index for total ordering.
                        val tieBreaker = a.value.compareTo(b.value)
                        if (tieBreaker != 0) tieBreaker else a.index.compareTo(b.index)
                    }
                }
                .map { it.value }
                .toMutableList()
        }

        if (reverse) {
            lines.reverse()
        }

        return buildOutput(lines)
    }

    /**
     * Parses the field index from `--field` / `-k` flags.
     *
     * @return The 1-based field index, or `null` if not specified.
     *         Returns -1 if the value is not a valid integer.
     */
    private fun parseFieldIndex(flags: Map<String, String?>): Int? {
        val raw = flags["field"] ?: flags["k"] ?: return null
        return raw.toIntOrNull() ?: -1
    }

    /**
     * Builds a [Comparator] for sorting lines based on the requested options.
     *
     * @param numeric If true, compare by parsed numeric value.
     * @param ignoreCase If true, ignore character case during comparison.
     * @param fieldIndex Optional 1-based field index to extract the sort key from.
     * @param delimiter Regex pattern used to split lines into fields.
     * @return A [Comparator] implementing the requested sort strategy.
     */
    private fun buildComparator(
        numeric: Boolean,
        ignoreCase: Boolean,
        fieldIndex: Int?,
        delimiter: String
    ): Comparator<String> {
        val delimiterRegex = try {
            Regex(delimiter)
        } catch (_: Exception) {
            Regex(Regex.escape(delimiter))
        }

        return Comparator { a, b ->
            val keyA = extractSortKey(a, fieldIndex, delimiterRegex)
            val keyB = extractSortKey(b, fieldIndex, delimiterRegex)

            if (numeric) {
                compareNumeric(keyA, keyB)
            } else if (ignoreCase) {
                keyA.lowercase().compareTo(keyB.lowercase())
            } else {
                keyA.compareTo(keyB)
            }
        }
    }

    /**
     * Extracts the sort key from a line, optionally selecting a specific field.
     *
     * @param line The full input line.
     * @param fieldIndex 1-based field index, or `null` to use the whole line.
     * @param delimiter Regex used to split the line into fields.
     * @return The extracted sort key string.
     */
    private fun extractSortKey(line: String, fieldIndex: Int?, delimiter: Regex): String {
        if (fieldIndex == null) return line
        val parts = line.split(delimiter)
        // fieldIndex is 1-based; return empty string if out of bounds
        return parts.getOrElse(fieldIndex - 1) { "" }
    }

    /**
     * Compares two strings as numeric values. Non-numeric strings sort after
     * numeric ones; two non-numeric strings fall back to lexicographic order.
     */
    private fun compareNumeric(a: String, b: String): Int {
        val numA = a.trim().toDoubleOrNull()
        val numB = b.trim().toDoubleOrNull()

        return when {
            numA != null && numB != null -> numA.compareTo(numB)
            numA != null -> -1 // numbers come before non-numbers
            numB != null -> 1
            else -> a.compareTo(b) // fallback to lexicographic
        }
    }

    /**
     * Wraps sorted lines into a successful [CommandResult].
     */
    private fun buildOutput(lines: List<String>): CommandResult {
        if (lines.isEmpty()) {
            return CommandResult.empty()
        }
        val outputLines = lines.map { OutputLine(it, OutputStyle.NORMAL) }
        return CommandResult.success(outputLines)
    }
}
