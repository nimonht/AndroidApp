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
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh `unpublish` — go xuat ban quiz (unpublish).
 *
 * Cho phep admin go xuat ban quiz cong khai theo ID hoac bo loc. Khi go xuat ban,
 * quiz se duoc dat `isPublic = false`, lam cho quiz khong con hien thi trong
 * ket qua tim kiem cong khai. Quiz van giu nguyen trang thai `isDraft` hien tai.
 *
 * Day la thao tac kiem duyet noi dung — dung de an cac quiz vi pham quy dinh
 * hoac chua san sang de cong khai.
 *
 * Cac flag ho tro:
 * - `--owner <userId>`: chi go xuat ban quiz cua chu so huu cu the.
 * - `--tag <tag>`: chi go xuat ban quiz co tag cu the.
 * - `--search <query>`: tim quiz theo tu khoa truoc khi go xuat ban.
 * - `--no-attempts`: chi go xuat ban quiz chua co ai lam.
 * - `--before <timestamp>`: chi go xuat ban quiz tao truoc thoi diem nay (epoch ms).
 * - `--after <timestamp>`: chi go xuat ban quiz tao sau thoi diem nay (epoch ms).
 * - `--dry-run`: mo phong thao tac, khong thuc su go xuat ban.
 * - `--confirm`: xac nhan thao tac huy diet (bat buoc neu khong co dry-run).
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--verbose`: hien thi chi tiet.
 */
class UnpublishCommand : Command {

    override val name: String = "unpublish"

    override val aliases: List<String> = listOf("unpub")

    override val description: String = "Go xuat ban quiz cong khai (unpublish)"

    override val usage: String =
        "unpublish <quizId> [...] [--owner <userId>] [--tag <tag>] [--search <query>] " +
                "[--no-attempts] [--before <ts>] [--after <ts>] [--dry-run] [--confirm] " +
                "[--format <table|json>] [--verbose]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.PUBLISH_QUIZZES

    override val isDestructive: Boolean = true

    override val examples: List<Pair<String, String>> = listOf(
        "unpublish quizId123 --confirm" to "Go xuat ban quiz theo ID",
        "unpublish qid1 qid2 --confirm" to "Go xuat ban nhieu quiz theo ID",
        "unpublish --owner userId1 --confirm" to "Go xuat ban tat ca quiz cong khai cua mot nguoi dung",
        "unpublish --tag math --dry-run" to "Mo phong go xuat ban quiz co tag 'math'",
        "unpublish --search \"noi dung vi pham\" --dry-run" to "Mo phong go xuat ban quiz tim theo tu khoa",
        "unpublish --no-attempts --dry-run" to "Mo phong go xuat ban quiz chua co ai lam",
        "unpublish --before 1700000000000 --confirm" to "Go xuat ban quiz cong khai tao truoc moc thoi gian",
        "unpublish quizId123 --format json --verbose --dry-run" to "Mo phong go xuat ban, xuat JSON chi tiet",
        "unpub --tag science --owner userId1 --confirm" to "Go xuat ban quiz cua nguoi dung co tag cu the (dung alias)"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--owner" to "Loc theo chu so huu (userId)",
            "--tag" to "Loc theo tag",
            "--search" to "Tim quiz theo tu khoa",
            "--no-attempts" to "Chi quiz chua co ai lam",
            "--before" to "Quiz tao truoc moc thoi gian (epoch ms)",
            "--after" to "Quiz tao sau moc thoi gian (epoch ms)",
            "--dry-run" to "Mo phong thao tac, khong thuc su go xuat ban",
            "--confirm" to "Xac nhan thao tac go xuat ban",
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
        val verbose = "verbose" in flags || "v" in flags
        val format = flags["format"]?.lowercase() ?: "table"
        val ownerFilter = flags["owner"]
        val tagFilter = flags["tag"]
        val searchQuery = flags["search"]
        val noAttempts = "no-attempts" in flags
        val beforeFilter = flags["before"]?.toLongOrNull()
        val afterFilter = flags["after"]?.toLongOrNull()

        if (!dryRun && !confirm) {
            return CommandResult.error(
                "Thao tac huy diet: go xuat ban quiz (isPublic=false). " +
                        "Quiz se khong con hien thi trong tim kiem cong khai. " +
                        "Su dung --confirm de xac nhan hoac --dry-run de mo phong."
            )
        }

        val adminRepo = context.repositories.adminRepository
        val quizzesToUnpublish = mutableListOf<Quiz>()

        if (args.isNotEmpty() && !hasFilterFlags(flags)) {
            // Go xuat ban theo ID cu the
            val allQuizzes = adminRepo.getAllQuizzes(includeDeleted = false).first()
            val quizMap = allQuizzes.associateBy { it.id }
            for (quizId in args) {
                val quiz = quizMap[quizId]
                if (quiz == null) {
                    return CommandResult.error("Khong tim thay quiz voi ID: '$quizId'")
                }
                if (!quiz.isPublic) {
                    return CommandResult.error(
                        "Quiz '${CommandFormatUtils.truncate(quiz.title, 40)}' ($quizId) khong phai la quiz cong khai."
                    )
                }
                if (quiz.deletedAt != null) {
                    return CommandResult.error(
                        "Quiz '${CommandFormatUtils.truncate(quiz.title, 40)}' ($quizId) da bi xoa."
                    )
                }
                quizzesToUnpublish.add(quiz)
            }
        } else {
            // Go xuat ban theo bo loc
            var allQuizzes = adminRepo.getAllQuizzes(includeDeleted = false).first()

            // Chi lay quiz dang cong khai va chua bi xoa
            allQuizzes = allQuizzes.filter { it.isPublic && it.deletedAt == null }

            // Tim kiem theo tu khoa
            if (searchQuery != null) {
                val searchResults = adminRepo.searchQuizzes(searchQuery, includeDeleted = false).first()
                val searchIds = searchResults.map { it.id }.toSet()
                allQuizzes = allQuizzes.filter { it.id in searchIds }
            }

            var filtered = allQuizzes.toList()

            // Loc theo ID neu co args kem filter
            if (args.isNotEmpty()) {
                val idSet = args.toSet()
                filtered = filtered.filter { it.id in idSet }
            }

            filtered = applyFilters(
                quizzes = filtered,
                ownerFilter = ownerFilter,
                tagFilter = tagFilter,
                noAttempts = noAttempts,
                beforeFilter = beforeFilter,
                afterFilter = afterFilter
            )

            quizzesToUnpublish.addAll(filtered)
        }

        if (quizzesToUnpublish.isEmpty()) {
            return CommandResult.success("Khong tim thay quiz cong khai nao phu hop voi bo loc.")
        }

        if (dryRun) {
            return buildDryRunOutput(quizzesToUnpublish, verbose, format)
        }

        return executeUnpublish(quizzesToUnpublish, verbose, format, adminRepo)
    }

    /**
     * Kiem tra xem co flag loc nao duoc su dung khong.
     */
    private fun hasFilterFlags(flags: Map<String, String?>): Boolean {
        val filterKeys = setOf("owner", "tag", "search", "no-attempts", "before", "after")
        return flags.keys.any { it in filterKeys }
    }

    /**
     * Ap dung cac bo loc len danh sach quiz cong khai.
     *
     * @param quizzes Danh sach quiz goc (da loc chi cong khai).
     * @param ownerFilter Loc theo chu so huu.
     * @param tagFilter Loc theo tag.
     * @param noAttempts Chi lay quiz chua co ai lam.
     * @param beforeFilter Loc quiz tao truoc moc thoi gian.
     * @param afterFilter Loc quiz tao sau moc thoi gian.
     * @return Danh sach quiz da loc.
     */
    private fun applyFilters(
        quizzes: List<Quiz>,
        ownerFilter: String?,
        tagFilter: String?,
        noAttempts: Boolean,
        beforeFilter: Long?,
        afterFilter: Long?
    ): List<Quiz> {
        var result = quizzes

        if (ownerFilter != null) {
            result = result.filter { it.ownerId == ownerFilter }
        }

        if (tagFilter != null) {
            val tagLower = tagFilter.lowercase()
            result = result.filter { quiz ->
                quiz.tags.any { it.lowercase() == tagLower }
            }
        }

        if (noAttempts) {
            result = result.filter { it.attemptCount == 0 }
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
     * Xay dung dau ra mo phong (dry-run) cho thao tac go xuat ban quiz.
     */
    private fun buildDryRunOutput(
        quizzes: List<Quiz>,
        verbose: Boolean,
        format: String
    ): CommandResult {
        if (format == "json") {
            return buildDryRunJson(quizzes)
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[DRY-RUN] Mo phong go xuat ban quiz", OutputStyle.WARNING))
        lines.add(OutputLine(""))

        lines.add(
            OutputLine(
                CommandFormatUtils.padRight("ID", 24) + CommandFormatUtils.padRight(
                    "Tieu de",
                    30
                ) + CommandFormatUtils.padRight("Chu so huu", 18) +
                        CommandFormatUtils.padRight("Luot lam", 10),
                OutputStyle.TABLE_HEADER
            )
        )

        val totalAttempts = quizzes.sumOf { it.attemptCount }
        val uniqueOwners = quizzes.map { it.ownerId }.distinct().size

        for (quiz in quizzes) {
            lines.add(
                OutputLine(
                    CommandFormatUtils.padRight(quiz.id, 24) + CommandFormatUtils.padRight(
                        CommandFormatUtils.truncate(
                            quiz.title,
                            28
                        ), 30
                    ) +
                            CommandFormatUtils.padRight(CommandFormatUtils.truncate(quiz.ownerId, 16), 18) +
                            CommandFormatUtils.padRight(quiz.attemptCount.toString(), 10),
                    OutputStyle.TABLE_ROW
                )
            )

            if (verbose) {
                lines.add(
                    OutputLine(
                        "  Tac gia: ${quiz.authorName.ifBlank { "(khong ro)" }}",
                        OutputStyle.MUTED
                    )
                )
                if (quiz.tags.isNotEmpty()) {
                    lines.add(
                        OutputLine(
                            "  Tags: ${quiz.tags.joinToString(", ")}",
                            OutputStyle.MUTED
                        )
                    )
                }
                lines.add(
                    OutputLine(
                        "  So cau hoi: ${quiz.questionCount}",
                        OutputStyle.MUTED
                    )
                )
                lines.add(
                    OutputLine(
                        "  Tao: ${CommandFormatUtils.formatTimestamp(quiz.createdAt)} | Cap nhat: ${
                            CommandFormatUtils.formatTimestamp(
                                quiz.updatedAt
                            )
                        }",
                        OutputStyle.MUTED
                    )
                )
                if (quiz.shareCode != null) {
                    lines.add(
                        OutputLine(
                            "  Share code: ${quiz.shareCode}",
                            OutputStyle.MUTED
                        )
                    )
                }
            }
        }

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "[DRY-RUN] Se go xuat ban ${quizzes.size} quiz.",
                OutputStyle.WARNING
            )
        )
        lines.add(
            OutputLine(
                "  Chu so huu lien quan: $uniqueOwners | Tong luot lam bi anh huong: $totalAttempts",
                OutputStyle.MUTED
            )
        )
        lines.add(
            OutputLine(
                "Tac dong: isPublic=false cho tat ca quiz tren. Quiz se khong con hien thi trong tim kiem cong khai.",
                OutputStyle.INFO
            )
        )
        lines.add(OutputLine("Them --confirm va bo --dry-run de thuc hien.", OutputStyle.MUTED))

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra dry-run dinh dang JSON.
     */
    private fun buildDryRunJson(quizzes: List<Quiz>): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"dryRun\": true,", OutputStyle.CODE))
        lines.add(OutputLine("  \"operation\": \"unpublish\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${quizzes.size},", OutputStyle.CODE))

        val totalAttempts = quizzes.sumOf { it.attemptCount }
        val uniqueOwners = quizzes.map { it.ownerId }.distinct().size
        lines.add(OutputLine("  \"totalAffectedAttempts\": $totalAttempts,", OutputStyle.CODE))
        lines.add(OutputLine("  \"uniqueOwners\": $uniqueOwners,", OutputStyle.CODE))

        lines.add(OutputLine("  \"quizzes\": [", OutputStyle.CODE))

        for ((index, quiz) in quizzes.withIndex()) {
            val comma = if (index < quizzes.size - 1) "," else ""
            val tagsStr = quiz.tags.joinToString(", ") { "\"${CommandFormatUtils.escapeJson(it)}\"" }
            val shareCodeStr =
                if (quiz.shareCode != null) "\"${CommandFormatUtils.escapeJson(quiz.shareCode)}\"" else "null"
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${CommandFormatUtils.escapeJson(quiz.id)}\",", OutputStyle.CODE))
            lines.add(
                OutputLine(
                    "      \"title\": \"${CommandFormatUtils.escapeJson(quiz.title)}\",",
                    OutputStyle.CODE
                )
            )
            lines.add(
                OutputLine(
                    "      \"ownerId\": \"${CommandFormatUtils.escapeJson(quiz.ownerId)}\",",
                    OutputStyle.CODE
                )
            )
            lines.add(
                OutputLine(
                    "      \"authorName\": \"${CommandFormatUtils.escapeJson(quiz.authorName)}\",",
                    OutputStyle.CODE
                )
            )
            lines.add(OutputLine("      \"questionCount\": ${quiz.questionCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"attemptCount\": ${quiz.attemptCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"tags\": [$tagsStr],", OutputStyle.CODE))
            lines.add(OutputLine("      \"shareCode\": $shareCodeStr,", OutputStyle.CODE))
            lines.add(OutputLine("      \"createdAt\": ${quiz.createdAt},", OutputStyle.CODE))
            lines.add(OutputLine("      \"updatedAt\": ${quiz.updatedAt}", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Thuc hien go xuat ban quiz that su.
     */
    private suspend fun executeUnpublish(
        quizzes: List<Quiz>,
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
                        "Dang go xuat ban: ${CommandFormatUtils.truncate(quiz.title, 40)} (${quiz.id})...",
                        OutputStyle.INFO
                    )
                )
            }

            val result = adminRepo.unpublishQuiz(quiz.id)
            if (result.isSuccess) {
                successCount++
                lines.add(
                    OutputLine(
                        "Da go xuat ban: ${CommandFormatUtils.truncate(quiz.title, 40)} (${quiz.id})",
                        OutputStyle.SUCCESS
                    )
                )
            } else {
                failCount++
                val errorMsg = result.exceptionOrNull()?.message ?: "Loi khong xac dinh"
                errors.add("${quiz.id}: $errorMsg")
                lines.add(
                    OutputLine(
                        "Loi khi go xuat ban '${CommandFormatUtils.truncate(quiz.title, 30)}': $errorMsg",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        if (format == "json") {
            return buildUnpublishResultJson(quizzes.size, successCount, failCount, errors)
        }

        lines.add(OutputLine(""))
        lines.add(OutputLine("== Ket qua go xuat ban quiz ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so     : ${quizzes.size}", OutputStyle.NORMAL))
        lines.add(OutputLine("  Thanh cong  : $successCount", OutputStyle.SUCCESS))

        if (failCount > 0) {
            lines.add(OutputLine("  That bai    : $failCount", OutputStyle.ERROR))
        }

        val totalAttempts = quizzes.sumOf { it.attemptCount }
        val uniqueOwners = quizzes.map { it.ownerId }.distinct().size

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "  Chu so huu lien quan: $uniqueOwners | Tong luot lam: $totalAttempts",
                OutputStyle.MUTED
            )
        )
        lines.add(
            OutputLine(
                "Quiz da go xuat ban se khong con hien thi trong tim kiem cong khai. " +
                        "Van truy cap duoc qua share code (neu co).",
                OutputStyle.MUTED
            )
        )

        val isSuccess = failCount == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
    }

    /**
     * Xay dung ket qua go xuat ban dinh dang JSON.
     */
    private fun buildUnpublishResultJson(
        total: Int,
        success: Int,
        failed: Int,
        errors: List<String>
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"operation\": \"unpublish\",", OutputStyle.CODE))
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
