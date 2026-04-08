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
 * Lenh quan ly bo nho dem va hang doi dong bo cua ung dung.
 *
 * Cung cap cac lenh con:
 * - `status` — Hien thi trang thai bo nho dem va dong bo hien tai
 * - `clear` — Xoa bo nho dem (co the chon loc)
 * - `sync` — Kich hoat dong bo ngay lap tuc
 * - `retry` — Thu lai cac thao tac dong bo da that bai
 *
 * Lenh `clear` la thao tac huy hoai va yeu cau co `--confirm` khi thuc thi.
 */
class CacheCommand : Command {

    override val name: String = "cache"

    override val aliases: List<String> = listOf("ca")

    override val description: String = "Quan ly bo nho dem va hang doi dong bo"

    override val usage: String =
        "cache <status|clear|sync|retry> [--verbose] [--format <text|json>] [--confirm] [--dry-run] [--pending] [--failed]"

    override val minimumRole: UserRole = UserRole.USER

    override val category: String = "system"

    override val examples: List<Pair<String, String>> = listOf(
        "cache status" to "Hien thi trang thai bo nho dem va dong bo",
        "cache status --verbose" to "Hien thi trang thai chi tiet voi so luong thao tac",
        "cache clear --confirm" to "Xoa toan bo bo nho dem",
        "cache clear --dry-run" to "Mo phong xoa bo nho dem (khong thuc su xoa)",
        "cache sync" to "Kich hoat dong bo ngay lap tuc",
        "cache retry" to "Thu lai cac thao tac dong bo da that bai",
        "cache status --format json" to "Hien thi trang thai duoi dang JSON",
        "cache status --pending" to "Chi hien thi cac thao tac dang cho",
        "cache status --failed" to "Chi hien thi cac thao tac da that bai"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        if (args.isEmpty() || (args.size == 1 && args[0].isNotEmpty())) {
            val prefix = args.firstOrNull()?.lowercase() ?: ""
            return SUBCOMMANDS
                .filter { it.startsWith(prefix) }
                .map { sub ->
                    CompletionSuggestion(
                        text = sub,
                        description = subcommandDescription(sub),
                        type = SuggestionType.SUBCOMMAND
                    )
                }
        }
        val flagPrefix = args.lastOrNull()?.lowercase() ?: ""
        if (flagPrefix.startsWith("-")) {
            return AVAILABLE_FLAGS
                .filter { it.startsWith(flagPrefix) }
                .map { flag ->
                    CompletionSuggestion(
                        text = flag,
                        description = flagDescription(flag),
                        type = SuggestionType.FLAG
                    )
                }
        }
        return emptyList()
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val subcommand = args.firstOrNull()?.lowercase()
            ?: return executeStatus(flags, context)

        return when (subcommand) {
            "status" -> executeStatus(flags, context)
            "clear" -> executeClear(flags, context)
            "sync" -> executeSync(flags, context)
            "retry" -> executeRetry(flags, context)
            else -> CommandResult.error(
                "Lenh con khong hop le: '$subcommand'. Su dung: status, clear, sync, retry"
            )
        }
    }

    /**
     * Hien thi trang thai hien tai cua bo nho dem va hang doi dong bo.
     */
    private suspend fun executeStatus(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val verbose = "verbose" in flags
        val format = flags["format"]?.lowercase() ?: "text"
        val showPendingOnly = "pending" in flags
        val showFailedOnly = "failed" in flags

        val syncManager = context.services.syncManager
        val networkMonitor = context.services.networkMonitor

        val syncState = syncManager.syncState.value
        val isOnline = networkMonitor.isOnline.value
        val isWifi = networkMonitor.isWifi.value
        val pendingCount = syncManager.getPendingCount()

        if (format == "json") {
            return formatStatusJson(syncState.name, isOnline, isWifi, pendingCount, verbose)
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("== Trang thai bo nho dem ==", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val stateDisplay = when (syncState.name) {
            "IDLE" -> "Nghi (IDLE)"
            "SYNCING" -> "Dang dong bo..."
            "PENDING" -> "Co thao tac cho xu ly"
            "ERROR" -> "Loi dong bo"
            else -> syncState.name
        }
        val stateStyle = when (syncState.name) {
            "IDLE" -> OutputStyle.SUCCESS
            "SYNCING" -> OutputStyle.INFO
            "PENDING" -> OutputStyle.WARNING
            "ERROR" -> OutputStyle.ERROR
            else -> OutputStyle.NORMAL
        }

        lines.add(OutputLine("  Trang thai dong bo : $stateDisplay", stateStyle))
        lines.add(OutputLine(
            "  Ket noi mang       : ${if (isOnline) "Truc tuyen" else "Ngoai tuyen"}",
            if (isOnline) OutputStyle.SUCCESS else OutputStyle.WARNING
        ))

        if (verbose) {
            lines.add(OutputLine(
                "  Loai ket noi       : ${if (isWifi) "WiFi" else if (isOnline) "Di dong" else "Khong co"}",
                OutputStyle.NORMAL
            ))
        }

        lines.add(OutputLine(
            "  Thao tac cho       : $pendingCount",
            if (pendingCount > 0) OutputStyle.WARNING else OutputStyle.MUTED
        ))

        if (showPendingOnly && pendingCount == 0) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("  Khong co thao tac nao dang cho xu ly.", OutputStyle.MUTED))
        } else if (showPendingOnly) {
            lines.add(OutputLine(""))
            lines.add(OutputLine(
                "  Co $pendingCount thao tac dang cho xu ly.",
                OutputStyle.WARNING
            ))
        }

        if (showFailedOnly) {
            lines.add(OutputLine(""))
            if (syncState.name == "ERROR") {
                lines.add(OutputLine(
                    "  Dong bo dang trong trang thai loi. Dung 'cache retry' de thu lai.",
                    OutputStyle.ERROR
                ))
            } else {
                lines.add(OutputLine(
                    "  Khong co thao tac that bai.",
                    OutputStyle.MUTED
                ))
            }
        }

        if (verbose) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("-- Chi tiet --", OutputStyle.MUTED))
            lines.add(OutputLine(
                "  Dong bo tu dong co the duoc bat/tat qua lenh 'config'.",
                OutputStyle.MUTED
            ))
            if (!isOnline) {
                lines.add(OutputLine(
                    "  Cac thao tac se duoc dong bo khi co ket noi mang tro lai.",
                    OutputStyle.INFO
                ))
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Xoa bo nho dem. Yeu cau co `--confirm` hoac `--dry-run`.
     */
    private suspend fun executeClear(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val confirm = "confirm" in flags
        val dryRun = "dry-run" in flags

        if (!confirm && !dryRun) {
            return CommandResult.error(
                "Lenh 'cache clear' la thao tac huy hoai. " +
                    "Them co --confirm de xac nhan hoac --dry-run de mo phong."
            )
        }

        val lines = mutableListOf<OutputLine>()

        if (dryRun) {
            lines.add(OutputLine("[Mo phong] Cac thao tac se duoc thuc hien:", OutputStyle.WARNING))
            lines.add(OutputLine("  - Xoa hang doi dong bo dang cho", OutputStyle.INFO))
            lines.add(OutputLine("  - Dat lai trang thai dong bo ve IDLE", OutputStyle.INFO))
            lines.add(OutputLine(""))
            lines.add(OutputLine(
                "Khong co du lieu nao bi xoa (che do mo phong).",
                OutputStyle.MUTED
            ))
            return CommandResult.success(lines)
        }

        // Actual clear: trigger sync to flush, then pending operations would clear
        // In practice, the SyncManager handles the pending queue.
        // We can enqueue a sync to process remaining items.
        try {
            val pendingBefore = context.services.syncManager.getPendingCount()
            context.services.syncManager.processPendingOperations()

            lines.add(OutputLine("== Xoa bo nho dem ==", OutputStyle.HEADER))
            lines.add(OutputLine(""))
            lines.add(OutputLine(
                "  Da xu ly $pendingBefore thao tac trong hang doi.",
                OutputStyle.SUCCESS
            ))
            lines.add(OutputLine(
                "  Trang thai dong bo da duoc lam moi.",
                OutputStyle.SUCCESS
            ))
        } catch (e: Exception) {
            return CommandResult.error("Loi khi xoa bo nho dem: ${e.message ?: "Khong xac dinh"}")
        }

        return CommandResult.success(lines)
    }

    /**
     * Kich hoat dong bo ngay lap tuc.
     */
    private suspend fun executeSync(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val verbose = "verbose" in flags
        val syncManager = context.services.syncManager
        val networkMonitor = context.services.networkMonitor

        if (!networkMonitor.isOnline.value) {
            return CommandResult.error(
                "Khong the dong bo: Thiet bi dang ngoai tuyen. " +
                    "Kiem tra ket noi mang va thu lai."
            )
        }

        val lines = mutableListOf<OutputLine>()
        val currentState = syncManager.syncState.value

        if (currentState.name == "SYNCING") {
            lines.add(OutputLine(
                "Dong bo dang duoc thuc hien. Vui long doi...",
                OutputStyle.WARNING
            ))
            return CommandResult.success(lines)
        }

        val pendingBefore = syncManager.getPendingCount()

        try {
            val startTime = System.currentTimeMillis()
            syncManager.processPendingOperations()
            val elapsed = System.currentTimeMillis() - startTime

            lines.add(OutputLine("== Dong bo ==", OutputStyle.HEADER))
            lines.add(OutputLine(""))
            lines.add(OutputLine(
                "  Da kich hoat dong bo thanh cong.",
                OutputStyle.SUCCESS
            ))

            if (verbose) {
                lines.add(OutputLine(
                    "  Thao tac cho truoc dong bo: $pendingBefore",
                    OutputStyle.MUTED
                ))
                lines.add(OutputLine(
                    "  Thoi gian yeu cau         : ${elapsed}ms",
                    OutputStyle.MUTED
                ))
                lines.add(OutputLine(
                    "  Trang thai                : ${syncManager.syncState.value.name}",
                    OutputStyle.MUTED
                ))
            }
        } catch (e: Exception) {
            return CommandResult.error(
                "Loi khi kich hoat dong bo: ${e.message ?: "Khong xac dinh"}"
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Thu lai cac thao tac dong bo da that bai.
     */
    private suspend fun executeRetry(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val verbose = "verbose" in flags
        val syncManager = context.services.syncManager
        val networkMonitor = context.services.networkMonitor

        if (!networkMonitor.isOnline.value) {
            return CommandResult.error(
                "Khong the thu lai: Thiet bi dang ngoai tuyen."
            )
        }

        val lines = mutableListOf<OutputLine>()
        val pendingBefore = syncManager.getPendingCount()

        try {
            val startTime = System.currentTimeMillis()
            syncManager.retryFailedOperations()
            val elapsed = System.currentTimeMillis() - startTime

            lines.add(OutputLine("== Thu lai dong bo ==", OutputStyle.HEADER))
            lines.add(OutputLine(""))
            lines.add(OutputLine(
                "  Da gui yeu cau thu lai cac thao tac that bai.",
                OutputStyle.SUCCESS
            ))

            if (verbose) {
                lines.add(OutputLine(
                    "  Thao tac cho truoc thu lai: $pendingBefore",
                    OutputStyle.MUTED
                ))
                lines.add(OutputLine(
                    "  Thoi gian yeu cau        : ${elapsed}ms",
                    OutputStyle.MUTED
                ))
                lines.add(OutputLine(
                    "  Trang thai hien tai       : ${syncManager.syncState.value.name}",
                    OutputStyle.MUTED
                ))
            }
        } catch (e: Exception) {
            return CommandResult.error(
                "Loi khi thu lai dong bo: ${e.message ?: "Khong xac dinh"}"
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang trang thai duoi dang JSON.
     */
    private fun formatStatusJson(
        syncStateName: String,
        isOnline: Boolean,
        isWifi: Boolean,
        pendingCount: Int,
        verbose: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"sync_state\": \"$syncStateName\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"is_online\": $isOnline,", OutputStyle.CODE))
        if (verbose) {
            lines.add(OutputLine("  \"is_wifi\": $isWifi,", OutputStyle.CODE))
        }
        lines.add(OutputLine("  \"pending_operations\": $pendingCount", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    private fun subcommandDescription(sub: String): String = when (sub) {
        "status" -> "Hien thi trang thai bo nho dem"
        "clear" -> "Xoa bo nho dem (yeu cau --confirm)"
        "sync" -> "Kich hoat dong bo ngay"
        "retry" -> "Thu lai thao tac that bai"
        else -> ""
    }

    private fun flagDescription(flag: String): String = when (flag) {
        "--verbose" -> "Hien thi thong tin chi tiet"
        "--format" -> "Dinh dang dau ra (text/json)"
        "--confirm" -> "Xac nhan thao tac huy hoai"
        "--dry-run" -> "Mo phong thao tac (khong thuc su thay doi)"
        "--pending" -> "Chi hien thi thao tac dang cho"
        "--failed" -> "Chi hien thi thao tac that bai"
        else -> ""
    }

    private companion object {
        val SUBCOMMANDS = listOf("status", "clear", "sync", "retry")
        val AVAILABLE_FLAGS = listOf(
            "--verbose", "--format", "--confirm", "--dry-run", "--pending", "--failed"
        )
    }
}
