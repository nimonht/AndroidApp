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
 * Pipe filter command that searches input lines for a pattern.
 *
 * Operates exclusively on piped input ([CommandContext.pipeInput]). Supports
 * substring matching (default), regex matching, case sensitivity control,
 * inverted matching, context lines, and count-only output.
 *
 * Usage examples:
 * ```
 * ls -u | grep admin
 * ls -q | grep --regex "quiz_[0-9]+" --ignore-case
 * ls -u | grep --invert banned
 * ls -u | grep --count admin
 * ```
 */
class GrepCommand : Command {

    override val name: String = "grep"

    override val aliases: List<String> = listOf("filter")

    override val description: String = "Loc dong khop voi mau trong du lieu pipe"

    override val usage: String =
        "grep <pattern> [--regex|-r] [--ignore-case|-i] [--invert|-v] [--count|-c] " +
                "[--line-number|-n] [--context|-C N] [--before-context|-B N] [--after-context|-A N] " +
                "[--max-count|-m N] [--only-matching|-o] [--word|-w] [--fixed-string|-F] [--color]"

    override val minimumRole: UserRole = UserRole.USER

    override val category: String = "pipe"

    override val examples: List<Pair<String, String>> = listOf(
        "ls -u | grep admin" to "Loc tat ca dong chua 'admin'",
        "ls -u | grep -i -n admin" to "Loc khong phan biet hoa thuong, hien so dong",
        "ls -q | grep --regex \"quiz_[0-9]+\"" to "Loc bang bieu thuc chinh quy",
        "ls -u | grep -v banned" to "Hien dong KHONG khop",
        "ls -u | grep -c admin" to "Dem so dong khop",
        "ls -u | grep -C 2 admin" to "Hien 2 dong truoc va sau moi ket qua",
        "ls -u | grep -m 5 admin" to "Chi hien toi da 5 ket qua",
        "ls -u | grep -w admin" to "Chi khop toan bo tu 'admin'"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val allFlags = listOf(
            Triple(listOf("--regex", "-r"), setOf("regex", "r"), "Su dung bieu thuc chinh quy"),
            Triple(listOf("--ignore-case", "-i"), setOf("ignore-case", "i"), "Khong phan biet hoa thuong"),
            Triple(listOf("--invert", "-v"), setOf("invert", "v"), "Hien dong khong khop"),
            Triple(listOf("--count", "-c"), setOf("count", "c"), "Chi hien so dong khop"),
            Triple(listOf("--line-number", "-n"), setOf("line-number", "n"), "Hien so dong"),
            Triple(listOf("--context", "-C"), setOf("context", "C"), "So dong ngu canh truoc va sau"),
            Triple(listOf("--before-context", "-B"), setOf("before-context", "B"), "So dong ngu canh truoc"),
            Triple(listOf("--after-context", "-A"), setOf("after-context", "A"), "So dong ngu canh sau"),
            Triple(listOf("--max-count", "-m"), setOf("max-count", "m"), "Gioi han so ket qua"),
            Triple(listOf("--only-matching", "-o"), setOf("only-matching", "o"), "Chi hien phan khop"),
            Triple(listOf("--word", "-w"), setOf("word", "w"), "Chi khop toan bo tu"),
            Triple(listOf("--fixed-string", "-F"), setOf("fixed-string", "F"), "Tim kiem chuoi co dinh (khong regex)"),
            Triple(listOf("--color"), setOf("color"), "To mau phan khop")
        )
        val usedFlags = flags.keys

        return allFlags
            .filter { (_, normalizedFlags, _) -> normalizedFlags.none { it in usedFlags } }
            .flatMap { (variants, _, desc) ->
                variants.map { flag ->
                    CompletionSuggestion(
                        text = flag,
                        description = desc,
                        type = SuggestionType.FLAG
                    )
                }
            }
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val input = context.pipeInput
            ?: return CommandResult.error("grep: Lenh nay yeu cau du lieu pipe. Su dung voi dau '|', vi du: ls -u | grep admin")

        if (args.isEmpty()) {
            return CommandResult.error("grep: Thieu mau tim kiem. Su dung: grep <pattern> [flags]")
        }

        val rawPattern = args[0]

        val useRegex = "regex" in flags || "r" in flags
        val ignoreCase = "ignore-case" in flags || "i" in flags
        val invert = "invert" in flags || "v" in flags
        val countOnly = "count" in flags || "c" in flags
        val showLineNumbers = "line-number" in flags || "n" in flags
        val onlyMatching = "only-matching" in flags || "o" in flags
        val wordMatch = "word" in flags || "w" in flags
        val fixedString = "fixed-string" in flags || "F" in flags
        val useColor = "color" in flags

        val beforeContext = (flags["before-context"] ?: flags["B"] ?: flags["context"] ?: flags["C"])
            ?.toIntOrNull() ?: 0
        val afterContext = (flags["after-context"] ?: flags["A"] ?: flags["context"] ?: flags["C"])
            ?.toIntOrNull() ?: 0
        val maxCount = (flags["max-count"] ?: flags["m"])?.toIntOrNull() ?: Int.MAX_VALUE

        // ReDoS protection: reject overly long regex patterns
        if (useRegex && rawPattern.length > 200) {
            return CommandResult.error(
                "Bieu thuc chinh quy qua dai (toi da 200 ky tu). " +
                        "Su dung --fixed-string cho chuoi dai."
            )
        }

        // Build the effective pattern
        val effectivePattern = when {
            fixedString -> Regex.escape(rawPattern)
            wordMatch -> "\\b${if (useRegex) rawPattern else Regex.escape(rawPattern)}\\b"
            useRegex -> rawPattern
            else -> Regex.escape(rawPattern)
        }

        val regex = try {
            if (ignoreCase) {
                Regex(effectivePattern, RegexOption.IGNORE_CASE)
            } else {
                Regex(effectivePattern)
            }
        } catch (e: Exception) {
            return CommandResult.error("grep: Bieu thuc chinh quy khong hop le: ${e.message}")
        }

        // Determine matching lines (index-based)
        val matchingIndices = mutableSetOf<Int>()
        var matchCount = 0

        for ((index, line) in input.withIndex()) {
            if (matchCount >= maxCount) break

            val matches = regex.containsMatchIn(line)
            val isMatch = if (invert) !matches else matches

            if (isMatch) {
                matchingIndices.add(index)
                matchCount++
            }
        }

        // Count-only mode
        if (countOnly) {
            return CommandResult.success(
                listOf(
                    OutputLine(
                        "Ket qua: $matchCount dong khop",
                        OutputStyle.INFO
                    )
                )
            )
        }

        if (matchingIndices.isEmpty()) {
            return CommandResult.success(
                listOf(
                    OutputLine(
                        "grep: Khong tim thay dong nao khop voi '$rawPattern'",
                        OutputStyle.MUTED
                    )
                )
            )
        }

        // Expand indices to include context lines
        val visibleIndices = mutableSetOf<Int>()
        for (idx in matchingIndices) {
            val start = (idx - beforeContext).coerceAtLeast(0)
            val end = (idx + afterContext).coerceAtMost(input.size - 1)
            for (i in start..end) {
                visibleIndices.add(i)
            }
        }

        val sortedVisible = visibleIndices.sorted()
        val outputLines = mutableListOf<OutputLine>()

        var previousIdx = -2
        for (idx in sortedVisible) {
            // Insert separator between non-contiguous groups
            if (previousIdx >= 0 && idx - previousIdx > 1) {
                outputLines.add(OutputLine("--", OutputStyle.MUTED))
            }
            previousIdx = idx

            val line = input[idx]
            val isMatchLine = idx in matchingIndices

            val displayText = when {
                showLineNumbers -> "${idx + 1}: $line"
                else -> line
            }

            val style = when {
                isMatchLine && useColor -> OutputStyle.SUCCESS
                isMatchLine -> OutputStyle.NORMAL
                else -> OutputStyle.MUTED
            }

            if (onlyMatching && isMatchLine && !invert) {
                // Each match on its own line
                val allMatches = regex.findAll(line).map { it.value }.toList()
                for (m in allMatches) {
                    val prefix = if (showLineNumbers) "${idx + 1}: " else ""
                    outputLines.add(OutputLine("$prefix$m", style))
                }
            } else {
                outputLines.add(OutputLine(displayText, style))
            }
        }

        // Append summary
        outputLines.add(
            OutputLine(
                "-- $matchCount dong khop / ${input.size} tong so dong --",
                OutputStyle.MUTED
            )
        )

        return CommandResult.success(outputLines)
    }
}
