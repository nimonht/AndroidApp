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
 * Lenh `publish` — bat buoc xuat ban quiz (force publish).
 *
 * Cho phep admin bat buoc xuat ban quiz theo ID hoac bo loc. Khi xuat ban,
 * quiz se duoc dat `isPublic = true` va `isDraft = false`, giup quiz hien thi
 * trong ket qua tim kiem cong khai.
 *
 * Cac flag ho tro:
 * - `--owner <userId>`: chi xuat ban quiz cua chu so huu cu the.
 * - `--draft`: chi xuat ban quiz dang o trang thai nhap.
 * - `--tag <tag>`: chi xuat ban quiz co tag cu the.
 * - `--search <query>`: tim quiz theo tu khoa truoc khi xuat ban.
 * - `--before <timestamp>`: chi xuat ban quiz tao truoc thoi diem nay (epoch ms).
 * - `--after <timestamp>`: chi xuat ban quiz tao sau thoi diem nay (epoch ms).
 * - `--dry-run`: mo phong thao tac, khong thuc su xuat ban.
 * - `--confirm`: xac nhan thao tac huy diet (bat buoc neu khong co dry-run).
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--verbose`: hien thi chi tiet.
 */
class PublishCommand : Command {

    override val name: String = "publish"

    override val aliases: List<String> = listOf("pub")

    override val description: String = "Bat buoc xuat ban quiz (force publish)"

    override val usage: String =
        "publish <quizId> [...] [--owner <userId>] [--draft] [--tag <tag>] [--search <query>] " +
                "[--before <ts>] [--after <ts>] [--dry-run] [--confirm] [--format <table|json>] [--verbose]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.PUBLISH_QUIZZES

    override val isDestructive: Boolean = true

    override val examples: List<Pair<String, String>> = listOf(
        "publish quizId123 --confirm" to "Xuat ban quiz theo ID",
        "publish qid1 qid2 qid3 --confirm" to "Xuat ban nhieu quiz theo ID",
        "publish --draft --owner userId1 --confirm" to "Xuat ban tat ca quiz nhap cua mot nguoi dung",
        "publish --tag math --dry-run" to "Mo phong xuat ban quiz co tag 'math'",
        "publish --search \"toan hoc\" --dry-run" to "Mo phong xuat ban quiz tim theo tu khoa",
        "publish --draft --after 1700000000000 --confirm" to "Xuat ban quiz nhap tao sau moc thoi gian",
        "publish quizId123 --format json --verbose --dry-run" to "Mo phong xuat ban, xuat JSON chi tiet",
        "pub --draft --tag science --confirm" to "Xuat ban quiz nhap co tag 'science' (dung alias)"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--owner" to "Loc theo chu so huu (userId)",
            "--draft" to "Chi quiz dang o trang thai nhap",
            "--tag" to "Loc theo tag",
            "--search" to "Tim quiz theo tu khoa",
            "--before" to "Quiz tao truoc moc thoi gian (epoch ms)",
            "--after" to "Quiz tao sau moc thoi gian (epoch ms)",
            "--dry-run" to "Mo phong thao tac, khong thuc su xuat ban",
            "--confirm" to "Xac nhan thao tac xuat ban",
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
        val draftOnly = "draft" in flags
        val ownerFilter = flags["owner"]
        val tagFilter = flags["tag"]
        val searchQuery = flags["search"]
        val beforeFilter = flags["before"]?.toLongOrNull()
        val afterFilter = flags["after"]?.toLongOrNull()

        if (!dryRun && !confirm) {
            return CommandResult.error(
                "Thao tac huy diet: bat buoc xuat ban quiz (isPublic=true, isDraft=false). " +
                        "Su dung --confirm de xac nhan hoac --dry-run de mo phong."
            )
        }

        val adminRepo = context.repositories.adminRepository
        val quizzesToPublish = mutableListOf<Quiz>()
        val skippedLines = mutableListOf<OutputLine>()

        if (args.isNotEmpty() && !hasFilterFlags(flags)) {
            // Xuat ban theo ID cu the
            val allQuizzes = adminRepo.getAllQuizzes(includeDeleted = false).first()
            val quizMap = allQuizzes.associateBy { it.id }
            for (quizId in args) {
                val quiz = quizMap[quizId]
                if (quiz == null) {
                    return CommandResult.error("Khong tim thay quiz voi ID: '$quizId'")
                }
                if (quiz.isPublic && !quiz.isDraft) {
                    if (verbose) {
                        skippedLines.add(
                            OutputLine(
                                "Bo qua: Quiz '${
                                    CommandFormatUtils.truncate(
                                        quiz.title,
                                        40
                                    )
                                }' ($quizId) da duoc xuat ban.",
                                OutputStyle.WARNING
                            )
                        )
                    }
                    continue
                }
                if (quiz.deletedAt != null) {
                    return CommandResult.error(
                        "Quiz '${CommandFormatUtils.truncate(quiz.title, 40)}' ($quizId) da bi xoa. " +
                                "Vui long khoi phuc truoc khi xuat ban (su dung lenh 'restore')."
                    )
                }
                quizzesToPublish.add(quiz)
            }
        } else {
            // Xuat ban theo bo loc
            var allQuizzes = adminRepo.getAllQuizzes(includeDeleted = false).first()

            // Loai bo quiz da xuat ban va quiz da xoa
            allQuizzes = allQuizzes.filter { !it.isPublic || it.isDraft }
            allQuizzes = allQuizzes.filter { it.deletedAt == null }

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
                draftOnly = draftOnly,
                ownerFilter = ownerFilter,
                tagFilter = tagFilter,
                beforeFilter = beforeFilter,
                afterFilter = afterFilter
            )

            quizzesToPublish.addAll(filtered)
        }

        if (quizzesToPublish.isEmpty()) {
            val emptyResult = CommandResult.success("Khong tim thay quiz nao can xuat ban phu hop voi bo loc.")
            return if (skippedLines.isNotEmpty()) {
                emptyResult.copy(output = skippedLines + emptyResult.output)
            } else {
                emptyResult
            }
        }

        if (dryRun) {
            val result = buildDryRunOutput(quizzesToPublish, verbose, format)
            return if (skippedLines.isNotEmpty()) {
                result.copy(output = skippedLines + result.output)
            } else {
                result
            }
        }

        val result = executePublish(quizzesToPublish, verbose, format, adminRepo)
        return if (skippedLines.isNotEmpty()) {
            result.copy(output = skippedLines + result.output)
        } else {
            result
        }
    }

    /**
     * Kiem tra xem co flag loc nao duoc su dung khong.
     */
    private fun hasFilterFlags(flags: Map<String, String?>): Boolean {
        val filterKeys = setOf("owner", "tag", "draft", "search", "before", "after")
        return flags.keys.any { it in filterKeys }
    }

    /**
     * Ap dung cac bo loc len danh sach quiz.
     *
     * @param quizzes Danh sach quiz goc.
     * @param draftOnly Chi lay quiz nhap.
     * @param ownerFilter Loc theo chu so huu.
     * @param tagFilter Loc theo tag.
     * @param beforeFilter Loc quiz tao truoc moc thoi gian.
     * @param afterFilter Loc quiz tao sau moc thoi gian.
     * @return Danh sach quiz da loc.
     */
    private fun applyFilters(
        quizzes: List<Quiz>,
        draftOnly: Boolean,
        ownerFilter: String?,
        tagFilter: String?,
        beforeFilter: Long?,
        afterFilter: Long?
    ): List<Quiz> {
        var result = quizzes

        if (draftOnly) {
            result = result.filter { it.isDraft }
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
     * Xay dung dau ra mo phong (dry-run) cho thao tac xuat ban quiz.
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
        lines.add(OutputLine("[DRY-RUN] Mo phong xuat ban quiz", OutputStyle.WARNING))
        lines.add(OutputLine(""))

        lines.add(
            OutputLine(
                CommandFormatUtils.padRight("ID", 24) + CommandFormatUtils.padRight(
                    "Tieu de",
                    30
                ) + CommandFormatUtils.padRight("Trang thai", 14) +
                        CommandFormatUtils.padRight("Chu so huu", 16),
                OutputStyle.TABLE_HEADER
            )
        )

        val draftCount = quizzes.count { it.isDraft }
        val privateCount = quizzes.count { !it.isPublic && !it.isDraft }

        for (quiz in quizzes) {
            val status = buildCurrentStatusLabel(quiz)
            lines.add(
                OutputLine(
                    CommandFormatUtils.padRight(quiz.id, 24) + CommandFormatUtils.padRight(
                        CommandFormatUtils.truncate(
                            quiz.title,
                            28
                        ), 30
                    ) +
                            CommandFormatUtils.padRight(
                                status,
                                14
                            ) + CommandFormatUtils.padRight(CommandFormatUtils.truncate(quiz.ownerId, 14), 16),
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
                        "  So cau hoi: ${quiz.questionCount} | Luot lam: ${quiz.attemptCount}",
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
            }
        }

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "[DRY-RUN] Se xuat ban ${quizzes.size} quiz " +
                        "(nhap: $draftCount, rieng tu: $privateCount).",
                OutputStyle.WARNING
            )
        )
        lines.add(
            OutputLine(
                "Tac dong: isDraft=false, isPublic=true cho tat ca quiz tren.",
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
        lines.add(OutputLine("  \"operation\": \"forcePublish\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${quizzes.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"draftCount\": ${quizzes.count { it.isDraft }},", OutputStyle.CODE))
        lines.add(OutputLine("  \"privateCount\": ${quizzes.count { !it.isPublic && !it.isDraft }},", OutputStyle.CODE))
        lines.add(OutputLine("  \"quizzes\": [", OutputStyle.CODE))

        for ((index, quiz) in quizzes.withIndex()) {
            val comma = if (index < quizzes.size - 1) "," else ""
            val tagsStr = quiz.tags.joinToString(", ") { "\"${CommandFormatUtils.escapeJson(it)}\"" }
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
            lines.add(OutputLine("      \"currentStatus\": \"${buildCurrentStatusLabel(quiz)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"isPublic\": ${quiz.isPublic},", OutputStyle.CODE))
            lines.add(OutputLine("      \"isDraft\": ${quiz.isDraft},", OutputStyle.CODE))
            lines.add(OutputLine("      \"questionCount\": ${quiz.questionCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"attemptCount\": ${quiz.attemptCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"tags\": [$tagsStr],", OutputStyle.CODE))
            lines.add(OutputLine("      \"createdAt\": ${quiz.createdAt},", OutputStyle.CODE))
            lines.add(OutputLine("      \"updatedAt\": ${quiz.updatedAt}", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Thuc hien xuat ban quiz that su.
     */
    private suspend fun executePublish(
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
                        "Dang xuat ban: ${CommandFormatUtils.truncate(quiz.title, 40)} (${quiz.id})...",
                        OutputStyle.INFO
                    )
                )
            }

            val result = adminRepo.forcePublishQuiz(quiz.id)
            if (result.isSuccess) {
                successCount++
                lines.add(
                    OutputLine(
                        "Da xuat ban: ${CommandFormatUtils.truncate(quiz.title, 40)} (${quiz.id})",
                        OutputStyle.SUCCESS
                    )
                )
            } else {
                failCount++
                val errorMsg = result.exceptionOrNull()?.message ?: "Loi khong xac dinh"
                errors.add("${quiz.id}: $errorMsg")
                lines.add(
                    OutputLine(
                        "Loi khi xuat ban '${CommandFormatUtils.truncate(quiz.title, 30)}': $errorMsg",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        if (format == "json") {
            return buildPublishResultJson(quizzes.size, successCount, failCount, errors)
        }

        lines.add(OutputLine(""))
        lines.add(OutputLine("== Ket qua xuat ban quiz ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so     : ${quizzes.size}", OutputStyle.NORMAL))
        lines.add(OutputLine("  Thanh cong  : $successCount", OutputStyle.SUCCESS))

        if (failCount > 0) {
            lines.add(OutputLine("  That bai    : $failCount", OutputStyle.ERROR))
        }

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "Quiz da xuat ban se hien thi trong ket qua tim kiem cong khai.",
                OutputStyle.MUTED
            )
        )

        val isSuccess = failCount == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
    }

    /**
     * Xay dung ket qua xuat ban dinh dang JSON.
     */
    private fun buildPublishResultJson(
        total: Int,
        success: Int,
        failed: Int,
        errors: List<String>
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"operation\": \"forcePublish\",", OutputStyle.CODE))
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

    /**
     * Tao nhan trang thai hien tai cho quiz.
     */
    private fun buildCurrentStatusLabel(quiz: Quiz): String {
        return when {
            quiz.deletedAt != null -> "Da xoa"
            quiz.isDraft -> "Nhap"
            quiz.isPublic -> "Cong khai"
            else -> "Rieng tu"
        }
    }
}
