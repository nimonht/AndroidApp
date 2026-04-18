package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.CommandFormatUtils
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh `export` — xuat du lieu he thong duoi dang CSV, JSON hoac bang.
 *
 * Cho phep quan tri vien xuat danh sach nguoi dung, quiz, luot lam,
 * thong ke he thong hoac nhat ky ung dung ra cac dinh dang khac nhau.
 * Ho tro loc truong (`--fields`), gioi han so luong (`--limit`),
 * va chon dinh dang dau ra (`--format`).
 *
 * Cac lenh con:
 * - `export users`    — Xuat danh sach nguoi dung
 * - `export quizzes`  — Xuat danh sach quiz
 * - `export attempts` — Xuat danh sach luot lam
 * - `export stats`    — Xuat thong ke he thong
 * - `export logs`     — Xuat bo nho dem nhat ky
 *
 * Flags:
 * - `--format <csv|json|table>` : Dinh dang dau ra (mac dinh: table)
 * - `--fields <field1,field2>`  : Chon cac truong can xuat
 * - `--limit <n>`               : Gioi han so ban ghi
 *
 * Yeu cau quyen [AdminPermission.VIEW_REPORTS] va vai tro toi thieu [UserRole.ADMIN].
 */
class ExportCommand : Command {

    override val name: String = "export"

    override val aliases: List<String> = emptyList()

    override val description: String = "Xuat du lieu he thong (nguoi dung, quiz, luot lam, thong ke, nhat ky)"

    override val usage: String =
        "export <users|quizzes|attempts|stats|logs> [--format <csv|json|table>] " +
                "[--fields <field1,field2,...>] [--limit <n>]"

    override val requiredPermission: AdminPermission = AdminPermission.VIEW_REPORTS

    override val minimumRole: UserRole = UserRole.ADMIN

    override val category: String = "admin"

    override val examples: List<Pair<String, String>> = listOf(
        "export users --format csv" to "Xuat danh sach nguoi dung dang CSV",
        "export users --format json --fields id,email,role" to "Xuat nguoi dung dang JSON voi cac truong cu the",
        "export quizzes --limit 50" to "Xuat 50 quiz dau tien dang bang",
        "export quizzes --format csv --fields id,title,tags" to "Xuat quiz dang CSV voi truong chon loc",
        "export attempts --format json" to "Xuat tat ca luot lam dang JSON",
        "export stats" to "Xuat thong ke he thong dang bang",
        "export stats --format csv" to "Xuat thong ke he thong dang CSV",
        "export logs" to "Xuat nhat ky ung dung",
        "export logs --format json --limit 100" to "Xuat 100 ban ghi nhat ky gan nhat dang JSON"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        // Goi y lenh con neu chua nhap
        if (args.isEmpty()) {
            for ((sub, desc) in SUBCOMMANDS) {
                suggestions.add(
                    CompletionSuggestion(
                        text = sub,
                        description = desc,
                        type = SuggestionType.SUBCOMMAND
                    )
                )
            }
            return suggestions
        }

        // Goi y lenh con theo tien to
        if (args.size == 1) {
            val prefix = args[0].lowercase()
            val matchesExact = SUBCOMMANDS.any { it.first == prefix }
            if (!matchesExact) {
                for ((sub, desc) in SUBCOMMANDS) {
                    if (sub.startsWith(prefix)) {
                        suggestions.add(
                            CompletionSuggestion(
                                text = sub,
                                description = desc,
                                type = SuggestionType.SUBCOMMAND
                            )
                        )
                    }
                }
                return suggestions
            }
        }

        // Goi y gia tri cho --format
        if ("format" in flags && flags["format"] == null) {
            for (fmt in SUPPORTED_FORMATS) {
                suggestions.add(
                    CompletionSuggestion(
                        text = fmt,
                        description = "Dinh dang $fmt",
                        type = SuggestionType.ARGUMENT
                    )
                )
            }
            return suggestions
        }

        // Goi y gia tri cho --fields dua tren lenh con
        if ("fields" in flags && flags["fields"] == null) {
            val subcommand = args.firstOrNull()?.lowercase() ?: ""
            val availableFields = fieldsForSubcommand(subcommand)
            for (field in availableFields) {
                suggestions.add(
                    CompletionSuggestion(
                        text = field,
                        description = "Truong $field",
                        type = SuggestionType.ARGUMENT
                    )
                )
            }
            return suggestions
        }

        // Goi y cac flag chua su dung
        val availableFlags = listOf(
            "--format" to "Dinh dang dau ra (csv/json/table)",
            "--fields" to "Chon truong can xuat (cach nhau boi dau phay)",
            "--limit" to "Gioi han so ban ghi xuat"
        )

        for ((flag, desc) in availableFlags) {
            val flagName = flag.removePrefix("--")
            if (flagName !in flags) {
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
        if (args.isEmpty()) {
            return buildNoSubcommandError()
        }

        val subcommand = args[0].lowercase()
        val format = (flags["format"] ?: "table").lowercase()
        val requestedFields = flags["fields"]?.split(",")?.map { it.trim().lowercase() }
        val limit = flags["limit"]?.toIntOrNull()

        if (format !in SUPPORTED_FORMATS) {
            return CommandResult.error(
                "Dinh dang khong hop le: '$format'. Cac dinh dang ho tro: ${SUPPORTED_FORMATS.joinToString(", ")}"
            )
        }

        return when (subcommand) {
            "users" -> exportUsers(context, format, requestedFields, limit)
            "quizzes" -> exportQuizzes(context, format, requestedFields, limit)
            "attempts" -> exportAttempts(context, format, requestedFields, limit)
            "stats" -> exportStats(context, format, requestedFields)
            "logs" -> exportLogs(context, format, requestedFields, limit)
            else -> CommandResult.error(
                "Lenh con khong hop le: '$subcommand'. " +
                        "Cac lenh con ho tro: ${SUBCOMMANDS.joinToString(", ") { it.first }}"
            )
        }
    }

    // ====================================================================
    // Subcommand: users
    // ====================================================================

    /**
     * Xuat danh sach nguoi dung tu adminRepository.
     *
     * @param context Context lenh hien tai.
     * @param format Dinh dang dau ra (csv/json/table).
     * @param requestedFields Danh sach truong can xuat, hoac null de xuat tat ca.
     * @param limit Gioi han so ban ghi, hoac null de xuat tat ca.
     * @return [CommandResult] chua du lieu nguoi dung da dinh dang.
     */
    private suspend fun exportUsers(
        context: CommandContext,
        format: String,
        requestedFields: List<String>?,
        limit: Int?
    ): CommandResult {
        val allUsers: List<User> = try {
            context.repositories.adminRepository.getAllUsers().first()
        } catch (e: Exception) {
            return CommandResult.error("Khong the tai danh sach nguoi dung: ${e.message}")
        }

        if (allUsers.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong co nguoi dung nao de xuat.", OutputStyle.WARNING))
            )
        }

        val users = if (limit != null && limit > 0) allUsers.take(limit) else allUsers
        val fields = requestedFields ?: USER_DEFAULT_FIELDS
        val validFields = fields.filter { it in USER_ALL_FIELDS }

        if (validFields.isEmpty()) {
            return CommandResult.error(
                "Khong co truong hop le. Cac truong ho tro: ${USER_ALL_FIELDS.joinToString(", ")}"
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Xuat ${users.size}/${allUsers.size} nguoi dung (dinh dang: $format)",
                OutputStyle.HEADER
            )
        )

        when (format) {
            "csv" -> buildUsersCsv(users, validFields, lines)
            "json" -> buildUsersJson(users, validFields, lines)
            else -> buildUsersTable(users, validFields, lines)
        }

        lines.add(OutputLine("Tong: ${users.size} ban ghi da xuat.", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Xuat nguoi dung dang CSV.
     */
    private fun buildUsersCsv(
        users: List<User>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine(fields.joinToString(","), OutputStyle.TABLE_HEADER))
        for (user in users) {
            val values = fields.map { field -> CommandFormatUtils.csvEscape(getUserFieldValue(user, field)) }
            lines.add(OutputLine(values.joinToString(","), OutputStyle.TABLE_ROW))
        }
    }

    /**
     * Xuat nguoi dung dang JSON.
     */
    private fun buildUsersJson(
        users: List<User>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine("[", OutputStyle.CODE))
        for ((index, user) in users.withIndex()) {
            val comma = if (index < users.size - 1) "," else ""
            val pairs = fields.joinToString(", ") { field ->
                "\"$field\": \"${CommandFormatUtils.escapeJson(getUserFieldValue(user, field))}\""
            }
            lines.add(OutputLine("  { $pairs }$comma", OutputStyle.CODE))
        }
        lines.add(OutputLine("]", OutputStyle.CODE))
    }

    /**
     * Xuat nguoi dung dang bang can chinh cot.
     */
    private fun buildUsersTable(
        users: List<User>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        val widths = calculateColumnWidths(fields, users.map { u -> fields.map { getUserFieldValue(u, it) } })
        val header = fields.mapIndexed { i, f -> CommandFormatUtils.padRight(f.uppercase(), widths[i]) }
            .joinToString(COL_SEPARATOR)
        lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))

        for (user in users) {
            val row = fields.mapIndexed { i, f ->
                CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(getUserFieldValue(user, f), widths[i]),
                    widths[i]
                )
            }.joinToString(COL_SEPARATOR)
            val style = if (user.isBanned) OutputStyle.WARNING else OutputStyle.TABLE_ROW
            lines.add(OutputLine(row, style))
        }
    }

    /**
     * Lay gia tri cua truong cho nguoi dung.
     */
    private fun getUserFieldValue(user: User, field: String): String = when (field) {
        "id" -> user.id
        "email" -> user.email
        "displayname", "name" -> user.displayName
        "username" -> user.username
        "role" -> user.role.name
        "banned", "isbanned" -> if (user.isBanned) "true" else "false"
        "photourl", "photo" -> user.photoUrl ?: ""
        "permissions" -> user.permissions.joinToString(";") { it.name }
        else -> ""
    }

    // ====================================================================
    // Subcommand: quizzes
    // ====================================================================

    /**
     * Xuat danh sach quiz tu adminRepository.
     *
     * @param context Context lenh hien tai.
     * @param format Dinh dang dau ra (csv/json/table).
     * @param requestedFields Danh sach truong can xuat, hoac null de xuat tat ca.
     * @param limit Gioi han so ban ghi, hoac null de xuat tat ca.
     * @return [CommandResult] chua du lieu quiz da dinh dang.
     */
    private suspend fun exportQuizzes(
        context: CommandContext,
        format: String,
        requestedFields: List<String>?,
        limit: Int?
    ): CommandResult {
        val allQuizzes: List<Quiz> = try {
            context.repositories.adminRepository.getAllQuizzes(includeDeleted = true).first()
        } catch (e: Exception) {
            return CommandResult.error("Khong the tai danh sach quiz: ${e.message}")
        }

        if (allQuizzes.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong co quiz nao de xuat.", OutputStyle.WARNING))
            )
        }

        val quizzes = if (limit != null && limit > 0) allQuizzes.take(limit) else allQuizzes
        val fields = requestedFields ?: QUIZ_DEFAULT_FIELDS
        val validFields = fields.filter { it in QUIZ_ALL_FIELDS }

        if (validFields.isEmpty()) {
            return CommandResult.error(
                "Khong co truong hop le. Cac truong ho tro: ${QUIZ_ALL_FIELDS.joinToString(", ")}"
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Xuat ${quizzes.size}/${allQuizzes.size} quiz (dinh dang: $format)",
                OutputStyle.HEADER
            )
        )

        when (format) {
            "csv" -> buildQuizzesCsv(quizzes, validFields, lines)
            "json" -> buildQuizzesJson(quizzes, validFields, lines)
            else -> buildQuizzesTable(quizzes, validFields, lines)
        }

        lines.add(OutputLine("Tong: ${quizzes.size} ban ghi da xuat.", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Xuat quiz dang CSV.
     */
    private fun buildQuizzesCsv(
        quizzes: List<Quiz>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine(fields.joinToString(","), OutputStyle.TABLE_HEADER))
        for (quiz in quizzes) {
            val values = fields.map { field -> CommandFormatUtils.csvEscape(getQuizFieldValue(quiz, field)) }
            lines.add(OutputLine(values.joinToString(","), OutputStyle.TABLE_ROW))
        }
    }

    /**
     * Xuat quiz dang JSON.
     */
    private fun buildQuizzesJson(
        quizzes: List<Quiz>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine("[", OutputStyle.CODE))
        for ((index, quiz) in quizzes.withIndex()) {
            val comma = if (index < quizzes.size - 1) "," else ""
            val pairs = fields.joinToString(", ") { field ->
                "\"$field\": \"${CommandFormatUtils.escapeJson(getQuizFieldValue(quiz, field))}\""
            }
            lines.add(OutputLine("  { $pairs }$comma", OutputStyle.CODE))
        }
        lines.add(OutputLine("]", OutputStyle.CODE))
    }

    /**
     * Xuat quiz dang bang can chinh cot.
     */
    private fun buildQuizzesTable(
        quizzes: List<Quiz>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        val widths = calculateColumnWidths(fields, quizzes.map { q -> fields.map { getQuizFieldValue(q, it) } })
        val header = fields.mapIndexed { i, f -> CommandFormatUtils.padRight(f.uppercase(), widths[i]) }
            .joinToString(COL_SEPARATOR)
        lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))

        for (quiz in quizzes) {
            val row = fields.mapIndexed { i, f ->
                CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(getQuizFieldValue(quiz, f), widths[i]),
                    widths[i]
                )
            }.joinToString(COL_SEPARATOR)
            val style = when {
                quiz.deletedAt != null -> OutputStyle.ERROR
                quiz.isDraft -> OutputStyle.MUTED
                else -> OutputStyle.TABLE_ROW
            }
            lines.add(OutputLine(row, style))
        }
    }

    /**
     * Lay gia tri cua truong cho quiz.
     */
    private fun getQuizFieldValue(quiz: Quiz, field: String): String = when (field) {
        "id" -> quiz.id
        "ownerid", "owner" -> quiz.ownerId
        "title" -> quiz.title
        "description", "desc" -> quiz.description ?: ""
        "authorname", "author" -> quiz.authorName
        "tags" -> quiz.tags.joinToString(";")
        "questioncount", "questions" -> quiz.questionCount.toString()
        "attemptcount", "attempts" -> quiz.attemptCount.toString()
        "ispublic", "public" -> if (quiz.isPublic) "true" else "false"
        "isdraft", "draft" -> if (quiz.isDraft) "true" else "false"
        "sharecode" -> quiz.shareCode ?: ""
        "checksum" -> quiz.checksum ?: ""
        "createdat", "created" -> formatTimestamp(quiz.createdAt)
        "updatedat", "updated" -> formatTimestamp(quiz.updatedAt)
        "deletedat", "deleted" -> if (quiz.deletedAt != null) formatTimestamp(quiz.deletedAt) else ""
        "status" -> quizStatusLabel(quiz)
        else -> ""
    }

    /**
     * Tra ve nhan trang thai cua quiz.
     */
    private fun quizStatusLabel(quiz: Quiz): String = when {
        quiz.deletedAt != null -> "DA XOA"
        quiz.isDraft -> "NHAP"
        quiz.isPublic -> "CONG KHAI"
        else -> "RIENG TU"
    }

    // ====================================================================
    // Subcommand: attempts
    // ====================================================================

    /**
     * Xuat danh sach luot lam tu adminRepository.
     *
     * @param context Context lenh hien tai.
     * @param format Dinh dang dau ra (csv/json/table).
     * @param requestedFields Danh sach truong can xuat, hoac null de xuat tat ca.
     * @param limit Gioi han so ban ghi, hoac null de xuat tat ca.
     * @return [CommandResult] chua du lieu luot lam da dinh dang.
     */
    private suspend fun exportAttempts(
        context: CommandContext,
        format: String,
        requestedFields: List<String>?,
        limit: Int?
    ): CommandResult {
        val allAttempts: List<Attempt> = try {
            context.repositories.adminRepository.getAllAttempts().first()
        } catch (e: Exception) {
            return CommandResult.error("Khong the tai danh sach luot lam: ${e.message}")
        }

        if (allAttempts.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong co luot lam nao de xuat.", OutputStyle.WARNING))
            )
        }

        val attempts = if (limit != null && limit > 0) allAttempts.take(limit) else allAttempts
        val fields = requestedFields ?: ATTEMPT_DEFAULT_FIELDS
        val validFields = fields.filter { it in ATTEMPT_ALL_FIELDS }

        if (validFields.isEmpty()) {
            return CommandResult.error(
                "Khong co truong hop le. Cac truong ho tro: ${ATTEMPT_ALL_FIELDS.joinToString(", ")}"
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Xuat ${attempts.size}/${allAttempts.size} luot lam (dinh dang: $format)",
                OutputStyle.HEADER
            )
        )

        when (format) {
            "csv" -> buildAttemptsCsv(attempts, validFields, lines)
            "json" -> buildAttemptsJson(attempts, validFields, lines)
            else -> buildAttemptsTable(attempts, validFields, lines)
        }

        lines.add(OutputLine("Tong: ${attempts.size} ban ghi da xuat.", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Xuat luot lam dang CSV.
     */
    private fun buildAttemptsCsv(
        attempts: List<Attempt>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine(fields.joinToString(","), OutputStyle.TABLE_HEADER))
        for (attempt in attempts) {
            val values = fields.map { field -> CommandFormatUtils.csvEscape(getAttemptFieldValue(attempt, field)) }
            lines.add(OutputLine(values.joinToString(","), OutputStyle.TABLE_ROW))
        }
    }

    /**
     * Xuat luot lam dang JSON.
     */
    private fun buildAttemptsJson(
        attempts: List<Attempt>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine("[", OutputStyle.CODE))
        for ((index, attempt) in attempts.withIndex()) {
            val comma = if (index < attempts.size - 1) "," else ""
            val pairs = fields.joinToString(", ") { field ->
                "\"$field\": \"${CommandFormatUtils.escapeJson(getAttemptFieldValue(attempt, field))}\""
            }
            lines.add(OutputLine("  { $pairs }$comma", OutputStyle.CODE))
        }
        lines.add(OutputLine("]", OutputStyle.CODE))
    }

    /**
     * Xuat luot lam dang bang can chinh cot.
     */
    private fun buildAttemptsTable(
        attempts: List<Attempt>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        val widths = calculateColumnWidths(
            fields,
            attempts.map { a -> fields.map { getAttemptFieldValue(a, it) } }
        )
        val header = fields.mapIndexed { i, f -> CommandFormatUtils.padRight(f.uppercase(), widths[i]) }
            .joinToString(COL_SEPARATOR)
        lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))

        for (attempt in attempts) {
            val row = fields.mapIndexed { i, f ->
                CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(getAttemptFieldValue(attempt, f), widths[i]),
                    widths[i]
                )
            }.joinToString(COL_SEPARATOR)
            lines.add(OutputLine(row, OutputStyle.TABLE_ROW))
        }
    }

    /**
     * Lay gia tri cua truong cho luot lam.
     */
    private fun getAttemptFieldValue(attempt: Attempt, field: String): String = when (field) {
        "id" -> attempt.id
        "userid", "user" -> attempt.userId
        "quizid", "quiz" -> attempt.quizId
        "score" -> attempt.score.toString()
        "totalquestions", "total" -> attempt.maxScore.toString()
        "percentage", "pct" -> if (attempt.maxScore > 0) {
            val pct = (attempt.score.toDouble() / attempt.maxScore * 100)
            String.format(java.util.Locale.ROOT, "%.1f%%", pct)
        } else {
            "0.0%"
        }

        "starttime", "start" -> formatTimestamp(attempt.startTimeMillis)
        "endtime", "end" -> if (attempt.endTimeMillis != null) {
            formatTimestamp(attempt.endTimeMillis)
        } else {
            "Chua hoan thanh"
        }

        "duration" -> {
            val endMs = attempt.endTimeMillis
            if (endMs != null) {
                CommandFormatUtils.formatDuration((endMs - attempt.startTimeMillis) / 1000)
            } else {
                "N/A"
            }
        }

        "answercount", "answers" -> attempt.answers.size.toString()
        else -> ""
    }

    // ====================================================================
    // Subcommand: stats
    // ====================================================================

    /**
     * Xuat thong ke he thong tu adminRepository.
     *
     * @param context Context lenh hien tai.
     * @param format Dinh dang dau ra (csv/json/table).
     * @param requestedFields Danh sach truong can xuat, hoac null de xuat tat ca.
     * @return [CommandResult] chua thong ke he thong da dinh dang.
     */
    private suspend fun exportStats(
        context: CommandContext,
        format: String,
        requestedFields: List<String>?
    ): CommandResult {
        val stats: SystemStats = try {
            context.repositories.adminRepository.getSystemStats().first()
        } catch (e: Exception) {
            return CommandResult.error("Khong the tai thong ke he thong: ${e.message}")
        }

        val fields = requestedFields ?: STATS_ALL_FIELDS
        val validFields = fields.filter { it in STATS_ALL_FIELDS }

        if (validFields.isEmpty()) {
            return CommandResult.error(
                "Khong co truong hop le. Cac truong ho tro: ${STATS_ALL_FIELDS.joinToString(", ")}"
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Thong ke he thong (dinh dang: $format)", OutputStyle.HEADER))

        val statsMap = buildStatsMap(stats)
        val filteredStats = validFields.mapNotNull { field ->
            statsMap[field]?.let { field to it }
        }

        when (format) {
            "csv" -> buildStatsCsv(filteredStats, lines)
            "json" -> buildStatsJson(filteredStats, lines)
            else -> buildStatsTable(filteredStats, lines)
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung map truong -> gia tri cho thong ke.
     */
    private fun buildStatsMap(stats: SystemStats): Map<String, String> = mapOf(
        "totalusers" to stats.totalUsers.toString(),
        "totalquizzes" to stats.totalQuizzes.toString(),
        "totalattempts" to stats.totalAttempts.toString(),
        "totalquestionsinpool" to stats.totalQuestionsInPool.toString(),
        "activeusers" to stats.activeUsers.toString(),
        "publicquizzes" to stats.publicQuizzes.toString(),
        "privatequizzes" to stats.privateQuizzes.toString(),
        "avgattemptsperquiz" to String.format(java.util.Locale.ROOT, "%.2f", stats.averageAttemptsPerQuiz)
    )

    /**
     * Xuat thong ke dang CSV.
     */
    private fun buildStatsCsv(
        stats: List<Pair<String, String>>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine("metric,value", OutputStyle.TABLE_HEADER))
        for ((key, value) in stats) {
            lines.add(
                OutputLine(
                    "${CommandFormatUtils.csvEscape(key)},${CommandFormatUtils.csvEscape(value)}",
                    OutputStyle.TABLE_ROW
                )
            )
        }
    }

    /**
     * Xuat thong ke dang JSON.
     */
    private fun buildStatsJson(
        stats: List<Pair<String, String>>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine("{", OutputStyle.CODE))
        for ((index, entry) in stats.withIndex()) {
            val comma = if (index < stats.size - 1) "," else ""
            lines.add(
                OutputLine(
                    "  \"${entry.first}\": \"${CommandFormatUtils.escapeJson(entry.second)}\"$comma",
                    OutputStyle.CODE
                )
            )
        }
        lines.add(OutputLine("}", OutputStyle.CODE))
    }

    /**
     * Xuat thong ke dang bang (key-value).
     */
    private fun buildStatsTable(
        stats: List<Pair<String, String>>,
        lines: MutableList<OutputLine>
    ) {
        val labels = mapOf(
            "totalusers" to "Tong nguoi dung",
            "totalquizzes" to "Tong quiz",
            "totalattempts" to "Tong luot lam",
            "totalquestions" to "Tong cau hoi",
            "activeusers" to "Nguoi dung hoat dong",
            "publicquizzes" to "Quiz cong khai",
            "privatequizzes" to "Quiz rieng tu",
            "avgquestionsperquiz" to "TB cau hoi/quiz"
        )

        val labelWidth = labels.values.maxOfOrNull { it.length }?.coerceAtLeast(COL_LABEL_MIN) ?: COL_LABEL_MIN
        lines.add(
            OutputLine(
                "${CommandFormatUtils.padRight("CHI SO", labelWidth)}$COL_SEPARATOR${"GIA TRI"}",
                OutputStyle.TABLE_HEADER
            )
        )

        for ((key, value) in stats) {
            val label = labels[key] ?: key
            lines.add(
                OutputLine(
                    "${CommandFormatUtils.padRight(label, labelWidth)}$COL_SEPARATOR$value",
                    OutputStyle.TABLE_ROW
                )
            )
        }
    }

    // ====================================================================
    // Subcommand: logs
    // ====================================================================

    /**
     * Xuat bo nho dem nhat ky tu logCollector.
     *
     * Uu tien su dung `logCollector.export()` cho dinh dang table/csv (xuat toan bo).
     * Doi voi JSON hoac khi can loc truong, su dung `logCollector.logs` StateFlow.
     *
     * @param context Context lenh hien tai.
     * @param format Dinh dang dau ra (csv/json/table).
     * @param requestedFields Danh sach truong can xuat, hoac null de xuat tat ca.
     * @param limit Gioi han so ban ghi, hoac null de xuat tat ca.
     * @return [CommandResult] chua du lieu nhat ky da dinh dang.
     */
    private suspend fun exportLogs(
        context: CommandContext,
        format: String,
        requestedFields: List<String>?,
        limit: Int?
    ): CommandResult {
        val logService = context.services.logService

        // Dinh dang table khong co fields tuy chinh va khong co limit: su dung export() nhanh
        if (format == "table" && requestedFields == null && limit == null) {
            val exported = logService.export()
            if (exported.isBlank()) {
                return CommandResult.success(
                    listOf(OutputLine("Khong co ban ghi nhat ky nao de xuat.", OutputStyle.WARNING))
                )
            }
            val lines = mutableListOf<OutputLine>()
            lines.add(OutputLine("Xuat nhat ky ung dung", OutputStyle.HEADER))
            for (logLine in exported.lines()) {
                lines.add(OutputLine(logLine, OutputStyle.CODE))
            }
            lines.add(OutputLine("--- Ket thuc xuat nhat ky ---", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        // Su dung StateFlow de loc/dinh dang tu chinh
        val allLogs = logService.logs.first()
        if (allLogs.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong co ban ghi nhat ky nao de xuat.", OutputStyle.WARNING))
            )
        }

        val logs = if (limit != null && limit > 0) allLogs.takeLast(limit) else allLogs
        val fields = requestedFields ?: LOG_DEFAULT_FIELDS
        val validFields = fields.filter { it in LOG_ALL_FIELDS }

        if (validFields.isEmpty()) {
            return CommandResult.error(
                "Khong co truong hop le. Cac truong ho tro: ${LOG_ALL_FIELDS.joinToString(", ")}"
            )
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Xuat ${logs.size}/${allLogs.size} ban ghi nhat ky (dinh dang: $format)",
                OutputStyle.HEADER
            )
        )

        when (format) {
            "csv" -> buildLogsCsv(logs, validFields, lines)
            "json" -> buildLogsJson(logs, validFields, lines)
            else -> buildLogsTable(logs, validFields, lines)
        }

        lines.add(OutputLine("Tong: ${logs.size} ban ghi da xuat.", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Xuat nhat ky dang CSV.
     */
    private fun buildLogsCsv(
        logs: List<com.example.androidapp.domain.model.LogEntry>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine(fields.joinToString(","), OutputStyle.TABLE_HEADER))
        for (entry in logs) {
            val values = fields.map { field -> CommandFormatUtils.csvEscape(getLogFieldValue(entry, field)) }
            lines.add(OutputLine(values.joinToString(","), OutputStyle.TABLE_ROW))
        }
    }

    /**
     * Xuat nhat ky dang JSON.
     */
    private fun buildLogsJson(
        logs: List<com.example.androidapp.domain.model.LogEntry>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine("[", OutputStyle.CODE))
        for ((index, entry) in logs.withIndex()) {
            val comma = if (index < logs.size - 1) "," else ""
            val pairs = fields.joinToString(", ") { field ->
                "\"$field\": \"${CommandFormatUtils.escapeJson(getLogFieldValue(entry, field))}\""
            }
            lines.add(OutputLine("  { $pairs }$comma", OutputStyle.CODE))
        }
        lines.add(OutputLine("]", OutputStyle.CODE))
    }

    /**
     * Xuat nhat ky dang bang can chinh cot.
     */
    private fun buildLogsTable(
        logs: List<com.example.androidapp.domain.model.LogEntry>,
        fields: List<String>,
        lines: MutableList<OutputLine>
    ) {
        val widths = calculateColumnWidths(
            fields,
            logs.map { e -> fields.map { getLogFieldValue(e, it) } }
        )
        val header = fields.mapIndexed { i, f -> CommandFormatUtils.padRight(f.uppercase(), widths[i]) }
            .joinToString(COL_SEPARATOR)
        lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))

        for (entry in logs) {
            val row = fields.mapIndexed { i, f ->
                CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(getLogFieldValue(entry, f), widths[i]),
                    widths[i]
                )
            }.joinToString(COL_SEPARATOR)
            val style = when (entry.level) {
                com.example.androidapp.domain.model.LogLevel.ERROR,
                com.example.androidapp.domain.model.LogLevel.ASSERT -> OutputStyle.ERROR

                com.example.androidapp.domain.model.LogLevel.WARN -> OutputStyle.WARNING
                com.example.androidapp.domain.model.LogLevel.DEBUG,
                com.example.androidapp.domain.model.LogLevel.VERBOSE -> OutputStyle.MUTED

                else -> OutputStyle.TABLE_ROW
            }
            lines.add(OutputLine(row, style))
        }
    }

    /**
     * Lay gia tri cua truong cho ban ghi nhat ky.
     */
    private fun getLogFieldValue(
        entry: com.example.androidapp.domain.model.LogEntry,
        field: String
    ): String = when (field) {
        "id" -> entry.id.toString()
        "timestamp", "time" -> formatTimestamp(entry.timestamp)
        "level" -> entry.level.name
        "tag" -> entry.tag
        "message", "msg" -> entry.message
        "thread", "threadname" -> entry.threadName
        else -> ""
    }

    // ====================================================================
    // Error builders
    // ====================================================================

    /**
     * Xay dung thong bao loi khi khong co lenh con.
     *
     * @return [CommandResult] loi voi huong dan su dung.
     */
    private fun buildNoSubcommandError(): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Loi: Vui long chi dinh loai du lieu can xuat.", OutputStyle.ERROR))
        lines.add(OutputLine(""))
        lines.add(OutputLine("Cac lenh con ho tro:", OutputStyle.HEADER))
        for ((sub, desc) in SUBCOMMANDS) {
            lines.add(OutputLine("  ${CommandFormatUtils.padRight(sub, 12)}$desc", OutputStyle.INFO))
        }
        lines.add(OutputLine(""))
        lines.add(OutputLine("Vi du:", OutputStyle.HEADER))
        lines.add(OutputLine("  export users --format csv", OutputStyle.MUTED))
        lines.add(OutputLine("  export quizzes --format json --fields id,title", OutputStyle.MUTED))
        lines.add(OutputLine("  export stats", OutputStyle.MUTED))
        return CommandResult(output = lines, isSuccess = false, exitCode = 1)
    }

    // ====================================================================
    // Formatting & utility helpers
    // ====================================================================

    /**
     * Tra ve danh sach truong khong dinh cho lenh con.
     */
    private fun fieldsForSubcommand(subcommand: String): List<String> = when (subcommand) {
        "users" -> USER_ALL_FIELDS
        "quizzes" -> QUIZ_ALL_FIELDS
        "attempts" -> ATTEMPT_ALL_FIELDS
        "stats" -> STATS_ALL_FIELDS
        "logs" -> LOG_ALL_FIELDS
        else -> emptyList()
    }

    /**
     * Tinh toan do rong cot dua tren noi dung du lieu.
     *
     * @param headers Ten cac cot.
     * @param rows Du lieu hang (moi hang la danh sach gia tri tuong ung voi cot).
     * @return Danh sach do rong cho moi cot.
     */
    private fun calculateColumnWidths(
        headers: List<String>,
        rows: List<List<String>>
    ): List<Int> {
        val widths = headers.map { it.length }.toMutableList()
        for (row in rows) {
            for ((i, value) in row.withIndex()) {
                if (i < widths.size) {
                    widths[i] = maxOf(widths[i], value.length.coerceAtMost(MAX_COL_WIDTH))
                }
            }
        }
        return widths.map { it.coerceIn(MIN_COL_WIDTH, MAX_COL_WIDTH) }
    }

    /**
     * Dinh dang timestamp thanh chuoi ngay gio doc duoc.
     */
    private fun formatTimestamp(millis: Long): String {
        if (millis == 0L) return ""
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    // ====================================================================
    // Constants
    // ====================================================================

    companion object {

        /** Cac lenh con ho tro. */
        private val SUBCOMMANDS = listOf(
            "users" to "Xuat danh sach nguoi dung",
            "quizzes" to "Xuat danh sach quiz",
            "attempts" to "Xuat danh sach luot lam",
            "stats" to "Xuat thong ke he thong",
            "logs" to "Xuat nhat ky ung dung"
        )

        /** Cac dinh dang dau ra ho tro. */
        private val SUPPORTED_FORMATS = listOf("csv", "json", "table")

        // -- User fields --

        private val USER_ALL_FIELDS = listOf(
            "id", "email", "displayname", "username", "role",
            "banned", "photourl", "permissions"
        )
        private val USER_DEFAULT_FIELDS = listOf("id", "email", "displayname", "role", "banned")

        // -- Quiz fields --

        private val QUIZ_ALL_FIELDS = listOf(
            "id", "ownerid", "title", "description", "authorname", "tags",
            "questioncount", "attemptcount", "ispublic", "isdraft",
            "sharecode", "checksum", "createdat", "updatedat", "deletedat", "status"
        )
        private val QUIZ_DEFAULT_FIELDS = listOf("id", "title", "authorname", "status", "questioncount", "createdat")

        // -- Attempt fields --

        private val ATTEMPT_ALL_FIELDS = listOf(
            "id", "userid", "quizid", "score", "totalquestions",
            "percentage", "starttime", "endtime", "duration", "answercount"
        )
        private val ATTEMPT_DEFAULT_FIELDS =
            listOf("id", "userid", "quizid", "score", "totalquestions", "percentage", "duration")

        // -- Stats fields --

        private val STATS_ALL_FIELDS = listOf(
            "totalusers", "totalquizzes", "totalattempts", "totalquestions",
            "activeusers", "publicquizzes", "privatequizzes", "avgquestionsperquiz"
        )

        // -- Log fields --

        private val LOG_ALL_FIELDS = listOf(
            "id", "timestamp", "level", "tag", "message", "thread"
        )
        private val LOG_DEFAULT_FIELDS = listOf("timestamp", "level", "tag", "message")

        // -- Table formatting --

        private const val COL_SEPARATOR = "  "
        private const val MIN_COL_WIDTH = 6
        private const val MAX_COL_WIDTH = 40
        private const val COL_LABEL_MIN = 20
    }
}
