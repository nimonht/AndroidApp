package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh `stats` — hien thi thong ke he thong cho admin console.
 *
 * Truy van [SystemStats] tu [AdminRepository.getSystemStats] va trinh bay
 * cac chi so tong quan ve nguoi dung, quiz, luot lam va ngan hang cau hoi.
 *
 * Mac dinh hien thi toan bo thong ke he thong. Co the loc theo nhom bang
 * cac flag `--users`, `--quizzes`, `--attempts`, hoac `--sync`.
 *
 * Dinh dang dau ra ho tro:
 * - `--format table` (mac dinh): bang co dinh dang.
 * - `--format json`: dau ra JSON.
 *
 * Cac flag bo sung:
 * - `--verbose`: hien thi them chi so phu (ty le phan tram, trung binh).
 * - `--export`: danh dau dau ra de xuat (tuong duong JSON khong co trang tri).
 *
 * Yeu cau vai tro toi thieu [UserRole.ADMIN] va quyen [AdminPermission.VIEW_REPORTS].
 */
class StatsCommand : Command {

    override val name: String = "stats"

    override val aliases: List<String> = listOf("stat")

    override val description: String = "Hien thi thong ke he thong"

    override val usage: String =
        "stats [--users] [--quizzes] [--attempts] [--sync] " +
                "[--format <table|json>] [--verbose] [--export]"

    override val requiredPermission: AdminPermission = AdminPermission.VIEW_REPORTS

    override val minimumRole: UserRole = UserRole.ADMIN

    override val category: String = "admin"

    override val examples: List<Pair<String, String>> = listOf(
        "stats" to "Hien thi toan bo thong ke he thong",
        "stats --users" to "Chi hien thi thong ke nguoi dung",
        "stats --quizzes" to "Chi hien thi thong ke quiz",
        "stats --attempts" to "Chi hien thi thong ke luot lam",
        "stats --sync" to "Hien thi thong ke dong bo",
        "stats --verbose" to "Hien thi thong ke chi tiet voi ty le phan tram",
        "stats --format json" to "Xuat thong ke dang JSON",
        "stats --users --quizzes --verbose" to "Thong ke nguoi dung va quiz chi tiet",
        "stats --export" to "Xuat thong ke dang JSON de luu tru"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()
        val usedFlags = flags.keys

        val availableFlags = mapOf(
            "--users" to "Chi hien thi thong ke nguoi dung",
            "--quizzes" to "Chi hien thi thong ke quiz",
            "--attempts" to "Chi hien thi thong ke luot lam",
            "--sync" to "Hien thi thong ke dong bo",
            "--format" to "Dinh dang dau ra (table/json)",
            "--verbose" to "Hien thi chi tiet",
            "--export" to "Xuat du lieu de luu tru"
        )

        // Neu flag cuoi la --format va chua co gia tri, goi y gia tri
        if ("format" in usedFlags && flags["format"] == null) {
            suggestions.add(
                CompletionSuggestion(
                    text = "table",
                    description = "Dinh dang bang (mac dinh)",
                    type = SuggestionType.ARGUMENT
                )
            )
            suggestions.add(
                CompletionSuggestion(
                    text = "json",
                    description = "Dinh dang JSON",
                    type = SuggestionType.ARGUMENT
                )
            )
            return suggestions
        }

        for ((flag, desc) in availableFlags) {
            val flagName = flag.removePrefix("--")
            if (flagName !in usedFlags) {
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
        // Kiem tra cac flag chua ho tro
        val comingSoonFlags = listOf("period", "compare-period", "breakdown", "trend")
        for (flag in comingSoonFlags) {
            if (flag in flags) {
                return CommandResult.success(
                    listOf(
                        OutputLine(
                            "Co '--$flag' chua duoc ho tro.",
                            OutputStyle.WARNING
                        )
                    )
                )
            }
        }

        val verbose = "verbose" in flags || "v" in flags
        val export = "export" in flags
        val format = when {
            export -> "json"
            else -> flags["format"]?.lowercase() ?: "table"
        }

        if (format !in listOf("table", "json")) {
            return CommandResult.error(
                "stats: Dinh dang khong hop le '$format'. Chi ho tro: table, json."
            )
        }

        // Xac dinh nhom thong ke can hien thi
        val showUsers = "users" in flags
        val showQuizzes = "quizzes" in flags
        val showAttempts = "attempts" in flags
        val showSync = "sync" in flags
        val showAll = !showUsers && !showQuizzes && !showAttempts && !showSync

        // Lay thong ke he thong
        val stats = try {
            context.repositories.adminRepository.getSystemStats().first()
        } catch (e: Exception) {
            return CommandResult.error(
                "stats: Khong the lay thong ke he thong: ${e.message ?: "Loi khong xac dinh"}"
            )
        }

        return when (format) {
            "json" -> buildJsonOutput(stats, showAll, showUsers, showQuizzes, showAttempts, showSync, verbose, context)
            else -> buildTableOutput(stats, showAll, showUsers, showQuizzes, showAttempts, showSync, verbose, context)
        }
    }

    // ====================================================================
    // Table output
    // ====================================================================

    /**
     * Xay dung dau ra dang bang.
     */
    private suspend fun buildTableOutput(
        stats: com.example.androidapp.domain.model.SystemStats,
        showAll: Boolean,
        showUsers: Boolean,
        showQuizzes: Boolean,
        showAttempts: Boolean,
        showSync: Boolean,
        verbose: Boolean,
        context: CommandContext
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("Thong ke he thong", OutputStyle.HEADER))
        lines.add(OutputLine(SEPARATOR, OutputStyle.MUTED))

        if (showAll || showUsers) {
            buildUserStats(lines, stats, verbose)
        }

        if (showAll || showQuizzes) {
            buildQuizStats(lines, stats, verbose)
        }

        if (showAll || showAttempts) {
            buildAttemptStats(lines, stats, verbose)
        }

        if (showAll || showSync) {
            buildSyncStats(lines, verbose, context)
        }

        lines.add(OutputLine(SEPARATOR, OutputStyle.MUTED))

        return CommandResult.success(lines)
    }

    /**
     * Xay dung phan thong ke nguoi dung.
     */
    private fun buildUserStats(
        lines: MutableList<OutputLine>,
        stats: com.example.androidapp.domain.model.SystemStats,
        verbose: Boolean
    ) {
        lines.add(OutputLine(""))
        lines.add(OutputLine("  Nguoi dung", OutputStyle.INFO))
        lines.add(
            OutputLine(
                formatTableRow("Tong nguoi dung", stats.totalUsers.toString()),
                OutputStyle.TABLE_ROW
            )
        )
        lines.add(
            OutputLine(
                formatTableRow("Nguoi dung hoat dong", stats.activeUsers.toString()),
                OutputStyle.TABLE_ROW
            )
        )
        lines.add(
            OutputLine(
                formatTableRow("Quan tri vien", stats.adminUsers.toString()),
                OutputStyle.TABLE_ROW
            )
        )

        if (verbose) {
            val activePercent = stats.activeUserPercentage
            lines.add(
                OutputLine(
                    formatTableRow(
                        "Ty le hoat dong",
                        String.format(java.util.Locale.ROOT, "%.1f%%", activePercent)
                    ),
                    OutputStyle.MUTED
                )
            )
            val regularUsers = stats.totalUsers - stats.adminUsers
            lines.add(
                OutputLine(
                    formatTableRow("Nguoi dung thuong", regularUsers.toString()),
                    OutputStyle.MUTED
                )
            )
        }
    }

    /**
     * Xay dung phan thong ke quiz.
     */
    private fun buildQuizStats(
        lines: MutableList<OutputLine>,
        stats: com.example.androidapp.domain.model.SystemStats,
        verbose: Boolean
    ) {
        lines.add(OutputLine(""))
        lines.add(OutputLine("  Quiz", OutputStyle.INFO))
        lines.add(
            OutputLine(
                formatTableRow("Tong quiz", stats.totalQuizzes.toString()),
                OutputStyle.TABLE_ROW
            )
        )
        lines.add(
            OutputLine(
                formatTableRow("Quiz cong khai", stats.publicQuizzes.toString()),
                OutputStyle.TABLE_ROW
            )
        )
        lines.add(
            OutputLine(
                formatTableRow("Quiz rieng tu", stats.privateQuizzes.toString()),
                OutputStyle.TABLE_ROW
            )
        )
        lines.add(
            OutputLine(
                formatTableRow("Quiz da xoa", stats.deletedQuizzes.toString()),
                OutputStyle.TABLE_ROW
            )
        )
        lines.add(
            OutputLine(
                formatTableRow("Cau hoi trong pool", stats.totalQuestionsInPool.toString()),
                OutputStyle.TABLE_ROW
            )
        )

        if (verbose) {
            val publicPercent = stats.publicQuizPercentage
            lines.add(
                OutputLine(
                    formatTableRow(
                        "Ty le cong khai",
                        String.format(java.util.Locale.ROOT, "%.1f%%", publicPercent)
                    ),
                    OutputStyle.MUTED
                )
            )
        }
    }

    /**
     * Xay dung phan thong ke luot lam.
     */
    private fun buildAttemptStats(
        lines: MutableList<OutputLine>,
        stats: com.example.androidapp.domain.model.SystemStats,
        verbose: Boolean
    ) {
        lines.add(OutputLine(""))
        lines.add(OutputLine("  Luot lam quiz", OutputStyle.INFO))
        lines.add(
            OutputLine(
                formatTableRow("Tong luot lam", stats.totalAttempts.toString()),
                OutputStyle.TABLE_ROW
            )
        )

        if (verbose) {
            val avgAttempts = stats.averageAttemptsPerQuiz
            lines.add(
                OutputLine(
                    formatTableRow(
                        "Trung binh/quiz",
                        String.format(java.util.Locale.ROOT, "%.2f", avgAttempts)
                    ),
                    OutputStyle.MUTED
                )
            )
        }
    }

    /**
     * Xay dung phan thong ke dong bo.
     */
    private suspend fun buildSyncStats(
        lines: MutableList<OutputLine>,
        verbose: Boolean,
        context: CommandContext
    ) {
        lines.add(OutputLine(""))
        lines.add(OutputLine("  Dong bo", OutputStyle.INFO))

        val syncService = context.services.syncService
        val networkService = context.services.networkService
        val isOnline = networkService.isOnline.value
        val syncState = syncService.consoleSyncState.value

        lines.add(
            OutputLine(
                formatTableRow("Trang thai mang", if (isOnline) "Truc tuyen" else "Ngoai tuyen"),
                OutputStyle.TABLE_ROW
            )
        )
        lines.add(
            OutputLine(
                formatTableRow("Trang thai dong bo", translateSyncState(syncState.name)),
                OutputStyle.TABLE_ROW
            )
        )

        try {
            val pendingCount = syncService.getPendingCount()
            lines.add(
                OutputLine(
                    formatTableRow("Thao tac cho xu ly", pendingCount.toString()),
                    OutputStyle.TABLE_ROW
                )
            )
        } catch (_: Exception) {
            lines.add(
                OutputLine(
                    formatTableRow("Thao tac cho xu ly", "Khong xac dinh"),
                    OutputStyle.TABLE_ROW
                )
            )
        }

        if (verbose) {
            val settingsService = context.services.settingsService
            try {
                val autoSync = settingsService.autoSyncEnabled.first()
                val wifiOnly = settingsService.wifiOnlySync.first()
                lines.add(
                    OutputLine(
                        formatTableRow(
                            "Tu dong dong bo",
                            if (autoSync) "Bat" else "Tat"
                        ),
                        OutputStyle.MUTED
                    )
                )
                lines.add(
                    OutputLine(
                        formatTableRow(
                            "Chi dong bo WiFi",
                            if (wifiOnly) "Bat" else "Tat"
                        ),
                        OutputStyle.MUTED
                    )
                )
            } catch (_: Exception) {
                // Bo qua neu khong doc duoc cai dat
            }
        }
    }

    // ====================================================================
    // JSON output
    // ====================================================================

    /**
     * Xay dung dau ra dang JSON.
     */
    private suspend fun buildJsonOutput(
        stats: com.example.androidapp.domain.model.SystemStats,
        showAll: Boolean,
        showUsers: Boolean,
        showQuizzes: Boolean,
        showAttempts: Boolean,
        showSync: Boolean,
        verbose: Boolean,
        context: CommandContext
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("{", OutputStyle.CODE))

        val sections = mutableListOf<List<OutputLine>>()

        if (showAll || showUsers) {
            sections.add(buildUserStatsJson(stats, verbose))
        }
        if (showAll || showQuizzes) {
            sections.add(buildQuizStatsJson(stats, verbose))
        }
        if (showAll || showAttempts) {
            sections.add(buildAttemptStatsJson(stats, verbose))
        }
        if (showAll || showSync) {
            sections.add(buildSyncStatsJson(verbose, context))
        }

        for ((index, section) in sections.withIndex()) {
            val isLast = index == sections.size - 1
            for ((lineIdx, line) in section.withIndex()) {
                val isLastLineOfSection = lineIdx == section.size - 1
                if (isLastLineOfSection && !isLast) {
                    // Them dau phay sau phan tu cuoi cua section (tru section cuoi)
                    lines.add(OutputLine(line.text + ",", line.style))
                } else {
                    lines.add(line)
                }
            }
        }

        lines.add(OutputLine("}", OutputStyle.CODE))

        return CommandResult.success(lines)
    }

    /**
     * Xay dung JSON cho thong ke nguoi dung.
     */
    private fun buildUserStatsJson(
        stats: com.example.androidapp.domain.model.SystemStats,
        verbose: Boolean
    ): List<OutputLine> {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("  \"users\": {", OutputStyle.CODE))
        lines.add(OutputLine("    \"total\": ${stats.totalUsers},", OutputStyle.CODE))
        lines.add(OutputLine("    \"active\": ${stats.activeUsers},", OutputStyle.CODE))

        if (verbose) {
            val activePercent = stats.activeUserPercentage
            val regularUsers = stats.totalUsers - stats.adminUsers
            lines.add(OutputLine("    \"admins\": ${stats.adminUsers},", OutputStyle.CODE))
            lines.add(OutputLine("    \"regular\": $regularUsers,", OutputStyle.CODE))
            lines.add(
                OutputLine(
                    "    \"active_percentage\": ${String.format(java.util.Locale.ROOT, "%.1f", activePercent)}",
                    OutputStyle.CODE
                )
            )
        } else {
            lines.add(OutputLine("    \"admins\": ${stats.adminUsers}", OutputStyle.CODE))
        }

        lines.add(OutputLine("  }", OutputStyle.CODE))
        return lines
    }

    /**
     * Xay dung JSON cho thong ke quiz.
     */
    private fun buildQuizStatsJson(
        stats: com.example.androidapp.domain.model.SystemStats,
        verbose: Boolean
    ): List<OutputLine> {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("  \"quizzes\": {", OutputStyle.CODE))
        lines.add(OutputLine("    \"total\": ${stats.totalQuizzes},", OutputStyle.CODE))
        lines.add(OutputLine("    \"public\": ${stats.publicQuizzes},", OutputStyle.CODE))
        lines.add(OutputLine("    \"private\": ${stats.privateQuizzes},", OutputStyle.CODE))
        lines.add(OutputLine("    \"deleted\": ${stats.deletedQuizzes},", OutputStyle.CODE))

        if (verbose) {
            val publicPercent = stats.publicQuizPercentage
            lines.add(
                OutputLine(
                    "    \"questions_in_pool\": ${stats.totalQuestionsInPool},",
                    OutputStyle.CODE
                )
            )
            lines.add(
                OutputLine(
                    "    \"public_percentage\": ${String.format(java.util.Locale.ROOT, "%.1f", publicPercent)}",
                    OutputStyle.CODE
                )
            )
        } else {
            lines.add(
                OutputLine(
                    "    \"questions_in_pool\": ${stats.totalQuestionsInPool}",
                    OutputStyle.CODE
                )
            )
        }

        lines.add(OutputLine("  }", OutputStyle.CODE))
        return lines
    }

    /**
     * Xay dung JSON cho thong ke luot lam.
     */
    private fun buildAttemptStatsJson(
        stats: com.example.androidapp.domain.model.SystemStats,
        verbose: Boolean
    ): List<OutputLine> {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("  \"attempts\": {", OutputStyle.CODE))

        if (verbose) {
            val avgAttempts = stats.averageAttemptsPerQuiz
            lines.add(OutputLine("    \"total\": ${stats.totalAttempts},", OutputStyle.CODE))
            lines.add(
                OutputLine(
                    "    \"average_per_quiz\": ${String.format(java.util.Locale.ROOT, "%.2f", avgAttempts)}",
                    OutputStyle.CODE
                )
            )
        } else {
            lines.add(OutputLine("    \"total\": ${stats.totalAttempts}", OutputStyle.CODE))
        }

        lines.add(OutputLine("  }", OutputStyle.CODE))
        return lines
    }

    /**
     * Xay dung JSON cho thong ke dong bo.
     */
    private suspend fun buildSyncStatsJson(
        verbose: Boolean,
        context: CommandContext
    ): List<OutputLine> {
        val lines = mutableListOf<OutputLine>()
        val syncService = context.services.syncService
        val networkService = context.services.networkService
        val isOnline = networkService.isOnline.value
        val syncState = syncService.consoleSyncState.value

        lines.add(OutputLine("  \"sync\": {", OutputStyle.CODE))
        lines.add(
            OutputLine(
                "    \"network\": \"${if (isOnline) "online" else "offline"}\",",
                OutputStyle.CODE
            )
        )
        lines.add(
            OutputLine(
                "    \"state\": \"${syncState.name.lowercase()}\",",
                OutputStyle.CODE
            )
        )

        val pendingCount = try {
            syncService.getPendingCount()
        } catch (_: Exception) {
            -1
        }

        if (verbose) {
            lines.add(OutputLine("    \"pending_operations\": $pendingCount,", OutputStyle.CODE))
            try {
                val autoSync = context.services.settingsService.autoSyncEnabled.first()
                val wifiOnly = context.services.settingsService.wifiOnlySync.first()
                lines.add(OutputLine("    \"auto_sync\": $autoSync,", OutputStyle.CODE))
                lines.add(OutputLine("    \"wifi_only\": $wifiOnly", OutputStyle.CODE))
            } catch (_: Exception) {
                lines.add(OutputLine("    \"auto_sync\": null,", OutputStyle.CODE))
                lines.add(OutputLine("    \"wifi_only\": null", OutputStyle.CODE))
            }
        } else {
            lines.add(OutputLine("    \"pending_operations\": $pendingCount", OutputStyle.CODE))
        }

        lines.add(OutputLine("  }", OutputStyle.CODE))
        return lines
    }

    // ====================================================================
    // Helpers
    // ====================================================================

    /**
     * Dinh dang mot dong bang voi nhan va gia tri can chinh.
     */
    private fun formatTableRow(label: String, value: String): String {
        val paddedLabel = if (label.length >= LABEL_WIDTH) {
            label.take(LABEL_WIDTH)
        } else {
            label.padEnd(LABEL_WIDTH)
        }
        return "    $paddedLabel $value"
    }

    /**
     * Dich trang thai dong bo sang tieng Viet.
     */
    private fun translateSyncState(state: String): String = when (state.uppercase()) {
        "IDLE" -> "San sang"
        "SYNCING" -> "Dang dong bo"
        "PENDING" -> "Cho xu ly"
        "ERROR" -> "Loi"
        else -> state
    }

    private companion object {
        /** Do rong cot nhan trong bang. */
        const val LABEL_WIDTH = 24

        /** Duong ke ngan cach. */
        const val SEPARATOR = "----------------------------------------"
    }
}
