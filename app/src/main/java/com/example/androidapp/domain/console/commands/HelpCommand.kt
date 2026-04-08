package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandRegistry
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.UserRole

/**
 * Lenh `help` — hien thi danh sach lenh hoac huong dan chi tiet cho mot lenh cu the.
 *
 * Khi goi khong co tham so, liet ke tat ca cac lenh ma nguoi dung hien tai
 * co quyen truy cap, nhom theo danh muc. Khi truyen ten lenh lam doi so,
 * hien thi huong dan su dung chi tiet cho lenh do.
 *
 * @param registry Tham chieu den [CommandRegistry] de tra cuu lenh.
 */
class HelpCommand(
    private val registry: CommandRegistry
) : Command {

    override val name: String = "help"

    override val aliases: List<String> = listOf("h", "?")

    override val description: String = "Hien thi danh sach lenh hoac huong dan chi tiet cho mot lenh"

    override val usage: String =
        "help [<lenh>] [--all] [--category <danh-muc>] [--search <tu-khoa>] [--flags] [--examples] [--format <dinh-dang>]"

    override val category: String = "general"

    override val examples: List<Pair<String, String>> = listOf(
        "help" to "Liet ke tat ca lenh kha dung",
        "help ping" to "Xem huong dan chi tiet lenh ping",
        "help --all" to "Hien thi tat ca lenh ke ca lenh bi khoa",
        "help --category system" to "Chi hien thi lenh trong danh muc 'system'",
        "help --search sync" to "Tim lenh co chua tu khoa 'sync'",
        "help ping --examples" to "Xem vi du su dung lenh ping",
        "help ping --flags" to "Xem danh sach co (flag) cua lenh ping"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        if (args.size <= 1) {
            val prefix = args.firstOrNull()?.lowercase() ?: ""
            val visibleCommands = registry.commandsForRole(context.currentUser.role)
            for (cmd in visibleCommands) {
                if (cmd.name.lowercase().startsWith(prefix)) {
                    suggestions.add(
                        CompletionSuggestion(
                            text = cmd.name,
                            description = cmd.description,
                            type = SuggestionType.COMMAND
                        )
                    )
                }
            }
        }

        if (args.isEmpty() || (args.size == 1 && args[0].startsWith("-"))) {
            val flagSuggestions = listOf(
                "--all" to "Hien thi ca lenh bi khoa",
                "--category" to "Loc theo danh muc",
                "--search" to "Tim kiem lenh",
                "--flags" to "Hien thi danh sach co",
                "--examples" to "Hien thi vi du",
                "--format" to "Dinh dang dau ra (table/json)"
            )
            for ((flag, desc) in flagSuggestions) {
                suggestions.add(
                    CompletionSuggestion(
                        text = flag,
                        description = desc,
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        return suggestions
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val showAll = flags.containsKey("all")
        val categoryFilter = flags["category"]
        val searchQuery = flags["search"]
        val format = flags["format"] ?: "table"
        val showFlags = flags.containsKey("flags")
        val showExamples = flags.containsKey("examples")

        // Search mode
        if (searchQuery != null) {
            return executeSearch(searchQuery, context.currentUser.role, format)
        }

        // Detailed help for a specific command
        if (args.isNotEmpty()) {
            val commandName = args[0]
            val command = registry.resolve(commandName)
                ?: return CommandResult.error("Khong tim thay lenh: '$commandName'. Dung 'help' de xem danh sach lenh.")
            return formatDetailedHelp(command, context.currentUser.role, showFlags, showExamples, format)
        }

        // List all commands
        return formatCommandList(context.currentUser.role, showAll, categoryFilter, format)
    }

    /**
     * Dinh dang danh sach lenh nhom theo danh muc.
     */
    private fun formatCommandList(
        role: UserRole,
        showAll: Boolean,
        categoryFilter: String?,
        format: String
    ): CommandResult {
        val allCommands = registry.allCommands()
        val visibleCommands = if (showAll) {
            allCommands
        } else {
            allCommands.filter { role.ordinal >= it.minimumRole.ordinal }
        }

        val filtered = if (categoryFilter != null) {
            visibleCommands.filter { it.category.equals(categoryFilter, ignoreCase = true) }
        } else {
            visibleCommands
        }

        if (filtered.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong co lenh nao kha dung.", OutputStyle.WARNING))
            )
        }

        if (format == "json") {
            return formatCommandListJson(filtered, role, showAll)
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("=== Danh sach lenh ===", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val grouped = filtered.groupBy { it.category }.toSortedMap()
        for ((category, commands) in grouped) {
            val categoryLabel = categoryDisplayName(category)
            lines.add(OutputLine("[$categoryLabel]", OutputStyle.INFO))

            for (cmd in commands.sortedBy { it.name }) {
                val locked = if (showAll && role.ordinal < cmd.minimumRole.ordinal) " [khoa]" else ""
                val nameStr = cmd.name.padEnd(16)
                lines.add(OutputLine("  $nameStr${cmd.description}$locked", OutputStyle.TABLE_ROW))
            }
            lines.add(OutputLine(""))
        }

        val total = filtered.size
        val hint = if (showAll) {
            "Tong cong: $total lenh (bao gom lenh bi khoa)"
        } else {
            "Tong cong: $total lenh. Dung 'help --all' de xem tat ca."
        }
        lines.add(OutputLine(hint, OutputStyle.MUTED))
        lines.add(OutputLine("Dung 'help <lenh>' de xem chi tiet.", OutputStyle.MUTED))

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang danh sach lenh dang JSON.
     */
    private fun formatCommandListJson(
        commands: List<Command>,
        role: UserRole,
        showAll: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[", OutputStyle.CODE))
        commands.forEachIndexed { index, cmd ->
            val locked = showAll && role.ordinal < cmd.minimumRole.ordinal
            val comma = if (index < commands.size - 1) "," else ""
            lines.add(OutputLine("  {", OutputStyle.CODE))
            lines.add(OutputLine("    \"name\": \"${cmd.name}\",", OutputStyle.CODE))
            lines.add(OutputLine("    \"category\": \"${cmd.category}\",", OutputStyle.CODE))
            lines.add(OutputLine("    \"description\": \"${cmd.description}\",", OutputStyle.CODE))
            lines.add(OutputLine("    \"minimumRole\": \"${cmd.minimumRole.name}\",", OutputStyle.CODE))
            lines.add(OutputLine("    \"locked\": $locked", OutputStyle.CODE))
            lines.add(OutputLine("  }$comma", OutputStyle.CODE))
        }
        lines.add(OutputLine("]", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Dinh dang huong dan chi tiet cho mot lenh.
     */
    private fun formatDetailedHelp(
        command: Command,
        role: UserRole,
        showFlags: Boolean,
        showExamples: Boolean,
        format: String
    ): CommandResult {
        if (format == "json") {
            return formatDetailedHelpJson(command)
        }

        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("=== ${command.name} ===", OutputStyle.HEADER))
        lines.add(OutputLine(""))
        lines.add(OutputLine("Mo ta:      ${command.description}", OutputStyle.NORMAL))
        lines.add(OutputLine("Su dung:    ${command.usage}", OutputStyle.CODE))
        lines.add(OutputLine("Danh muc:   ${categoryDisplayName(command.category)}", OutputStyle.NORMAL))
        lines.add(OutputLine("Quyen toi thieu: ${command.minimumRole.name}", OutputStyle.NORMAL))

        if (command.aliases.isNotEmpty()) {
            lines.add(OutputLine("Bi danh:    ${command.aliases.joinToString(", ")}", OutputStyle.NORMAL))
        }

        if (command.isDestructive) {
            lines.add(
                OutputLine(
                    "Canh bao:   Lenh nay co the thay doi du lieu khong the hoan tac!",
                    OutputStyle.WARNING
                )
            )
        }

        if (command.requiredPermission != null) {
            lines.add(OutputLine("Quyen admin: ${command.requiredPermission!!.name}", OutputStyle.NORMAL))
        }

        val isAccessible = role.ordinal >= command.minimumRole.ordinal
        if (!isAccessible) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("Ban khong co quyen su dung lenh nay.", OutputStyle.WARNING))
        }

        if (showFlags) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("Xem 'help ${command.name}' de biet cac co ho tro.", OutputStyle.MUTED))
        }

        if (showExamples || command.examples.isNotEmpty()) {
            val exampleList = command.examples
            if (exampleList.isNotEmpty()) {
                lines.add(OutputLine(""))
                lines.add(OutputLine("Vi du:", OutputStyle.INFO))
                for ((example, desc) in exampleList) {
                    lines.add(OutputLine("  $ $example", OutputStyle.CODE))
                    lines.add(OutputLine("    $desc", OutputStyle.MUTED))
                }
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang huong dan chi tiet dang JSON.
     */
    private fun formatDetailedHelpJson(command: Command): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"name\": \"${command.name}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"aliases\": [${command.aliases.joinToString(", ") { "\"$it\"" }}],", OutputStyle.CODE))
        lines.add(OutputLine("  \"description\": \"${command.description}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"usage\": \"${command.usage}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"category\": \"${command.category}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"minimumRole\": \"${command.minimumRole.name}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"isDestructive\": ${command.isDestructive},", OutputStyle.CODE))
        if (command.examples.isNotEmpty()) {
            lines.add(OutputLine("  \"examples\": [", OutputStyle.CODE))
            command.examples.forEachIndexed { index, (ex, desc) ->
                val comma = if (index < command.examples.size - 1) "," else ""
                lines.add(OutputLine("    {\"command\": \"$ex\", \"description\": \"$desc\"}$comma", OutputStyle.CODE))
            }
            lines.add(OutputLine("  ]", OutputStyle.CODE))
        }
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Tim kiem lenh theo tu khoa.
     */
    private fun executeSearch(query: String, role: UserRole, format: String): CommandResult {
        val results = registry.searchCommands(query, role)

        if (results.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong tim thay lenh nao voi tu khoa '$query'.", OutputStyle.WARNING))
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Ket qua tim kiem cho '$query': ${results.size} lenh", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        if (format == "json") {
            lines.add(OutputLine("[", OutputStyle.CODE))
            results.forEachIndexed { index, cmd ->
                val comma = if (index < results.size - 1) "," else ""
                lines.add(
                    OutputLine(
                        "  {\"name\": \"${cmd.name}\", \"description\": \"${cmd.description}\"}$comma",
                        OutputStyle.CODE
                    )
                )
            }
            lines.add(OutputLine("]", OutputStyle.CODE))
        } else {
            for (cmd in results) {
                val nameStr = cmd.name.padEnd(16)
                lines.add(OutputLine("  $nameStr${cmd.description}", OutputStyle.TABLE_ROW))
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Chuyen ma danh muc thanh ten hien thi tieng Viet.
     */
    private fun categoryDisplayName(category: String): String {
        return when (category.lowercase()) {
            "general" -> "Chung"
            "system" -> "He thong"
            "util" -> "Tien ich"
            "user" -> "Nguoi dung"
            "admin" -> "Quan tri"
            "quiz" -> "Quiz"
            "pipe" -> "Pipe"
            else -> category.replaceFirstChar { it.uppercaseChar() }
        }
    }
}
