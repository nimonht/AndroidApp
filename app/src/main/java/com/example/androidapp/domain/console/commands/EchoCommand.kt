package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType

/**
 * Lenh echo — in lai van ban voi cac tuy chon dinh dang va bien doi.
 *
 * Ho tro cac co:
 * - `--style`: kieu hien thi (normal, success, error, warning, info, header, muted, code)
 * - `--repeat` / `-r`: lap lai van ban N lan
 * - `--upper`: chuyen thanh chu hoa
 * - `--lower`: chuyen thanh chu thuong
 * - `--trim`: loai bo khoang trang thua
 * - `--timestamp`: them thoi gian hien tai vao dau dong
 * - `--no-newline`: gop tat ca thanh mot dong duy nhat
 *
 * Neu khong co tham so, doc tu pipe input (neu co).
 */
class EchoCommand : Command {

    override val name: String = "echo"

    override val aliases: List<String> = listOf("print")

    override val description: String = "In van ban ra console voi cac tuy chon dinh dang"

    override val usage: String = "echo [van_ban...] [--style <kieu>] [--repeat <n>] [--upper] [--lower] [--trim] [--timestamp] [--no-newline]"

    override val category: String = "util"

    override val examples: List<Pair<String, String>> = listOf(
        "echo Xin chao" to "In ra 'Xin chao'",
        "echo --style success Thanh cong!" to "In voi kieu thanh cong (mau xanh)",
        "echo -r 3 Ha ha" to "Lap lai 'Ha ha' 3 lan",
        "echo --upper xin chao" to "In ra 'XIN CHAO'",
        "echo --lower XIN CHAO" to "In ra 'xin chao'",
        "echo --timestamp Bat dau xu ly" to "In kem thoi gian hien tai",
        "echo --no-newline A B C" to "Gop tat ca thanh mot dong"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val lastArg = args.lastOrNull()?.lowercase() ?: ""

        val allFlags = listOf(
            CompletionSuggestion("--style", description = "Kieu hien thi (normal/success/error/warning/info/header/muted/code)", type = SuggestionType.FLAG),
            CompletionSuggestion("--repeat", description = "Lap lai van ban N lan", type = SuggestionType.FLAG),
            CompletionSuggestion("-r", displayText = "-r (--repeat)", description = "Lap lai van ban N lan", type = SuggestionType.FLAG),
            CompletionSuggestion("--upper", description = "Chuyen thanh chu hoa", type = SuggestionType.FLAG),
            CompletionSuggestion("--lower", description = "Chuyen thanh chu thuong", type = SuggestionType.FLAG),
            CompletionSuggestion("--trim", description = "Loai bo khoang trang thua", type = SuggestionType.FLAG),
            CompletionSuggestion("--timestamp", description = "Them thoi gian vao dau dong", type = SuggestionType.FLAG),
            CompletionSuggestion("--no-newline", description = "Gop tat ca thanh mot dong", type = SuggestionType.FLAG)
        )

        // If user is completing a --style value, suggest styles
        if (flags.containsKey("style") && flags["style"] == null) {
            return STYLE_NAMES.map { styleName ->
                CompletionSuggestion(styleName, description = "Kieu $styleName", type = SuggestionType.ARGUMENT)
            }
        }

        if (lastArg.startsWith("-")) {
            return allFlags.filter {
                it.text.startsWith(lastArg) && !flags.containsKey(it.text.removePrefix("--"))
            }
        }

        return allFlags.filter { !flags.containsKey(it.text.removePrefix("--").removePrefix("-")) }
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        // Resolve text: from args, or from pipe input, or empty
        val rawText = if (args.isNotEmpty()) {
            args.joinToString(" ")
        } else if (!context.pipeInput.isNullOrEmpty()) {
            context.pipeInput.joinToString("\n")
        } else {
            ""
        }

        if (rawText.isEmpty() && flags.isEmpty()) {
            return CommandResult.success("")
        }

        // Apply text transformations
        var text = rawText

        if (flags.containsKey("trim")) {
            text = text.trim().replace(Regex("\\s+"), " ")
        }

        if (flags.containsKey("upper")) {
            text = text.uppercase()
        } else if (flags.containsKey("lower")) {
            text = text.lowercase()
        }

        if (flags.containsKey("timestamp")) {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            text = "[$now] $text"
        }

        // Determine repeat count
        val repeatCount = (flags["repeat"] ?: flags["r"])?.toIntOrNull() ?: 1
        if (repeatCount < 1) {
            return CommandResult.error("Loi: so lan lap phai lon hon 0")
        }
        if (repeatCount > 100) {
            return CommandResult.error("Loi: so lan lap toi da la 100")
        }

        // Determine output style
        val style = resolveStyle(flags["style"])

        // Build output lines
        val noNewline = flags.containsKey("no-newline")

        if (noNewline) {
            // Combine all repetitions into a single line
            val combined = (1..repeatCount).joinToString(" ") { text }
            return CommandResult.success(listOf(OutputLine(combined, style)))
        }

        val lines = (1..repeatCount).map { OutputLine(text, style) }
        return CommandResult.success(lines)
    }

    /**
     * Phan giai ten kieu thanh [OutputStyle].
     *
     * @param styleName Ten kieu (khong phan biet hoa thuong), hoac null de dung mac dinh.
     * @return [OutputStyle] tuong ung.
     */
    private fun resolveStyle(styleName: String?): OutputStyle {
        if (styleName == null) return OutputStyle.NORMAL
        return when (styleName.lowercase()) {
            "normal" -> OutputStyle.NORMAL
            "success" -> OutputStyle.SUCCESS
            "error" -> OutputStyle.ERROR
            "warning", "warn" -> OutputStyle.WARNING
            "info" -> OutputStyle.INFO
            "header" -> OutputStyle.HEADER
            "muted" -> OutputStyle.MUTED
            "code" -> OutputStyle.CODE
            "table-header", "table_header" -> OutputStyle.TABLE_HEADER
            "table-row", "table_row" -> OutputStyle.TABLE_ROW
            else -> OutputStyle.NORMAL
        }
    }

    private companion object {
        val STYLE_NAMES = listOf(
            "normal", "success", "error", "warning", "info",
            "header", "muted", "code", "table-header", "table-row"
        )
    }
}
