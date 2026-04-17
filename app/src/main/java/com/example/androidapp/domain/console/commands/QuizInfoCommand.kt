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
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh `quizinfo` — hien thi thong tin chi tiet cua mot quiz.
 *
 * Tra cuu quiz theo ID hoac share code va hien thi cac thong tin bao gom:
 * metadata co ban, danh sach cau hoi, lich su luot lam, va thong ke diem so.
 *
 * Cac flag ho tro:
 * - `--questions`: hien thi danh sach cau hoi cua quiz.
 * - `--attempts`: hien thi lich su luot lam quiz.
 * - `--stats`: hien thi thong ke diem so.
 * - `--all`: hien thi tat ca cac muc tren.
 * - `--share-code <code>`: tra cuu quiz theo share code thay vi ID.
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--verbose`: hien thi chi tiet bo sung.
 * - `--fields <field1,field2,...>`: chi hien thi cac truong cu the.
 */
class QuizInfoCommand : Command {

    override val name: String = "quizinfo"

    override val aliases: List<String> = listOf("qi")

    override val description: String = "Hien thi thong tin chi tiet cua mot quiz"

    override val usage: String =
        "quizinfo <quizId> [--questions] [--attempts] [--stats] [--all] " +
                "[--share-code <code>] [--format <table|json>] [--verbose] [--fields <fields>]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.MANAGE_QUIZZES

    override val examples: List<Pair<String, String>> = listOf(
        "quizinfo quizId123" to "Hien thi thong tin co ban cua quiz",
        "quizinfo quizId123 --all" to "Hien thi tat ca thong tin (cau hoi, luot lam, thong ke)",
        "quizinfo quizId123 --questions" to "Hien thi danh sach cau hoi cua quiz",
        "quizinfo quizId123 --attempts --stats" to "Hien thi lich su luot lam va thong ke diem",
        "quizinfo --share-code ABC123" to "Tra cuu quiz theo share code",
        "quizinfo quizId123 --format json --verbose" to "Xuat thong tin chi tiet dang JSON",
        "quizinfo quizId123 --fields title,owner,tags,status" to "Chi hien thi cac truong cu the",
        "qi quizId123 --all --verbose" to "Xem toan bo thong tin quiz chi tiet (dung alias)"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--questions" to "Hien thi danh sach cau hoi",
            "--attempts" to "Hien thi lich su luot lam",
            "--stats" to "Hien thi thong ke diem so",
            "--all" to "Hien thi tat ca cac muc",
            "--share-code" to "Tra cuu theo share code",
            "--format" to "Dinh dang dau ra (table/json)",
            "--verbose" to "Hien thi chi tiet bo sung",
            "--fields" to "Chi hien thi cac truong cu the"
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
        val showQuestions = "questions" in flags || "all" in flags
        val showAttempts = "attempts" in flags || "all" in flags
        val showStats = "stats" in flags || "all" in flags
        val verbose = "verbose" in flags || "v" in flags
        val format = flags["format"]?.lowercase() ?: "table"
        val shareCode = flags["share-code"]
        val fieldsStr = flags["fields"]
        val requestedFields = fieldsStr?.split(",")?.map { it.trim().lowercase() }

        val quizRepo = context.repositories.quizRepository

        // Tra cuu quiz
        val quiz: Quiz? = if (shareCode != null) {
            quizRepo.getQuizByShareCode(shareCode)
        } else {
            val quizId = args.firstOrNull()
                ?: return CommandResult.error(
                    "Vui long cung cap quiz ID hoac su dung --share-code <code> de tra cuu."
                )
            quizRepo.getQuizById(quizId)
        }

        if (quiz == null) {
            val identifier = shareCode ?: args.firstOrNull() ?: "?"
            return CommandResult.error("Khong tim thay quiz: '$identifier'")
        }

        // Thu thap du lieu bo sung
        val questions = if (showQuestions || showStats) {
            quizRepo.getQuestionsForQuizOnce(quiz.id)
        } else {
            emptyList()
        }

        val attempts = if (showAttempts || showStats) {
            context.repositories.attemptRepository.getAttemptsByQuiz(quiz.id).first()
        } else {
            emptyList()
        }

        return when (format) {
            "json" -> buildJsonOutput(quiz, questions, attempts, showQuestions, showAttempts, showStats, verbose)
            else -> buildTableOutput(
                quiz,
                questions,
                attempts,
                showQuestions,
                showAttempts,
                showStats,
                verbose,
                requestedFields
            )
        }
    }

    /**
     * Xay dung dau ra dinh dang bang cho thong tin quiz.
     */
    private fun buildTableOutput(
        quiz: Quiz,
        questions: List<Question>,
        attempts: List<Attempt>,
        showQuestions: Boolean,
        showAttempts: Boolean,
        showStats: Boolean,
        verbose: Boolean,
        requestedFields: List<String>?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        // === Thong tin co ban ===
        lines.add(OutputLine("== Thong tin quiz ==", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val basicFields = buildBasicFields(quiz, verbose)

        if (requestedFields != null) {
            for ((label, value, style) in basicFields) {
                val fieldKey = label.lowercase().replace(" ", "").replace(":", "")
                if (requestedFields.any { fieldKey.contains(it) }) {
                    lines.add(OutputLine("  ${CommandFormatUtils.padRight(label, 18)}: $value", style))
                }
            }
        } else {
            for ((label, value, style) in basicFields) {
                lines.add(OutputLine("  ${CommandFormatUtils.padRight(label, 18)}: $value", style))
            }
        }

        // === Danh sach cau hoi ===
        if (showQuestions) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("-- Danh sach cau hoi (${questions.size}) --", OutputStyle.HEADER))

            if (questions.isEmpty()) {
                lines.add(OutputLine("  (Khong co cau hoi nao)", OutputStyle.MUTED))
            } else {
                lines.add(OutputLine(""))
                lines.add(
                    OutputLine(
                        "  " + CommandFormatUtils.padRight("#", 4) + CommandFormatUtils.padRight("Noi dung", 44) +
                                CommandFormatUtils.padRight("Lua chon", 10) + CommandFormatUtils.padRight("Diem", 6) +
                                CommandFormatUtils.padRight("Nhieu dap an", 12),
                        OutputStyle.TABLE_HEADER
                    )
                )

                for ((index, question) in questions.withIndex()) {
                    val num = (index + 1).toString()
                    val content = CommandFormatUtils.truncate(question.content, 42)
                    val choiceCount = question.choices.size.toString()
                    val points = question.points.toString()
                    val multiSelect = if (question.isMultiSelect) "Co" else "Khong"

                    lines.add(
                        OutputLine(
                            "  " + CommandFormatUtils.padRight(num, 4) + CommandFormatUtils.padRight(content, 44) +
                                    CommandFormatUtils.padRight(choiceCount, 10) + CommandFormatUtils.padRight(
                                points,
                                6
                            ) +
                                    CommandFormatUtils.padRight(multiSelect, 12),
                            OutputStyle.TABLE_ROW
                        )
                    )

                    if (verbose) {
                        for ((ci, choice) in question.choices.withIndex()) {
                            val prefix = if (choice.isCorrect) "[V]" else "[ ]"
                            lines.add(
                                OutputLine(
                                    "       ${('A' + ci)}. $prefix ${CommandFormatUtils.truncate(choice.content, 50)}",
                                    if (choice.isCorrect) OutputStyle.SUCCESS else OutputStyle.MUTED
                                )
                            )
                        }
                        if (question.explanation != null) {
                            lines.add(
                                OutputLine(
                                    "       Giai thich: ${CommandFormatUtils.truncate(question.explanation, 60)}",
                                    OutputStyle.INFO
                                )
                            )
                        }
                        if (question.mediaUrl != null) {
                            lines.add(
                                OutputLine(
                                    "       Media: ${question.mediaUrl}",
                                    OutputStyle.MUTED
                                )
                            )
                        }
                    }
                }
            }
        }

        // === Lich su luot lam ===
        if (showAttempts) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("-- Lich su luot lam (${attempts.size}) --", OutputStyle.HEADER))

            if (attempts.isEmpty()) {
                lines.add(OutputLine("  (Chua co luot lam nao)", OutputStyle.MUTED))
            } else {
                lines.add(OutputLine(""))
                lines.add(
                    OutputLine(
                        "  " + CommandFormatUtils.padRight("User ID", 22) + CommandFormatUtils.padRight("Diem", 10) +
                                CommandFormatUtils.padRight("Thoi gian", 14) + CommandFormatUtils.padRight(
                            "Trang thai",
                            12
                        ),
                        OutputStyle.TABLE_HEADER
                    )
                )

                val sortedAttempts = attempts.sortedByDescending { it.startTimeMillis }
                val displayLimit = if (verbose) sortedAttempts.size else minOf(sortedAttempts.size, 20)

                for (attempt in sortedAttempts.take(displayLimit)) {
                    val scoreStr = "${attempt.score}/${attempt.maxScore}"
                    val duration = if (attempt.endTimeMillis != null) {
                        CommandFormatUtils.formatDuration((attempt.endTimeMillis - attempt.startTimeMillis) / 1000)
                    } else {
                        "-"
                    }
                    val status = if (attempt.endTimeMillis != null) "Hoan thanh" else "Dang lam"

                    lines.add(
                        OutputLine(
                            "  " + CommandFormatUtils.padRight(CommandFormatUtils.truncate(attempt.userId, 20), 22) +
                                    CommandFormatUtils.padRight(scoreStr, 10) + CommandFormatUtils.padRight(
                                duration,
                                14
                            ) +
                                    CommandFormatUtils.padRight(status, 12),
                            OutputStyle.TABLE_ROW
                        )
                    )

                    if (verbose) {
                        lines.add(
                            OutputLine(
                                "    ID: ${attempt.id} | Bat dau: ${CommandFormatUtils.formatTimestamp(attempt.startTimeMillis)}",
                                OutputStyle.MUTED
                            )
                        )
                    }
                }

                if (!verbose && sortedAttempts.size > 20) {
                    lines.add(
                        OutputLine(
                            "  ... va ${sortedAttempts.size - 20} luot lam khac (dung --verbose de xem tat ca)",
                            OutputStyle.MUTED
                        )
                    )
                }
            }
        }

        // === Thong ke diem so ===
        if (showStats) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("-- Thong ke diem so --", OutputStyle.HEADER))

            if (attempts.isEmpty()) {
                lines.add(OutputLine("  (Chua co du lieu thong ke)", OutputStyle.MUTED))
            } else {
                val completedAttempts = attempts.filter { it.endTimeMillis != null }
                val totalAttempts = attempts.size
                val completedCount = completedAttempts.size
                val incompleteCount = totalAttempts - completedCount
                val uniqueUsers = attempts.map { it.userId }.distinct().size

                lines.add(OutputLine(""))
                lines.add(OutputLine("  Tong luot lam        : $totalAttempts", OutputStyle.NORMAL))
                lines.add(OutputLine("  Hoan thanh           : $completedCount", OutputStyle.SUCCESS))
                if (incompleteCount > 0) {
                    lines.add(OutputLine("  Chua hoan thanh      : $incompleteCount", OutputStyle.WARNING))
                }
                lines.add(OutputLine("  Nguoi lam duy nhat   : $uniqueUsers", OutputStyle.NORMAL))

                if (completedAttempts.isNotEmpty()) {
                    val scores = completedAttempts.map { it.score }
                    val maxPossibleScore = completedAttempts.first().maxScore
                    val avgScore = scores.average()
                    val maxScore = scores.max()
                    val minScore = scores.min()
                    val medianScore = calculateMedian(scores)

                    val perfectCount = scores.count { it == maxPossibleScore }
                    val zeroCount = scores.count { it == 0 }

                    lines.add(OutputLine(""))
                    lines.add(
                        OutputLine(
                            "  Diem trung binh      : ${"%.2f".format(avgScore)}/$maxPossibleScore (${
                                "%.1f".format(
                                    avgScore / maxPossibleScore * 100
                                )
                            }%)", OutputStyle.INFO
                        )
                    )
                    lines.add(OutputLine("  Diem cao nhat        : $maxScore/$maxPossibleScore", OutputStyle.SUCCESS))
                    lines.add(
                        OutputLine(
                            "  Diem thap nhat       : $minScore/$maxPossibleScore",
                            if (minScore == 0) OutputStyle.WARNING else OutputStyle.NORMAL
                        )
                    )
                    lines.add(
                        OutputLine(
                            "  Diem trung vi        : ${"%.1f".format(medianScore)}/$maxPossibleScore",
                            OutputStyle.NORMAL
                        )
                    )

                    if (perfectCount > 0) {
                        lines.add(
                            OutputLine(
                                "  Diem tuyet doi       : $perfectCount (${"%.1f".format(perfectCount.toDouble() / completedCount * 100)}%)",
                                OutputStyle.SUCCESS
                            )
                        )
                    }
                    if (zeroCount > 0) {
                        lines.add(
                            OutputLine(
                                "  Diem 0               : $zeroCount (${"%.1f".format(zeroCount.toDouble() / completedCount * 100)}%)",
                                OutputStyle.WARNING
                            )
                        )
                    }

                    // Phan bo diem
                    if (verbose && maxPossibleScore > 0) {
                        lines.add(OutputLine(""))
                        lines.add(OutputLine("  Phan bo diem:", OutputStyle.HEADER))

                        val bucketSize = maxOf(1, maxPossibleScore / 5)
                        val buckets = mutableMapOf<String, Int>()
                        for (score in scores) {
                            val bucketStart = (score / bucketSize) * bucketSize
                            val bucketEnd = minOf(bucketStart + bucketSize - 1, maxPossibleScore)
                            val key = "$bucketStart-$bucketEnd"
                            buckets[key] = (buckets[key] ?: 0) + 1
                        }

                        val maxBucketCount = buckets.values.maxOrNull() ?: 1
                        for ((range, count) in buckets.toSortedMap()) {
                            val barLength = if (maxBucketCount > 0) (count * 20) / maxBucketCount else 0
                            val bar = "#".repeat(barLength)
                            val pct = "${"%.0f".format(count.toDouble() / completedCount * 100)}%"
                            lines.add(
                                OutputLine(
                                    "    ${CommandFormatUtils.padRight(range, 8)} ${
                                        CommandFormatUtils.padRight(
                                            bar,
                                            22
                                        )
                                    } $count ($pct)",
                                    OutputStyle.TABLE_ROW
                                )
                            )
                        }
                    }

                    // Thong ke thoi gian
                    val durations = completedAttempts.mapNotNull { a ->
                        a.endTimeMillis?.let { end -> (end - a.startTimeMillis) / 1000 }
                    }
                    if (durations.isNotEmpty()) {
                        val avgDuration = durations.average().toLong()
                        val maxDuration = durations.max()
                        val minDuration = durations.min()

                        lines.add(OutputLine(""))
                        lines.add(
                            OutputLine(
                                "  Thoi gian trung binh : ${CommandFormatUtils.formatDuration(avgDuration)}",
                                OutputStyle.NORMAL
                            )
                        )
                        lines.add(
                            OutputLine(
                                "  Thoi gian dai nhat   : ${CommandFormatUtils.formatDuration(maxDuration)}",
                                OutputStyle.NORMAL
                            )
                        )
                        lines.add(
                            OutputLine(
                                "  Thoi gian ngan nhat  : ${CommandFormatUtils.formatDuration(minDuration)}",
                                OutputStyle.NORMAL
                            )
                        )
                    }
                }
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung danh sach cac truong co ban cua quiz.
     *
     * @param quiz Quiz can hien thi.
     * @param verbose Hien thi chi tiet bo sung.
     * @return Danh sach (label, value, style) cho tung truong.
     */
    private fun buildBasicFields(
        quiz: Quiz,
        verbose: Boolean
    ): List<Triple<String, String, OutputStyle>> {
        val fields = mutableListOf<Triple<String, String, OutputStyle>>()

        fields.add(Triple("ID", quiz.id, OutputStyle.MUTED))
        fields.add(Triple("Tieu de", quiz.title, OutputStyle.NORMAL))

        if (quiz.description != null && quiz.description.isNotBlank()) {
            fields.add(Triple("Mo ta", CommandFormatUtils.truncate(quiz.description, 60), OutputStyle.NORMAL))
        }

        fields.add(Triple("Tac gia", quiz.authorName.ifBlank { "(khong ro)" }, OutputStyle.NORMAL))
        fields.add(Triple("Chu so huu", quiz.ownerId, OutputStyle.MUTED))

        // Trang thai
        val status = buildStatusLabel(quiz)
        val statusStyle = when {
            quiz.deletedAt != null -> OutputStyle.ERROR
            quiz.isDraft -> OutputStyle.WARNING
            quiz.isPublic -> OutputStyle.SUCCESS
            else -> OutputStyle.INFO
        }
        fields.add(Triple("Trang thai", status, statusStyle))

        fields.add(Triple("So cau hoi", quiz.questionCount.toString(), OutputStyle.NORMAL))
        fields.add(Triple("Luot lam", quiz.attemptCount.toString(), OutputStyle.NORMAL))

        if (quiz.tags.isNotEmpty()) {
            fields.add(Triple("Tags", quiz.tags.joinToString(", "), OutputStyle.INFO))
        }

        if (quiz.shareCode != null) {
            fields.add(Triple("Share code", quiz.shareCode, OutputStyle.INFO))
        }

        fields.add(Triple("Ngay tao", CommandFormatUtils.formatTimestamp(quiz.createdAt), OutputStyle.MUTED))
        fields.add(Triple("Cap nhat", CommandFormatUtils.formatTimestamp(quiz.updatedAt), OutputStyle.MUTED))

        if (quiz.deletedAt != null) {
            fields.add(Triple("Ngay xoa", CommandFormatUtils.formatTimestamp(quiz.deletedAt), OutputStyle.ERROR))
        }

        if (verbose) {
            if (quiz.thumbnailUrl != null) {
                fields.add(Triple("Anh thu nho", quiz.thumbnailUrl, OutputStyle.MUTED))
            }
            if (quiz.checksum != null) {
                fields.add(Triple("Checksum", quiz.checksum, OutputStyle.MUTED))
            }
            fields.add(Triple("Cong khai", if (quiz.isPublic) "Co" else "Khong", OutputStyle.NORMAL))
            fields.add(Triple("Nhap", if (quiz.isDraft) "Co" else "Khong", OutputStyle.NORMAL))
            fields.add(
                Triple(
                    "Xoa cloud", if (quiz.isRemovedFromCloud) "Co" else "Khong",
                    if (quiz.isRemovedFromCloud) OutputStyle.WARNING else OutputStyle.NORMAL
                )
            )
        }

        return fields
    }

    /**
     * Xay dung dau ra dinh dang JSON cho thong tin quiz.
     */
    private fun buildJsonOutput(
        quiz: Quiz,
        questions: List<Question>,
        attempts: List<Attempt>,
        showQuestions: Boolean,
        showAttempts: Boolean,
        showStats: Boolean,
        verbose: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))

        // Thong tin co ban
        lines.add(OutputLine("  \"id\": \"${CommandFormatUtils.escapeJson(quiz.id)}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"title\": \"${CommandFormatUtils.escapeJson(quiz.title)}\",", OutputStyle.CODE))

        val descStr = if (quiz.description != null) "\"${CommandFormatUtils.escapeJson(quiz.description)}\"" else "null"
        lines.add(OutputLine("  \"description\": $descStr,", OutputStyle.CODE))

        lines.add(
            OutputLine(
                "  \"authorName\": \"${CommandFormatUtils.escapeJson(quiz.authorName)}\",",
                OutputStyle.CODE
            )
        )
        lines.add(OutputLine("  \"ownerId\": \"${CommandFormatUtils.escapeJson(quiz.ownerId)}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"status\": \"${buildStatusLabel(quiz)}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"isPublic\": ${quiz.isPublic},", OutputStyle.CODE))
        lines.add(OutputLine("  \"isDraft\": ${quiz.isDraft},", OutputStyle.CODE))
        lines.add(OutputLine("  \"questionCount\": ${quiz.questionCount},", OutputStyle.CODE))
        lines.add(OutputLine("  \"attemptCount\": ${quiz.attemptCount},", OutputStyle.CODE))

        val tagsStr = quiz.tags.joinToString(", ") { "\"${CommandFormatUtils.escapeJson(it)}\"" }
        lines.add(OutputLine("  \"tags\": [$tagsStr],", OutputStyle.CODE))

        val shareCodeStr =
            if (quiz.shareCode != null) "\"${CommandFormatUtils.escapeJson(quiz.shareCode)}\"" else "null"
        lines.add(OutputLine("  \"shareCode\": $shareCodeStr,", OutputStyle.CODE))

        lines.add(OutputLine("  \"createdAt\": ${quiz.createdAt},", OutputStyle.CODE))
        lines.add(OutputLine("  \"updatedAt\": ${quiz.updatedAt},", OutputStyle.CODE))

        val deletedAtStr = quiz.deletedAt?.toString() ?: "null"
        lines.add(OutputLine("  \"deletedAt\": $deletedAtStr,", OutputStyle.CODE))

        if (verbose) {
            val thumbStr =
                if (quiz.thumbnailUrl != null) "\"${CommandFormatUtils.escapeJson(quiz.thumbnailUrl)}\"" else "null"
            lines.add(OutputLine("  \"thumbnailUrl\": $thumbStr,", OutputStyle.CODE))
            val checksumStr =
                if (quiz.checksum != null) "\"${CommandFormatUtils.escapeJson(quiz.checksum)}\"" else "null"
            lines.add(OutputLine("  \"checksum\": $checksumStr,", OutputStyle.CODE))
            lines.add(OutputLine("  \"isRemovedFromCloud\": ${quiz.isRemovedFromCloud},", OutputStyle.CODE))
        }

        // Cau hoi
        if (showQuestions) {
            lines.add(OutputLine("  \"questions\": [", OutputStyle.CODE))
            for ((qi, question) in questions.withIndex()) {
                val qComma = if (qi < questions.size - 1) "," else ""
                lines.add(OutputLine("    {", OutputStyle.CODE))
                lines.add(
                    OutputLine(
                        "      \"id\": \"${CommandFormatUtils.escapeJson(question.id)}\",",
                        OutputStyle.CODE
                    )
                )
                lines.add(
                    OutputLine(
                        "      \"content\": \"${CommandFormatUtils.escapeJson(question.content)}\",",
                        OutputStyle.CODE
                    )
                )
                lines.add(OutputLine("      \"isMultiSelect\": ${question.isMultiSelect},", OutputStyle.CODE))
                lines.add(OutputLine("      \"points\": ${question.points},", OutputStyle.CODE))
                lines.add(OutputLine("      \"position\": ${question.position},", OutputStyle.CODE))

                if (verbose) {
                    val explStr =
                        if (question.explanation != null) "\"${CommandFormatUtils.escapeJson(question.explanation)}\"" else "null"
                    lines.add(OutputLine("      \"explanation\": $explStr,", OutputStyle.CODE))
                    val mediaStr =
                        if (question.mediaUrl != null) "\"${CommandFormatUtils.escapeJson(question.mediaUrl)}\"" else "null"
                    lines.add(OutputLine("      \"mediaUrl\": $mediaStr,", OutputStyle.CODE))
                }

                lines.add(OutputLine("      \"choices\": [", OutputStyle.CODE))
                for ((ci, choice) in question.choices.withIndex()) {
                    val cComma = if (ci < question.choices.size - 1) "," else ""
                    lines.add(OutputLine("        {", OutputStyle.CODE))
                    lines.add(
                        OutputLine(
                            "          \"id\": \"${CommandFormatUtils.escapeJson(choice.id)}\",",
                            OutputStyle.CODE
                        )
                    )
                    lines.add(
                        OutputLine(
                            "          \"text\": \"${CommandFormatUtils.escapeJson(choice.content)}\",",
                            OutputStyle.CODE
                        )
                    )
                    lines.add(OutputLine("          \"isCorrect\": ${choice.isCorrect}", OutputStyle.CODE))
                    lines.add(OutputLine("        }$cComma", OutputStyle.CODE))
                }
                lines.add(OutputLine("      ]", OutputStyle.CODE))

                lines.add(OutputLine("    }$qComma", OutputStyle.CODE))
            }
            lines.add(OutputLine("  ],", OutputStyle.CODE))
        }

        // Luot lam
        if (showAttempts) {
            lines.add(OutputLine("  \"attempts\": [", OutputStyle.CODE))
            val sorted = attempts.sortedByDescending { it.startTimeMillis }
            for ((ai, attempt) in sorted.withIndex()) {
                val aComma = if (ai < sorted.size - 1) "," else ""
                val endStr = attempt.endTimeMillis?.toString() ?: "null"
                lines.add(OutputLine("    {", OutputStyle.CODE))
                lines.add(
                    OutputLine(
                        "      \"id\": \"${CommandFormatUtils.escapeJson(attempt.id)}\",",
                        OutputStyle.CODE
                    )
                )
                lines.add(
                    OutputLine(
                        "      \"userId\": \"${CommandFormatUtils.escapeJson(attempt.userId)}\",",
                        OutputStyle.CODE
                    )
                )
                lines.add(OutputLine("      \"score\": ${attempt.score},", OutputStyle.CODE))
                lines.add(OutputLine("      \"totalQuestions\": ${attempt.maxScore},", OutputStyle.CODE))
                lines.add(OutputLine("      \"startTimeMillis\": ${attempt.startTimeMillis},", OutputStyle.CODE))
                lines.add(OutputLine("      \"endTimeMillis\": $endStr", OutputStyle.CODE))
                lines.add(OutputLine("    }$aComma", OutputStyle.CODE))
            }
            lines.add(OutputLine("  ],", OutputStyle.CODE))
        }

        // Thong ke
        if (showStats) {
            lines.add(OutputLine("  \"stats\": {", OutputStyle.CODE))
            val completedAttempts = attempts.filter { it.endTimeMillis != null }
            lines.add(OutputLine("    \"totalAttempts\": ${attempts.size},", OutputStyle.CODE))
            lines.add(OutputLine("    \"completedAttempts\": ${completedAttempts.size},", OutputStyle.CODE))
            lines.add(
                OutputLine(
                    "    \"incompleteAttempts\": ${attempts.size - completedAttempts.size},",
                    OutputStyle.CODE
                )
            )
            lines.add(
                OutputLine(
                    "    \"uniqueUsers\": ${attempts.map { it.userId }.distinct().size},",
                    OutputStyle.CODE
                )
            )

            if (completedAttempts.isNotEmpty()) {
                val scores = completedAttempts.map { it.score }
                val avgScore = scores.average()
                lines.add(OutputLine("    \"averageScore\": ${"%.2f".format(avgScore)},", OutputStyle.CODE))
                lines.add(OutputLine("    \"maxScore\": ${scores.max()},", OutputStyle.CODE))
                lines.add(OutputLine("    \"minScore\": ${scores.min()},", OutputStyle.CODE))
                lines.add(
                    OutputLine(
                        "    \"medianScore\": ${"%.1f".format(calculateMedian(scores))},",
                        OutputStyle.CODE
                    )
                )

                val durations = completedAttempts.mapNotNull { a ->
                    a.endTimeMillis?.let { end -> (end - a.startTimeMillis) / 1000 }
                }
                if (durations.isNotEmpty()) {
                    lines.add(
                        OutputLine(
                            "    \"avgDurationSeconds\": ${durations.average().toLong()},",
                            OutputStyle.CODE
                        )
                    )
                    lines.add(OutputLine("    \"maxDurationSeconds\": ${durations.max()},", OutputStyle.CODE))
                    lines.add(OutputLine("    \"minDurationSeconds\": ${durations.min()}", OutputStyle.CODE))
                } else {
                    // Bo dau phay cuoi cua truong truoc
                    removeTrailingComma(lines)
                }
            } else {
                // Bo dau phay cuoi cua truong truoc
                removeTrailingComma(lines)
            }

            lines.add(OutputLine("  },", OutputStyle.CODE))
        }

        // Bo dau phay cuoi cung truoc khi dong ngoac
        removeTrailingComma(lines)

        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
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
     * Tinh trung vi (median) cua danh sach diem so.
     *
     * @param scores Danh sach diem so.
     * @return Gia tri trung vi.
     */
    private fun calculateMedian(scores: List<Int>): Double {
        if (scores.isEmpty()) return 0.0
        val sorted = scores.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid].toDouble()
        }
    }

    /**
     * Loai bo dau phay cuoi cung khoi dong JSON cuoi trong danh sach.
     */
    private fun removeTrailingComma(lines: MutableList<OutputLine>) {
        val lastIndex = lines.lastIndex
        if (lastIndex >= 0) {
            val lastLine = lines[lastIndex]
            if (lastLine.text.endsWith(",")) {
                lines[lastIndex] = lastLine.copy(text = lastLine.text.dropLast(1))
            }
        }
    }

}
