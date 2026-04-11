package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandFormatUtils
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.UserRole

/**
 * Lenh hien thi va quan ly lich su cac lenh da thuc thi trong phien lam viec.
 *
 * Ho tro hien thi, tim kiem, loc va xuat lich su lenh. Doc du lieu tu
 * [CommandContext.commandHistory].
 *
 * Vi du su dung:
 * - `history` — hien thi toan bo lich su
 * - `history 10` — hien thi 10 lenh gan nhat
 * - `history --search ping` — tim lenh chua "ping"
 * - `history --clear` — xoa lich su (tra ve tin hieu cho ViewModel)
 * - `history --unique` — chi hien thi cac lenh khong trung lap
 * - `history --export` — xuat lich su dang van ban
 */
class HistoryCommand : Command {

    /** @inheritDoc */
    override val name: String = "history"

    /** @inheritDoc */
    override val aliases: List<String> = listOf("hist")

    /** @inheritDoc */
    override val description: String = "Hien thi va quan ly lich su lenh"

    /** @inheritDoc */
    override val usage: String =
        "history [<so_luong>] [--search <tu_khoa>] [--regex <mau>] [--clear] " +
                "[--unique] [--reverse] [--numbered] [--no-numbered] [--since <thoi_gian>] " +
                "[--format <dinh_dang>] [--export]"

    /** @inheritDoc */
    override val minimumRole: UserRole = UserRole.USER

    /** @inheritDoc */
    override val category: String = "util"

    /** @inheritDoc */
    override val examples: List<Pair<String, String>> = listOf(
        "history" to "Hien thi toan bo lich su lenh",
        "history 5" to "Hien thi 5 lenh gan nhat",
        "history --search quiz" to "Tim cac lenh chua 'quiz'",
        "history --regex ^my" to "Tim cac lenh bat dau bang 'my'",
        "history --unique" to "Hien thi cac lenh khong trung lap",
        "history --reverse" to "Hien thi lich su theo thu tu nguoc",
        "history --clear" to "Xoa toan bo lich su lenh",
        "history --export" to "Xuat lich su lenh dang van ban",
        "history --format json" to "Hien thi lich su dang JSON",
        "history 10 --numbered" to "Hien thi 10 lenh gan nhat co danh so"
    )

    /** @inheritDoc */
    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        if (args.isEmpty()) {
            suggestions.addAll(
                listOf(
                    CompletionSuggestion(
                        "--search",
                        description = "Tim kiem trong lich su",
                        type = SuggestionType.FLAG
                    ),
                    CompletionSuggestion(
                        "--regex",
                        description = "Tim kiem bang bieu thuc chinh quy",
                        type = SuggestionType.FLAG
                    ),
                    CompletionSuggestion("--clear", description = "Xoa lich su lenh", type = SuggestionType.FLAG),
                    CompletionSuggestion(
                        "--unique",
                        description = "Chi hien thi lenh khong trung",
                        type = SuggestionType.FLAG
                    ),
                    CompletionSuggestion("--reverse", description = "Dao nguoc thu tu", type = SuggestionType.FLAG),
                    CompletionSuggestion("--numbered", description = "Danh so cac dong", type = SuggestionType.FLAG),
                    CompletionSuggestion("--no-numbered", description = "Khong danh so", type = SuggestionType.FLAG),
                    CompletionSuggestion(
                        "--since",
                        description = "Chi hien thi lenh tu thoi diem",
                        type = SuggestionType.FLAG
                    ),
                    CompletionSuggestion(
                        "--format",
                        description = "Dinh dang dau ra (text/json)",
                        type = SuggestionType.FLAG
                    ),
                    CompletionSuggestion("--export", description = "Xuat lich su", type = SuggestionType.FLAG)
                )
            )
        }

        if ("format" in flags && flags["format"] == null) {
            suggestions.add(CompletionSuggestion("text", description = "Van ban thuan", type = SuggestionType.ARGUMENT))
            suggestions.add(
                CompletionSuggestion(
                    "json",
                    description = "Dinh dang JSON",
                    type = SuggestionType.ARGUMENT
                )
            )
        }

        return suggestions
    }

    /** @inheritDoc */
    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val history = context.commandHistory

        // --clear: signal the ViewModel to clear history
        if ("clear" in flags) {
            return CommandResult.success(
                listOf(
                    OutputLine("__CLEAR_HISTORY__", OutputStyle.NORMAL),
                    OutputLine("Da xoa lich su lenh.", OutputStyle.SUCCESS)
                )
            )
        }

        if (history.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Lich su lenh trong.", OutputStyle.MUTED))
            )
        }

        // Build indexed entries (1-based index, command string)
        var entries = history.mapIndexed { index, cmd -> IndexedEntry(index + 1, cmd) }

        // --search: filter by substring
        val searchQuery = flags["search"]
        if (searchQuery != null) {
            val lowerQuery = searchQuery.lowercase()
            entries = entries.filter { it.command.lowercase().contains(lowerQuery) }
        }

        // --regex: filter by regex pattern
        val regexPattern = flags["regex"]
        if (regexPattern != null && regexPattern.length > 200) {
            return CommandResult.error("Bieu thuc chinh quy qua dai (toi da 200 ky tu).")
        }
        if (regexPattern != null) {
            val regex = try {
                Regex(regexPattern, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                return CommandResult.error("Bieu thuc chinh quy khong hop le: $regexPattern")
            }
            entries = entries.filter { regex.containsMatchIn(it.command) }
        }

        // --since: filter commands after a given index
        val sinceValue = flags["since"]
        if (sinceValue != null) {
            val sinceIndex = sinceValue.toIntOrNull()
            if (sinceIndex != null && sinceIndex > 0) {
                entries = entries.filter { it.index >= sinceIndex }
            } else {
                return CommandResult.error("Gia tri --since khong hop le: $sinceValue (can la so nguyen duong)")
            }
        }

        // --unique: deduplicate, keeping last occurrence
        if ("unique" in flags) {
            val seen = mutableSetOf<String>()
            entries = entries.reversed().filter { seen.add(it.command) }.reversed()
        }

        // --reverse: reverse order
        if ("reverse" in flags) {
            entries = entries.reversed()
        }

        // Positional arg: limit to last N entries
        if (args.isNotEmpty()) {
            val limit = args[0].toIntOrNull()
            if (limit != null && limit > 0) {
                entries = entries.takeLast(limit)
            } else if (args[0].toIntOrNull() == null) {
                return CommandResult.error("Tham so khong hop le: '${args[0]}'. Hay nhap so luong lenh can hien thi.")
            }
        }

        if (entries.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong tim thay lenh nao phu hop.", OutputStyle.MUTED))
            )
        }

        // Determine numbering
        val showNumbers = when {
            "no-numbered" in flags -> false
            "numbered" in flags -> true
            else -> true // default: show numbers
        }

        val format = flags["format"]?.lowercase() ?: "text"

        // --export: produce plain export format
        if ("export" in flags) {
            return buildExportOutput(entries, showNumbers)
        }

        return when (format) {
            "json" -> buildJsonOutput(entries)
            else -> buildTextOutput(entries, showNumbers, searchQuery)
        }
    }

    /**
     * Xay dung ket qua dang van ban (mac dinh).
     */
    private fun buildTextOutput(
        entries: List<IndexedEntry>,
        showNumbers: Boolean,
        searchQuery: String?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        val headerText = if (searchQuery != null) {
            "Lich su lenh (loc: \"$searchQuery\") - ${entries.size} ket qua"
        } else {
            "Lich su lenh - ${entries.size} muc"
        }
        lines.add(OutputLine(headerText, OutputStyle.HEADER))
        lines.add(OutputLine(buildSeparator(headerText.length), OutputStyle.MUTED))

        val indexWidth = if (showNumbers) {
            entries.maxOfOrNull { it.index.toString().length } ?: 1
        } else {
            0
        }

        for (entry in entries) {
            val text = if (showNumbers) {
                val paddedIndex = entry.index.toString().padStart(indexWidth)
                "  $paddedIndex  ${entry.command}"
            } else {
                "  ${entry.command}"
            }
            lines.add(OutputLine(text, OutputStyle.TABLE_ROW))
        }

        lines.add(OutputLine("", OutputStyle.NORMAL))
        lines.add(
            OutputLine(
                "Tong cong: ${entries.size} lenh. Dung --search de tim kiem, --clear de xoa.",
                OutputStyle.MUTED
            )
        )

        return CommandResult.success(lines)
    }

    /**
     * Xay dung ket qua dang JSON.
     */
    private fun buildJsonOutput(entries: List<IndexedEntry>): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[", OutputStyle.CODE))

        entries.forEachIndexed { i, entry ->
            val comma = if (i < entries.size - 1) "," else ""
            val escapedCmd = CommandFormatUtils.escapeJson(entry.command)
            lines.add(
                OutputLine(
                    "  { \"index\": ${entry.index}, \"command\": \"$escapedCmd\" }$comma",
                    OutputStyle.CODE
                )
            )
        }

        lines.add(OutputLine("]", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung ket qua xuat lich su.
     */
    private fun buildExportOutput(
        entries: List<IndexedEntry>,
        showNumbers: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("# Lich su lenh Quizzez Console", OutputStyle.HEADER))
        lines.add(OutputLine("# Tong cong: ${entries.size} lenh", OutputStyle.MUTED))
        lines.add(OutputLine("", OutputStyle.NORMAL))

        for (entry in entries) {
            val text = if (showNumbers) {
                "${entry.index}: ${entry.command}"
            } else {
                entry.command
            }
            lines.add(OutputLine(text, OutputStyle.CODE))
        }

        return CommandResult.success(lines)
    }

    /**
     * Tao duong ke phan cach.
     */
    private fun buildSeparator(length: Int): String {
        val effectiveLength = length.coerceIn(10, 60)
        return "-".repeat(effectiveLength)
    }

    /**
     * Muc lich su da danh chi muc.
     *
     * @property index Vi tri 1-based trong lich su goc.
     * @property command Chuoi lenh da thuc thi.
     */
    private data class IndexedEntry(
        val index: Int,
        val command: String
    )
}
