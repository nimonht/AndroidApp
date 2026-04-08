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
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh `search` — tim kiem nguoi dung hoac quiz trong he thong.
 *
 * Day la lenh tim kiem da nang danh cho admin console, ho tro tim kiem
 * nguoi dung (flag `-u`) hoac quiz (flag `-q`) thong qua
 * [AdminRepository.searchUsers] va [AdminRepository.searchQuizzes].
 *
 * Quyen truy cap duoc kiem tra ben trong [execute] dua tren loai thuc the:
 * - `-u` (nguoi dung): yeu cau [AdminPermission.MANAGE_USERS]
 * - `-q` (quiz): yeu cau [AdminPermission.MANAGE_QUIZZES]
 *
 * Cac flag ho tro:
 * - `-u <query>`: tim kiem nguoi dung theo email hoac username.
 * - `-q <query>`: tim kiem quiz theo tieu de, mo ta, hoac tac gia.
 * - `--regex`: xu ly query nhu bieu thuc chinh quy (loc cuc bo).
 * - `--tag <tag>`: ket hop voi bo loc tag (chi ap dung cho quiz).
 * - `--role <role>`: loc nguoi dung theo vai tro.
 * - `--exact`: chi doi khop chinh xac.
 * - `--sort <field>`: sap xep ket qua (name/email/title/date/attempts).
 * - `--limit <n>`: gioi han so ket qua (mac dinh 25).
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--fields <f1,f2,...>`: chi hien thi cac truong cu the.
 * - `--output <full|count|ids>`: che do dau ra.
 *
 * Vi du:
 * ```
 * search -u admin
 * search -q "toan hoc" --tag math --limit 10
 * search -u test --regex --role admin
 * ```
 */
class SearchCommand : Command {

    /** @inheritDoc */
    override val name: String = "search"

    /** @inheritDoc */
    override val aliases: List<String> = listOf("find")

    /** @inheritDoc */
    override val description: String = "Tim kiem nguoi dung hoac quiz trong he thong"

    /** @inheritDoc */
    override val usage: String =
        "search <-u|-q> <query> [--regex] [--tag <tag>] [--role <role>] " +
                "[--exact] [--sort <field>] [--limit <n>] [--format <table|json>] " +
                "[--fields <fields>] [--output <full|count|ids>]"

    /** @inheritDoc */
    override val minimumRole: UserRole = UserRole.ADMIN

    /**
     * Quyen duoc kiem tra ben trong [execute] dua tren loai thuc the.
     * Ban than lenh `search` khong yeu cau quyen cu the nao.
     */
    override val requiredPermission: AdminPermission? = null

    /** @inheritDoc */
    override val category: String = "admin"

    /** @inheritDoc */
    override val examples: List<Pair<String, String>> = listOf(
        "search -u admin" to "Tim nguoi dung co ten hoac email chua 'admin'",
        "search -u admin --role admin" to "Tim nguoi dung co vai tro admin",
        "search -u test --exact" to "Tim nguoi dung khop chinh xac 'test'",
        "search -u \"@gmail\" --regex" to "Tim nguoi dung co email Gmail (regex)",
        "search -q toan" to "Tim quiz co tieu de chua 'toan'",
        "search -q \"khoa hoc\" --tag science" to "Tim quiz co tag 'science'",
        "search -q math --sort title --limit 10" to "Tim 10 quiz, sap xep theo tieu de",
        "search -q quiz --format json" to "Tim quiz va xuat ket qua dang JSON",
        "search -q all --output count" to "Dem so quiz tim thay",
        "search -u user --output ids" to "Chi hien thi danh sach ID nguoi dung",
        "search -u user --fields id,email,role" to "Tim nguoi dung, chi hien thi cac truong cu the"
    )

    /** @inheritDoc */
    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()
        val usedFlags = flags.keys

        // Goi y flag loai thuc the neu chua chon
        val hasEntityFlag = "u" in usedFlags || "q" in usedFlags
        if (!hasEntityFlag) {
            suggestions.add(
                CompletionSuggestion(
                    text = "-u",
                    description = "Tim kiem nguoi dung",
                    type = SuggestionType.FLAG
                )
            )
            suggestions.add(
                CompletionSuggestion(
                    text = "-q",
                    description = "Tim kiem quiz",
                    type = SuggestionType.FLAG
                )
            )
        }

        // Cac flag chung
        val commonFlags = mapOf(
            "--regex" to "Xu ly query nhu bieu thuc chinh quy",
            "--exact" to "Chi doi khop chinh xac",
            "--sort" to "Sap xep ket qua theo truong",
            "--limit" to "Gioi han so ket qua",
            "--format" to "Dinh dang dau ra (table/json)",
            "--fields" to "Chi hien thi cac truong cu the",
            "--output" to "Che do dau ra (full/count/ids)"
        )

        for ((flag, desc) in commonFlags) {
            if (flag.removePrefix("--") !in usedFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = flag,
                        description = desc,
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        // Flag rieng cho nguoi dung
        if ("u" in usedFlags) {
            if ("role" !in usedFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = "--role",
                        description = "Loc theo vai tro (guest/user/admin/superuser)",
                        type = SuggestionType.FLAG
                    )
                )
            }
            // Goi y gia tri cho --role
            if ("role" in usedFlags && flags["role"] == null) {
                for (role in UserRole.entries) {
                    suggestions.add(
                        CompletionSuggestion(
                            text = role.name.lowercase(),
                            description = "Vai tro: ${CommandFormatUtils.formatRole(role)}",
                            type = SuggestionType.ARGUMENT
                        )
                    )
                }
            }
        }

        // Flag rieng cho quiz
        if ("q" in usedFlags) {
            if ("tag" !in usedFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = "--tag",
                        description = "Loc theo tag",
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        // Goi y gia tri cho --format
        if ("format" in usedFlags && flags["format"] == null) {
            suggestions.add(
                CompletionSuggestion(text = "table", description = "Dinh dang bang", type = SuggestionType.ARGUMENT)
            )
            suggestions.add(
                CompletionSuggestion(text = "json", description = "Dinh dang JSON", type = SuggestionType.ARGUMENT)
            )
        }

        // Goi y gia tri cho --output
        if ("output" in usedFlags && flags["output"] == null) {
            suggestions.add(
                CompletionSuggestion(text = "full", description = "Hien thi day du", type = SuggestionType.ARGUMENT)
            )
            suggestions.add(
                CompletionSuggestion(text = "count", description = "Chi dem so luong", type = SuggestionType.ARGUMENT)
            )
            suggestions.add(
                CompletionSuggestion(text = "ids", description = "Chi hien thi ID", type = SuggestionType.ARGUMENT)
            )
        }

        // Goi y gia tri cho --sort
        if ("sort" in usedFlags && flags["sort"] == null) {
            if ("u" in usedFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = "name",
                        description = "Sap xep theo ten",
                        type = SuggestionType.ARGUMENT
                    )
                )
                suggestions.add(
                    CompletionSuggestion(
                        text = "email",
                        description = "Sap xep theo email",
                        type = SuggestionType.ARGUMENT
                    )
                )
                suggestions.add(
                    CompletionSuggestion(
                        text = "role",
                        description = "Sap xep theo vai tro",
                        type = SuggestionType.ARGUMENT
                    )
                )
            } else if ("q" in usedFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = "title",
                        description = "Sap xep theo tieu de",
                        type = SuggestionType.ARGUMENT
                    )
                )
                suggestions.add(
                    CompletionSuggestion(
                        text = "date",
                        description = "Sap xep theo ngay tao",
                        type = SuggestionType.ARGUMENT
                    )
                )
                suggestions.add(
                    CompletionSuggestion(
                        text = "attempts",
                        description = "Sap xep theo luot lam",
                        type = SuggestionType.ARGUMENT
                    )
                )
            }
        }

        return suggestions
    }

    /** @inheritDoc */
    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val searchUsers = "u" in flags
        val searchQuizzes = "q" in flags

        if (!searchUsers && !searchQuizzes) {
            return buildNoSearchTypeError()
        }

        if (searchUsers && searchQuizzes) {
            return CommandResult.error(
                "Khong the tim kiem dong thoi nguoi dung va quiz. " +
                        "Vui long chi dinh -u hoac -q, khong dung ca hai."
            )
        }

        return if (searchUsers) {
            executeSearchUsers(args, flags, context)
        } else {
            executeSearchQuizzes(args, flags, context)
        }
    }

    // ====================================================================
    // User search
    // ====================================================================

    /**
     * Thuc hien tim kiem nguoi dung.
     *
     * @param args Tham so vi tri (query).
     * @param flags Cac flag da phan tich.
     * @param context Context lenh hien tai.
     * @return [CommandResult] chua ket qua tim kiem.
     */
    private suspend fun executeSearchUsers(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        // Kiem tra quyen
        if (!context.currentUser.hasPermission(AdminPermission.MANAGE_USERS)) {
            return CommandResult.error(
                "Khong du quyen de tim kiem nguoi dung. " +
                        "Yeu cau quyen: ${CommandFormatUtils.formatPermission(AdminPermission.MANAGE_USERS)}."
            )
        }

        val query = resolveQuery(args, flags["u"])
        if (query.isBlank()) {
            return CommandResult.error("Vui long nhap tu khoa tim kiem. Vi du: search -u admin")
        }

        val useRegex = "regex" in flags
        val exactMatch = "exact" in flags
        val roleFilter = flags["role"]?.uppercase()?.let { roleName ->
            try {
                UserRole.valueOf(roleName)
            } catch (_: IllegalArgumentException) {
                return CommandResult.error(
                    "Vai tro khong hop le: '${flags["role"]}'. " +
                            "Cac vai tro hop le: ${UserRole.entries.joinToString(", ") { it.name.lowercase() }}"
                )
            }
        }
        val limit = parseLimit(flags)
        val format = flags["format"]?.lowercase() ?: "table"
        val outputMode = flags["output"]?.lowercase() ?: "full"
        val sortField = flags["sort"]?.lowercase()
        val requestedFields = flags["fields"]?.split(",")?.map { it.trim().lowercase() }

        // Truy van tu adminRepository
        val adminRepo = context.repositories.adminRepository
        var users = try {
            adminRepo.searchUsers(query).first()
        } catch (e: Exception) {
            return CommandResult.error("Loi khi tim kiem nguoi dung: ${e.message ?: "Loi khong xac dinh"}")
        }

        // Loc regex cuc bo
        if (useRegex) {
            val regex = try {
                Regex(query, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                return CommandResult.error("Bieu thuc chinh quy khong hop le: '$query'")
            }
            users = users.filter { user ->
                regex.containsMatchIn(user.displayName) ||
                        regex.containsMatchIn(user.email) ||
                        regex.containsMatchIn(user.username)
            }
        }

        // Loc exact match
        if (exactMatch) {
            val queryLower = query.lowercase()
            users = users.filter { user ->
                user.displayName.lowercase() == queryLower ||
                        user.email.lowercase() == queryLower ||
                        user.username.lowercase() == queryLower
            }
        }

        // Loc theo role
        if (roleFilter != null) {
            users = users.filter { it.role == roleFilter }
        }

        // Sap xep
        users = sortUsers(users, sortField)

        val total = users.size

        // Ap dung limit
        users = users.take(limit)

        return when (outputMode) {
            "count" -> CommandResult.success(
                listOf(OutputLine("Tim thay: $total nguoi dung", OutputStyle.INFO))
            )

            "ids" -> buildIdsOutput(users.map { it.id }, total, limit)
            else -> when (format) {
                "json" -> buildUsersJson(users, total, limit, query, requestedFields)
                else -> buildUsersTable(users, total, limit, query, requestedFields)
            }
        }
    }

    /**
     * Sap xep danh sach nguoi dung theo truong chi dinh.
     */
    private fun sortUsers(users: List<User>, sortField: String?): List<User> {
        if (sortField == null) return users
        val comparator: Comparator<User> = when (sortField) {
            "name" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
            "email" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.email }
            "role" -> compareBy { it.role.ordinal }
            "username" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.username }
            else -> return users
        }
        return users.sortedWith(comparator)
    }

    /**
     * Xay dung dau ra dang bang cho ket qua tim kiem nguoi dung.
     */
    private fun buildUsersTable(
        users: List<User>,
        total: Int,
        limit: Int,
        query: String,
        requestedFields: List<String>?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("Tim kiem nguoi dung: \"$query\"", OutputStyle.HEADER))
        lines.add(
            OutputLine(
                "Tim thay $total ket qua${if (total > limit) " (hien thi $limit)" else ""}",
                OutputStyle.INFO
            )
        )
        lines.add(OutputLine(""))

        if (users.isEmpty()) {
            lines.add(OutputLine("Khong tim thay nguoi dung nao phu hop.", OutputStyle.WARNING))
            return CommandResult.success(lines)
        }

        val defaultFields = listOf("id", "email", "name", "role", "status")
        val fields = requestedFields ?: defaultFields

        // Tieu de bang
        val header = buildUserHeader(fields)
        lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))

        // Du lieu
        for (user in users) {
            val row = buildUserRow(user, fields)
            val style = if (user.isBanned) OutputStyle.WARNING else OutputStyle.TABLE_ROW
            lines.add(OutputLine(row, style))
        }

        if (total > limit) {
            lines.add(OutputLine(""))
            lines.add(
                OutputLine(
                    "Con ${total - limit} ket qua khac. Dung --limit de xem them.",
                    OutputStyle.MUTED
                )
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dong tieu de bang cho nguoi dung.
     */
    private fun buildUserHeader(fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> CommandFormatUtils.padRight("ID", COL_ID)
                "email" -> CommandFormatUtils.padRight("EMAIL", COL_EMAIL)
                "name" -> CommandFormatUtils.padRight("TEN", COL_NAME)
                "username" -> CommandFormatUtils.padRight("USERNAME", COL_NAME)
                "role" -> CommandFormatUtils.padRight("VAI TRO", COL_ROLE)
                "status" -> CommandFormatUtils.padRight("TRANG THAI", COL_STATUS)
                "permissions" -> CommandFormatUtils.padRight("QUYEN", COL_PERMS)
                else -> CommandFormatUtils.padRight(field.uppercase(), COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dong du lieu cho mot nguoi dung.
     */
    private fun buildUserRow(user: User, fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> CommandFormatUtils.padRight(CommandFormatUtils.truncate(user.id, COL_ID - 2), COL_ID)
                "email" -> CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(user.email, COL_EMAIL - 2),
                    COL_EMAIL
                )

                "name" -> CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(user.displayName, COL_NAME - 2),
                    COL_NAME
                )

                "username" -> CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(user.username, COL_NAME - 2),
                    COL_NAME
                )

                "role" -> CommandFormatUtils.padRight(CommandFormatUtils.formatRole(user.role), COL_ROLE)
                "status" -> CommandFormatUtils.padRight(if (user.isBanned) "Bi cam" else "Hoat dong", COL_STATUS)
                "permissions" -> CommandFormatUtils.padRight(user.permissions.size.toString() + " quyen", COL_PERMS)
                else -> CommandFormatUtils.padRight("-", COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dau ra JSON cho ket qua tim kiem nguoi dung.
     */
    private fun buildUsersJson(
        users: List<User>,
        total: Int,
        limit: Int,
        query: String,
        requestedFields: List<String>?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val defaultFields = listOf("id", "email", "displayName", "username", "role", "isBanned")
        val fields = requestedFields ?: defaultFields

        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"query\": \"${CommandFormatUtils.escapeJson(query)}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"showing\": ${users.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"results\": [", OutputStyle.CODE))

        for ((index, user) in users.withIndex()) {
            val comma = if (index < users.size - 1) "," else ""
            val jsonFields = mutableListOf<String>()

            for (f in fields) {
                when (f.lowercase()) {
                    "id" -> jsonFields.add("\"id\": \"${CommandFormatUtils.escapeJson(user.id)}\"")
                    "email" -> jsonFields.add("\"email\": \"${CommandFormatUtils.escapeJson(user.email)}\"")
                    "displayname", "name" -> jsonFields.add("\"displayName\": \"${CommandFormatUtils.escapeJson(user.displayName)}\"")
                    "username" -> jsonFields.add("\"username\": \"${CommandFormatUtils.escapeJson(user.username)}\"")
                    "role" -> jsonFields.add("\"role\": \"${user.role.name}\"")
                    "isbanned", "banned", "status" -> jsonFields.add("\"isBanned\": ${user.isBanned}")
                    "permissions" -> {
                        val permsStr = user.permissions.joinToString(", ") { "\"${it.name}\"" }
                        jsonFields.add("\"permissions\": [$permsStr]")
                    }

                    else -> jsonFields.add("\"$f\": null")
                }
            }

            lines.add(OutputLine("    {${jsonFields.joinToString(", ")}}$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))

        return CommandResult.success(lines)
    }

    // ====================================================================
    // Quiz search
    // ====================================================================

    /**
     * Thuc hien tim kiem quiz.
     *
     * @param args Tham so vi tri (query).
     * @param flags Cac flag da phan tich.
     * @param context Context lenh hien tai.
     * @return [CommandResult] chua ket qua tim kiem.
     */
    private suspend fun executeSearchQuizzes(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        // Kiem tra quyen
        if (!context.currentUser.hasPermission(AdminPermission.MANAGE_QUIZZES)) {
            return CommandResult.error(
                "Khong du quyen de tim kiem quiz. " +
                        "Yeu cau quyen: ${CommandFormatUtils.formatPermission(AdminPermission.MANAGE_QUIZZES)}."
            )
        }

        val query = resolveQuery(args, flags["q"])
        if (query.isBlank()) {
            return CommandResult.error("Vui long nhap tu khoa tim kiem. Vi du: search -q toan")
        }

        val useRegex = "regex" in flags
        val exactMatch = "exact" in flags
        val tagFilter = flags["tag"]?.lowercase()
        val limit = parseLimit(flags)
        val format = flags["format"]?.lowercase() ?: "table"
        val outputMode = flags["output"]?.lowercase() ?: "full"
        val sortField = flags["sort"]?.lowercase()
        val requestedFields = flags["fields"]?.split(",")?.map { it.trim().lowercase() }

        // Truy van tu adminRepository
        val adminRepo = context.repositories.adminRepository
        var quizzes = try {
            adminRepo.searchQuizzes(query).first()
        } catch (e: Exception) {
            return CommandResult.error("Loi khi tim kiem quiz: ${e.message ?: "Loi khong xac dinh"}")
        }

        // Loc regex cuc bo
        if (useRegex) {
            val regex = try {
                Regex(query, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                return CommandResult.error("Bieu thuc chinh quy khong hop le: '$query'")
            }
            quizzes = quizzes.filter { quiz ->
                regex.containsMatchIn(quiz.title) ||
                        (quiz.description?.let { regex.containsMatchIn(it) } ?: false) ||
                        regex.containsMatchIn(quiz.authorName)
            }
        }

        // Loc exact match
        if (exactMatch) {
            val queryLower = query.lowercase()
            quizzes = quizzes.filter { quiz ->
                quiz.title.lowercase() == queryLower ||
                        quiz.description?.lowercase() == queryLower ||
                        quiz.authorName.lowercase() == queryLower
            }
        }

        // Loc theo tag
        if (tagFilter != null) {
            quizzes = quizzes.filter { quiz ->
                quiz.tags.any { it.lowercase() == tagFilter }
            }
        }

        // Sap xep
        quizzes = sortQuizzes(quizzes, sortField)

        val total = quizzes.size

        // Ap dung limit
        quizzes = quizzes.take(limit)

        return when (outputMode) {
            "count" -> CommandResult.success(
                listOf(OutputLine("Tim thay: $total quiz", OutputStyle.INFO))
            )

            "ids" -> buildIdsOutput(quizzes.map { it.id }, total, limit)
            else -> when (format) {
                "json" -> buildQuizzesJson(quizzes, total, limit, query, requestedFields)
                else -> buildQuizzesTable(quizzes, total, limit, query, requestedFields)
            }
        }
    }

    /**
     * Sap xep danh sach quiz theo truong chi dinh.
     */
    private fun sortQuizzes(quizzes: List<Quiz>, sortField: String?): List<Quiz> {
        if (sortField == null) return quizzes
        val comparator: Comparator<Quiz> = when (sortField) {
            "title" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            "date" -> compareByDescending { it.createdAt }
            "updated" -> compareByDescending { it.updatedAt }
            "attempts" -> compareByDescending { it.attemptCount }
            "questions" -> compareByDescending { it.questionCount }
            "author" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.authorName }
            else -> return quizzes
        }
        return quizzes.sortedWith(comparator)
    }

    /**
     * Xay dung dau ra dang bang cho ket qua tim kiem quiz.
     */
    private fun buildQuizzesTable(
        quizzes: List<Quiz>,
        total: Int,
        limit: Int,
        query: String,
        requestedFields: List<String>?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("Tim kiem quiz: \"$query\"", OutputStyle.HEADER))
        lines.add(
            OutputLine(
                "Tim thay $total ket qua${if (total > limit) " (hien thi $limit)" else ""}",
                OutputStyle.INFO
            )
        )
        lines.add(OutputLine(""))

        if (quizzes.isEmpty()) {
            lines.add(OutputLine("Khong tim thay quiz nao phu hop.", OutputStyle.WARNING))
            return CommandResult.success(lines)
        }

        val defaultFields = listOf("id", "title", "author", "status", "questions", "attempts")
        val fields = requestedFields ?: defaultFields

        // Tieu de bang
        val header = buildQuizHeader(fields)
        lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))

        // Du lieu
        for (quiz in quizzes) {
            val row = buildQuizRow(quiz, fields)
            val style = when {
                quiz.deletedAt != null -> OutputStyle.WARNING
                quiz.isDraft -> OutputStyle.MUTED
                else -> OutputStyle.TABLE_ROW
            }
            lines.add(OutputLine(row, style))
        }

        if (total > limit) {
            lines.add(OutputLine(""))
            lines.add(
                OutputLine(
                    "Con ${total - limit} ket qua khac. Dung --limit de xem them.",
                    OutputStyle.MUTED
                )
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dong tieu de bang cho quiz.
     */
    private fun buildQuizHeader(fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> CommandFormatUtils.padRight("ID", COL_ID)
                "title" -> CommandFormatUtils.padRight("TIEU DE", COL_TITLE)
                "author" -> CommandFormatUtils.padRight("TAC GIA", COL_NAME)
                "owner" -> CommandFormatUtils.padRight("CHU SO HUU", COL_ID)
                "status" -> CommandFormatUtils.padRight("TRANG THAI", COL_STATUS)
                "questions" -> CommandFormatUtils.padRight("CAU HOI", COL_SHORT)
                "attempts" -> CommandFormatUtils.padRight("LUOT LAM", COL_SHORT)
                "tags" -> CommandFormatUtils.padRight("TAGS", COL_TITLE)
                "date" -> CommandFormatUtils.padRight("NGAY TAO", COL_DATE)
                "updated" -> CommandFormatUtils.padRight("CAP NHAT", COL_DATE)
                else -> CommandFormatUtils.padRight(field.uppercase(), COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dong du lieu cho mot quiz.
     */
    private fun buildQuizRow(quiz: Quiz, fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> CommandFormatUtils.padRight(CommandFormatUtils.truncate(quiz.id, COL_ID - 2), COL_ID)
                "title" -> CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(quiz.title, COL_TITLE - 2),
                    COL_TITLE
                )

                "author" -> CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(quiz.authorName, COL_NAME - 2),
                    COL_NAME
                )

                "owner" -> CommandFormatUtils.padRight(CommandFormatUtils.truncate(quiz.ownerId, COL_ID - 2), COL_ID)
                "status" -> CommandFormatUtils.padRight(quizStatusLabel(quiz), COL_STATUS)
                "questions" -> CommandFormatUtils.padRight(quiz.questionCount.toString(), COL_SHORT)
                "attempts" -> CommandFormatUtils.padRight(quiz.attemptCount.toString(), COL_SHORT)
                "tags" -> CommandFormatUtils.padRight(
                    CommandFormatUtils.truncate(
                        quiz.tags.joinToString(", "),
                        COL_TITLE - 2
                    ), COL_TITLE
                )

                "date" -> CommandFormatUtils.padRight(
                    if (quiz.createdAt == 0L) "-" else CommandFormatUtils.formatTimestampShort(
                        quiz.createdAt
                    ), COL_DATE
                )

                "updated" -> CommandFormatUtils.padRight(
                    if (quiz.updatedAt == 0L) "-" else CommandFormatUtils.formatTimestampShort(
                        quiz.updatedAt
                    ), COL_DATE
                )

                "sharecode" -> CommandFormatUtils.padRight(quiz.shareCode ?: "-", COL_DEFAULT)
                else -> CommandFormatUtils.padRight("-", COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dau ra JSON cho ket qua tim kiem quiz.
     */
    private fun buildQuizzesJson(
        quizzes: List<Quiz>,
        total: Int,
        limit: Int,
        query: String,
        requestedFields: List<String>?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val defaultFields =
            listOf("id", "title", "authorName", "isPublic", "isDraft", "questionCount", "attemptCount", "tags")
        val fields = requestedFields ?: defaultFields

        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"query\": \"${CommandFormatUtils.escapeJson(query)}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"showing\": ${quizzes.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"results\": [", OutputStyle.CODE))

        for ((index, quiz) in quizzes.withIndex()) {
            val comma = if (index < quizzes.size - 1) "," else ""
            val jsonFields = mutableListOf<String>()

            for (f in fields) {
                when (f.lowercase()) {
                    "id" -> jsonFields.add("\"id\": \"${CommandFormatUtils.escapeJson(quiz.id)}\"")
                    "title" -> jsonFields.add("\"title\": \"${CommandFormatUtils.escapeJson(quiz.title)}\"")
                    "authorname", "author" -> jsonFields.add("\"authorName\": \"${CommandFormatUtils.escapeJson(quiz.authorName)}\"")
                    "ownerid", "owner" -> jsonFields.add("\"ownerId\": \"${CommandFormatUtils.escapeJson(quiz.ownerId)}\"")
                    "description" -> {
                        val descStr = quiz.description?.let { "\"${CommandFormatUtils.escapeJson(it)}\"" } ?: "null"
                        jsonFields.add("\"description\": $descStr")
                    }

                    "ispublic", "public" -> jsonFields.add("\"isPublic\": ${quiz.isPublic}")
                    "isdraft", "draft" -> jsonFields.add("\"isDraft\": ${quiz.isDraft}")
                    "questioncount", "questions" -> jsonFields.add("\"questionCount\": ${quiz.questionCount}")
                    "attemptcount", "attempts" -> jsonFields.add("\"attemptCount\": ${quiz.attemptCount}")
                    "tags" -> {
                        val tagsStr = quiz.tags.joinToString(", ") { "\"${CommandFormatUtils.escapeJson(it)}\"" }
                        jsonFields.add("\"tags\": [$tagsStr]")
                    }

                    "sharecode" -> {
                        val shareStr = quiz.shareCode?.let { "\"${CommandFormatUtils.escapeJson(it)}\"" } ?: "null"
                        jsonFields.add("\"shareCode\": $shareStr")
                    }

                    "createdat", "date" -> jsonFields.add("\"createdAt\": ${quiz.createdAt}")
                    "updatedat", "updated" -> jsonFields.add("\"updatedAt\": ${quiz.updatedAt}")
                    "status" -> jsonFields.add("\"status\": \"${quizStatusLabel(quiz)}\"")
                    else -> jsonFields.add("\"$f\": null")
                }
            }

            lines.add(OutputLine("    {${jsonFields.joinToString(", ")}}$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))

        return CommandResult.success(lines)
    }

    // ====================================================================
    // Shared helpers
    // ====================================================================

    /**
     * Giai quyet query tu args va gia tri flag.
     *
     * Uu tien args (tham so vi tri), sau do la gia tri cua flag (`-u <value>` hoac `-q <value>`).
     */
    private fun resolveQuery(args: List<String>, flagValue: String?): String {
        // Lay tu args truoc, roi flag value
        val fromArgs = args.joinToString(" ").trim()
        if (fromArgs.isNotBlank()) return fromArgs
        return flagValue?.trim() ?: ""
    }

    /**
     * Phan tich gia tri --limit tu flags.
     */
    private fun parseLimit(flags: Map<String, String?>): Int {
        val raw = flags["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
        return raw.coerceIn(1, MAX_LIMIT)
    }

    /**
     * Xay dung dau ra che do ids (chi hien thi danh sach ID).
     */
    private fun buildIdsOutput(ids: List<String>, total: Int, limit: Int): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Tim thay $total ket qua${if (total > limit) " (hien thi $limit)" else ""}:",
                OutputStyle.INFO
            )
        )

        if (ids.isEmpty()) {
            lines.add(OutputLine("(khong co ket qua)", OutputStyle.MUTED))
        } else {
            for (id in ids) {
                lines.add(OutputLine(id, OutputStyle.CODE))
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung thong bao loi khi khong chi dinh loai tim kiem.
     */
    private fun buildNoSearchTypeError(): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Loi: Vui long chi dinh loai tim kiem (-u hoac -q).",
                OutputStyle.ERROR
            )
        )
        lines.add(OutputLine(""))
        lines.add(OutputLine("Cac loai tim kiem ho tro:", OutputStyle.HEADER))
        lines.add(OutputLine("  -u <query>    Tim kiem nguoi dung (yeu cau quyen MANAGE_USERS)", OutputStyle.INFO))
        lines.add(OutputLine("  -q <query>    Tim kiem quiz (yeu cau quyen MANAGE_QUIZZES)", OutputStyle.INFO))
        lines.add(OutputLine(""))
        lines.add(OutputLine("Cac flag bo sung:", OutputStyle.HEADER))
        lines.add(OutputLine("  --regex          Xu ly query nhu bieu thuc chinh quy", OutputStyle.MUTED))
        lines.add(OutputLine("  --exact          Chi doi khop chinh xac", OutputStyle.MUTED))
        lines.add(OutputLine("  --tag <tag>      Loc quiz theo tag", OutputStyle.MUTED))
        lines.add(OutputLine("  --role <role>    Loc nguoi dung theo vai tro", OutputStyle.MUTED))
        lines.add(OutputLine("  --sort <field>   Sap xep ket qua", OutputStyle.MUTED))
        lines.add(OutputLine("  --limit <n>      Gioi han so ket qua (mac dinh 25)", OutputStyle.MUTED))
        lines.add(OutputLine("  --format <fmt>   Dinh dang: table, json", OutputStyle.MUTED))
        lines.add(OutputLine("  --fields <list>  Chi hien thi truong cu the", OutputStyle.MUTED))
        lines.add(OutputLine("  --output <mode>  Che do: full, count, ids", OutputStyle.MUTED))
        lines.add(OutputLine(""))
        lines.add(OutputLine("Vi du:", OutputStyle.HEADER))
        lines.add(OutputLine("  search -u admin                Tim nguoi dung chua 'admin'", OutputStyle.MUTED))
        lines.add(OutputLine("  search -q toan --tag math      Tim quiz co tag 'math'", OutputStyle.MUTED))
        lines.add(OutputLine("  search -u test --exact --role admin", OutputStyle.MUTED))

        return CommandResult(output = lines, isSuccess = false, exitCode = 1)
    }

    /**
     * Xac dinh nhan trang thai cua quiz.
     */
    private fun quizStatusLabel(quiz: Quiz): String = when {
        quiz.deletedAt != null -> "Da xoa"
        quiz.isDraft -> "Nhap"
        quiz.isPublic -> "Cong khai"
        else -> "Rieng tu"
    }

    companion object {
        /** Gioi han mac dinh so ket qua. */
        const val DEFAULT_LIMIT = 25

        /** Gioi han toi da so ket qua. */
        const val MAX_LIMIT = 500

        // Do rong cot bang
        private const val COL_ID = 24
        private const val COL_EMAIL = 30
        private const val COL_NAME = 20
        private const val COL_TITLE = 30
        private const val COL_ROLE = 16
        private const val COL_STATUS = 14
        private const val COL_SHORT = 10
        private const val COL_DATE = 18
        private const val COL_PERMS = 16
        private const val COL_DEFAULT = 16
    }
}
