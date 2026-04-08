package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lenh `my` — truy van du lieu ca nhan cua nguoi dung hien tai.
 *
 * Cac lenh con:
 * - `my quizzes` — liet ke cac quiz cua ban
 * - `my attempts` — liet ke cac lan lam bai
 * - `my stats` — thong ke ca nhan
 * - `my pool` — cac dong gop vao ngan hang cau hoi
 *
 * Day la lenh chinh de nguoi dung xem du lieu cua minh tu console.
 */
class MyCommand : Command {

    override val name: String = "my"

    override val aliases: List<String> = listOf("me")

    override val description: String = "Truy van du lieu ca nhan (quiz, lan lam bai, thong ke, dong gop)"

    override val usage: String =
        "my <quizzes|attempts|stats|pool> [--public] [--private] [--draft] [--deleted] " +
            "[--tag <tag>] [--sort <field>] [--search <text>] [--format <table|json|list>] " +
            "[--limit <n>] [--page <n>] [--score-above <n>] [--score-below <n>] [--perfect] [--failed]"

    override val category: String = "user"

    override val examples: List<Pair<String, String>> = listOf(
        "my quizzes" to "Liet ke tat ca quiz cua ban",
        "my quizzes --public --sort title" to "Liet ke quiz cong khai, sap xep theo tieu de",
        "my quizzes --draft --tag kotlin" to "Liet ke ban nhap co tag 'kotlin'",
        "my quizzes --deleted" to "Liet ke quiz da xoa (thung rac)",
        "my attempts --perfect" to "Liet ke cac lan lam bai diem tuyet doi",
        "my attempts --score-above 80" to "Liet ke lan lam bai co diem tren 80%",
        "my attempts --sort score --format json" to "Xuat cac lan lam bai dang JSON, sap xep theo diem",
        "my stats" to "Hien thi thong ke ca nhan tong hop",
        "my pool" to "Liet ke cac dong gop vao ngan hang cau hoi"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        if (args.isEmpty()) {
            return SUBCOMMANDS.map { (name, desc) ->
                CompletionSuggestion(
                    text = name,
                    description = desc,
                    type = SuggestionType.SUBCOMMAND
                )
            }
        }

        if (args.size == 1) {
            val prefix = args[0].lowercase()
            return SUBCOMMANDS
                .filter { it.first.startsWith(prefix) }
                .map { (name, desc) ->
                    CompletionSuggestion(
                        text = name,
                        description = desc,
                        type = SuggestionType.SUBCOMMAND
                    )
                }
        }

        val sub = args[0].lowercase()
        return when (sub) {
            "quizzes" -> QUIZ_FLAGS.map { (flag, desc) ->
                CompletionSuggestion(text = flag, description = desc, type = SuggestionType.FLAG)
            }
            "attempts" -> ATTEMPT_FLAGS.map { (flag, desc) ->
                CompletionSuggestion(text = flag, description = desc, type = SuggestionType.FLAG)
            }
            "stats" -> listOf(
                CompletionSuggestion("--format", description = "Dinh dang xuat (table/json)", type = SuggestionType.FLAG),
                CompletionSuggestion("--verbose", description = "Hien thi chi tiet", type = SuggestionType.FLAG)
            )
            "pool" -> listOf(
                CompletionSuggestion("--format", description = "Dinh dang xuat", type = SuggestionType.FLAG),
                CompletionSuggestion("--active", description = "Chi hien dong gop dang hoat dong", type = SuggestionType.FLAG),
                CompletionSuggestion("--limit", description = "Gioi han so ket qua", type = SuggestionType.FLAG)
            )
            else -> emptyList()
        }
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error(
                "Thieu lenh con. Su dung: my <quizzes|attempts|stats|pool>\n" +
                    "Chay 'help my' de xem huong dan chi tiet."
            )
        }

        return when (args[0].lowercase()) {
            "quizzes", "quiz", "q" -> executeQuizzes(flags, context)
            "attempts", "attempt", "a" -> executeAttempts(flags, context)
            "stats", "stat", "s" -> executeStats(flags, context)
            "pool", "p" -> executePool(flags, context)
            else -> CommandResult.error(
                "Lenh con khong hop le: '${args[0]}'. " +
                    "Cac lenh con ho tro: quizzes, attempts, stats, pool"
            )
        }
    }

    // -------------------------------------------------------------------------
    // my quizzes
    // -------------------------------------------------------------------------

    /**
     * Xu ly lenh con `my quizzes` — liet ke quiz cua nguoi dung hien tai.
     */
    private suspend fun executeQuizzes(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val userId = context.currentUser.id
        val quizRepo = context.repositories.quizRepository

        val showDeleted = flags.containsKey("deleted")
        val quizzes: List<Quiz> = if (showDeleted) {
            quizRepo.getDeletedQuizzes(userId).first()
        } else {
            quizRepo.getMyQuizzes(userId).first()
        }

        var filtered = quizzes

        // --public / --private / --draft filters
        if (flags.containsKey("public")) {
            filtered = filtered.filter { it.isPublic && !it.isDraft }
        }
        if (flags.containsKey("private")) {
            filtered = filtered.filter { !it.isPublic && !it.isDraft }
        }
        if (flags.containsKey("draft")) {
            filtered = filtered.filter { it.isDraft }
        }

        // --tag <tag>
        val tagFilter = flags["tag"]
        if (tagFilter != null) {
            val tagLower = tagFilter.lowercase()
            filtered = filtered.filter { quiz ->
                quiz.tags.any { it.lowercase().contains(tagLower) }
            }
        }

        // --search <text>
        val searchText = flags["search"]
        if (searchText != null) {
            val queryLower = searchText.lowercase()
            filtered = filtered.filter { quiz ->
                quiz.title.lowercase().contains(queryLower) ||
                    quiz.description?.lowercase()?.contains(queryLower) == true
            }
        }

        // --since <date> (yyyy-MM-dd)
        val sinceDate = flags["since"]?.let { parseDateToMillis(it) }
        if (sinceDate != null) {
            filtered = filtered.filter { it.createdAt >= sinceDate }
        }

        // --before <date>
        val beforeDate = flags["before"]?.let { parseDateToMillis(it) }
        if (beforeDate != null) {
            filtered = filtered.filter { it.createdAt < beforeDate }
        }

        // --sort <field>
        val sortField = flags["sort"]?.lowercase() ?: "updated"
        filtered = when (sortField) {
            "title", "name" -> filtered.sortedBy { it.title.lowercase() }
            "created", "date" -> filtered.sortedByDescending { it.createdAt }
            "updated" -> filtered.sortedByDescending { it.updatedAt }
            "questions", "count" -> filtered.sortedByDescending { it.questionCount }
            "attempts", "popularity" -> filtered.sortedByDescending { it.attemptCount }
            else -> filtered.sortedByDescending { it.updatedAt }
        }

        // --limit <n> and --page <n>
        val limit = flags["limit"]?.toIntOrNull() ?: 20
        val page = flags["page"]?.toIntOrNull() ?: 1
        val totalCount = filtered.size
        val startIndex = (page - 1) * limit
        val paged = if (startIndex < filtered.size) {
            filtered.subList(startIndex, minOf(startIndex + limit, filtered.size))
        } else {
            emptyList()
        }

        // Format output
        val format = flags["format"]?.lowercase() ?: "table"
        val fields = flags["fields"]?.split(",")?.map { it.trim().lowercase() }

        return when (format) {
            "json" -> formatQuizzesJson(paged, totalCount, page, limit, fields)
            "list" -> formatQuizzesList(paged, totalCount, page, limit, showDeleted)
            else -> formatQuizzesTable(paged, totalCount, page, limit, showDeleted, fields)
        }
    }

    /**
     * Dinh dang danh sach quiz dang bang.
     */
    private fun formatQuizzesTable(
        quizzes: List<Quiz>,
        total: Int,
        page: Int,
        limit: Int,
        showDeleted: Boolean,
        fields: List<String>?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val label = if (showDeleted) "Quiz da xoa" else "Quiz cua ban"
        lines.add(OutputLine("=== $label (${quizzes.size}/$total) ===", OutputStyle.HEADER))

        if (quizzes.isEmpty()) {
            lines.add(OutputLine("  Khong tim thay quiz nao.", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        val showFields = fields ?: listOf("id", "title", "status", "questions", "attempts", "updated")

        // Header
        val headerParts = mutableListOf<String>()
        if ("id" in showFields) headerParts.add(padRight("ID", 10))
        if ("title" in showFields) headerParts.add(padRight("Tieu de", 30))
        if ("status" in showFields) headerParts.add(padRight("Trang thai", 14))
        if ("questions" in showFields) headerParts.add(padRight("Cau hoi", 8))
        if ("attempts" in showFields) headerParts.add(padRight("Luot lam", 9))
        if ("tags" in showFields) headerParts.add(padRight("Tags", 20))
        if ("updated" in showFields) headerParts.add(padRight("Cap nhat", 12))
        if ("created" in showFields) headerParts.add(padRight("Tao luc", 12))
        lines.add(OutputLine(headerParts.joinToString(" | "), OutputStyle.TABLE_HEADER))

        // Rows
        for (quiz in quizzes) {
            val rowParts = mutableListOf<String>()
            if ("id" in showFields) rowParts.add(padRight(quiz.id.take(8) + "..", 10))
            if ("title" in showFields) rowParts.add(padRight(truncate(quiz.title, 28), 30))
            if ("status" in showFields) rowParts.add(padRight(quizStatus(quiz), 14))
            if ("questions" in showFields) rowParts.add(padRight(quiz.questionCount.toString(), 8))
            if ("attempts" in showFields) rowParts.add(padRight(quiz.attemptCount.toString(), 9))
            if ("tags" in showFields) rowParts.add(padRight(truncate(quiz.tags.joinToString(", "), 18), 20))
            if ("updated" in showFields) rowParts.add(padRight(formatDate(quiz.updatedAt), 12))
            if ("created" in showFields) rowParts.add(padRight(formatDate(quiz.createdAt), 12))
            lines.add(OutputLine(rowParts.joinToString(" | "), OutputStyle.TABLE_ROW))
        }

        // Pagination
        val totalPages = (total + limit - 1) / limit
        if (totalPages > 1) {
            lines.add(OutputLine("Trang $page/$totalPages (dung --page <n> de chuyen trang)", OutputStyle.MUTED))
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang danh sach quiz dang danh sach don gian.
     */
    private fun formatQuizzesList(
        quizzes: List<Quiz>,
        total: Int,
        page: Int,
        limit: Int,
        showDeleted: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val label = if (showDeleted) "Quiz da xoa" else "Quiz cua ban"
        lines.add(OutputLine("$label ($total):", OutputStyle.HEADER))

        if (quizzes.isEmpty()) {
            lines.add(OutputLine("  (trong)", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        for ((index, quiz) in quizzes.withIndex()) {
            val num = (page - 1) * limit + index + 1
            val status = quizStatus(quiz)
            lines.add(OutputLine(
                "  $num. ${quiz.title} [$status] (${quiz.questionCount} cau, ${quiz.attemptCount} luot)",
                OutputStyle.NORMAL
            ))
            if (quiz.tags.isNotEmpty()) {
                lines.add(OutputLine("     Tags: ${quiz.tags.joinToString(", ")}", OutputStyle.MUTED))
            }
        }

        val totalPages = (total + limit - 1) / limit
        if (totalPages > 1) {
            lines.add(OutputLine("Trang $page/$totalPages", OutputStyle.MUTED))
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang danh sach quiz dang JSON.
     */
    private fun formatQuizzesJson(
        quizzes: List<Quiz>,
        total: Int,
        page: Int,
        limit: Int,
        fields: List<String>?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"page\": $page,", OutputStyle.CODE))
        lines.add(OutputLine("  \"limit\": $limit,", OutputStyle.CODE))
        lines.add(OutputLine("  \"quizzes\": [", OutputStyle.CODE))

        for ((index, quiz) in quizzes.withIndex()) {
            val comma = if (index < quizzes.size - 1) "," else ""
            lines.add(OutputLine("    {", OutputStyle.CODE))

            val showFields = fields ?: listOf("id", "title", "status", "questions", "attempts", "tags", "updated")
            val entries = mutableListOf<String>()
            if ("id" in showFields) entries.add("\"id\": \"${quiz.id}\"")
            if ("title" in showFields) entries.add("\"title\": \"${escapeJson(quiz.title)}\"")
            if ("status" in showFields) entries.add("\"status\": \"${quizStatus(quiz)}\"")
            if ("public" in showFields) entries.add("\"isPublic\": ${quiz.isPublic}")
            if ("draft" in showFields) entries.add("\"isDraft\": ${quiz.isDraft}")
            if ("questions" in showFields) entries.add("\"questionCount\": ${quiz.questionCount}")
            if ("attempts" in showFields) entries.add("\"attemptCount\": ${quiz.attemptCount}")
            if ("tags" in showFields) entries.add("\"tags\": [${quiz.tags.joinToString(", ") { "\"$it\"" }}]")
            if ("created" in showFields) entries.add("\"createdAt\": ${quiz.createdAt}")
            if ("updated" in showFields) entries.add("\"updatedAt\": ${quiz.updatedAt}")
            if ("share_code" in showFields) entries.add("\"shareCode\": ${quiz.shareCode?.let { "\"$it\"" } ?: "null"}")

            for ((ei, entry) in entries.withIndex()) {
                val eComma = if (ei < entries.size - 1) "," else ""
                lines.add(OutputLine("      $entry$eComma", OutputStyle.CODE))
            }
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    // -------------------------------------------------------------------------
    // my attempts
    // -------------------------------------------------------------------------

    /**
     * Xu ly lenh con `my attempts` — liet ke cac lan lam bai cua nguoi dung.
     */
    private suspend fun executeAttempts(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val userId = context.currentUser.id
        val attemptRepo = context.repositories.attemptRepository

        var attempts = attemptRepo.getAttemptsByUser(userId).first()

        // --quiz <quizId>
        val quizFilter = flags["quiz"]
        if (quizFilter != null) {
            attempts = attempts.filter { it.quizId == quizFilter }
        }

        // Score filters
        val scoreAbove = flags["score-above"]?.toIntOrNull()
        val scoreBelow = flags["score-below"]?.toIntOrNull()
        val isPerfect = flags.containsKey("perfect")
        val isFailed = flags.containsKey("failed")

        if (scoreAbove != null) {
            attempts = attempts.filter { attempt ->
                percentageScore(attempt) >= scoreAbove
            }
        }
        if (scoreBelow != null) {
            attempts = attempts.filter { attempt ->
                percentageScore(attempt) <= scoreBelow
            }
        }
        if (isPerfect) {
            attempts = attempts.filter { it.score == it.totalQuestions }
        }
        if (isFailed) {
            attempts = attempts.filter { percentageScore(it) < 50 }
        }

        // --since / --before date filters
        val sinceDate = flags["since"]?.let { parseDateToMillis(it) }
        if (sinceDate != null) {
            attempts = attempts.filter { it.startTimeMillis >= sinceDate }
        }
        val beforeDate = flags["before"]?.let { parseDateToMillis(it) }
        if (beforeDate != null) {
            attempts = attempts.filter { it.startTimeMillis < beforeDate }
        }

        // --sort <field>
        val sortField = flags["sort"]?.lowercase() ?: "date"
        attempts = when (sortField) {
            "score" -> attempts.sortedByDescending { percentageScore(it) }
            "date", "time", "recent" -> attempts.sortedByDescending { it.startTimeMillis }
            "duration" -> attempts.sortedByDescending { attemptDurationMs(it) }
            "questions" -> attempts.sortedByDescending { it.totalQuestions }
            else -> attempts.sortedByDescending { it.startTimeMillis }
        }

        // Pagination
        val limit = flags["limit"]?.toIntOrNull() ?: 20
        val page = flags["page"]?.toIntOrNull() ?: 1
        val totalCount = attempts.size
        val startIndex = (page - 1) * limit
        val paged = if (startIndex < attempts.size) {
            attempts.subList(startIndex, minOf(startIndex + limit, attempts.size))
        } else {
            emptyList()
        }

        val format = flags["format"]?.lowercase() ?: "table"

        return when (format) {
            "json" -> formatAttemptsJson(paged, totalCount, page, limit, context)
            "list" -> formatAttemptsList(paged, totalCount, page, limit, context)
            else -> formatAttemptsTable(paged, totalCount, page, limit, context)
        }
    }

    /**
     * Dinh dang danh sach lan lam bai dang bang.
     */
    private suspend fun formatAttemptsTable(
        attempts: List<Attempt>,
        total: Int,
        page: Int,
        limit: Int,
        context: CommandContext
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("=== Lan lam bai cua ban ($total) ===", OutputStyle.HEADER))

        if (attempts.isEmpty()) {
            lines.add(OutputLine("  Chua co lan lam bai nao.", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        lines.add(OutputLine(
            "${padRight("ID", 10)} | ${padRight("Quiz", 25)} | ${padRight("Diem", 10)} | " +
                "${padRight("Thoi gian", 10)} | ${padRight("Ngay", 12)}",
            OutputStyle.TABLE_HEADER
        ))

        for (attempt in attempts) {
            val quizTitle = resolveQuizTitle(attempt.quizId, context)
            val pct = percentageScore(attempt)
            val scoreText = "${attempt.score}/${attempt.totalQuestions} ($pct%)"
            val duration = formatDurationMs(attemptDurationMs(attempt))
            val dateText = formatDate(attempt.startTimeMillis)

            val scoreStyle = when {
                pct == 100 -> OutputStyle.SUCCESS
                pct < 50 -> OutputStyle.WARNING
                else -> OutputStyle.TABLE_ROW
            }

            lines.add(OutputLine(
                "${padRight(attempt.id.take(8) + "..", 10)} | ${padRight(truncate(quizTitle, 23), 25)} | " +
                    "${padRight(scoreText, 10)} | ${padRight(duration, 10)} | ${padRight(dateText, 12)}",
                scoreStyle
            ))
        }

        val totalPages = (total + limit - 1) / limit
        if (totalPages > 1) {
            lines.add(OutputLine("Trang $page/$totalPages", OutputStyle.MUTED))
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang danh sach lan lam bai dang danh sach don gian.
     */
    private suspend fun formatAttemptsList(
        attempts: List<Attempt>,
        total: Int,
        page: Int,
        limit: Int,
        context: CommandContext
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Lan lam bai ($total):", OutputStyle.HEADER))

        if (attempts.isEmpty()) {
            lines.add(OutputLine("  (trong)", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        for ((index, attempt) in attempts.withIndex()) {
            val num = (page - 1) * limit + index + 1
            val quizTitle = resolveQuizTitle(attempt.quizId, context)
            val pct = percentageScore(attempt)
            val duration = formatDurationMs(attemptDurationMs(attempt))
            lines.add(OutputLine(
                "  $num. $quizTitle - ${attempt.score}/${attempt.totalQuestions} ($pct%) - $duration",
                OutputStyle.NORMAL
            ))
        }

        val totalPages = (total + limit - 1) / limit
        if (totalPages > 1) {
            lines.add(OutputLine("Trang $page/$totalPages", OutputStyle.MUTED))
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang danh sach lan lam bai dang JSON.
     */
    private suspend fun formatAttemptsJson(
        attempts: List<Attempt>,
        total: Int,
        page: Int,
        limit: Int,
        context: CommandContext
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"page\": $page,", OutputStyle.CODE))
        lines.add(OutputLine("  \"limit\": $limit,", OutputStyle.CODE))
        lines.add(OutputLine("  \"attempts\": [", OutputStyle.CODE))

        for ((index, attempt) in attempts.withIndex()) {
            val comma = if (index < attempts.size - 1) "," else ""
            val quizTitle = resolveQuizTitle(attempt.quizId, context)
            val pct = percentageScore(attempt)
            val duration = attemptDurationMs(attempt)
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${attempt.id}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"quizId\": \"${attempt.quizId}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"quizTitle\": \"${escapeJson(quizTitle)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"score\": ${attempt.score},", OutputStyle.CODE))
            lines.add(OutputLine("      \"totalQuestions\": ${attempt.totalQuestions},", OutputStyle.CODE))
            lines.add(OutputLine("      \"percentage\": $pct,", OutputStyle.CODE))
            lines.add(OutputLine("      \"durationMs\": $duration,", OutputStyle.CODE))
            lines.add(OutputLine("      \"startTime\": ${attempt.startTimeMillis}", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    // -------------------------------------------------------------------------
    // my stats
    // -------------------------------------------------------------------------

    /**
     * Xu ly lenh con `my stats` — hien thi thong ke ca nhan tong hop.
     */
    private suspend fun executeStats(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val userId = context.currentUser.id
        val quizRepo = context.repositories.quizRepository
        val attemptRepo = context.repositories.attemptRepository

        val myQuizzes = quizRepo.getMyQuizzes(userId).first()
        val deletedQuizzes = quizRepo.getDeletedQuizzes(userId).first()
        val attempts = attemptRepo.getAttemptsByUser(userId).first()

        val totalQuizzes = myQuizzes.size
        val publicQuizzes = myQuizzes.count { it.isPublic && !it.isDraft }
        val privateQuizzes = myQuizzes.count { !it.isPublic && !it.isDraft }
        val draftQuizzes = myQuizzes.count { it.isDraft }
        val deletedCount = deletedQuizzes.size
        val totalQuestions = myQuizzes.sumOf { it.questionCount }
        val totalAttemptsOnMyQuizzes = myQuizzes.sumOf { it.attemptCount }

        val totalAttempts = attempts.size
        val perfectAttempts = attempts.count { it.score == it.totalQuestions }
        val avgScore = if (attempts.isNotEmpty()) {
            attempts.map { percentageScore(it) }.average()
        } else {
            0.0
        }
        val totalTimeTaken = attempts.sumOf { attemptDurationMs(it) }

        // All unique tags from user's quizzes
        val allTags = myQuizzes.flatMap { it.tags }.distinct().sorted()

        val format = flags["format"]?.lowercase() ?: "table"
        val verbose = flags.containsKey("verbose")

        if (format == "json") {
            return formatStatsJson(
                totalQuizzes, publicQuizzes, privateQuizzes, draftQuizzes, deletedCount,
                totalQuestions, totalAttemptsOnMyQuizzes, totalAttempts, perfectAttempts,
                avgScore, totalTimeTaken, allTags, verbose
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("=== Thong ke ca nhan ===", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        // Quiz section
        lines.add(OutputLine("-- Quiz --", OutputStyle.INFO))
        lines.add(OutputLine("  Tong so quiz:       $totalQuizzes", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Cong khai:          $publicQuizzes", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Rieng tu:           $privateQuizzes", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Ban nhap:           $draftQuizzes", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Da xoa:             $deletedCount", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Tong so cau hoi:    $totalQuestions", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Luot lam quiz ban:  $totalAttemptsOnMyQuizzes", OutputStyle.TABLE_ROW))
        lines.add(OutputLine(""))

        // Attempt section
        lines.add(OutputLine("-- Lam bai --", OutputStyle.INFO))
        lines.add(OutputLine("  Tong lan lam:       $totalAttempts", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Diem tuyet doi:     $perfectAttempts", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Diem trung binh:    ${"%.1f".format(avgScore)}%", OutputStyle.TABLE_ROW))
        lines.add(OutputLine("  Tong thoi gian:     ${formatDurationMs(totalTimeTaken)}", OutputStyle.TABLE_ROW))

        if (verbose && attempts.isNotEmpty()) {
            val maxScore = attempts.maxOf { percentageScore(it) }
            val minScore = attempts.minOf { percentageScore(it) }
            val medianScore = attempts.map { percentageScore(it) }.sorted().let { sorted ->
                if (sorted.size % 2 == 0) {
                    (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
                } else {
                    sorted[sorted.size / 2]
                }
            }
            lines.add(OutputLine("  Diem cao nhat:      $maxScore%", OutputStyle.TABLE_ROW))
            lines.add(OutputLine("  Diem thap nhat:     $minScore%", OutputStyle.TABLE_ROW))
            lines.add(OutputLine("  Diem trung vi:      $medianScore%", OutputStyle.TABLE_ROW))

            val uniqueQuizzesTaken = attempts.map { it.quizId }.distinct().size
            lines.add(OutputLine("  Quiz da lam (duy nhat): $uniqueQuizzesTaken", OutputStyle.TABLE_ROW))
        }

        if (verbose && allTags.isNotEmpty()) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("-- Tags --", OutputStyle.INFO))
            lines.add(OutputLine("  ${allTags.joinToString(", ")}", OutputStyle.MUTED))
        }

        return CommandResult.success(lines)
    }

    /**
     * Dinh dang thong ke ca nhan dang JSON.
     */
    private fun formatStatsJson(
        totalQuizzes: Int,
        publicQuizzes: Int,
        privateQuizzes: Int,
        draftQuizzes: Int,
        deletedCount: Int,
        totalQuestions: Int,
        totalAttemptsOnMyQuizzes: Int,
        totalAttempts: Int,
        perfectAttempts: Int,
        avgScore: Double,
        totalTimeTaken: Long,
        allTags: List<String>,
        verbose: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"quizzes\": {", OutputStyle.CODE))
        lines.add(OutputLine("    \"total\": $totalQuizzes,", OutputStyle.CODE))
        lines.add(OutputLine("    \"public\": $publicQuizzes,", OutputStyle.CODE))
        lines.add(OutputLine("    \"private\": $privateQuizzes,", OutputStyle.CODE))
        lines.add(OutputLine("    \"draft\": $draftQuizzes,", OutputStyle.CODE))
        lines.add(OutputLine("    \"deleted\": $deletedCount,", OutputStyle.CODE))
        lines.add(OutputLine("    \"totalQuestions\": $totalQuestions,", OutputStyle.CODE))
        lines.add(OutputLine("    \"attemptsReceived\": $totalAttemptsOnMyQuizzes", OutputStyle.CODE))
        lines.add(OutputLine("  },", OutputStyle.CODE))
        lines.add(OutputLine("  \"attempts\": {", OutputStyle.CODE))
        lines.add(OutputLine("    \"total\": $totalAttempts,", OutputStyle.CODE))
        lines.add(OutputLine("    \"perfect\": $perfectAttempts,", OutputStyle.CODE))
        lines.add(OutputLine("    \"averageScore\": ${"%.1f".format(avgScore)},", OutputStyle.CODE))
        lines.add(OutputLine("    \"totalTimeMs\": $totalTimeTaken", OutputStyle.CODE))
        lines.add(OutputLine("  }", OutputStyle.CODE))

        if (verbose && allTags.isNotEmpty()) {
            // Replace the last line's closing brace with comma
            lines[lines.size - 1] = OutputLine("  },", OutputStyle.CODE)
            lines.add(OutputLine("  \"tags\": [${allTags.joinToString(", ") { "\"$it\"" }}]", OutputStyle.CODE))
        }

        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    // -------------------------------------------------------------------------
    // my pool
    // -------------------------------------------------------------------------

    /**
     * Xu ly lenh con `my pool` — liet ke cac dong gop vao ngan hang cau hoi.
     */
    private suspend fun executePool(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val userId = context.currentUser.id
        val poolRepo = context.repositories.poolRepository

        val result = poolRepo.getMyContributions(userId)
        if (result.isFailure) {
            return CommandResult.error(
                "Khong the tai dong gop: ${result.exceptionOrNull()?.message ?: "Loi khong xac dinh"}"
            )
        }

        var contributions = result.getOrDefault(emptyList())

        // --active filter
        if (flags.containsKey("active")) {
            contributions = contributions.filter { it.isActive }
        }

        // --tag filter
        val tagFilter = flags["tag"]
        if (tagFilter != null) {
            val tagLower = tagFilter.lowercase()
            contributions = contributions.filter { item ->
                item.tags.any { it.lowercase().contains(tagLower) }
            }
        }

        // --limit
        val limit = flags["limit"]?.toIntOrNull() ?: 20
        val totalCount = contributions.size
        val limited = contributions.take(limit)

        val format = flags["format"]?.lowercase() ?: "table"

        if (format == "json") {
            val lines = mutableListOf<OutputLine>()
            lines.add(OutputLine("{", OutputStyle.CODE))
            lines.add(OutputLine("  \"total\": $totalCount,", OutputStyle.CODE))
            lines.add(OutputLine("  \"contributions\": [", OutputStyle.CODE))
            for ((index, item) in limited.withIndex()) {
                val comma = if (index < limited.size - 1) "," else ""
                lines.add(OutputLine("    {", OutputStyle.CODE))
                lines.add(OutputLine("      \"id\": \"${item.id}\",", OutputStyle.CODE))
                lines.add(OutputLine("      \"question\": \"${escapeJson(item.question.content)}\",", OutputStyle.CODE))
                lines.add(OutputLine("      \"tags\": [${item.tags.joinToString(", ") { "\"$it\"" }}],", OutputStyle.CODE))
                lines.add(OutputLine("      \"usageCount\": ${item.usageCount},", OutputStyle.CODE))
                lines.add(OutputLine("      \"isActive\": ${item.isActive}", OutputStyle.CODE))
                lines.add(OutputLine("    }$comma", OutputStyle.CODE))
            }
            lines.add(OutputLine("  ]", OutputStyle.CODE))
            lines.add(OutputLine("}", OutputStyle.CODE))
            return CommandResult.success(lines)
        }

        // Table format
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("=== Dong gop cua ban ($totalCount) ===", OutputStyle.HEADER))

        if (limited.isEmpty()) {
            lines.add(OutputLine("  Chua co dong gop nao.", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        lines.add(OutputLine(
            "${padRight("ID", 10)} | ${padRight("Cau hoi", 35)} | ${padRight("Tags", 18)} | " +
                "${padRight("Luot dung", 10)} | ${padRight("Trang thai", 11)}",
            OutputStyle.TABLE_HEADER
        ))

        for (item in limited) {
            val status = if (item.isActive) "Hoat dong" else "Ngung"
            val statusStyle = if (item.isActive) OutputStyle.TABLE_ROW else OutputStyle.MUTED
            lines.add(OutputLine(
                "${padRight(item.id.take(8) + "..", 10)} | " +
                    "${padRight(truncate(item.question.content, 33), 35)} | " +
                    "${padRight(truncate(item.tags.joinToString(", "), 16), 18)} | " +
                    "${padRight(item.usageCount.toString(), 10)} | " +
                    padRight(status, 11),
                statusStyle
            ))
        }

        if (totalCount > limit) {
            lines.add(OutputLine("Hien thi $limit/$totalCount (dung --limit <n> de xem them)", OutputStyle.MUTED))
        }

        return CommandResult.success(lines)
    }

    // -------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------

    /**
     * Phan giai tieu de quiz tu quizId. Tra ve "???" neu khong tim thay.
     */
    private suspend fun resolveQuizTitle(quizId: String, context: CommandContext): String {
        return try {
            context.repositories.quizRepository.getQuizById(quizId)?.title ?: "(Quiz #${quizId.take(6)})"
        } catch (_: Exception) {
            "(Quiz #${quizId.take(6)})"
        }
    }

    /**
     * Tinh diem phan tram cua mot lan lam bai.
     */
    private fun percentageScore(attempt: Attempt): Int {
        if (attempt.totalQuestions == 0) return 0
        return (attempt.score * 100) / attempt.totalQuestions
    }

    /**
     * Tinh thoi gian lam bai (milliseconds).
     */
    private fun attemptDurationMs(attempt: Attempt): Long {
        val end = attempt.endTimeMillis ?: attempt.startTimeMillis
        return end - attempt.startTimeMillis
    }

    /**
     * Tao chuoi trang thai cua quiz.
     */
    private fun quizStatus(quiz: Quiz): String {
        return when {
            quiz.deletedAt != null -> "Da xoa"
            quiz.isDraft -> "Ban nhap"
            quiz.isPublic -> "Cong khai"
            else -> "Rieng tu"
        }
    }

    /**
     * Dinh dang thoi gian tu milliseconds thanh chuoi doc duoc.
     */
    private fun formatDurationMs(ms: Long): String {
        if (ms <= 0) return "0s"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }

    /**
     * Phan tich ngay tu chuoi (ho tro yyyy-MM-dd) thanh epoch milliseconds.
     */
    private fun parseDateToMillis(dateStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Dinh dang epoch millis thanh chuoi ngay dd/MM/yy.
     */
    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yy", Locale.US)
        return sdf.format(Date(millis))
    }

    /**
     * Cat chuoi va them "..." neu qua dai.
     */
    private fun truncate(text: String, maxLen: Int): String {
        return if (text.length <= maxLen) text
        else text.take(maxLen - 2) + ".."
    }

    /**
     * Do them khoang trang ben phai chuoi de dat do dai toi thieu.
     */
    private fun padRight(text: String, width: Int): String {
        return if (text.length >= width) text.take(width)
        else text + " ".repeat(width - text.length)
    }

    /**
     * Escape cac ky tu dac biet trong JSON string.
     */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private companion object {
        /** Cac lenh con ho tro boi lenh `my`. */
        val SUBCOMMANDS = listOf(
            "quizzes" to "Liet ke quiz cua ban",
            "attempts" to "Liet ke cac lan lam bai",
            "stats" to "Thong ke ca nhan tong hop",
            "pool" to "Dong gop vao ngan hang cau hoi"
        )

        /** Cac co cho lenh con `my quizzes`. */
        val QUIZ_FLAGS = listOf(
            "--public" to "Chi quiz cong khai",
            "--private" to "Chi quiz rieng tu",
            "--draft" to "Chi ban nhap",
            "--deleted" to "Quiz da xoa (thung rac)",
            "--tag" to "Loc theo tag",
            "--search" to "Tim kiem theo tieu de/mo ta",
            "--sort" to "Sap xep (title/created/updated/questions/attempts)",
            "--since" to "Loc tu ngay (yyyy-MM-dd)",
            "--before" to "Loc truoc ngay (yyyy-MM-dd)",
            "--format" to "Dinh dang xuat (table/json/list)",
            "--fields" to "Chon truong hien thi",
            "--limit" to "Gioi han so ket qua",
            "--page" to "So trang"
        )

        /** Cac co cho lenh con `my attempts`. */
        val ATTEMPT_FLAGS = listOf(
            "--quiz" to "Loc theo quiz ID",
            "--score-above" to "Diem tren nguong (%)",
            "--score-below" to "Diem duoi nguong (%)",
            "--perfect" to "Chi lan diem tuyet doi",
            "--failed" to "Chi lan diem duoi 50%",
            "--sort" to "Sap xep (date/score/duration/questions)",
            "--since" to "Loc tu ngay (yyyy-MM-dd)",
            "--before" to "Loc truoc ngay (yyyy-MM-dd)",
            "--format" to "Dinh dang xuat (table/json/list)",
            "--limit" to "Gioi han so ket qua",
            "--page" to "So trang"
        )
    }
}
