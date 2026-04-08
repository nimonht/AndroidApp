package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh quan ly cau hinh ung dung tu console.
 *
 * Ho tro cac lenh con:
 * - `config get <key>` — Xem gia tri cau hinh hien tai.
 * - `config set <key> <value>` — Thay doi gia tri cau hinh.
 * - `config reset <key>` — Dat lai gia tri mac dinh.
 * - `config list` — Liet ke tat ca cac khoa cau hinh.
 *
 * Cac khoa kha dung: `dark_theme`, `auto_sync`, `wifi_only`.
 */
class ConfigCommand : Command {

    override val name: String = "config"

    override val aliases: List<String> = listOf("cfg", "settings")

    override val description: String = "Quan ly cau hinh ung dung"

    override val usage: String =
        "config <get|set|reset|list> [key] [value] [--format <text|json>] [--verbose] [--keys] [--diff] [--export]"

    override val minimumRole: UserRole = UserRole.USER

    override val category: String = "system"

    override val examples: List<Pair<String, String>> = listOf(
        "config list" to "Liet ke tat ca cac khoa cau hinh va gia tri hien tai",
        "config get dark_theme" to "Xem che do giao dien hien tai",
        "config set dark_theme dark" to "Chuyen sang giao dien toi",
        "config set auto_sync true" to "Bat dong bo tu dong",
        "config set wifi_only true" to "Chi dong bo qua WiFi",
        "config reset dark_theme" to "Dat lai che do giao dien ve mac dinh (system)",
        "config list --format json" to "Xuat cau hinh duoi dang JSON",
        "config list --export" to "Xuat toan bo cau hinh de sao luu"
    )

    companion object {
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_WIFI_ONLY = "wifi_only"

        private const val THEME_MODE_SYSTEM = 0
        private const val THEME_MODE_LIGHT = 1
        private const val THEME_MODE_DARK = 2

        private val VALID_KEYS = setOf(KEY_DARK_THEME, KEY_AUTO_SYNC, KEY_WIFI_ONLY)

        private val KEY_DESCRIPTIONS = mapOf(
            KEY_DARK_THEME to "Che do giao dien (system/light/dark)",
            KEY_AUTO_SYNC to "Dong bo tu dong (true/false)",
            KEY_WIFI_ONLY to "Chi dong bo qua WiFi (true/false)"
        )

        private val DEFAULT_VALUES = mapOf(
            KEY_DARK_THEME to "system",
            KEY_AUTO_SYNC to "true",
            KEY_WIFI_ONLY to "false"
        )

        private val VALID_THEME_VALUES = setOf("system", "light", "dark")
        private val VALID_BOOL_VALUES = setOf("true", "false")
    }

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        return when {
            args.isEmpty() -> listOf(
                CompletionSuggestion("get", description = "Xem gia tri cau hinh", type = SuggestionType.SUBCOMMAND),
                CompletionSuggestion("set", description = "Thay doi gia tri cau hinh", type = SuggestionType.SUBCOMMAND),
                CompletionSuggestion("reset", description = "Dat lai gia tri mac dinh", type = SuggestionType.SUBCOMMAND),
                CompletionSuggestion("list", description = "Liet ke tat ca cau hinh", type = SuggestionType.SUBCOMMAND)
            )
            args.size == 1 -> {
                val prefix = args[0].lowercase()
                listOf("get", "set", "reset", "list")
                    .filter { it.startsWith(prefix) }
                    .map {
                        CompletionSuggestion(
                            it,
                            description = when (it) {
                                "get" -> "Xem gia tri cau hinh"
                                "set" -> "Thay doi gia tri cau hinh"
                                "reset" -> "Dat lai gia tri mac dinh"
                                "list" -> "Liet ke tat ca cau hinh"
                                else -> ""
                            },
                            type = SuggestionType.SUBCOMMAND
                        )
                    }
            }
            args.size == 2 && args[0].lowercase() in setOf("get", "set", "reset") -> {
                val prefix = args[1].lowercase()
                VALID_KEYS.filter { it.startsWith(prefix) }
                    .map {
                        CompletionSuggestion(
                            it,
                            description = KEY_DESCRIPTIONS[it] ?: "",
                            type = SuggestionType.ARGUMENT
                        )
                    }
            }
            args.size == 3 && args[0].lowercase() == "set" -> {
                val key = args[1].lowercase()
                val prefix = args[2].lowercase()
                when (key) {
                    KEY_DARK_THEME -> VALID_THEME_VALUES
                    KEY_AUTO_SYNC, KEY_WIFI_ONLY -> VALID_BOOL_VALUES
                    else -> emptySet()
                }.filter { it.startsWith(prefix) }
                    .map { CompletionSuggestion(it, type = SuggestionType.ARGUMENT) }
            }
            else -> emptyList()
        }
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error(
                "Thieu lenh con. Su dung: config <get|set|reset|list>\n" +
                    "Dung 'help config' de xem huong dan chi tiet."
            )
        }

        val subcommand = args[0].lowercase()
        val format = flags["format"]?.lowercase() ?: "text"

        return when (subcommand) {
            "list" -> executeList(flags, context, format)
            "get" -> executeGet(args, flags, context, format)
            "set" -> executeSet(args, context, format)
            "reset" -> executeReset(args, context, format)
            else -> CommandResult.error(
                "Lenh con khong hop le: '$subcommand'. Cac lenh con kha dung: get, set, reset, list"
            )
        }
    }

    /**
     * Liet ke tat ca cac khoa cau hinh va gia tri hien tai.
     */
    private suspend fun executeList(
        flags: Map<String, String?>,
        context: CommandContext,
        format: String
    ): CommandResult {
        val prefs = context.services.settingsPreferences
        val currentValues = readAllValues(prefs)

        val showKeysOnly = flags.containsKey("keys")
        val showDiff = flags.containsKey("diff")
        val showExport = flags.containsKey("export")
        val verbose = flags.containsKey("verbose")

        if (showKeysOnly) {
            return if (format == "json") {
                val jsonArray = VALID_KEYS.joinToString(", ") { "\"$it\"" }
                CommandResult.success("[$jsonArray]")
            } else {
                val lines = mutableListOf(
                    OutputLine("Cac khoa cau hinh kha dung:", OutputStyle.HEADER)
                )
                VALID_KEYS.sorted().forEach { key ->
                    lines.add(OutputLine("  $key", OutputStyle.INFO))
                }
                CommandResult.success(lines)
            }
        }

        if (showExport) {
            return executeExport(currentValues, format)
        }

        if (format == "json") {
            return formatListAsJson(currentValues, showDiff, verbose)
        }

        return formatListAsTable(currentValues, showDiff, verbose)
    }

    /**
     * Xem gia tri cau hinh cho mot khoa cu the.
     */
    private suspend fun executeGet(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext,
        format: String
    ): CommandResult {
        if (args.size < 2) {
            return CommandResult.error(
                "Thieu ten khoa. Su dung: config get <key>\nCac khoa kha dung: ${VALID_KEYS.joinToString(", ")}"
            )
        }

        val key = args[1].lowercase()
        if (key !in VALID_KEYS) {
            return CommandResult.error(
                "Khoa khong hop le: '$key'\nCac khoa kha dung: ${VALID_KEYS.joinToString(", ")}"
            )
        }

        val prefs = context.services.settingsPreferences
        val value = readValue(prefs, key)
        val defaultValue = DEFAULT_VALUES[key] ?: ""
        val verbose = flags.containsKey("verbose")

        if (format == "json") {
            val json = buildString {
                append("{")
                append("\"key\": \"$key\", ")
                append("\"value\": \"$value\", ")
                append("\"default\": \"$defaultValue\"")
                if (verbose) {
                    append(", \"description\": \"${KEY_DESCRIPTIONS[key] ?: ""}\"")
                    append(", \"is_default\": ${value == defaultValue}")
                }
                append("}")
            }
            return CommandResult.success(json)
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("$key = $value", OutputStyle.INFO))
        if (verbose) {
            lines.add(OutputLine("  Mo ta: ${KEY_DESCRIPTIONS[key] ?: "Khong co"}", OutputStyle.MUTED))
            lines.add(OutputLine("  Mac dinh: $defaultValue", OutputStyle.MUTED))
            if (value != defaultValue) {
                lines.add(OutputLine("  Trang thai: Da thay doi", OutputStyle.WARNING))
            } else {
                lines.add(OutputLine("  Trang thai: Mac dinh", OutputStyle.MUTED))
            }
        }
        return CommandResult.success(lines)
    }

    /**
     * Thay doi gia tri cau hinh.
     */
    private suspend fun executeSet(
        args: List<String>,
        context: CommandContext,
        format: String
    ): CommandResult {
        if (args.size < 2) {
            return CommandResult.error(
                "Thieu ten khoa. Su dung: config set <key> <value>"
            )
        }
        if (args.size < 3) {
            return CommandResult.error(
                "Thieu gia tri. Su dung: config set ${args[1]} <value>"
            )
        }

        val key = args[1].lowercase()
        val newValue = args[2].lowercase()

        if (key !in VALID_KEYS) {
            return CommandResult.error(
                "Khoa khong hop le: '$key'\nCac khoa kha dung: ${VALID_KEYS.joinToString(", ")}"
            )
        }

        val validationError = validateValue(key, newValue)
        if (validationError != null) {
            return CommandResult.error(validationError)
        }

        val prefs = context.services.settingsPreferences
        val oldValue = readValue(prefs, key)

        if (oldValue == newValue) {
            return if (format == "json") {
                CommandResult.success("{\"key\": \"$key\", \"value\": \"$newValue\", \"changed\": false}")
            } else {
                CommandResult.success(listOf(
                    OutputLine("Gia tri '$key' da la '$newValue', khong can thay doi.", OutputStyle.WARNING)
                ))
            }
        }

        writeValue(prefs, key, newValue)

        return if (format == "json") {
            CommandResult.success(
                "{\"key\": \"$key\", \"old_value\": \"$oldValue\", \"new_value\": \"$newValue\", \"changed\": true}"
            )
        } else {
            CommandResult.success(listOf(
                OutputLine("Da cap nhat cau hinh:", OutputStyle.SUCCESS),
                OutputLine("  $key: $oldValue -> $newValue", OutputStyle.INFO)
            ))
        }
    }

    /**
     * Dat lai gia tri cau hinh ve mac dinh.
     */
    private suspend fun executeReset(
        args: List<String>,
        context: CommandContext,
        format: String
    ): CommandResult {
        if (args.size < 2) {
            return CommandResult.error(
                "Thieu ten khoa. Su dung: config reset <key>\n" +
                    "Dung 'config reset all' de dat lai tat ca."
            )
        }

        val key = args[1].lowercase()

        if (key == "all") {
            return resetAll(context, format)
        }

        if (key !in VALID_KEYS) {
            return CommandResult.error(
                "Khoa khong hop le: '$key'\nCac khoa kha dung: ${VALID_KEYS.joinToString(", ")}, all"
            )
        }

        val prefs = context.services.settingsPreferences
        val oldValue = readValue(prefs, key)
        val defaultValue = DEFAULT_VALUES[key] ?: ""

        if (oldValue == defaultValue) {
            return if (format == "json") {
                CommandResult.success("{\"key\": \"$key\", \"value\": \"$defaultValue\", \"was_default\": true}")
            } else {
                CommandResult.success(listOf(
                    OutputLine("'$key' da o gia tri mac dinh ($defaultValue).", OutputStyle.WARNING)
                ))
            }
        }

        writeValue(prefs, key, defaultValue)

        return if (format == "json") {
            CommandResult.success(
                "{\"key\": \"$key\", \"old_value\": \"$oldValue\", \"new_value\": \"$defaultValue\", \"reset\": true}"
            )
        } else {
            CommandResult.success(listOf(
                OutputLine("Da dat lai cau hinh:", OutputStyle.SUCCESS),
                OutputLine("  $key: $oldValue -> $defaultValue (mac dinh)", OutputStyle.INFO)
            ))
        }
    }

    /**
     * Dat lai tat ca cac gia tri cau hinh ve mac dinh.
     */
    private suspend fun resetAll(
        context: CommandContext,
        format: String
    ): CommandResult {
        val prefs = context.services.settingsPreferences
        val oldValues = readAllValues(prefs)
        val changes = mutableListOf<Triple<String, String, String>>()

        for (key in VALID_KEYS) {
            val defaultValue = DEFAULT_VALUES[key] ?: continue
            val currentValue = oldValues[key] ?: continue
            if (currentValue != defaultValue) {
                writeValue(prefs, key, defaultValue)
                changes.add(Triple(key, currentValue, defaultValue))
            }
        }

        if (changes.isEmpty()) {
            return if (format == "json") {
                CommandResult.success("{\"reset_count\": 0, \"message\": \"Tat ca da o gia tri mac dinh\"}")
            } else {
                CommandResult.success(listOf(
                    OutputLine("Tat ca cau hinh da o gia tri mac dinh.", OutputStyle.WARNING)
                ))
            }
        }

        return if (format == "json") {
            val changesJson = changes.joinToString(", ") { (key, old, new) ->
                "{\"key\": \"$key\", \"old\": \"$old\", \"new\": \"$new\"}"
            }
            CommandResult.success("{\"reset_count\": ${changes.size}, \"changes\": [$changesJson]}")
        } else {
            val lines = mutableListOf<OutputLine>(
                OutputLine("Da dat lai ${changes.size} cau hinh ve mac dinh:", OutputStyle.SUCCESS)
            )
            changes.forEach { (key, old, new) ->
                lines.add(OutputLine("  $key: $old -> $new", OutputStyle.INFO))
            }
            CommandResult.success(lines)
        }
    }

    /**
     * Xuat toan bo cau hinh duoi dang co the nhap lai.
     */
    private fun executeExport(
        currentValues: Map<String, String>,
        format: String
    ): CommandResult {
        if (format == "json") {
            val entries = currentValues.entries.joinToString(", ") { (k, v) ->
                "\"$k\": \"$v\""
            }
            return CommandResult.success("{$entries}")
        }

        val lines = mutableListOf<OutputLine>(
            OutputLine("# Xuat cau hinh Quizzez", OutputStyle.MUTED),
            OutputLine("# Dung 'config set <key> <value>' de nhap lai", OutputStyle.MUTED),
            OutputLine("", OutputStyle.NORMAL)
        )
        currentValues.entries.sortedBy { it.key }.forEach { (key, value) ->
            lines.add(OutputLine("config set $key $value", OutputStyle.CODE))
        }
        return CommandResult.success(lines)
    }

    /**
     * Dinh dang danh sach cau hinh dang bang.
     */
    private fun formatListAsTable(
        values: Map<String, String>,
        showDiff: Boolean,
        verbose: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Cau hinh ung dung", OutputStyle.HEADER))
        lines.add(OutputLine("", OutputStyle.NORMAL))

        if (verbose) {
            lines.add(
                OutputLine(
                    padEnd("Khoa", 16) + padEnd("Gia tri", 12) +
                        padEnd("Mac dinh", 12) + "Mo ta",
                    OutputStyle.TABLE_HEADER
                )
            )
            lines.add(OutputLine("-".repeat(70), OutputStyle.MUTED))
        } else {
            lines.add(
                OutputLine(
                    padEnd("Khoa", 16) + padEnd("Gia tri", 12) + "Mac dinh",
                    OutputStyle.TABLE_HEADER
                )
            )
            lines.add(OutputLine("-".repeat(40), OutputStyle.MUTED))
        }

        values.entries.sortedBy { it.key }.forEach { (key, value) ->
            val defaultValue = DEFAULT_VALUES[key] ?: ""
            val isChanged = value != defaultValue

            if (showDiff && !isChanged) return@forEach

            val row = if (verbose) {
                padEnd(key, 16) + padEnd(value, 12) +
                    padEnd(defaultValue, 12) + (KEY_DESCRIPTIONS[key] ?: "")
            } else {
                padEnd(key, 16) + padEnd(value, 12) + defaultValue
            }

            val style = if (isChanged) OutputStyle.WARNING else OutputStyle.TABLE_ROW
            lines.add(OutputLine(row, style))
        }

        if (showDiff) {
            val changedCount = values.count { (k, v) -> v != DEFAULT_VALUES[k] }
            lines.add(OutputLine("", OutputStyle.NORMAL))
            if (changedCount == 0) {
                lines.add(OutputLine("Khong co cau hinh nao bi thay doi.", OutputStyle.MUTED))
            } else {
                lines.add(OutputLine("$changedCount cau hinh da thay doi so voi mac dinh.", OutputStyle.INFO))
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang danh sach cau hinh dang JSON.
     */
    private fun formatListAsJson(
        values: Map<String, String>,
        showDiff: Boolean,
        verbose: Boolean
    ): CommandResult {
        val entries = values.entries
            .sortedBy { it.key }
            .let { list ->
                if (showDiff) list.filter { (k, v) -> v != DEFAULT_VALUES[k] }
                else list
            }

        val jsonEntries = entries.joinToString(",\n  ") { (key, value) ->
            if (verbose) {
                val defaultValue = DEFAULT_VALUES[key] ?: ""
                val desc = KEY_DESCRIPTIONS[key] ?: ""
                "\"$key\": {\"value\": \"$value\", \"default\": \"$defaultValue\", " +
                    "\"description\": \"$desc\", \"is_default\": ${value == defaultValue}}"
            } else {
                "\"$key\": \"$value\""
            }
        }

        return CommandResult.success("{\n  $jsonEntries\n}")
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Doc gia tri hien tai cua tat ca cac khoa.
     */
    private suspend fun readAllValues(
        prefs: com.example.androidapp.data.preferences.SettingsPreferences
    ): Map<String, String> {
        val themeMode = prefs.darkThemeMode.first()
        val autoSync = prefs.autoSyncEnabled.first()
        val wifiOnly = prefs.wifiOnlySync.first()

        return mapOf(
            KEY_DARK_THEME to themeModeToString(themeMode),
            KEY_AUTO_SYNC to autoSync.toString(),
            KEY_WIFI_ONLY to wifiOnly.toString()
        )
    }

    /**
     * Doc gia tri hien tai cua mot khoa cu the.
     */
    private suspend fun readValue(
        prefs: com.example.androidapp.data.preferences.SettingsPreferences,
        key: String
    ): String {
        return when (key) {
            KEY_DARK_THEME -> themeModeToString(prefs.darkThemeMode.first())
            KEY_AUTO_SYNC -> prefs.autoSyncEnabled.first().toString()
            KEY_WIFI_ONLY -> prefs.wifiOnlySync.first().toString()
            else -> ""
        }
    }

    /**
     * Ghi gia tri moi cho mot khoa cu the.
     */
    private suspend fun writeValue(
        prefs: com.example.androidapp.data.preferences.SettingsPreferences,
        key: String,
        value: String
    ) {
        when (key) {
            KEY_DARK_THEME -> prefs.setDarkThemeMode(stringToThemeMode(value))
            KEY_AUTO_SYNC -> prefs.setAutoSyncEnabled(value.toBooleanStrictOrNull() ?: true)
            KEY_WIFI_ONLY -> prefs.setWifiOnlySync(value.toBooleanStrictOrNull() ?: false)
        }
    }

    /**
     * Kiem tra tinh hop le cua gia tri cho mot khoa cu the.
     *
     * @return Thong bao loi neu khong hop le, hoac `null` neu hop le.
     */
    private fun validateValue(key: String, value: String): String? {
        return when (key) {
            KEY_DARK_THEME -> {
                if (value !in VALID_THEME_VALUES) {
                    "Gia tri khong hop le cho '$key': '$value'\n" +
                        "Cac gia tri hop le: ${VALID_THEME_VALUES.joinToString(", ")}"
                } else null
            }
            KEY_AUTO_SYNC, KEY_WIFI_ONLY -> {
                if (value !in VALID_BOOL_VALUES) {
                    "Gia tri khong hop le cho '$key': '$value'\n" +
                        "Cac gia tri hop le: true, false"
                } else null
            }
            else -> "Khoa khong hop le: '$key'"
        }
    }

    /**
     * Chuyen doi gia tri int cua che do giao dien sang chuoi.
     */
    private fun themeModeToString(mode: Int): String = when (mode) {
        THEME_MODE_LIGHT -> "light"
        THEME_MODE_DARK -> "dark"
        else -> "system"
    }

    /**
     * Chuyen doi chuoi che do giao dien sang gia tri int.
     */
    private fun stringToThemeMode(value: String): Int = when (value.lowercase()) {
        "light" -> THEME_MODE_LIGHT
        "dark" -> THEME_MODE_DARK
        else -> THEME_MODE_SYSTEM
    }

    /**
     * Them khoang trang vao ben phai chuoi de can chinh cot.
     */
    private fun padEnd(text: String, length: Int): String {
        return if (text.length >= length) "$text " else text + " ".repeat(length - text.length)
    }
}
