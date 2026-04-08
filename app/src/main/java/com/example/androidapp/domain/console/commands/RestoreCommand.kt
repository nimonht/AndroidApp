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
 * Lenh `restore` — khoi phuc quiz da bi xoa mem (soft-deleted).
 *
 * Cho phep admin khoi phuc quiz tu thung rac theo ID hoac bo loc. Khi khoi phuc,
 * truong `deletedAt` cua quiz se duoc dat lai ve `null`, dua quiz tro lai
 * trang thai truoc khi bi xoa.
 *
 * Cac flag ho tro:
 * - `--owner <userId>`: chi khoi phuc quiz cua chu so huu cu the.
 * - `--all`: khoi phuc tat ca quiz da xoa mem (can ket hop --confirm).
 * - `--tag <tag>`: chi khoi phuc quiz co tag cu the.
 * - `--deleted-before <timestamp>`: chi khoi phuc quiz bi xoa truoc thoi diem nay (epoch ms).
 * - `--deleted-after <timestamp>`: chi khoi phuc quiz bi xoa sau thoi diem nay (epoch ms).
 * - `--deleted-between <start>,<end>`: chi khoi phuc quiz bi xoa trong khoang thoi gian (epoch ms).
 * - `--dry-run`: mo phong thao tac, khong thuc su khoi phuc.
 * - `--confirm`: xac nhan thao tac (bat buoc khi khoi phuc nhieu quiz).
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--verbose`: hien thi chi tiet.
 */
class RestoreCommand : Command {

    override val name: String = "restore"

    override val description: String = "Khoi phuc quiz da bi xoa mem tu thung rac"

    override val usage: String =
        "restore <quizId> [...] [--owner <userId>] [--all] [--tag <tag>] " +
            "[--deleted-before <ts>] [--deleted-after <ts>] [--deleted-between <start>,<end>] " +
            "[--dry-run] [--confirm] [--format <table|json>] [--verbose]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.MANAGE_QUIZZES

    override val examples: List<Pair<String, String>> = listOf(
        "restore quizId123 --confirm" to "Khoi phuc quiz theo ID",
        "restore qid1 qid2 qid3 --confirm" to "Khoi phuc nhieu quiz theo ID",
        "restore --owner userId1 --confirm" to "Khoi phuc tat ca quiz da xoa cua mot nguoi dung",
        "restore --all --dry-run" to "Mo phong khoi phuc tat ca quiz da xoa",
        "restore --all --confirm" to "Khoi phuc tat ca quiz da xoa",
        "restore --tag math --dry-run" to "Mo phong khoi phuc quiz co tag 'math'",
        "restore --deleted-after 1700000000000 --confirm" to "Khoi phuc quiz bi xoa sau moc thoi gian",
        "restore --deleted-between 1700000000000,1710000000000 --dry-run" to
            "Mo phong khoi phuc quiz bi xoa trong khoang thoi gian",
        "restore quizId123 --format json --verbose" to "Khoi phuc va hien thi ket qua dang JSON chi tiet"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--owner" to "Loc theo chu so huu (userId)",
            "--all" to "Khoi phuc tat ca quiz da xoa",
            "--tag" to "Loc theo tag",
            "--deleted-before" to "Quiz bi xoa truoc moc thoi gian (epoch ms)",
            "--deleted-after" to "Quiz bi xoa sau moc thoi gian (epoch ms)",
            "--deleted-between" to "Quiz bi xoa trong khoang (start,end epoch ms)",
            "--dry-run" to "Mo phong thao tac, khong thuc su khoi phuc",
            "--confirm" to "Xac nhan thao tac khoi phuc",
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
        val restoreAll = "all" in flags
        val ownerFilter = flags["owner"]
        val tagFilter = flags["tag"]
        val deletedBefore = flags["deleted-before"]?.toLongOrNull()
        val deletedAfter = flags["deleted-after"]?.toLongOrNull()
        val deletedBetween = parseDeletedBetween(flags["deleted-between"])

        val adminRepo = context.repositories.adminRepository

        // Thu thap quiz da xoa mem (includeDeleted = true, roi loc chi lay da xoa)
        val allQuizzes = adminRepo.getAllQuizzes(includeDeleted = true).first()
        val deletedQuizzes = allQuizzes.filter { it.deletedAt != null }

        val quizzesToRestore = mutableListOf<Quiz>()

        if (args.isNotEmpty() && !hasFilterFlags(flags) && !restoreAll) {
            // Khoi phuc theo ID cu the — khong bat buoc --confirm cho tung quiz
            for (quizId in args) {
                val quiz = deletedQuizzes.find { it.id == quizId }
                if (quiz == null) {
                    // Kiem tra xem quiz co ton tai nhung chua bi xoa khong
                    val existingQuiz = allQuizzes.find { it.id == quizId }
                    if (existingQuiz != null) {
                        return CommandResult.error(
                            "Quiz '${truncate(existingQuiz.title, 40)}' ($quizId) chua bi xoa, " +
                                "khong can khoi phuc."
                        )
                    }
                    return CommandResult.error("Khong tim thay quiz da xoa voi ID: '$quizId'")
                }
                quizzesToRestore.add(quiz)
            }
        } else if (restoreAll || hasFilterFlags(flags)) {
            // Khoi phuc theo bo loc — bat buoc --confirm hoac --dry-run
            if (!dryRun && !confirm) {
                return CommandResult.error(
                    "Khoi phuc hang loat yeu cau --confirm hoac --dry-run. " +
                        "Su dung --confirm de xac nhan hoac --dry-run de mo phong."
                )
            }

            var filtered = deletedQuizzes.toList()

            // Loc theo ID neu co args kem filter
            if (args.isNotEmpty()) {
                val idSet = args.toSet()
                filtered = filtered.filter { it.id in idSet }
            }

            filtered = applyFilters(
                quizzes = filtered,
                ownerFilter = ownerFilter,
                tagFilter = tagFilter,
                deletedBefore = deletedBefore,
                deletedAfter = deletedAfter,
                deletedBetween = deletedBetween
            )

            quizzesToRestore.addAll(filtered)
        } else {
            return CommandResult.error(
                "Vui long cung cap quiz ID, --all, hoac bo loc (--owner, --tag, " +
                    "--deleted-before/after/between) de xac dinh quiz can khoi phuc."
            )
        }

        if (quizzesToRestore.isEmpty()) {
            return CommandResult.success("Khong tim thay quiz da xoa nao phu hop voi bo loc.")
        }

        if (dryRun) {
            return buildDryRunOutput(quizzesToRestore, verbose, format)
        }

        return executeRestore(quizzesToRestore, verbose, format, adminRepo)
    }

    /**
     * Kiem tra xem co flag loc nao duoc su dung khong.
     */
    private fun hasFilterFlags(flags: Map<String, String?>): Boolean {
        val filterKeys = setOf(
            "owner", "tag", "deleted-before", "deleted-after", "deleted-between"
        )
        return flags.keys.any { it in filterKeys }
    }

    /**
     * Phan tich gia tri `--deleted-between` thanh cap (start, end).
     *
     * @param value Chuoi dinh dang "start,end" (epoch ms).
     * @return Cap (start, end) neu hop le, null neu khong.
     */
    private fun parseDeletedBetween(value: String?): Pair<Long, Long>? {
        if (value == null) return null
        val parts = value.split(",")
        if (parts.size != 2) return null
        val start = parts[0].trim().toLongOrNull() ?: return null
        val end = parts[1].trim().toLongOrNull() ?: return null
        if (start > end) return null
        return Pair(start, end)
    }

    /**
     * Ap dung cac bo loc len danh sach quiz da xoa.
     *
     * @param quizzes Danh sach quiz da xoa.
     * @param ownerFilter Loc theo chu so huu.
     * @param tagFilter Loc theo tag.
     * @param deletedBefore Loc quiz bi xoa truoc moc thoi gian.
     * @param deletedAfter Loc quiz bi xoa sau moc thoi gian.
     * @param deletedBetween Loc quiz bi xoa trong khoang thoi gian.
     * @return Danh sach quiz da loc.
     */
    private fun applyFilters(
        quizzes: List<Quiz>,
        ownerFilter: String?,
        tagFilter: String?,
        deletedBefore: Long?,
        deletedAfter: Long?,
        deletedBetween: Pair<Long, Long>?
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

        if (deletedBefore != null) {
            result = result.filter { quiz ->
                val deletedAt = quiz.deletedAt ?: return@filter false
                deletedAt < deletedBefore
            }
        }

        if (deletedAfter != null) {
            result = result.filter { quiz ->
                val deletedAt = quiz.deletedAt ?: return@filter false
                deletedAt > deletedAfter
            }
        }

        if (deletedBetween != null) {
            val (start, end) = deletedBetween
            result = result.filter { quiz ->
                val deletedAt = quiz.deletedAt ?: return@filter false
                deletedAt in start..end
            }
        }

        return result
    }

    /**
     * Xay dung dau ra mo phong (dry-run) cho thao tac khoi phuc quiz.
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
        lines.add(OutputLine("[DRY-RUN] Mo phong khoi phuc quiz da xoa", OutputStyle.WARNING))
        lines.add(OutputLine(""))

        lines.add(
            OutputLine(
                padRight("ID", 24) + padRight("Tieu de", 30) +
                    padRight("Chu so huu", 18) + padRight("Ngay xoa", 22),
                OutputStyle.TABLE_HEADER
            )
        )

        val uniqueOwners = quizzes.map { it.ownerId }.distinct().size

        for (quiz in quizzes) {
            val deletedAtStr = if (quiz.deletedAt != null) {
                formatTimestamp(quiz.deletedAt)
            } else {
                "-"
            }

            lines.add(
                OutputLine(
                    padRight(quiz.id, 24) + padRight(truncate(quiz.title, 28), 30) +
                        padRight(truncate(quiz.ownerId, 16), 18) +
                        padRight(deletedAtStr, 22),
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
                val statusBefore = when {
                    quiz.isDraft -> "Nhap"
                    quiz.isPublic -> "Cong khai"
                    else -> "Rieng tu"
                }
                lines.add(
                    OutputLine(
                        "  Trang thai truoc khi xoa: $statusBefore",
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
                        "  Tao: ${formatTimestamp(quiz.createdAt)} | Cap nhat: ${formatTimestamp(quiz.updatedAt)}",
                        OutputStyle.MUTED
                    )
                )
                if (quiz.isRemovedFromCloud) {
                    lines.add(
                        OutputLine(
                            "  Canh bao: Quiz da bi xoa khoi cloud. Khoi phuc co the khong dong bo duoc.",
                            OutputStyle.WARNING
                        )
                    )
                }
            }
        }

        lines.add(OutputLine(""))

        // Phan tich them
        val cloudRemovedCount = quizzes.count { it.isRemovedFromCloud }

        lines.add(
            OutputLine(
                "[DRY-RUN] Se khoi phuc ${quizzes.size} quiz.",
                OutputStyle.WARNING
            )
        )
        lines.add(
            OutputLine(
                "  Chu so huu lien quan: $uniqueOwners",
                OutputStyle.MUTED
            )
        )
        if (cloudRemovedCount > 0) {
            lines.add(
                OutputLine(
                    "  Canh bao: $cloudRemovedCount quiz da bi xoa khoi cloud (co the khong dong bo duoc).",
                    OutputStyle.WARNING
                )
            )
        }
        lines.add(
            OutputLine(
                "Tac dong: deletedAt=null cho tat ca quiz tren. Quiz se tro lai trang thai truoc khi bi xoa.",
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
        lines.add(OutputLine("  \"operation\": \"restore\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${quizzes.size},", OutputStyle.CODE))

        val uniqueOwners = quizzes.map { it.ownerId }.distinct().size
        val cloudRemovedCount = quizzes.count { it.isRemovedFromCloud }
        lines.add(OutputLine("  \"uniqueOwners\": $uniqueOwners,", OutputStyle.CODE))
        lines.add(OutputLine("  \"cloudRemovedCount\": $cloudRemovedCount,", OutputStyle.CODE))

        lines.add(OutputLine("  \"quizzes\": [", OutputStyle.CODE))

        for ((index, quiz) in quizzes.withIndex()) {
            val comma = if (index < quizzes.size - 1) "," else ""
            val tagsStr = quiz.tags.joinToString(", ") { "\"${escapeJson(it)}\"" }
            val deletedAtStr = quiz.deletedAt?.toString() ?: "null"
            val statusBefore = when {
                quiz.isDraft -> "draft"
                quiz.isPublic -> "public"
                else -> "private"
            }
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${escapeJson(quiz.id)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"title\": \"${escapeJson(quiz.title)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"ownerId\": \"${escapeJson(quiz.ownerId)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"authorName\": \"${escapeJson(quiz.authorName)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"statusBeforeDelete\": \"$statusBefore\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"questionCount\": ${quiz.questionCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"attemptCount\": ${quiz.attemptCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"tags\": [$tagsStr],", OutputStyle.CODE))
            lines.add(OutputLine("      \"deletedAt\": $deletedAtStr,", OutputStyle.CODE))
            lines.add(OutputLine("      \"createdAt\": ${quiz.createdAt},", OutputStyle.CODE))
            lines.add(OutputLine("      \"isRemovedFromCloud\": ${quiz.isRemovedFromCloud}", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Thuc hien khoi phuc quiz that su.
     */
    private suspend fun executeRestore(
        quizzes: List<Quiz>,
        verbose: Boolean,
        format: String,
        adminRepo: com.example.androidapp.domain.repository.AdminRepository
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        for (quiz in quizzes) {
            if (verbose) {
                lines.add(
                    OutputLine(
                        "Dang khoi phuc: ${truncate(quiz.title, 40)} (${quiz.id})...",
                        OutputStyle.INFO
                    )
                )
            }

            if (quiz.isRemovedFromCloud) {
                val warningMsg = "Quiz '${truncate(quiz.title, 30)}' (${quiz.id}) " +
                    "da bi xoa khoi cloud — khoi phuc chi o local."
                warnings.add(warningMsg)
                if (verbose) {
                    lines.add(OutputLine("  Canh bao: $warningMsg", OutputStyle.WARNING))
                }
            }

            val result = adminRepo.restoreQuiz(quiz.id)
            if (result.isSuccess) {
                successCount++
                lines.add(
                    OutputLine(
                        "Da khoi phuc: ${truncate(quiz.title, 40)} (${quiz.id})",
                        OutputStyle.SUCCESS
                    )
                )
            } else {
                failCount++
                val errorMsg = result.exceptionOrNull()?.message ?: "Loi khong xac dinh"
                errors.add("${quiz.id}: $errorMsg")
                lines.add(
                    OutputLine(
                        "Loi khi khoi phuc '${truncate(quiz.title, 30)}': $errorMsg",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        if (format == "json") {
            return buildRestoreResultJson(quizzes.size, successCount, failCount, errors, warnings)
        }

        lines.add(OutputLine(""))
        lines.add(OutputLine("== Ket qua khoi phuc quiz ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so     : ${quizzes.size}", OutputStyle.NORMAL))
        lines.add(OutputLine("  Thanh cong  : $successCount", OutputStyle.SUCCESS))

        if (failCount > 0) {
            lines.add(OutputLine("  That bai    : $failCount", OutputStyle.ERROR))
        }

        if (warnings.isNotEmpty()) {
            lines.add(OutputLine("  Canh bao    : ${warnings.size}", OutputStyle.WARNING))
        }

        val uniqueOwners = quizzes.map { it.ownerId }.distinct().size
        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "  Chu so huu lien quan: $uniqueOwners",
                OutputStyle.MUTED
            )
        )
        lines.add(
            OutputLine(
                "Quiz da khoi phuc se tro lai trang thai truoc khi bi xoa.",
                OutputStyle.MUTED
            )
        )

        val isSuccess = failCount == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
    }

    /**
     * Xay dung ket qua khoi phuc dinh dang JSON.
     */
    private fun buildRestoreResultJson(
        total: Int,
        success: Int,
        failed: Int,
        errors: List<String>,
        warnings: List<String>
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"operation\": \"restore\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"success\": $success,", OutputStyle.CODE))
        lines.add(OutputLine("  \"failed\": $failed,", OutputStyle.CODE))

        if (errors.isNotEmpty()) {
            lines.add(OutputLine("  \"errors\": [", OutputStyle.CODE))
            for ((index, err) in errors.withIndex()) {
                val comma = if (index < errors.size - 1) "," else ""
                lines.add(OutputLine("    \"${escapeJson(err)}\"$comma", OutputStyle.CODE))
            }
            lines.add(OutputLine("  ],", OutputStyle.CODE))
        } else {
            lines.add(OutputLine("  \"errors\": [],", OutputStyle.CODE))
        }

        if (warnings.isNotEmpty()) {
            lines.add(OutputLine("  \"warnings\": [", OutputStyle.CODE))
            for ((index, warn) in warnings.withIndex()) {
                val comma = if (index < warnings.size - 1) "," else ""
                lines.add(OutputLine("    \"${escapeJson(warn)}\"$comma", OutputStyle.CODE))
            }
            lines.add(OutputLine("  ]", OutputStyle.CODE))
        } else {
            lines.add(OutputLine("  \"warnings\": []", OutputStyle.CODE))
        }

        lines.add(OutputLine("}", OutputStyle.CODE))

        val isSuccess = failed == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
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
