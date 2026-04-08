package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh noi bo `del-pool` — xoa cau hoi trong ngan hang cau hoi chung (question pool).
 *
 * Duoc goi tu [DeleteCommand] khi co flag `-p`/`--pool`. Ho tro xoa theo ID,
 * loc theo nguoi dong gop, tag, va trang thai hoat dong.
 *
 * Luu y: API backend cho viec xoa pool item hien tai con han che. Lenh nay
 * su dung [PoolRepository.revokeContribution] de vo hieu hoa cau hoi (isActive = false)
 * thay vi xoa vinh vien. Cac thao tac xoa vinh vien se duoc ghi nhan va bao cao
 * cho backend xu ly khi co ho tro.
 *
 * Cac flag ho tro:
 * - `--contributor <userId>`: chi xoa cau hoi cua nguoi dong gop cu the.
 * - `--tag <tag>`: chi xoa cau hoi co tag cu the.
 * - `--inactive`: chi xoa cau hoi da bi vo hieu hoa (isActive == false).
 * - `--source-quiz <quizId>`: chi xoa cau hoi tu quiz nguon cu the.
 * - `--dry-run`: mo phong thao tac, khong thuc su xoa.
 * - `--confirm`: xac nhan thao tac huy diet (bat buoc neu khong co dry-run).
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--verbose`: hien thi chi tiet.
 * - `--quiet`: chi hien thi ket qua tom tat.
 * - `--limit <n>`: gioi han so luong pool item xu ly (mac dinh: khong gioi han).
 */
class DeletePoolItemCommand : Command {

    override val name: String = "del-pool"

    override val description: String = "Xoa hoac vo hieu hoa cau hoi trong ngan hang cau hoi chung"

    override val usage: String =
        "del -p <poolItemId> [...] [--contributor <userId>] [--tag <tag>] [--inactive] " +
            "[--source-quiz <quizId>] [--limit <n>] [--dry-run] [--confirm] " +
            "[--format <table|json>] [--verbose] [--quiet]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.MANAGE_QUIZZES

    override val isDestructive: Boolean = true

    override val examples: List<Pair<String, String>> = listOf(
        "del -p poolId123 --confirm" to "Vo hieu hoa pool item theo ID",
        "del -p pid1 pid2 --confirm" to "Vo hieu hoa nhieu pool item theo ID",
        "del -p --contributor userId1 --confirm" to "Vo hieu hoa tat ca cau hoi cua nguoi dong gop",
        "del -p --tag math --dry-run" to "Mo phong vo hieu hoa cau hoi co tag 'math'",
        "del -p --inactive --dry-run" to "Mo phong xoa cau hoi da bi vo hieu hoa",
        "del -p --source-quiz quizId1 --confirm" to "Vo hieu hoa tat ca cau hoi tu quiz nguon",
        "del -p --contributor userId1 --tag science --limit 10 --dry-run" to
            "Mo phong vo hieu hoa toi da 10 cau hoi cua nguoi dong gop co tag 'science'",
        "del -p --inactive --format json --verbose --dry-run" to
            "Mo phong xoa cau hoi da vo hieu, xuat JSON chi tiet"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--contributor" to "Loc theo nguoi dong gop (userId)",
            "--tag" to "Loc theo tag",
            "--inactive" to "Chi cau hoi da bi vo hieu hoa",
            "--source-quiz" to "Loc theo quiz nguon (quizId)",
            "--limit" to "Gioi han so luong pool item xu ly",
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
        val inactive = "inactive" in flags
        val contributorFilter = flags["contributor"]
        val tagFilter = flags["tag"]
        val sourceQuizFilter = flags["source-quiz"]
        val limit = flags["limit"]?.toIntOrNull()

        if (!dryRun && !confirm) {
            return CommandResult.error(
                "Thao tac huy diet: vo hieu hoa/xoa cau hoi trong ngan hang cau hoi. " +
                    "Su dung --confirm de xac nhan hoac --dry-run de mo phong."
            )
        }

        val poolRepo = context.repositories.poolRepository
        val itemsToProcess = mutableListOf<QuestionPoolItem>()

        if (args.isNotEmpty() && !hasFilterFlags(flags)) {
            // Xu ly theo ID cu the — tim trong toan bo ngan hang
            val allItems = collectAllPoolItems(context, contributorFilter = null)
            val itemMap = allItems.associateBy { it.id }
            for (poolId in args) {
                val item = itemMap[poolId]
                if (item == null) {
                    return CommandResult.error("Khong tim thay pool item voi ID: '$poolId'")
                }
                itemsToProcess.add(item)
            }
        } else {
            // Xu ly theo bo loc
            if (contributorFilter == null && args.isEmpty() && !inactive) {
                return CommandResult.error(
                    "Vui long cung cap ID pool item, --contributor, --tag, --inactive, " +
                        "hoac --source-quiz de xac dinh cau hoi can xu ly."
                )
            }

            var collected = collectAllPoolItems(context, contributorFilter)

            // Loc theo ID neu co args kem filter
            if (args.isNotEmpty()) {
                val idSet = args.toSet()
                collected = collected.filter { it.id in idSet }
            }

            collected = applyFilters(
                items = collected,
                tagFilter = tagFilter,
                sourceQuizFilter = sourceQuizFilter,
                inactive = inactive
            )

            if (limit != null && limit > 0) {
                collected = collected.take(limit)
            }

            itemsToProcess.addAll(collected)
        }

        if (itemsToProcess.isEmpty()) {
            return CommandResult.success("Khong tim thay pool item nao phu hop voi bo loc.")
        }

        if (dryRun) {
            return buildDryRunOutput(itemsToProcess, verbose, quiet, format)
        }

        return executeRevoke(itemsToProcess, verbose, quiet, format, context)
    }

    /**
     * Thu thap cau hoi tu ngan hang, tuy chon loc theo nguoi dong gop.
     *
     * Su dung [PoolRepository.getMyContributions] khi co [contributorFilter],
     * hoac thu thap tu tat ca nguoi dung qua [AdminRepository.getAllUsers]
     * khi can toan bo ngan hang.
     *
     * @param context Context lenh hien tai.
     * @param contributorFilter userId nguoi dong gop de loc, hoac null de lay tat ca.
     * @return Danh sach [QuestionPoolItem] thu thap duoc.
     */
    private suspend fun collectAllPoolItems(
        context: CommandContext,
        contributorFilter: String?
    ): List<QuestionPoolItem> {
        val poolRepo = context.repositories.poolRepository

        if (contributorFilter != null) {
            val result = poolRepo.getMyContributions(contributorFilter)
            return result.getOrDefault(emptyList())
        }

        // Khong co contributor filter — thu lay qua tat ca nguoi dung da biet
        // Day la gioi han cua API hien tai; trong tuong lai co the can endpoint
        // getAllPoolItems() o backend.
        val adminRepo = context.repositories.adminRepository
        val allUsers = adminRepo.getAllUsers().first()
        val allItems = mutableListOf<QuestionPoolItem>()

        for (user in allUsers) {
            val result = poolRepo.getMyContributions(user.id)
            if (result.isSuccess) {
                allItems.addAll(result.getOrDefault(emptyList()))
            }
        }

        return allItems
    }

    /**
     * Kiem tra xem co flag loc nao duoc su dung khong.
     */
    private fun hasFilterFlags(flags: Map<String, String?>): Boolean {
        val filterKeys = setOf("contributor", "tag", "inactive", "source-quiz", "limit")
        return flags.keys.any { it in filterKeys }
    }

    /**
     * Ap dung cac bo loc len danh sach pool item.
     *
     * @param items Danh sach pool item goc.
     * @param tagFilter Loc theo tag.
     * @param sourceQuizFilter Loc theo quiz nguon.
     * @param inactive Chi lay item da bi vo hieu hoa.
     * @return Danh sach pool item da loc.
     */
    private fun applyFilters(
        items: List<QuestionPoolItem>,
        tagFilter: String?,
        sourceQuizFilter: String?,
        inactive: Boolean
    ): List<QuestionPoolItem> {
        var result = items

        if (inactive) {
            result = result.filter { !it.isActive }
        }

        if (tagFilter != null) {
            val tagLower = tagFilter.lowercase()
            result = result.filter { item ->
                item.tags.any { it.lowercase() == tagLower }
            }
        }

        if (sourceQuizFilter != null) {
            result = result.filter { it.sourceQuizId == sourceQuizFilter }
        }

        return result
    }

    /**
     * Xay dung dau ra mo phong (dry-run) cho thao tac xoa pool item.
     */
    private fun buildDryRunOutput(
        items: List<QuestionPoolItem>,
        verbose: Boolean,
        quiet: Boolean,
        format: String
    ): CommandResult {
        if (format == "json") {
            return buildDryRunJson(items)
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[DRY-RUN] Mo phong vo hieu hoa cau hoi ngan hang", OutputStyle.WARNING))
        lines.add(OutputLine(""))

        if (!quiet) {
            lines.add(
                OutputLine(
                    padRight("ID", 24) + padRight("Noi dung", 32) +
                        padRight("Trang thai", 12) + padRight("Su dung", 8),
                    OutputStyle.TABLE_HEADER
                )
            )

            for (item in items) {
                val status = if (item.isActive) "Hoat dong" else "Vo hieu"
                val questionPreview = truncate(item.question.content, 30)
                lines.add(
                    OutputLine(
                        padRight(item.id, 24) + padRight(questionPreview, 32) +
                            padRight(status, 12) + padRight(item.usageCount.toString(), 8),
                        OutputStyle.TABLE_ROW
                    )
                )

                if (verbose) {
                    if (item.contributorId != null) {
                        lines.add(
                            OutputLine(
                                "  Nguoi dong gop: ${item.contributorId}",
                                OutputStyle.MUTED
                            )
                        )
                    } else {
                        lines.add(OutputLine("  Nguoi dong gop: (an danh)", OutputStyle.MUTED))
                    }
                    lines.add(
                        OutputLine(
                            "  Quiz nguon: ${item.sourceQuizId}",
                            OutputStyle.MUTED
                        )
                    )
                    if (item.tags.isNotEmpty()) {
                        lines.add(
                            OutputLine(
                                "  Tags: ${item.tags.joinToString(", ")}",
                                OutputStyle.MUTED
                            )
                        )
                    }
                    lines.add(
                        OutputLine(
                            "  Tao: ${formatTimestamp(item.createdAtMillis)}",
                            OutputStyle.MUTED
                        )
                    )
                }
            }
        }

        lines.add(OutputLine(""))

        val activeCount = items.count { it.isActive }
        val inactiveCount = items.size - activeCount
        val uniqueContributors = items.mapNotNull { it.contributorId }.distinct().size
        val totalUsage = items.sumOf { it.usageCount }

        lines.add(
            OutputLine(
                "[DRY-RUN] Se xu ly ${items.size} pool item.",
                OutputStyle.WARNING
            )
        )
        if (!quiet) {
            lines.add(
                OutputLine(
                    "  Hoat dong: $activeCount | Da vo hieu: $inactiveCount",
                    OutputStyle.MUTED
                )
            )
            lines.add(
                OutputLine(
                    "  Nguoi dong gop: $uniqueContributors | Tong luot su dung: $totalUsage",
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
    private fun buildDryRunJson(items: List<QuestionPoolItem>): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"dryRun\": true,", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${items.size},", OutputStyle.CODE))

        val activeCount = items.count { it.isActive }
        val uniqueContributors = items.mapNotNull { it.contributorId }.distinct().size
        lines.add(OutputLine("  \"activeCount\": $activeCount,", OutputStyle.CODE))
        lines.add(OutputLine("  \"uniqueContributors\": $uniqueContributors,", OutputStyle.CODE))

        lines.add(OutputLine("  \"poolItems\": [", OutputStyle.CODE))

        for ((index, item) in items.withIndex()) {
            val comma = if (index < items.size - 1) "," else ""
            val contributorStr = if (item.contributorId != null) {
                "\"${escapeJson(item.contributorId)}\""
            } else {
                "null"
            }
            val tagsStr = item.tags.joinToString(", ") { "\"${escapeJson(it)}\"" }
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${escapeJson(item.id)}\",", OutputStyle.CODE))
            lines.add(
                OutputLine(
                    "      \"questionPreview\": \"${escapeJson(truncate(item.question.content, 60))}\",",
                    OutputStyle.CODE
                )
            )
            lines.add(OutputLine("      \"contributorId\": $contributorStr,", OutputStyle.CODE))
            lines.add(OutputLine("      \"sourceQuizId\": \"${escapeJson(item.sourceQuizId)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"tags\": [$tagsStr],", OutputStyle.CODE))
            lines.add(OutputLine("      \"isActive\": ${item.isActive},", OutputStyle.CODE))
            lines.add(OutputLine("      \"usageCount\": ${item.usageCount}", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Thuc hien vo hieu hoa pool item that su.
     *
     * Su dung [PoolRepository.revokeContribution] de vo hieu hoa cau hoi.
     * Item da bi vo hieu hoa (isActive == false) se duoc ghi nhan la "da xu ly truoc do".
     */
    private suspend fun executeRevoke(
        items: List<QuestionPoolItem>,
        verbose: Boolean,
        quiet: Boolean,
        format: String,
        context: CommandContext
    ): CommandResult {
        val poolRepo = context.repositories.poolRepository
        val lines = mutableListOf<OutputLine>()
        var revokedCount = 0
        var alreadyInactiveCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        for (item in items) {
            if (!item.isActive) {
                alreadyInactiveCount++
                if (verbose && !quiet) {
                    lines.add(
                        OutputLine(
                            "Bo qua (da vo hieu): ${item.id} - " +
                                truncate(item.question.content, 30),
                            OutputStyle.MUTED
                        )
                    )
                }
                continue
            }

            if (verbose && !quiet) {
                lines.add(
                    OutputLine(
                        "Dang vo hieu hoa: ${item.id} - " +
                            truncate(item.question.content, 30) + "...",
                        OutputStyle.INFO
                    )
                )
            }

            val result = poolRepo.revokeContribution(item.id)
            if (result.isSuccess) {
                revokedCount++
                if (!quiet) {
                    lines.add(
                        OutputLine(
                            "Da vo hieu hoa: ${item.id}",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                failCount++
                val errorMsg = result.exceptionOrNull()?.message ?: "Loi khong xac dinh"
                errors.add("${item.id}: $errorMsg")
                if (!quiet) {
                    lines.add(
                        OutputLine(
                            "Loi khi vo hieu hoa ${item.id}: $errorMsg",
                            OutputStyle.ERROR
                        )
                    )
                }
            }
        }

        if (format == "json") {
            return buildRevokeResultJson(
                total = items.size,
                revoked = revokedCount,
                alreadyInactive = alreadyInactiveCount,
                failed = failCount,
                errors = errors
            )
        }

        lines.add(OutputLine(""))
        lines.add(OutputLine("== Ket qua xu ly pool item ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so          : ${items.size}", OutputStyle.NORMAL))
        lines.add(OutputLine("  Da vo hieu hoa   : $revokedCount", OutputStyle.SUCCESS))

        if (alreadyInactiveCount > 0) {
            lines.add(
                OutputLine(
                    "  Da vo hieu tu truoc: $alreadyInactiveCount",
                    OutputStyle.MUTED
                )
            )
        }

        if (failCount > 0) {
            lines.add(OutputLine("  That bai         : $failCount", OutputStyle.ERROR))
        }

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "Ghi chu: Pool item duoc vo hieu hoa (isActive=false), khong xoa vinh vien. " +
                    "Xoa vinh vien can ho tro tu backend.",
                OutputStyle.MUTED
            )
        )

        val isSuccess = failCount == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
    }

    /**
     * Xay dung ket qua vo hieu hoa dinh dang JSON.
     */
    private fun buildRevokeResultJson(
        total: Int,
        revoked: Int,
        alreadyInactive: Int,
        failed: Int,
        errors: List<String>
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"operation\": \"revokePoolItems\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"revoked\": $revoked,", OutputStyle.CODE))
        lines.add(OutputLine("  \"alreadyInactive\": $alreadyInactive,", OutputStyle.CODE))
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
