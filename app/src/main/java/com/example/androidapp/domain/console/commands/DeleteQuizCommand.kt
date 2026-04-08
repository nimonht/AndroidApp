package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh noi bo `del-quiz` — xoa quiz vinh vien hoac hang loat theo bo loc.
 *
 * Duoc goi tu [DeleteCommand] khi co flag `-q`/`--quiz`. Ho tro xoa theo ID,
 * loc theo chu so huu, tag, trang thai (draft/public/private), ngay tao,
 * va quiz khong co luot lam.
 *
 * Cac flag ho tro:
 * - `--owner <userId>`: chi xoa quiz cua chu so huu cu the.
 * - `--tag <tag>`: chi xoa quiz co tag cu the.
 * - `--draft`: chi xoa quiz nhap.
 * - `--public`: chi xoa quiz cong khai.
 * - `--private`: chi xoa quiz rieng tu.
 * - `--no-attempts`: chi xoa quiz chua co ai lam.
 * - `--deleted-only`: chi xoa quiz da nam trong thung rac.
 * - `--permanent`: xoa vinh vien (khong qua thung rac).
 * - `--before <timestamp>`: chi xoa quiz tao truoc thoi diem nay.
 * - `--after <timestamp>`: chi xoa quiz tao sau thoi diem nay.
 * - `--dry-run`: mo phong thao tac, khong thuc su xoa.
 * - `--confirm`: xac nhan thao tac huy diet.
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--verbose`: hien thi chi tiet.
 */
class DeleteQuizCommand : Command {

    override val name: String = "del-quiz"

    override val description: String = "Xoa quiz vinh vien theo ID hoac bo loc"

    override val usage: String =
        "del -q <quizId> [...] [--owner <userId>] [--tag <tag>] [--draft] [--public] [--private] " +
            "[--no-attempts] [--deleted-only] [--permanent] [--before <ts>] [--after <ts>] " +
            "[--dry-run] [--confirm] [--format <table|json>] [--verbose]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.DELETE_QUIZZES

    override val isDestructive: Boolean = true

    override val examples: List<Pair<String, String>> = listOf(
        "del -q quizId123 --confirm" to "Xoa quiz theo ID",
        "del -q quizId1 quizId2 --permanent --confirm" to "Xoa vinh vien nhieu quiz",
        "del -q --draft --owner userId1 --confirm" to "Xoa tat ca quiz nhap cua mot nguoi dung",
        "del -q --no-attempts --dry-run" to "Mo phong xoa quiz chua co ai lam",
        "del -q --deleted-only --permanent --confirm" to "Xoa vinh vien tat ca quiz trong thung rac",
        "del -q --tag math --before 1700000000000 --dry-run" to "Mo phong xoa quiz tag 'math' tao truoc moc thoi gian",
        "del -q --public --format json --verbose --dry-run" to "Mo phong xoa quiz cong khai, xuat JSON chi tiet"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--owner" to "Loc theo chu so huu (userId)",
            "--tag" to "Loc theo tag",
            "--draft" to "Chi quiz nhap",
            "--public" to "Chi quiz cong khai",
            "--private" to "Chi quiz rieng tu",
            "--no-attempts" to "Chi quiz chua co ai lam",
            "--deleted-only" to "Chi quiz da trong thung rac",
            "--permanent" to "Xoa vinh vien (khong qua thung rac)",
            "--before" to "Quiz tao truoc moc thoi gian (epoch ms)",
            "--after" to "Quiz tao sau moc thoi gian (epoch ms)",
            "--dry-run" to "Mo phong thao tac",
            "--confirm" to "Xac nhan thao tac xoa",
            "--format" to "Dinh dang dau ra (table/json)",
            "--verbose" to "Hien thi chi tiet"
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
        val permanent = "permanent" in flags
        val verbose = "verbose" in flags || "v" in flags
        val format = flags["format"]?.lowercase() ?: "table"
        val deletedOnly = "deleted-only" in flags
        val draftOnly = "draft" in flags
        val publicOnly = "public" in flags
        val privateOnly = "private" in flags
        val noAttempts = "no-attempts" in flags
        val ownerFilter = flags["owner"]
        val tagFilter = flags["tag"]
        val beforeFilter = flags["before"]?.toLongOrNull()
        val afterFilter = flags["after"]?.toLongOrNull()

        if (!dryRun && !confirm) {
            return CommandResult.error(
                "Thao tac huy diet: xoa quiz vinh vien. " +
                    "Su dung --confirm de xac nhan hoac --dry-run de mo phong."
            )
        }

        val adminRepo = context.repositories.adminRepository
        val quizzesToDelete = mutableListOf<Quiz>()

        if (args.isNotEmpty() && !hasFilterFlags(flags)) {
            // Xoa theo ID cu the
            val allQuizzes = adminRepo.getAllQuizzes(includeDeleted = true).first()
            for (quizId in args) {
                val quiz = allQuizzes.find { it.id == quizId }
                if (quiz == null) {
                    return CommandResult.error("Khong tim thay quiz voi ID: '$quizId'")
                }
                quizzesToDelete.add(quiz)
            }
        } else {
            // Xoa theo bo loc
            val includeDeleted = deletedOnly || permanent
            val allQuizzes = adminRepo.getAllQuizzes(includeDeleted = includeDeleted).first()
            var filtered = allQuizzes.toList()

            // Loc theo ID neu co args
            if (args.isNotEmpty()) {
                val idSet = args.toSet()
                filtered = filtered.filter { it.id in idSet }
            }

            filtered = applyFilters(
                quizzes = filtered,
                deletedOnly = deletedOnly,
                draftOnly = draftOnly,
                publicOnly = publicOnly,
                privateOnly = privateOnly,
                noAttempts = noAttempts,
                ownerFilter = ownerFilter,
                tagFilter = tagFilter,
                beforeFilter = beforeFilter,
                afterFilter = afterFilter
            )

            quizzesToDelete.addAll(filtered)
        }

        if (quizzesToDelete.isEmpty()) {
            return CommandResult.success("Khong tim thay quiz nao phu hop voi bo loc.")
        }

        if (dryRun) {
            return buildDryRunOutput(quizzesToDelete, permanent, verbose, format)
        }

        return executeDelete(quizzesToDelete, permanent, verbose, format, adminRepo)
    }

    /**
     * Kiem tra xem co flag loc nao duoc su dung khong.
     */
    private fun hasFilterFlags(flags: Map<String, String?>): Boolean {
        val filterKeys = setOf(
            "owner", "tag", "draft", "public", "private",
            "no-attempts", "deleted-only", "before", "after"
        )
        return flags.keys.any { it in filterKeys }
    }

    /**
     * Ap dung cac bo loc len danh sach quiz.
     *
     * @param quizzes Danh sach quiz goc.
     * @param deletedOnly Chi lay quiz da xoa mem.
     * @param draftOnly Chi lay quiz nhap.
     * @param publicOnly Chi lay quiz cong khai.
     * @param privateOnly Chi lay quiz rieng tu.
     * @param noAttempts Chi lay quiz chua co ai lam.
     * @param ownerFilter Loc theo chu so huu.
     * @param tagFilter Loc theo tag.
     * @param beforeFilter Loc quiz tao truoc moc thoi gian.
     * @param afterFilter Loc quiz tao sau moc thoi gian.
     * @return Danh sach quiz da loc.
     */
    private fun applyFilters(
        quizzes: List<Quiz>,
        deletedOnly: Boolean,
        draftOnly: Boolean,
        publicOnly: Boolean,
        privateOnly: Boolean,
        noAttempts: Boolean,
        ownerFilter: String?,
        tagFilter: String?,
        beforeFilter: Long?,
        afterFilter: Long?
    ): List<Quiz> {
        var result = quizzes

        if (deletedOnly) {
            result = result.filter { it.deletedAt != null }
        }

        if (draftOnly) {
            result = result.filter { it.isDraft }
        }

        if (publicOnly) {
            result = result.filter { it.isPublic }
        }

        if (privateOnly) {
            result = result.filter { !it.isPublic && !it.isDraft }
        }

        if (noAttempts) {
            result = result.filter { it.attemptCount == 0 }
        }

        if (ownerFilter != null) {
            result = result.filter { it.ownerId == ownerFilter }
        }

        if (tagFilter != null) {
            val tagLower = tagFilter.lowercase()
            result = result.filter { quiz ->
                quiz.tags.any { it.lowercase() == tagLower }
            }
        }

        if (beforeFilter != null) {
            result = result.filter { it.createdAt < beforeFilter }
        }

        if (afterFilter != null) {
            result = result.filter { it.createdAt > afterFilter }
        }

        return result
    }

    /**
     * Xay dung dau ra mo phong (dry-run) cho thao tac xoa quiz.
     */
    private fun buildDryRunOutput(
        quizzes: List<Quiz>,
        permanent: Boolean,
        verbose: Boolean,
        format: String
    ): CommandResult {
        if (format == "json") {
            return buildDryRunJson(quizzes, permanent)
        }

        val lines = mutableListOf<OutputLine>()
        val action = if (permanent) "xoa vinh vien" else "xoa (vao thung rac)"
        lines.add(OutputLine("[DRY-RUN] Mo phong $action quiz", OutputStyle.WARNING))
        lines.add(OutputLine(""))

        lines.add(
            OutputLine(
                padRight("ID", 24) + padRight("Tieu de", 30) + padRight("Trang thai", 14) +
                    padRight("Luot lam", 10),
                OutputStyle.TABLE_HEADER
            )
        )

        for (quiz in quizzes) {
            val status = buildStatusLabel(quiz)
            lines.add(
                OutputLine(
                    padRight(quiz.id, 24) + padRight(truncate(quiz.title, 28), 30) +
                        padRight(status, 14) + padRight(quiz.attemptCount.toString(), 10),
                    OutputStyle.TABLE_ROW
                )
            )

            if (verbose) {
                lines.add(OutputLine("  Chu so huu: ${quiz.ownerId}", OutputStyle.MUTED))
                if (quiz.tags.isNotEmpty()) {
                    lines.add(OutputLine("  Tags: ${quiz.tags.joinToString(", ")}", OutputStyle.MUTED))
                }
                lines.add(OutputLine("  Tao: ${formatTimestamp(quiz.createdAt)}", OutputStyle.MUTED))
                if (quiz.deletedAt != null) {
                    lines.add(OutputLine("  Xoa mem: ${formatTimestamp(quiz.deletedAt)}", OutputStyle.MUTED))
                }
            }
        }

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "[DRY-RUN] Se $action ${quizzes.size} quiz.",
                OutputStyle.WARNING
            )
        )
        lines.add(OutputLine("Them --confirm va bo --dry-run de thuc hien.", OutputStyle.MUTED))

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra dry-run dinh dang JSON.
     */
    private fun buildDryRunJson(quizzes: List<Quiz>, permanent: Boolean): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"dryRun\": true,", OutputStyle.CODE))
        lines.add(OutputLine("  \"permanent\": $permanent,", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${quizzes.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"quizzes\": [", OutputStyle.CODE))

        for ((index, quiz) in quizzes.withIndex()) {
            val comma = if (index < quizzes.size - 1) "," else ""
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${escapeJson(quiz.id)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"title\": \"${escapeJson(quiz.title)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"ownerId\": \"${escapeJson(quiz.ownerId)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"isPublic\": ${quiz.isPublic},", OutputStyle.CODE))
            lines.add(OutputLine("      \"isDraft\": ${quiz.isDraft},", OutputStyle.CODE))
            lines.add(OutputLine("      \"attemptCount\": ${quiz.attemptCount},", OutputStyle.CODE))
            val deletedAtStr = quiz.deletedAt?.toString() ?: "null"
            lines.add(OutputLine("      \"deletedAt\": $deletedAtStr", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Thuc hien xoa quiz that su.
     */
    private suspend fun executeDelete(
        quizzes: List<Quiz>,
        permanent: Boolean,
        verbose: Boolean,
        format: String,
        adminRepo: com.example.androidapp.domain.repository.AdminRepository
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        for (quiz in quizzes) {
            if (verbose) {
                lines.add(
                    OutputLine(
                        "Dang xoa: ${truncate(quiz.title, 40)} (${quiz.id})...",
                        OutputStyle.INFO
                    )
                )
            }

            val result = adminRepo.deleteQuizPermanently(quiz.id)
            if (result.isSuccess) {
                successCount++
                lines.add(
                    OutputLine(
                        "Da xoa: ${truncate(quiz.title, 40)} (${quiz.id})",
                        OutputStyle.SUCCESS
                    )
                )
            } else {
                failCount++
                val errorMsg = result.exceptionOrNull()?.message ?: "Loi khong xac dinh"
                errors.add("${quiz.id}: $errorMsg")
                lines.add(
                    OutputLine(
                        "Loi khi xoa '${truncate(quiz.title, 30)}': $errorMsg",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        if (format == "json") {
            return buildDeleteResultJson(quizzes.size, successCount, failCount, errors, permanent)
        }

        lines.add(OutputLine(""))
        val action = if (permanent) "xoa vinh vien" else "xoa"
        lines.add(OutputLine("== Ket qua $action quiz ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so     : ${quizzes.size}", OutputStyle.NORMAL))
        lines.add(OutputLine("  Thanh cong  : $successCount", OutputStyle.SUCCESS))

        if (failCount > 0) {
            lines.add(OutputLine("  That bai    : $failCount", OutputStyle.ERROR))
        }

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
        errors: List<String>,
        permanent: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"operation\": \"deleteQuizzes\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"permanent\": $permanent,", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"success\": $success,", OutputStyle.CODE))
        lines.add(OutputLine("  \"failed\": $failed,", OutputStyle.CODE))

        if (errors.isNotEmpty()) {
            lines.add(OutputLine("  \"errors\": [", OutputStyle.CODE))
            for ((index, err) in errors.withIndex()) {
                val comma = if (index < errors.size - 1) "," else ""
                lines.add(OutputLine("    \"${escapeJson(err)}\"$comma", OutputStyle.CODE))
            }
            lines.add(OutputLine("  ]", OutputStyle.CODE))
        } else {
            lines.add(OutputLine("  \"errors\": []", OutputStyle.CODE))
        }

        lines.add(OutputLine("}", OutputStyle.CODE))

        val isSuccess = failed == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
    }

    /**
     * Tao nhan trang thai cho quiz.
     */
    private fun buildStatusLabel(quiz: Quiz): String {
        return when {
            quiz.deletedAt != null -> "Da xoa"
            quiz.isDraft -> "Nhap"
            quiz.isPublic -> "Cong khai"
            else -> "Rieng tu"
        }
    }

    /**
     * Dinh dang timestamp thanh chuoi ngay gio doc duoc.
     */
    private fun formatTimestamp(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    /**
     * Cat ngan chuoi va them "..." neu qua dai.
     */
    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text else text.take(maxLength - 3) + "..."
    }

    /**
     * Can chuoi ve do dai co dinh.
     */
    private fun padRight(text: String, length: Int): String {
        return if (text.length >= length) text.take(length) else text.padEnd(length)
    }

    /**
     * Thoat ky tu dac biet trong chuoi JSON.
     */
    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
