package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandFormatUtils
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh noi bo `del-attempt` — xoa luot lam quiz (attempt) theo ID hoac bo loc.
 *
 * Duoc goi tu [DeleteCommand] khi co flag `-a`/`--attempt`. Ho tro xoa theo
 * ID cu the, loc theo nguoi dung, quiz, khoang thoi gian, diem so, va trang
 * thai hoan thanh.
 *
 * Cac flag ho tro:
 * - `--user <userId>`: chi xoa attempt cua nguoi dung cu the.
 * - `--quiz <quizId>`: chi xoa attempt cua quiz cu the.
 * - `--before <timestamp>`: chi xoa attempt bat dau truoc thoi diem nay (epoch ms).
 * - `--after <timestamp>`: chi xoa attempt bat dau sau thoi diem nay (epoch ms).
 * - `--score-below <n>`: chi xoa attempt co diem thap hon n.
 * - `--score-above <n>`: chi xoa attempt co diem cao hon n.
 * - `--incomplete`: chi xoa attempt chua hoan thanh (endTimeMillis == null).
 * - `--dry-run`: mo phong thao tac, khong thuc su xoa.
 * - `--confirm`: xac nhan thao tac huy diet (bat buoc neu khong co dry-run).
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--verbose`: hien thi chi tiet.
 * - `--quiet`: chi hien thi ket qua tom tat.
 * - `--limit <n>`: gioi han so luong attempt xoa (mac dinh: khong gioi han).
 */
class DeleteAttemptCommand : Command {

    override val name: String = "del-attempt"

    override val description: String = "Xoa luot lam quiz theo ID hoac bo loc"

    override val usage: String =
        "del -a <attemptId> [...] [--user <userId>] [--quiz <quizId>] [--before <ts>] " +
                "[--after <ts>] [--score-below <n>] [--score-above <n>] [--incomplete] " +
                "[--limit <n>] [--dry-run] [--confirm] [--format <table|json>] [--verbose] [--quiet]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.MANAGE_QUIZZES

    override val isDestructive: Boolean = true

    override val examples: List<Pair<String, String>> = listOf(
        "del -a attemptId123 --confirm" to "Xoa attempt theo ID",
        "del -a att1 att2 att3 --confirm" to "Xoa nhieu attempt theo ID",
        "del -a --user userId1 --confirm" to "Xoa tat ca attempt cua mot nguoi dung",
        "del -a --quiz quizId1 --confirm" to "Xoa tat ca attempt cua mot quiz",
        "del -a --incomplete --dry-run" to "Mo phong xoa tat ca attempt chua hoan thanh",
        "del -a --score-below 3 --quiz quizId1 --dry-run" to "Mo phong xoa attempt diem thap cua quiz",
        "del -a --before 1700000000000 --limit 50 --confirm" to "Xoa toi da 50 attempt cu",
        "del -a --user userId1 --after 1700000000000 --format json --verbose --dry-run" to
                "Mo phong xoa attempt cua nguoi dung sau moc thoi gian, xuat JSON"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--user" to "Loc theo nguoi dung (userId)",
            "--quiz" to "Loc theo quiz (quizId)",
            "--before" to "Attempt bat dau truoc moc thoi gian (epoch ms)",
            "--after" to "Attempt bat dau sau moc thoi gian (epoch ms)",
            "--score-below" to "Diem thap hon gia tri chi dinh",
            "--score-above" to "Diem cao hon gia tri chi dinh",
            "--incomplete" to "Chi attempt chua hoan thanh",
            "--limit" to "Gioi han so luong attempt xoa",
            "--dry-run" to "Mo phong thao tac, khong thuc su xoa",
            "--confirm" to "Xac nhan thao tac xoa",
            "--format" to "Dinh dang dau ra (table/json)",
            "--verbose" to "Hien thi chi tiet",
            "--quiet" to "Chi hien thi tom tat"
        )
        val usedFlags = flags.keys.map { "--$it" }.toSet()

        for ((flag, desc) in availableFlags) {
            if (flag !in usedFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = flag,
                        description = desc,
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        if ("format" in flags && flags["format"] == null) {
            suggestions.clear()
            suggestions.add(
                CompletionSuggestion(text = "table", description = "Dinh dang bang", type = SuggestionType.ARGUMENT)
            )
            suggestions.add(
                CompletionSuggestion(text = "json", description = "Dinh dang JSON", type = SuggestionType.ARGUMENT)
            )
        }

        return suggestions
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val dryRun = "dry-run" in flags
        val confirm = "confirm" in flags
        val verbose = "verbose" in flags || "v" in flags
        val quiet = "quiet" in flags || "q" in flags
        val format = flags["format"]?.lowercase() ?: "table"
        val incomplete = "incomplete" in flags
        val userFilter = flags["user"]
        val quizFilter = flags["quiz"]
        val beforeFilter = flags["before"]?.toLongOrNull()
        val afterFilter = flags["after"]?.toLongOrNull()
        val scoreBelowFilter = flags["score-below"]?.toIntOrNull()
        val scoreAboveFilter = flags["score-above"]?.toIntOrNull()
        val limit = flags["limit"]?.toIntOrNull()

        if (!dryRun && !confirm) {
            return CommandResult.error(
                "Thao tac huy diet: xoa luot lam quiz vinh vien. " +
                        "Su dung --confirm de xac nhan hoac --dry-run de mo phong."
            )
        }

        val adminRepo = context.repositories.adminRepository
        val attemptsToDelete = mutableListOf<Attempt>()

        if (args.isNotEmpty() && !hasFilterFlags(flags)) {
            // Xoa theo ID cu the
            val allAttempts = adminRepo.getAllAttempts().first()
            val attemptMap = allAttempts.associateBy { it.id }
            for (attemptId in args) {
                val attempt = attemptMap[attemptId]
                if (attempt == null) {
                    return CommandResult.error("Khong tim thay attempt voi ID: '$attemptId'")
                }
                attemptsToDelete.add(attempt)
            }
        } else {
            // Xoa theo bo loc
            val allAttempts = adminRepo.getAllAttempts().first()
            var filtered = allAttempts.toList()

            // Loc theo ID neu co args kem filter
            if (args.isNotEmpty()) {
                val idSet = args.toSet()
                filtered = filtered.filter { it.id in idSet }
            }

            filtered = applyFilters(
                attempts = filtered,
                userFilter = userFilter,
                quizFilter = quizFilter,
                beforeFilter = beforeFilter,
                afterFilter = afterFilter,
                scoreBelowFilter = scoreBelowFilter,
                scoreAboveFilter = scoreAboveFilter,
                incomplete = incomplete
            )

            if (limit != null && limit > 0) {
                filtered = filtered.take(limit)
            }

            attemptsToDelete.addAll(filtered)
        }

        if (attemptsToDelete.isEmpty()) {
            return CommandResult.success("Khong tim thay attempt nao phu hop voi bo loc.")
        }

        if (dryRun) {
            return buildDryRunOutput(attemptsToDelete, verbose, quiet, format)
        }

        return executeDelete(attemptsToDelete, verbose, quiet, format, adminRepo)
    }

    /**
     * Kiem tra xem co flag loc nao duoc su dung khong.
     */
    private fun hasFilterFlags(flags: Map<String, String?>): Boolean {
        val filterKeys = setOf(
            "user", "quiz", "before", "after",
            "score-below", "score-above", "incomplete", "limit"
        )
        return flags.keys.any { it in filterKeys }
    }

    /**
     * Ap dung cac bo loc len danh sach attempt.
     *
     * @param attempts Danh sach attempt goc.
     * @param userFilter Loc theo userId.
     * @param quizFilter Loc theo quizId.
     * @param beforeFilter Loc attempt bat dau truoc moc thoi gian (epoch ms).
     * @param afterFilter Loc attempt bat dau sau moc thoi gian (epoch ms).
     * @param scoreBelowFilter Loc attempt co diem thap hon gia tri nay.
     * @param scoreAboveFilter Loc attempt co diem cao hon gia tri nay.
     * @param incomplete Chi lay attempt chua hoan thanh.
     * @return Danh sach attempt da loc.
     */
    private fun applyFilters(
        attempts: List<Attempt>,
        userFilter: String?,
        quizFilter: String?,
        beforeFilter: Long?,
        afterFilter: Long?,
        scoreBelowFilter: Int?,
        scoreAboveFilter: Int?,
        incomplete: Boolean
    ): List<Attempt> {
        var result = attempts

        if (userFilter != null) {
            result = result.filter { it.userId == userFilter }
        }

        if (quizFilter != null) {
            result = result.filter { it.quizId == quizFilter }
        }

        if (beforeFilter != null) {
            result = result.filter { it.startTimeMillis < beforeFilter }
        }

        if (afterFilter != null) {
            result = result.filter { it.startTimeMillis > afterFilter }
        }

        if (scoreBelowFilter != null) {
            result = result.filter { it.score < scoreBelowFilter }
        }

        if (scoreAboveFilter != null) {
            result = result.filter { it.score > scoreAboveFilter }
        }

        if (incomplete) {
            result = result.filter { it.endTimeMillis == null }
        }

        return result
    }

    /**
     * Xay dung dau ra mo phong (dry-run) cho thao tac xoa attempt.
     */
    private fun buildDryRunOutput(
        attempts: List<Attempt>,
        verbose: Boolean,
        quiet: Boolean,
        format: String
    ): CommandResult {
        if (format == "json") {
            return buildDryRunJson(attempts)
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[DRY-RUN] Mo phong xoa luot lam quiz", OutputStyle.WARNING))
        lines.add(OutputLine(""))

        if (!quiet) {
            lines.add(
                OutputLine(
                    CommandFormatUtils.padRight("ID", 24) + CommandFormatUtils.padRight(
                        "User ID",
                        20
                    ) + CommandFormatUtils.padRight("Quiz ID", 20) +
                            CommandFormatUtils.padRight("Diem", 8) + CommandFormatUtils.padRight("Trang thai", 14),
                    OutputStyle.TABLE_HEADER
                )
            )

            for (attempt in attempts) {
                val status = if (attempt.endTimeMillis != null) "Hoan thanh" else "Dang lam"
                val scoreStr = "${attempt.score}/${attempt.totalQuestions}"
                lines.add(
                    OutputLine(
                        CommandFormatUtils.padRight(
                            attempt.id,
                            24
                        ) + CommandFormatUtils.padRight(CommandFormatUtils.truncate(attempt.userId, 18), 20) +
                                CommandFormatUtils.padRight(CommandFormatUtils.truncate(attempt.quizId, 18), 20) +
                                CommandFormatUtils.padRight(scoreStr, 8) + CommandFormatUtils.padRight(status, 14),
                        OutputStyle.TABLE_ROW
                    )
                )

                if (verbose) {
                    lines.add(
                        OutputLine(
                            "  Bat dau: ${CommandFormatUtils.formatTimestamp(attempt.startTimeMillis)}",
                            OutputStyle.MUTED
                        )
                    )
                    if (attempt.endTimeMillis != null) {
                        lines.add(
                            OutputLine(
                                "  Ket thuc: ${CommandFormatUtils.formatTimestamp(attempt.endTimeMillis)}",
                                OutputStyle.MUTED
                            )
                        )
                        val durationSec = (attempt.endTimeMillis - attempt.startTimeMillis) / 1000
                        lines.add(
                            OutputLine(
                                "  Thoi gian: ${CommandFormatUtils.formatDuration(durationSec)}",
                                OutputStyle.MUTED
                            )
                        )
                    }
                    lines.add(
                        OutputLine(
                            "  So cau tra loi: ${attempt.answers.size}",
                            OutputStyle.MUTED
                        )
                    )
                }
            }
        }

        lines.add(OutputLine(""))

        // Thong ke tom tat
        val completedCount = attempts.count { it.endTimeMillis != null }
        val incompleteCount = attempts.size - completedCount
        val avgScore = if (attempts.isNotEmpty()) {
            attempts.sumOf { it.score }.toDouble() / attempts.size
        } else {
            0.0
        }
        val uniqueUsers = attempts.map { it.userId }.distinct().size
        val uniqueQuizzes = attempts.map { it.quizId }.distinct().size

        lines.add(OutputLine("[DRY-RUN] Se xoa ${attempts.size} attempt.", OutputStyle.WARNING))
        if (!quiet) {
            lines.add(
                OutputLine(
                    "  Hoan thanh: $completedCount | Dang lam: $incompleteCount",
                    OutputStyle.MUTED
                )
            )
            lines.add(
                OutputLine(
                    "  Nguoi dung lien quan: $uniqueUsers | Quiz lien quan: $uniqueQuizzes",
                    OutputStyle.MUTED
                )
            )
            lines.add(
                OutputLine(
                    "  Diem trung binh: ${"%.1f".format(avgScore)}",
                    OutputStyle.MUTED
                )
            )
        }
        lines.add(OutputLine("Them --confirm va bo --dry-run de thuc hien.", OutputStyle.MUTED))

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra dry-run dinh dang JSON.
     */
    private fun buildDryRunJson(attempts: List<Attempt>): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"dryRun\": true,", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${attempts.size},", OutputStyle.CODE))

        val uniqueUsers = attempts.map { it.userId }.distinct().size
        val uniqueQuizzes = attempts.map { it.quizId }.distinct().size
        lines.add(OutputLine("  \"uniqueUsers\": $uniqueUsers,", OutputStyle.CODE))
        lines.add(OutputLine("  \"uniqueQuizzes\": $uniqueQuizzes,", OutputStyle.CODE))

        lines.add(OutputLine("  \"attempts\": [", OutputStyle.CODE))

        for ((index, attempt) in attempts.withIndex()) {
            val comma = if (index < attempts.size - 1) "," else ""
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${CommandFormatUtils.escapeJson(attempt.id)}\",", OutputStyle.CODE))
            lines.add(
                OutputLine(
                    "      \"userId\": \"${CommandFormatUtils.escapeJson(attempt.userId)}\",",
                    OutputStyle.CODE
                )
            )
            lines.add(
                OutputLine(
                    "      \"quizId\": \"${CommandFormatUtils.escapeJson(attempt.quizId)}\",",
                    OutputStyle.CODE
                )
            )
            lines.add(OutputLine("      \"score\": ${attempt.score},", OutputStyle.CODE))
            lines.add(OutputLine("      \"totalQuestions\": ${attempt.totalQuestions},", OutputStyle.CODE))
            lines.add(OutputLine("      \"startTimeMillis\": ${attempt.startTimeMillis},", OutputStyle.CODE))
            val endStr = attempt.endTimeMillis?.toString() ?: "null"
            lines.add(OutputLine("      \"endTimeMillis\": $endStr", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Thuc hien xoa attempt that su.
     */
    private suspend fun executeDelete(
        attempts: List<Attempt>,
        verbose: Boolean,
        quiet: Boolean,
        format: String,
        adminRepo: com.example.androidapp.domain.repository.AdminRepository
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        for (attempt in attempts) {
            if (verbose && !quiet) {
                lines.add(
                    OutputLine(
                        "Dang xoa attempt: ${attempt.id} " +
                                "(user=${CommandFormatUtils.truncate(attempt.userId, 12)}, " +
                                "quiz=${CommandFormatUtils.truncate(attempt.quizId, 12)}, " +
                                "diem=${attempt.score}/${attempt.totalQuestions})...",
                        OutputStyle.INFO
                    )
                )
            }

            val result = adminRepo.deleteAttempt(attempt.id)
            if (result.isSuccess) {
                successCount++
                if (!quiet) {
                    lines.add(
                        OutputLine(
                            "Da xoa attempt: ${attempt.id}",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                failCount++
                val errorMsg = result.exceptionOrNull()?.message ?: "Loi khong xac dinh"
                errors.add("${attempt.id}: $errorMsg")
                if (!quiet) {
                    lines.add(
                        OutputLine(
                            "Loi khi xoa attempt ${attempt.id}: $errorMsg",
                            OutputStyle.ERROR
                        )
                    )
                }
            }
        }

        if (format == "json") {
            return buildDeleteResultJson(attempts.size, successCount, failCount, errors)
        }

        lines.add(OutputLine(""))
        lines.add(OutputLine("== Ket qua xoa luot lam quiz ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so     : ${attempts.size}", OutputStyle.NORMAL))
        lines.add(OutputLine("  Thanh cong  : $successCount", OutputStyle.SUCCESS))

        if (failCount > 0) {
            lines.add(OutputLine("  That bai    : $failCount", OutputStyle.ERROR))
        }

        // Thong tin bo sung
        val uniqueUsers = attempts.map { it.userId }.distinct().size
        val uniqueQuizzes = attempts.map { it.quizId }.distinct().size
        lines.add(
            OutputLine(
                "  Nguoi dung lien quan: $uniqueUsers | Quiz lien quan: $uniqueQuizzes",
                OutputStyle.MUTED
            )
        )

        val isSuccess = failCount == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
    }

    /**
     * Xay dung ket qua xoa dinh dang JSON.
     */
    private fun buildDeleteResultJson(
        total: Int,
        success: Int,
        failed: Int,
        errors: List<String>
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"operation\": \"deleteAttempts\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"success\": $success,", OutputStyle.CODE))
        lines.add(OutputLine("  \"failed\": $failed,", OutputStyle.CODE))

        if (errors.isNotEmpty()) {
            lines.add(OutputLine("  \"errors\": [", OutputStyle.CODE))
            for ((index, err) in errors.withIndex()) {
                val comma = if (index < errors.size - 1) "," else ""
                lines.add(OutputLine("    \"${CommandFormatUtils.escapeJson(err)}\"$comma", OutputStyle.CODE))
            }
            lines.add(OutputLine("  ]", OutputStyle.CODE))
        } else {
            lines.add(OutputLine("  \"errors\": []", OutputStyle.CODE))
        }

        lines.add(OutputLine("}", OutputStyle.CODE))

        val isSuccess = failed == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
    }

}
