package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType

/**
 * Lenh quan ly alias (ten tat) cho phien lam viec console.
 *
 * Cho phep nguoi dung tao, xoa va liet ke cac alias de rut gon
 * cac lenh thuong dung. Alias duoc luu trong phien (session state)
 * cua ViewModel, khong persist qua cac lan khoi dong lai.
 *
 * Giao tiep voi ViewModel thong qua cac marker dac biet trong output:
 * - `__ALIAS_SET__:<name>=<expansion>` — tao hoac cap nhat alias
 * - `__ALIAS_REMOVE__:<name>` — xoa mot alias
 * - `__ALIAS_CLEAR__` — xoa tat ca alias
 *
 * Usage:
 * ```
 * alias                          # Liet ke tat ca alias
 * alias ll=ls -u --verbose       # Tao alias moi
 * alias --remove ll              # Xoa alias
 * alias --clear                  # Xoa tat ca alias
 * ```
 */
class AliasCommand : Command {

    override val name: String = "alias"

    override val description: String = "Quan ly alias (ten tat) cho cac lenh console"

    override val usage: String = "alias [<name>=<expansion>] [--remove <name>] [--clear]"

    override val category: String = "util"

    override val examples: List<Pair<String, String>> = listOf(
        "alias" to "Liet ke tat ca alias hien co",
        "alias ll=ls -u --verbose" to "Tao alias 'll' de chay 'ls -u --verbose'",
        "alias grep-quiz=grep --regex quiz" to "Tao alias cho lenh grep voi tham so co dinh",
        "alias --remove ll" to "Xoa alias 'll'",
        "alias --clear" to "Xoa tat ca alias"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        if (flags.isEmpty() && args.isEmpty()) {
            suggestions.add(
                CompletionSuggestion(
                    text = "--remove",
                    description = "Xoa mot alias",
                    type = SuggestionType.FLAG
                )
            )
            suggestions.add(
                CompletionSuggestion(
                    text = "--clear",
                    description = "Xoa tat ca alias",
                    type = SuggestionType.FLAG
                )
            )

            context.aliases.keys.forEach { name ->
                suggestions.add(
                    CompletionSuggestion(
                        text = "$name=",
                        displayText = name,
                        description = "Cap nhat alias '$name'",
                        type = SuggestionType.ARGUMENT
                    )
                )
            }
        }

        if ("remove" in flags || "r" in flags) {
            val currentRemoveValue = flags["remove"] ?: flags["r"] ?: ""
            context.aliases.keys
                .filter { it.startsWith(currentRemoveValue, ignoreCase = true) }
                .forEach { name ->
                    suggestions.add(
                        CompletionSuggestion(
                            text = name,
                            description = context.aliases[name] ?: "",
                            type = SuggestionType.ARGUMENT
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
        val wantClear = "clear" in flags
        val removeName = flags["remove"] ?: flags["r"]

        if (wantClear) {
            return handleClear(context)
        }

        if (removeName != null) {
            return handleRemove(removeName, context)
        }

        if (args.isEmpty()) {
            return handleList(context)
        }

        val definition = args.joinToString(" ")
        return handleSet(definition, context)
    }

    // ------------------------------------------------------------------
    // Internal handlers
    // ------------------------------------------------------------------

    /**
     * Liet ke tat ca alias hien co trong phien.
     */
    private fun handleList(context: CommandContext): CommandResult {
        val aliases = context.aliases
        if (aliases.isEmpty()) {
            return CommandResult.success(
                listOf(
                    OutputLine(
                        "Chua co alias nao duoc thiet lap.",
                        OutputStyle.MUTED
                    )
                )
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Danh sach alias (${aliases.size}):", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val maxNameLen = aliases.keys.maxOf { it.length }
        aliases.entries.sortedBy { it.key }.forEach { (name, expansion) ->
            val paddedName = name.padEnd(maxNameLen)
            lines.add(
                OutputLine("  $paddedName  ->  $expansion", OutputStyle.TABLE_ROW)
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Tao hoac cap nhat mot alias tu chuoi dang `name=expansion`.
     *
     * @param definition Chuoi dinh nghia alias, vi du `"ll=ls -u --verbose"`.
     */
    private fun handleSet(definition: String, context: CommandContext): CommandResult {
        val eqIndex = definition.indexOf('=')
        if (eqIndex < 1) {
            return CommandResult.error(
                "Cu phap khong hop le. Su dung: alias <ten>=<lenh mo rong>\n" +
                        "Vi du: alias ll=ls -u --verbose"
            )
        }

        val name = definition.substring(0, eqIndex).trim()
        val expansion = definition.substring(eqIndex + 1).trim()

        if (name.isEmpty()) {
            return CommandResult.error("Ten alias khong duoc de trong.")
        }

        if (expansion.isEmpty()) {
            return CommandResult.error(
                "Lenh mo rong khong duoc de trong. " +
                        "De xoa alias, su dung: alias --remove $name"
            )
        }

        if (!isValidAliasName(name)) {
            return CommandResult.error(
                "Ten alias '$name' khong hop le. " +
                        "Chi cho phep chu cai, so, dau gach ngang va gach duoi."
            )
        }

        if (name == expansion.split(" ").firstOrNull()) {
            return CommandResult.error(
                "Alias '$name' khong the tro den lenh bat dau bang chinh no " +
                        "(tranh vong lap vo han)."
            )
        }

        val isUpdate = name in context.aliases
        val verb = if (isUpdate) "Cap nhat" else "Tao"

        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "$verb alias: $name -> $expansion",
                OutputStyle.SUCCESS
            )
        )
        lines.add(
            OutputLine(
                "$MARKER_SET$name=$expansion"
            )
        )

        return CommandResult.success(lines)
    }

    /**
     * Xoa mot alias theo ten.
     *
     * @param name Ten alias can xoa.
     */
    private fun handleRemove(name: String, context: CommandContext): CommandResult {
        if (name !in context.aliases) {
            return CommandResult.error("Alias '$name' khong ton tai.")
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine("Da xoa alias: $name", OutputStyle.SUCCESS)
        )
        lines.add(
            OutputLine("$MARKER_REMOVE$name")
        )

        return CommandResult.success(lines)
    }

    /**
     * Xoa tat ca alias trong phien.
     */
    private fun handleClear(context: CommandContext): CommandResult {
        if (context.aliases.isEmpty()) {
            return CommandResult.success(
                listOf(
                    OutputLine(
                        "Khong co alias nao de xoa.",
                        OutputStyle.MUTED
                    )
                )
            )
        }

        val count = context.aliases.size
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine("Da xoa tat ca $count alias.", OutputStyle.SUCCESS)
        )
        lines.add(
            OutputLine(MARKER_CLEAR)
        )

        return CommandResult.success(lines)
    }

    /**
     * Kiem tra ten alias chi chua ky tu hop le (chu cai, so, `-`, `_`).
     */
    private fun isValidAliasName(name: String): Boolean {
        return name.matches(VALID_NAME_REGEX)
    }

    companion object {
        /**
         * Marker prefix cho lenh tao/cap nhat alias.
         * ViewModel can doc dong nay de cap nhat session state.
         * Format: `__ALIAS_SET__:<name>=<expansion>`
         */
        const val MARKER_SET = "__ALIAS_SET__:"

        /**
         * Marker prefix cho lenh xoa mot alias.
         * Format: `__ALIAS_REMOVE__:<name>`
         */
        const val MARKER_REMOVE = "__ALIAS_REMOVE__:"

        /**
         * Marker cho lenh xoa tat ca alias.
         */
        const val MARKER_CLEAR = "__ALIAS_CLEAR__"

        private val VALID_NAME_REGEX = Regex("^[a-zA-Z0-9_-]+$")
    }
}
