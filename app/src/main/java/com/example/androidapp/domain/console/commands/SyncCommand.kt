package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Lenh dong bo du lieu giua thiet bi va cloud.
 *
 * Ho tro cac lenh con:
 * - `sync now` — Bat dau dong bo toan bo ngay lap tuc.
 * - `sync status` — Hien thi trang thai dong bo hien tai.
 * - `sync push` — Day du lieu local len cloud.
 * - `sync pull` — Tai du lieu tu cloud ve local.
 * - `sync retry` — Thu lai cac thao tac dong bo that bai.
 *
 * Lenh nay tuong tac voi [SyncManager] va [NetworkMonitor] de thuc hien
 * cac thao tac dong bo va bao cao trang thai mang.
 */
class SyncCommand : Command {

    override val name: String = "sync"

    override val aliases: List<String> = listOf("dong-bo")

    override val description: String = "Dong bo du lieu giua thiet bi va may chu"

    override val usage: String = "sync [now|status|push|pull|retry] [--verbose] [--force] [--format <format>] [--timeout <ms>]"

    override val minimumRole: UserRole = UserRole.USER

    override val category: String = "system"

    override val examples: List<Pair<String, String>> = listOf(
        "sync" to "Hien thi trang thai dong bo hien tai",
        "sync now" to "Bat dau dong bo toan bo ngay lap tuc",
        "sync now --force" to "Dong bo ngay ca khi khong co mang WiFi",
        "sync status --verbose" to "Hien thi trang thai dong bo chi tiet",
        "sync push" to "Day du lieu local len cloud",
        "sync pull" to "Tai du lieu tu cloud ve local",
        "sync retry" to "Thu lai cac thao tac dong bo that bai",
        "sync status --format json" to "Hien thi trang thai dong bo dang JSON"
    )

    /**
     * Cac lenh con hop le.
     */
    private val subcommands = listOf("now", "status", "push", "pull", "retry")

    /**
     * Cac flag hop le.
     */
    private val validFlags = listOf("--verbose", "-v", "--force", "-f", "--format", "--timeout")

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        if (args.isEmpty() || (args.size == 1 && args[0].isNotEmpty())) {
            val prefix = args.firstOrNull()?.lowercase() ?: ""
            subcommands.filter { it.startsWith(prefix) }.forEach { sub ->
                suggestions.add(
                    CompletionSuggestion(
                        text = sub,
                        description = descriptionForSubcommand(sub),
                        type = SuggestionType.SUBCOMMAND
                    )
                )
            }
        }

        val lastArg = args.lastOrNull() ?: ""
        if (lastArg.startsWith("-")) {
            validFlags.filter { it.startsWith(lastArg) }.forEach { flag ->
                suggestions.add(
                    CompletionSuggestion(
                        text = flag,
                        description = descriptionForFlag(flag),
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        if (flags.containsKey("format") && flags["format"] == null) {
            listOf("table", "json", "text").forEach { fmt ->
                suggestions.add(
                    CompletionSuggestion(
                        text = fmt,
                        description = "Dinh dang xuat: $fmt",
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
        val subcommand = args.firstOrNull()?.lowercase() ?: "status"
        val verbose = flags.containsKey("verbose") || flags.containsKey("v")
        val force = flags.containsKey("force") || flags.containsKey("f")
        val format = flags["format"]?.lowercase() ?: "table"

        return when (subcommand) {
            "now" -> executeNow(context, verbose, force, format)
            "status" -> executeStatus(context, verbose, format)
            "push" -> executePush(context, verbose, force, format)
            "pull" -> executePull(context, verbose, force, format)
            "retry" -> executeRetry(context, verbose, format)
            else -> CommandResult.error(
                "Lenh con khong hop le: '$subcommand'. Cac lenh con: ${subcommands.joinToString(", ")}"
            )
        }
    }

    /**
     * Thuc hien dong bo toan bo ngay lap tuc.
     */
    private suspend fun executeNow(
        context: CommandContext,
        verbose: Boolean,
        force: Boolean,
        format: String
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val syncManager = context.services.syncManager
        val networkMonitor = context.services.networkMonitor
        val isOnline = networkMonitor.isOnline.value

        if (!isOnline && !force) {
            return CommandResult.error(
                "Khong co ket noi mang. Su dung --force de bat buoc dong bo."
            )
        }

        if (!isOnline && force) {
            lines.add(OutputLine("Canh bao: Khong co ket noi mang, thu dong bo bat buoc...", OutputStyle.WARNING))
        }

        val currentState = syncManager.syncState.value
        if (currentState.name == "SYNCING") {
            return CommandResult.error("Dong bo dang duoc thuc hien. Vui long doi.")
        }

        lines.add(OutputLine("Dong bo du lieu", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val startTime = System.currentTimeMillis()

        if (verbose) {
            lines.add(OutputLine("Trang thai mang: ${if (isOnline) "Truc tuyen" else "Ngoai tuyen"}", OutputStyle.INFO))
            lines.add(OutputLine("Trang thai truoc: ${translateSyncState(currentState.name)}", OutputStyle.MUTED))
            lines.add(OutputLine(""))
        }

        try {
            lines.add(OutputLine("[1/3] Xu ly cac thao tac cho dong bo...", OutputStyle.INFO))
            syncManager.processPendingOperations()

            lines.add(OutputLine("[2/3] Day du lieu len cloud...", OutputStyle.INFO))
            syncManager.processPendingOperations()

            lines.add(OutputLine("[3/3] Hoan tat!", OutputStyle.INFO))

            val elapsed = System.currentTimeMillis() - startTime

            lines.add(OutputLine(""))
            lines.add(OutputLine("Dong bo hoan tat trong ${elapsed}ms", OutputStyle.SUCCESS))

            if (verbose) {
                val endState = syncManager.syncState.value
                lines.add(OutputLine("Trang thai sau: ${translateSyncState(endState.name)}", OutputStyle.MUTED))
                val pendingCount = syncManager.getPendingCount()
                lines.add(OutputLine("Thao tac cho xu ly: $pendingCount", OutputStyle.MUTED))
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            lines.add(OutputLine(""))
            lines.add(OutputLine("Dong bo that bai sau ${elapsed}ms: ${e.message ?: "Loi khong xac dinh"}", OutputStyle.ERROR))
            return CommandResult(output = lines, isSuccess = false)
        }

        return CommandResult.success(lines)
    }

    /**
     * Hien thi trang thai dong bo hien tai.
     */
    private suspend fun executeStatus(
        context: CommandContext,
        verbose: Boolean,
        format: String
    ): CommandResult {
        val syncManager = context.services.syncManager
        val networkMonitor = context.services.networkMonitor
        val isOnline = networkMonitor.isOnline.value
        val isWifi = networkMonitor.isWifi.value
        val currentState = syncManager.syncState.value
        val pendingCount = syncManager.getPendingCount()
        val timestamp = formatTimestamp(System.currentTimeMillis())

        if (format == "json") {
            return formatStatusAsJson(currentState.name, isOnline, isWifi, pendingCount, timestamp, verbose)
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Trang thai dong bo", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        lines.add(OutputLine(
            padRight("Trang thai:", 24) + translateSyncState(currentState.name),
            OutputStyle.TABLE_ROW
        ))
        lines.add(OutputLine(
            padRight("Ket noi mang:", 24) + if (isOnline) "Truc tuyen" else "Ngoai tuyen",
            if (isOnline) OutputStyle.TABLE_ROW else OutputStyle.WARNING
        ))
        lines.add(OutputLine(
            padRight("Loai mang:", 24) + if (isWifi) "WiFi" else if (isOnline) "Di dong" else "Khong co",
            OutputStyle.TABLE_ROW
        ))
        lines.add(OutputLine(
            padRight("Thao tac cho xu ly:", 24) + pendingCount.toString(),
            if (pendingCount > 0) OutputStyle.WARNING else OutputStyle.TABLE_ROW
        ))
        lines.add(OutputLine(
            padRight("Thoi diem kiem tra:", 24) + timestamp,
            OutputStyle.MUTED
        ))

        if (verbose) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("Chi tiet", OutputStyle.HEADER))
            lines.add(OutputLine(""))

            val settingsPreferences = context.services.settingsPreferences
            val autoSyncEnabled = try {
                settingsPreferences.autoSyncEnabled.first()
            } catch (_: Exception) {
                true
            }
            val wifiOnlySync = try {
                settingsPreferences.wifiOnlySync.first()
            } catch (_: Exception) {
                false
            }

            lines.add(OutputLine(
                padRight("Tu dong dong bo:", 24) + if (autoSyncEnabled) "Bat" else "Tat",
                OutputStyle.TABLE_ROW
            ))
            lines.add(OutputLine(
                padRight("Chi dong bo WiFi:", 24) + if (wifiOnlySync) "Bat" else "Tat",
                OutputStyle.TABLE_ROW
            ))

            if (pendingCount > 0) {
                lines.add(OutputLine(""))
                lines.add(OutputLine(
                    "Co $pendingCount thao tac cho xu ly. Dung 'sync now' de dong bo ngay.",
                    OutputStyle.WARNING
                ))
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Day du lieu local len cloud.
     */
    private suspend fun executePush(
        context: CommandContext,
        verbose: Boolean,
        force: Boolean,
        format: String
    ): CommandResult {
        val networkMonitor = context.services.networkMonitor
        val isOnline = networkMonitor.isOnline.value

        if (!isOnline && !force) {
            return CommandResult.error(
                "Khong co ket noi mang. Su dung --force de bat buoc day du lieu."
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Day du lieu len cloud", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val syncManager = context.services.syncManager
        val pendingBefore = syncManager.getPendingCount()

        if (pendingBefore == 0) {
            lines.add(OutputLine("Khong co du lieu nao can day len cloud.", OutputStyle.INFO))
            return CommandResult.success(lines)
        }

        if (verbose) {
            lines.add(OutputLine("So thao tac cho xu ly: $pendingBefore", OutputStyle.MUTED))
        }

        val startTime = System.currentTimeMillis()

        try {
            syncManager.processPendingOperations()
            val elapsed = System.currentTimeMillis() - startTime
            val pendingAfter = syncManager.getPendingCount()
            val pushed = pendingBefore - pendingAfter

            lines.add(OutputLine("Da day $pushed/$pendingBefore thao tac trong ${elapsed}ms", OutputStyle.SUCCESS))

            if (pendingAfter > 0) {
                lines.add(OutputLine(
                    "Con $pendingAfter thao tac chua duoc xu ly.",
                    OutputStyle.WARNING
                ))
            }
        } catch (e: Exception) {
            lines.add(OutputLine("Loi khi day du lieu: ${e.message ?: "Loi khong xac dinh"}", OutputStyle.ERROR))
            return CommandResult(output = lines, isSuccess = false)
        }

        return CommandResult.success(lines)
    }

    /**
     * Tai du lieu tu cloud ve local.
     */
    private suspend fun executePull(
        context: CommandContext,
        verbose: Boolean,
        force: Boolean,
        format: String
    ): CommandResult {
        val networkMonitor = context.services.networkMonitor
        val isOnline = networkMonitor.isOnline.value

        if (!isOnline && !force) {
            return CommandResult.error(
                "Khong co ket noi mang. Su dung --force de bat buoc tai du lieu."
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Tai du lieu tu cloud", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val startTime = System.currentTimeMillis()

        try {
            val syncManager = context.services.syncManager
            syncManager.processPendingOperations()
            val elapsed = System.currentTimeMillis() - startTime

            lines.add(OutputLine("Da xep hang dong bo tai du lieu trong ${elapsed}ms", OutputStyle.SUCCESS))

            if (verbose) {
                val state = syncManager.syncState.value
                lines.add(OutputLine("Trang thai: ${translateSyncState(state.name)}", OutputStyle.MUTED))
            }
        } catch (e: Exception) {
            lines.add(OutputLine("Loi khi tai du lieu: ${e.message ?: "Loi khong xac dinh"}", OutputStyle.ERROR))
            return CommandResult(output = lines, isSuccess = false)
        }

        return CommandResult.success(lines)
    }

    /**
     * Thu lai cac thao tac dong bo that bai.
     */
    private suspend fun executeRetry(
        context: CommandContext,
        verbose: Boolean,
        format: String
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val syncManager = context.services.syncManager
        val pendingBefore = syncManager.getPendingCount()

        lines.add(OutputLine("Thu lai dong bo", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        if (pendingBefore == 0) {
            lines.add(OutputLine("Khong co thao tac nao can thu lai.", OutputStyle.INFO))
            return CommandResult.success(lines)
        }

        if (verbose) {
            lines.add(OutputLine("So thao tac cho xu ly: $pendingBefore", OutputStyle.MUTED))
        }

        val startTime = System.currentTimeMillis()

        try {
            syncManager.retryFailedOperations()
            val elapsed = System.currentTimeMillis() - startTime
            val pendingAfter = syncManager.getPendingCount()

            lines.add(OutputLine(
                "Da thu lai xong trong ${elapsed}ms",
                OutputStyle.SUCCESS
            ))

            if (verbose) {
                lines.add(OutputLine("Truoc: $pendingBefore thao tac cho", OutputStyle.MUTED))
                lines.add(OutputLine("Sau:   $pendingAfter thao tac cho", OutputStyle.MUTED))
            }

            if (pendingAfter > 0) {
                lines.add(OutputLine(
                    "Van con $pendingAfter thao tac chua thanh cong.",
                    OutputStyle.WARNING
                ))
            }
        } catch (e: Exception) {
            lines.add(OutputLine("Loi khi thu lai: ${e.message ?: "Loi khong xac dinh"}", OutputStyle.ERROR))
            return CommandResult(output = lines, isSuccess = false)
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang trang thai dong bo dang JSON.
     */
    private fun formatStatusAsJson(
        state: String,
        isOnline: Boolean,
        isWifi: Boolean,
        pendingCount: Int,
        timestamp: String,
        verbose: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"sync_state\": \"${state.lowercase()}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"is_online\": $isOnline,", OutputStyle.CODE))
        lines.add(OutputLine("  \"network_type\": \"${if (isWifi) "wifi" else if (isOnline) "mobile" else "none"}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"pending_operations\": $pendingCount,", OutputStyle.CODE))
        lines.add(OutputLine("  \"checked_at\": \"$timestamp\"", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Dich trang thai dong bo sang tieng Viet.
     */
    private fun translateSyncState(state: String): String {
        return when (state.uppercase()) {
            "IDLE" -> "San sang"
            "SYNCING" -> "Dang dong bo..."
            "PENDING" -> "Cho xu ly"
            "ERROR" -> "Loi"
            else -> state
        }
    }

    /**
     * Can chuoi ben phai voi ky tu khoang trang.
     */
    private fun padRight(text: String, width: Int): String {
        return if (text.length >= width) text else text + " ".repeat(width - text.length)
    }

    /**
     * Dinh dang timestamp thanh chuoi doc duoc.
     */
    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    /**
     * Mo ta cho cac lenh con.
     */
    private fun descriptionForSubcommand(sub: String): String {
        return when (sub) {
            "now" -> "Dong bo toan bo ngay lap tuc"
            "status" -> "Hien thi trang thai dong bo"
            "push" -> "Day du lieu local len cloud"
            "pull" -> "Tai du lieu tu cloud ve local"
            "retry" -> "Thu lai cac thao tac that bai"
            else -> ""
        }
    }

    /**
     * Mo ta cho cac flag.
     */
    private fun descriptionForFlag(flag: String): String {
        return when (flag) {
            "--verbose", "-v" -> "Hien thi thong tin chi tiet"
            "--force", "-f" -> "Bat buoc dong bo ngay ca khi ngoai tuyen"
            "--format" -> "Dinh dang xuat (table, json, text)"
            "--timeout" -> "Thoi gian cho toi da (ms)"
            else -> ""
        }
    }
}
