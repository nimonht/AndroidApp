package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh `ls` — lenh liet ke tong hop cho admin console.
 *
 * Day la lenh liet ke da nang, ho tro hien thi danh sach nguoi dung, quiz,
 * luot lam quiz (attempt), va cau hoi trong ngan hang (pool item). Loai thuc the
 * duoc xac dinh boi flag `-u`, `-q`, `-a`, hoac `-p`.
 *
 * Quyen truy cap duoc kiem tra ben trong [execute] dua tren loai thuc the:
 * - `-u` (nguoi dung): yeu cau [AdminPermission.MANAGE_USERS]
 * - `-q` (quiz): yeu cau [AdminPermission.MANAGE_QUIZZES]
 * - `-a` (attempt): yeu cau [AdminPermission.MANAGE_QUIZZES]
 * - `-p` (pool item): yeu cau [AdminPermission.MANAGE_QUIZZES]
 *
 * Cac flag chung ho tro cho moi loai thuc the:
 * - `--sort <field>`: sap xep theo truong cu the.
 * - `--asc` / `--desc`: thu tu sap xep tang/giam.
 * - `--limit <n>`: gioi han so ket qua (mac dinh 25).
 * - `--offset <n>`: bo qua n ket qua dau tien.
 * - `--page <n>`: trang (tinh tu 1, tuong duong offset = (page-1)*limit).
 * - `--format <table|json|csv>`: dinh dang dau ra.
 * - `--fields <f1,f2,...>`: chi hien thi cac truong cu the.
 * - `--output <full|count|ids|summary>`: che do dau ra.
 * - `--verbose`: hien thi chi tiet.
 * - `--quiet`: chi hien thi du lieu toi thieu.
 * - `--no-header`: an dong tieu de bang.
 *
 * Cac flag loc rieng theo tung loai thuc the:
 *
 * Nguoi dung (`-u`):
 * - `--role <role>`: loc theo vai tro (guest/user/admin/superuser).
 * - `--banned`: chi nguoi dung bi cam.
 * - `--active`: chi nguoi dung dang hoat dong.
 *
 * Quiz (`-q`):
 * - `--owner <userId>`: loc theo chu so huu.
 * - `--tag <tag>`: loc theo tag.
 * - `--draft` / `--public` / `--private`: loc theo trang thai.
 * - `--deleted`: bao gom quiz da xoa mem.
 *
 * Attempt (`-a`):
 * - `--user <userId>`: loc theo nguoi lam.
 * - `--quiz <quizId>`: loc theo quiz.
 * - `--incomplete`: chi luot lam chua hoan thanh.
 * - `--score-below <n>` / `--score-above <n>`: loc theo diem.
 *
 * Pool item (`-p`):
 * - `--contributor <userId>`: loc theo nguoi dong gop.
 * - `--tag <tag>`: loc theo tag.
 * - `--inactive`: chi cau hoi da vo hieu hoa.
 * - `--active-only`: chi cau hoi dang hoat dong.
 */
class LsCommand : Command {

    override val name: String = "ls"

    override val aliases: List<String> = listOf("list")

    override val description: String = "Liet ke nguoi dung, quiz, luot lam, hoac pool item"

    override val usage: String =
        "ls <-u|-q|-a|-p> [bo loc...] [--sort <field>] [--asc|--desc] [--limit <n>] " +
            "[--offset <n>] [--page <n>] [--format <table|json|csv>] [--fields <fields>] " +
            "[--output <full|count|ids|summary>] [--verbose] [--quiet] [--no-header]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    /**
     * Quyen duoc kiem tra ben trong [execute] dua tren loai thuc the.
     * Ban than lenh `ls` khong yeu cau quyen cu the nao.
     */
    override val requiredPermission: AdminPermission? = null

    override val examples: List<Pair<String, String>> = listOf(
        "ls -u" to "Liet ke nguoi dung (mac dinh 25 ket qua)",
        "ls -u --role admin" to "Liet ke cac quan tri vien",
        "ls -u --banned --format json" to "Liet ke nguoi dung bi cam, xuat JSON",
        "ls -q" to "Liet ke quiz (mac dinh 25 ket qua)",
        "ls -q --draft --owner userId1" to "Liet ke quiz nhap cua mot nguoi dung",
        "ls -q --tag math --sort title --asc" to "Liet ke quiz co tag 'math' sap xep theo tieu de",
        "ls -q --deleted --limit 50" to "Liet ke 50 quiz da xoa",
        "ls -q --output count" to "Dem tong so quiz",
        "ls -a --user userId1 --limit 10" to "Liet ke 10 luot lam cua mot nguoi dung",
        "ls -a --incomplete" to "Liet ke luot lam chua hoan thanh",
        "ls -p --tag science" to "Liet ke pool item co tag 'science'",
        "ls -p --inactive --output ids" to "Liet ke ID cac pool item da vo hieu",
        "ls -u --page 2 --limit 10" to "Liet ke trang 2, moi trang 10 ket qua",
        "ls -q --fields id,title,owner,status --no-header" to "Liet ke quiz voi cac truong cu the"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()
        val usedFlags = flags.keys.map { "--$it" }.toSet()

        // Flag loai thuc the
        val entityFlags = listOf(
            "-u" to "Liet ke nguoi dung",
            "-q" to "Liet ke quiz",
            "-a" to "Liet ke luot lam quiz",
            "-p" to "Liet ke pool item"
        )

        val hasEntity = "u" in flags || "q" in flags || "a" in flags || "p" in flags

        if (!hasEntity) {
            for ((flag, desc) in entityFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = flag,
                        description = desc,
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        // Flag chung
        val commonFlags = listOf(
            "--sort" to "Sap xep theo truong",
            "--asc" to "Sap xep tang dan",
            "--desc" to "Sap xep giam dan",
            "--limit" to "Gioi han so ket qua (mac dinh 25)",
            "--offset" to "Bo qua n ket qua dau",
            "--page" to "So trang (bat dau tu 1)",
            "--format" to "Dinh dang dau ra (table/json/csv)",
            "--fields" to "Chi hien thi cac truong cu the",
            "--output" to "Che do dau ra (full/count/ids/summary)",
            "--verbose" to "Hien thi chi tiet",
            "--quiet" to "Chi hien thi du lieu toi thieu",
            "--no-header" to "An dong tieu de bang"
        )

        for ((flag, desc) in commonFlags) {
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

        // Flag loc theo loai thuc the
        if ("u" in flags) {
            val userFlags = listOf(
                "--role" to "Loc theo vai tro (guest/user/admin/superuser)",
                "--banned" to "Chi nguoi dung bi cam",
                "--active" to "Chi nguoi dung hoat dong"
            )
            for ((flag, desc) in userFlags) {
                if (flag !in usedFlags) {
                    suggestions.add(CompletionSuggestion(text = flag, description = desc, type = SuggestionType.FLAG))
                }
            }
        }

        if ("q" in flags) {
            val quizFlags = listOf(
                "--owner" to "Loc theo chu so huu (userId)",
                "--tag" to "Loc theo tag",
                "--draft" to "Chi quiz nhap",
                "--public" to "Chi quiz cong khai",
                "--private" to "Chi quiz rieng tu",
                "--deleted" to "Bao gom quiz da xoa"
            )
            for ((flag, desc) in quizFlags) {
                if (flag !in usedFlags) {
                    suggestions.add(CompletionSuggestion(text = flag, description = desc, type = SuggestionType.FLAG))
                }
            }
        }

        if ("a" in flags) {
            val attemptFlags = listOf(
                "--user" to "Loc theo nguoi lam (userId)",
                "--quiz" to "Loc theo quiz (quizId)",
                "--incomplete" to "Chi luot lam chua hoan thanh",
                "--score-below" to "Diem thap hon gia tri",
                "--score-above" to "Diem cao hon gia tri"
            )
            for ((flag, desc) in attemptFlags) {
                if (flag !in usedFlags) {
                    suggestions.add(CompletionSuggestion(text = flag, description = desc, type = SuggestionType.FLAG))
                }
            }
        }

        if ("p" in flags) {
            val poolFlags = listOf(
                "--contributor" to "Loc theo nguoi dong gop (userId)",
                "--tag" to "Loc theo tag",
                "--inactive" to "Chi cau hoi da vo hieu hoa",
                "--active-only" to "Chi cau hoi dang hoat dong"
            )
            for ((flag, desc) in poolFlags) {
                if (flag !in usedFlags) {
                    suggestions.add(CompletionSuggestion(text = flag, description = desc, type = SuggestionType.FLAG))
                }
            }
        }

        // Goi y gia tri cho cac flag can gia tri
        if ("format" in flags && flags["format"] == null) {
            suggestions.clear()
            suggestions.add(CompletionSuggestion(text = "table", description = "Dinh dang bang", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "json", description = "Dinh dang JSON", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "csv", description = "Dinh dang CSV", type = SuggestionType.ARGUMENT))
        }

        if ("output" in flags && flags["output"] == null) {
            suggestions.clear()
            suggestions.add(CompletionSuggestion(text = "full", description = "Hien thi day du", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "count", description = "Chi dem so luong", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "ids", description = "Chi hien thi ID", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "summary", description = "Hien thi tom tat", type = SuggestionType.ARGUMENT))
        }

        if ("role" in flags && flags["role"] == null) {
            suggestions.clear()
            suggestions.add(CompletionSuggestion(text = "guest", description = "Khach", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "user", description = "Nguoi dung", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "admin", description = "Quan tri vien", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "superuser", description = "Sieu quan tri", type = SuggestionType.ARGUMENT))
        }

        return suggestions
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val entityType = resolveEntityType(flags)
            ?: return buildNoEntityTypeError()

        // Kiem tra quyen truy cap
        val permissionError = checkPermission(entityType, context)
        if (permissionError != null) {
            return permissionError
        }

        return when (entityType) {
            EntityType.USER -> executeListUsers(flags, context)
            EntityType.QUIZ -> executeListQuizzes(flags, context)
            EntityType.ATTEMPT -> executeListAttempts(flags, context)
            EntityType.POOL -> executeListPoolItems(flags, context)
        }
    }

    // ====================================================================
    // Shared helpers
    // ====================================================================

    /**
     * Phan tich cac tham so phan trang va dinh dang chung.
     */
    private fun parseCommonFlags(flags: Map<String, String?>): CommonParams {
        val limit = flags["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
        val page = flags["page"]?.toIntOrNull()
        val offsetRaw = flags["offset"]?.toIntOrNull() ?: 0
        val offset = if (page != null && page > 0) (page - 1) * limit else offsetRaw
        val format = flags["format"]?.lowercase() ?: "table"
        val outputMode = flags["output"]?.lowercase() ?: "full"
        val sortField = flags["sort"]?.lowercase()
        val sortAsc = "asc" in flags || "desc" !in flags
        val verbose = "verbose" in flags || "v" in flags
        val quiet = "quiet" in flags
        val noHeader = "no-header" in flags
        val fieldsStr = flags["fields"]
        val requestedFields = fieldsStr?.split(",")?.map { it.trim().lowercase() }

        return CommonParams(
            limit = limit.coerceIn(1, MAX_LIMIT),
            offset = offset.coerceAtLeast(0),
            format = format,
            outputMode = outputMode,
            sortField = sortField,
            sortAsc = sortAsc,
            verbose = verbose,
            quiet = quiet,
            noHeader = noHeader,
            requestedFields = requestedFields
        )
    }

    // ====================================================================
    // Users
    // ====================================================================

    /**
     * Liet ke nguoi dung voi bo loc va phan trang.
     */
    private suspend fun executeListUsers(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val params = parseCommonFlags(flags)
        val roleFilter = flags["role"]?.let { UserRole.fromString(it) }
        val bannedOnly = "banned" in flags
        val activeOnly = "active" in flags

        val adminRepo = context.repositories.adminRepository
        var users = adminRepo.getAllUsers().first()

        // Ap dung bo loc
        if (roleFilter != null) {
            users = users.filter { it.role == roleFilter }
        }
        if (bannedOnly) {
            users = users.filter { it.isBanned }
        }
        if (activeOnly) {
            users = users.filter { !it.isBanned }
        }

        val total = users.size

        // Sap xep
        users = sortUsers(users, params.sortField, params.sortAsc)

        // Phan trang
        users = users.drop(params.offset).take(params.limit)

        return when (params.outputMode) {
            "count" -> CommandResult.success("Tong so nguoi dung: $total")
            "ids" -> buildIdsOutput(users.map { it.id }, total, params)
            "summary" -> buildUserSummary(users, total, params)
            else -> buildUserFullOutput(users, total, params)
        }
    }

    /**
     * Sap xep danh sach nguoi dung theo truong chi dinh.
     */
    private fun sortUsers(users: List<User>, field: String?, asc: Boolean): List<User> {
        val comparator: Comparator<User> = when (field) {
            "email" -> compareBy { it.email.lowercase() }
            "name", "displayname" -> compareBy { it.displayName.lowercase() }
            "username" -> compareBy { it.username.lowercase() }
            "role" -> compareBy { it.role.ordinal }
            "id" -> compareBy { it.id }
            else -> compareBy { it.email.lowercase() }
        }
        return if (asc) users.sortedWith(comparator) else users.sortedWith(comparator.reversed())
    }

    /**
     * Xay dung dau ra tom tat cho nguoi dung.
     */
    private fun buildUserSummary(
        users: List<User>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("== Tom tat nguoi dung ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so          : $total", OutputStyle.NORMAL))

        val byRole = users.groupBy { it.role }
        for (role in UserRole.entries) {
            val count = byRole[role]?.size ?: 0
            if (count > 0) {
                lines.add(OutputLine("  ${padRight(formatRole(role), 17)}: $count", OutputStyle.NORMAL))
            }
        }

        val bannedCount = users.count { it.isBanned }
        if (bannedCount > 0) {
            lines.add(OutputLine("  Bi cam           : $bannedCount", OutputStyle.WARNING))
        }

        lines.add(OutputLine("  Hien thi         : ${users.size} / $total", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra day du cho danh sach nguoi dung.
     */
    private fun buildUserFullOutput(
        users: List<User>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        if (params.format == "json") {
            return buildUsersJson(users, total, params)
        }

        if (params.format == "csv") {
            return buildUsersCsv(users, params)
        }

        val lines = mutableListOf<OutputLine>()

        if (!params.quiet) {
            lines.add(OutputLine("== Danh sach nguoi dung ($total) ==", OutputStyle.HEADER))
            lines.add(OutputLine(""))
        }

        if (users.isEmpty()) {
            lines.add(OutputLine("  (Khong co ket qua nao)", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        val defaultFields = listOf("email", "name", "role", "status")
        val fields = params.requestedFields ?: defaultFields

        if (!params.noHeader) {
            val header = buildUserHeader(fields)
            lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))
        }

        for (user in users) {
            val row = buildUserRow(user, fields)
            val style = when {
                user.isBanned -> OutputStyle.ERROR
                user.role == UserRole.SUPERUSER -> OutputStyle.INFO
                user.role == UserRole.ADMIN -> OutputStyle.INFO
                else -> OutputStyle.TABLE_ROW
            }
            lines.add(OutputLine(row, style))

            if (params.verbose) {
                lines.add(OutputLine("  ID: ${user.id}", OutputStyle.MUTED))
                if (user.username.isNotBlank()) {
                    lines.add(OutputLine("  Username: @${user.username}", OutputStyle.MUTED))
                }
                if (user.isAdmin()) {
                    val perms = user.effectivePermissions().joinToString(", ") { it.name }
                    lines.add(OutputLine("  Quyen: $perms", OutputStyle.MUTED))
                }
            }
        }

        if (!params.quiet) {
            lines.add(OutputLine(""))
            lines.add(
                OutputLine(
                    "Hien thi ${users.size} / $total ket qua" +
                        paginationHint(params.offset, params.limit, total),
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
                "id" -> padRight("ID", COL_ID)
                "email" -> padRight("Email", COL_EMAIL)
                "name", "displayname" -> padRight("Ten", COL_NAME)
                "username" -> padRight("Username", COL_NAME)
                "role" -> padRight("Vai tro", COL_ROLE)
                "status" -> padRight("Trang thai", COL_STATUS)
                "permissions", "perms" -> padRight("So quyen", COL_SHORT)
                else -> padRight(field, COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dong du lieu bang cho mot nguoi dung.
     */
    private fun buildUserRow(user: User, fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> padRight(user.id, COL_ID)
                "email" -> padRight(truncate(user.email, COL_EMAIL - 2), COL_EMAIL)
                "name", "displayname" -> padRight(truncate(user.displayName, COL_NAME - 2), COL_NAME)
                "username" -> padRight(if (user.username.isNotBlank()) "@${user.username}" else "-", COL_NAME)
                "role" -> padRight(formatRole(user.role), COL_ROLE)
                "status" -> padRight(if (user.isBanned) "Bi cam" else "Hoat dong", COL_STATUS)
                "permissions", "perms" -> padRight(user.effectivePermissions().size.toString(), COL_SHORT)
                else -> padRight("-", COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dau ra nguoi dung dang JSON.
     */
    private fun buildUsersJson(
        users: List<User>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"type\": \"users\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"offset\": ${params.offset},", OutputStyle.CODE))
        lines.add(OutputLine("  \"limit\": ${params.limit},", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${users.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"items\": [", OutputStyle.CODE))

        for ((index, user) in users.withIndex()) {
            val comma = if (index < users.size - 1) "," else ""
            val permsStr = user.effectivePermissions().joinToString(", ") { "\"${it.name}\"" }
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${escapeJson(user.id)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"email\": \"${escapeJson(user.email)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"displayName\": \"${escapeJson(user.displayName)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"username\": \"${escapeJson(user.username)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"role\": \"${user.role.name}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"isBanned\": ${user.isBanned},", OutputStyle.CODE))
            lines.add(OutputLine("      \"permissions\": [$permsStr]", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra nguoi dung dang CSV.
     */
    private fun buildUsersCsv(users: List<User>, params: CommonParams): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val fields = params.requestedFields ?: listOf("id", "email", "name", "role", "status")

        if (!params.noHeader) {
            lines.add(OutputLine(fields.joinToString(","), OutputStyle.TABLE_HEADER))
        }

        for (user in users) {
            val values = fields.map { field ->
                when (field) {
                    "id" -> csvEscape(user.id)
                    "email" -> csvEscape(user.email)
                    "name", "displayname" -> csvEscape(user.displayName)
                    "username" -> csvEscape(user.username)
                    "role" -> user.role.name
                    "status" -> if (user.isBanned) "banned" else "active"
                    "permissions", "perms" -> csvEscape(user.effectivePermissions().joinToString(";") { it.name })
                    else -> ""
                }
            }
            lines.add(OutputLine(values.joinToString(","), OutputStyle.TABLE_ROW))
        }

        return CommandResult.success(lines)
    }

    // ====================================================================
    // Quizzes
    // ====================================================================

    /**
     * Liet ke quiz voi bo loc va phan trang.
     */
    private suspend fun executeListQuizzes(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val params = parseCommonFlags(flags)
        val ownerFilter = flags["owner"]
        val tagFilter = flags["tag"]
        val draftOnly = "draft" in flags
        val publicOnly = "public" in flags
        val privateOnly = "private" in flags
        val includeDeleted = "deleted" in flags

        val adminRepo = context.repositories.adminRepository
        var quizzes = adminRepo.getAllQuizzes(includeDeleted = includeDeleted).first()

        // Ap dung bo loc
        if (ownerFilter != null) {
            quizzes = quizzes.filter { it.ownerId == ownerFilter }
        }
        if (tagFilter != null) {
            val tagLower = tagFilter.lowercase()
            quizzes = quizzes.filter { quiz -> quiz.tags.any { it.lowercase() == tagLower } }
        }
        if (draftOnly) {
            quizzes = quizzes.filter { it.isDraft }
        }
        if (publicOnly) {
            quizzes = quizzes.filter { it.isPublic && !it.isDraft }
        }
        if (privateOnly) {
            quizzes = quizzes.filter { !it.isPublic && !it.isDraft && it.deletedAt == null }
        }

        val total = quizzes.size

        // Sap xep
        quizzes = sortQuizzes(quizzes, params.sortField, params.sortAsc)

        // Phan trang
        quizzes = quizzes.drop(params.offset).take(params.limit)

        return when (params.outputMode) {
            "count" -> CommandResult.success("Tong so quiz: $total")
            "ids" -> buildIdsOutput(quizzes.map { it.id }, total, params)
            "summary" -> buildQuizSummary(quizzes, total, params)
            else -> buildQuizFullOutput(quizzes, total, params)
        }
    }

    /**
     * Sap xep danh sach quiz theo truong chi dinh.
     */
    private fun sortQuizzes(quizzes: List<Quiz>, field: String?, asc: Boolean): List<Quiz> {
        val comparator: Comparator<Quiz> = when (field) {
            "title" -> compareBy { it.title.lowercase() }
            "owner" -> compareBy { it.ownerId }
            "created", "createdat" -> compareBy { it.createdAt }
            "updated", "updatedat" -> compareBy { it.updatedAt }
            "attempts", "attemptcount" -> compareBy { it.attemptCount }
            "questions", "questioncount" -> compareBy { it.questionCount }
            "id" -> compareBy { it.id }
            else -> compareBy<Quiz> { it.updatedAt }.reversed()
        }
        return if (asc) quizzes.sortedWith(comparator) else quizzes.sortedWith(comparator.reversed())
    }

    /**
     * Xay dung dau ra tom tat cho quiz.
     */
    private fun buildQuizSummary(
        quizzes: List<Quiz>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("== Tom tat quiz ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so          : $total", OutputStyle.NORMAL))

        val publicCount = quizzes.count { it.isPublic && !it.isDraft && it.deletedAt == null }
        val privateCount = quizzes.count { !it.isPublic && !it.isDraft && it.deletedAt == null }
        val draftCount = quizzes.count { it.isDraft && it.deletedAt == null }
        val deletedCount = quizzes.count { it.deletedAt != null }
        val totalAttempts = quizzes.sumOf { it.attemptCount }
        val totalQuestions = quizzes.sumOf { it.questionCount }
        val uniqueOwners = quizzes.map { it.ownerId }.distinct().size

        lines.add(OutputLine("  Cong khai        : $publicCount", OutputStyle.SUCCESS))
        lines.add(OutputLine("  Rieng tu         : $privateCount", OutputStyle.NORMAL))
        lines.add(OutputLine("  Nhap             : $draftCount", OutputStyle.WARNING))
        if (deletedCount > 0) {
            lines.add(OutputLine("  Da xoa           : $deletedCount", OutputStyle.ERROR))
        }
        lines.add(OutputLine("  Tong luot lam    : $totalAttempts", OutputStyle.NORMAL))
        lines.add(OutputLine("  Tong cau hoi     : $totalQuestions", OutputStyle.NORMAL))
        lines.add(OutputLine("  Chu so huu       : $uniqueOwners", OutputStyle.NORMAL))

        if (quizzes.isNotEmpty()) {
            val avgQuestions = totalQuestions.toDouble() / quizzes.size
            lines.add(OutputLine("  TB cau hoi/quiz  : ${"%.1f".format(avgQuestions)}", OutputStyle.MUTED))
        }

        lines.add(OutputLine("  Hien thi         : ${quizzes.size} / $total", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra day du cho danh sach quiz.
     */
    private fun buildQuizFullOutput(
        quizzes: List<Quiz>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        if (params.format == "json") {
            return buildQuizzesJson(quizzes, total, params)
        }

        if (params.format == "csv") {
            return buildQuizzesCsv(quizzes, params)
        }

        val lines = mutableListOf<OutputLine>()

        if (!params.quiet) {
            lines.add(OutputLine("== Danh sach quiz ($total) ==", OutputStyle.HEADER))
            lines.add(OutputLine(""))
        }

        if (quizzes.isEmpty()) {
            lines.add(OutputLine("  (Khong co ket qua nao)", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        val defaultFields = listOf("id", "title", "status", "attempts", "questions")
        val fields = params.requestedFields ?: defaultFields

        if (!params.noHeader) {
            val header = buildQuizHeader(fields)
            lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))
        }

        for (quiz in quizzes) {
            val row = buildQuizRow(quiz, fields)
            val style = when {
                quiz.deletedAt != null -> OutputStyle.ERROR
                quiz.isDraft -> OutputStyle.WARNING
                else -> OutputStyle.TABLE_ROW
            }
            lines.add(OutputLine(row, style))

            if (params.verbose) {
                lines.add(OutputLine("  Chu so huu: ${quiz.ownerId}", OutputStyle.MUTED))
                lines.add(OutputLine("  Tac gia: ${quiz.authorName.ifBlank { "(khong ro)" }}", OutputStyle.MUTED))
                if (quiz.tags.isNotEmpty()) {
                    lines.add(OutputLine("  Tags: ${quiz.tags.joinToString(", ")}", OutputStyle.MUTED))
                }
                if (quiz.shareCode != null) {
                    lines.add(OutputLine("  Share code: ${quiz.shareCode}", OutputStyle.MUTED))
                }
                lines.add(
                    OutputLine(
                        "  Tao: ${formatTimestamp(quiz.createdAt)} | Cap nhat: ${formatTimestamp(quiz.updatedAt)}",
                        OutputStyle.MUTED
                    )
                )
            }
        }

        if (!params.quiet) {
            lines.add(OutputLine(""))
            lines.add(
                OutputLine(
                    "Hien thi ${quizzes.size} / $total ket qua" +
                        paginationHint(params.offset, params.limit, total),
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
                "id" -> padRight("ID", COL_ID)
                "title" -> padRight("Tieu de", COL_TITLE)
                "owner" -> padRight("Chu so huu", COL_NAME)
                "author" -> padRight("Tac gia", COL_NAME)
                "status" -> padRight("Trang thai", COL_STATUS)
                "attempts", "attemptcount" -> padRight("Luot lam", COL_SHORT)
                "questions", "questioncount" -> padRight("Cau hoi", COL_SHORT)
                "tags" -> padRight("Tags", COL_TITLE)
                "created" -> padRight("Ngay tao", COL_DATE)
                "updated" -> padRight("Cap nhat", COL_DATE)
                else -> padRight(field, COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dong du lieu bang cho mot quiz.
     */
    private fun buildQuizRow(quiz: Quiz, fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> padRight(quiz.id, COL_ID)
                "title" -> padRight(truncate(quiz.title, COL_TITLE - 2), COL_TITLE)
                "owner" -> padRight(truncate(quiz.ownerId, COL_NAME - 2), COL_NAME)
                "author" -> padRight(truncate(quiz.authorName.ifBlank { "-" }, COL_NAME - 2), COL_NAME)
                "status" -> padRight(quizStatusLabel(quiz), COL_STATUS)
                "attempts", "attemptcount" -> padRight(quiz.attemptCount.toString(), COL_SHORT)
                "questions", "questioncount" -> padRight(quiz.questionCount.toString(), COL_SHORT)
                "tags" -> padRight(truncate(quiz.tags.joinToString(","), COL_TITLE - 2), COL_TITLE)
                "created" -> padRight(formatTimestamp(quiz.createdAt), COL_DATE)
                "updated" -> padRight(formatTimestamp(quiz.updatedAt), COL_DATE)
                else -> padRight("-", COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dau ra quiz dang JSON.
     */
    private fun buildQuizzesJson(
        quizzes: List<Quiz>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"type\": \"quizzes\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"offset\": ${params.offset},", OutputStyle.CODE))
        lines.add(OutputLine("  \"limit\": ${params.limit},", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${quizzes.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"items\": [", OutputStyle.CODE))

        for ((index, quiz) in quizzes.withIndex()) {
            val comma = if (index < quizzes.size - 1) "," else ""
            val tagsStr = quiz.tags.joinToString(", ") { "\"${escapeJson(it)}\"" }
            val deletedAtStr = quiz.deletedAt?.toString() ?: "null"
            val descStr = if (quiz.description != null) "\"${escapeJson(quiz.description)}\"" else "null"
            val shareCodeStr = if (quiz.shareCode != null) "\"${escapeJson(quiz.shareCode)}\"" else "null"
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${escapeJson(quiz.id)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"title\": \"${escapeJson(quiz.title)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"description\": $descStr,", OutputStyle.CODE))
            lines.add(OutputLine("      \"ownerId\": \"${escapeJson(quiz.ownerId)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"authorName\": \"${escapeJson(quiz.authorName)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"status\": \"${quizStatusLabel(quiz)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"isPublic\": ${quiz.isPublic},", OutputStyle.CODE))
            lines.add(OutputLine("      \"isDraft\": ${quiz.isDraft},", OutputStyle.CODE))
            lines.add(OutputLine("      \"questionCount\": ${quiz.questionCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"attemptCount\": ${quiz.attemptCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"tags\": [$tagsStr],", OutputStyle.CODE))
            lines.add(OutputLine("      \"shareCode\": $shareCodeStr,", OutputStyle.CODE))
            lines.add(OutputLine("      \"createdAt\": ${quiz.createdAt},", OutputStyle.CODE))
            lines.add(OutputLine("      \"updatedAt\": ${quiz.updatedAt},", OutputStyle.CODE))
            lines.add(OutputLine("      \"deletedAt\": $deletedAtStr", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra quiz dang CSV.
     */
    private fun buildQuizzesCsv(quizzes: List<Quiz>, params: CommonParams): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val fields = params.requestedFields ?: listOf("id", "title", "owner", "status", "attempts", "questions")

        if (!params.noHeader) {
            lines.add(OutputLine(fields.joinToString(","), OutputStyle.TABLE_HEADER))
        }

        for (quiz in quizzes) {
            val values = fields.map { field ->
                when (field) {
                    "id" -> csvEscape(quiz.id)
                    "title" -> csvEscape(quiz.title)
                    "owner" -> csvEscape(quiz.ownerId)
                    "author" -> csvEscape(quiz.authorName)
                    "status" -> quizStatusLabel(quiz)
                    "attempts", "attemptcount" -> quiz.attemptCount.toString()
                    "questions", "questioncount" -> quiz.questionCount.toString()
                    "tags" -> csvEscape(quiz.tags.joinToString(";"))
                    "created" -> quiz.createdAt.toString()
                    "updated" -> quiz.updatedAt.toString()
                    else -> ""
                }
            }
            lines.add(OutputLine(values.joinToString(","), OutputStyle.TABLE_ROW))
        }

        return CommandResult.success(lines)
    }

    // ====================================================================
    // Attempts
    // ====================================================================

    /**
     * Liet ke luot lam quiz voi bo loc va phan trang.
     */
    private suspend fun executeListAttempts(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val params = parseCommonFlags(flags)
        val userFilter = flags["user"]
        val quizFilter = flags["quiz"]
        val incompleteOnly = "incomplete" in flags
        val scoreBelowFilter = flags["score-below"]?.toIntOrNull()
        val scoreAboveFilter = flags["score-above"]?.toIntOrNull()

        val adminRepo = context.repositories.adminRepository
        var attempts = adminRepo.getAllAttempts().first()

        // Ap dung bo loc
        if (userFilter != null) {
            attempts = attempts.filter { it.userId == userFilter }
        }
        if (quizFilter != null) {
            attempts = attempts.filter { it.quizId == quizFilter }
        }
        if (incompleteOnly) {
            attempts = attempts.filter { it.endTimeMillis == null }
        }
        if (scoreBelowFilter != null) {
            attempts = attempts.filter { it.score < scoreBelowFilter }
        }
        if (scoreAboveFilter != null) {
            attempts = attempts.filter { it.score > scoreAboveFilter }
        }

        val total = attempts.size

        // Sap xep
        attempts = sortAttempts(attempts, params.sortField, params.sortAsc)

        // Phan trang
        attempts = attempts.drop(params.offset).take(params.limit)

        return when (params.outputMode) {
            "count" -> CommandResult.success("Tong so luot lam: $total")
            "ids" -> buildIdsOutput(attempts.map { it.id }, total, params)
            "summary" -> buildAttemptSummary(attempts, total, params)
            else -> buildAttemptFullOutput(attempts, total, params)
        }
    }

    /**
     * Sap xep danh sach attempt theo truong chi dinh.
     */
    private fun sortAttempts(attempts: List<Attempt>, field: String?, asc: Boolean): List<Attempt> {
        val comparator: Comparator<Attempt> = when (field) {
            "score" -> compareBy { it.score }
            "user", "userid" -> compareBy { it.userId }
            "quiz", "quizid" -> compareBy { it.quizId }
            "start", "starttime" -> compareBy { it.startTimeMillis }
            "end", "endtime" -> compareBy { it.endTimeMillis ?: Long.MAX_VALUE }
            "id" -> compareBy { it.id }
            else -> compareBy<Attempt> { it.startTimeMillis }.reversed()
        }
        return if (asc) attempts.sortedWith(comparator) else attempts.sortedWith(comparator.reversed())
    }

    /**
     * Xay dung dau ra tom tat cho attempt.
     */
    private fun buildAttemptSummary(
        attempts: List<Attempt>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("== Tom tat luot lam quiz ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so          : $total", OutputStyle.NORMAL))

        val completedCount = attempts.count { it.endTimeMillis != null }
        val incompleteCount = attempts.size - completedCount
        val uniqueUsers = attempts.map { it.userId }.distinct().size
        val uniqueQuizzes = attempts.map { it.quizId }.distinct().size

        lines.add(OutputLine("  Hoan thanh       : $completedCount", OutputStyle.SUCCESS))
        if (incompleteCount > 0) {
            lines.add(OutputLine("  Chua hoan thanh  : $incompleteCount", OutputStyle.WARNING))
        }
        lines.add(OutputLine("  Nguoi dung       : $uniqueUsers", OutputStyle.NORMAL))
        lines.add(OutputLine("  Quiz             : $uniqueQuizzes", OutputStyle.NORMAL))

        if (attempts.isNotEmpty()) {
            val avgScore = attempts.sumOf { it.score }.toDouble() / attempts.size
            lines.add(OutputLine("  Diem trung binh  : ${"%.2f".format(avgScore)}", OutputStyle.NORMAL))
        }

        lines.add(OutputLine("  Hien thi         : ${attempts.size} / $total", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra day du cho danh sach attempt.
     */
    private fun buildAttemptFullOutput(
        attempts: List<Attempt>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        if (params.format == "json") {
            return buildAttemptsJson(attempts, total, params)
        }

        if (params.format == "csv") {
            return buildAttemptsCsv(attempts, params)
        }

        val lines = mutableListOf<OutputLine>()

        if (!params.quiet) {
            lines.add(OutputLine("== Danh sach luot lam quiz ($total) ==", OutputStyle.HEADER))
            lines.add(OutputLine(""))
        }

        if (attempts.isEmpty()) {
            lines.add(OutputLine("  (Khong co ket qua nao)", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        val defaultFields = listOf("id", "user", "quiz", "score", "status")
        val fields = params.requestedFields ?: defaultFields

        if (!params.noHeader) {
            val header = buildAttemptHeader(fields)
            lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))
        }

        for (attempt in attempts) {
            val row = buildAttemptRow(attempt, fields)
            val style = if (attempt.endTimeMillis == null) OutputStyle.WARNING else OutputStyle.TABLE_ROW
            lines.add(OutputLine(row, style))

            if (params.verbose) {
                lines.add(
                    OutputLine(
                        "  Bat dau: ${formatTimestamp(attempt.startTimeMillis)}",
                        OutputStyle.MUTED
                    )
                )
                if (attempt.endTimeMillis != null) {
                    val durationSec = (attempt.endTimeMillis - attempt.startTimeMillis) / 1000
                    lines.add(
                        OutputLine(
                            "  Ket thuc: ${formatTimestamp(attempt.endTimeMillis)} (${formatDuration(durationSec)})",
                            OutputStyle.MUTED
                        )
                    )
                }
                lines.add(OutputLine("  Cau tra loi: ${attempt.answers.size}", OutputStyle.MUTED))
            }
        }

        if (!params.quiet) {
            lines.add(OutputLine(""))
            lines.add(
                OutputLine(
                    "Hien thi ${attempts.size} / $total ket qua" +
                        paginationHint(params.offset, params.limit, total),
                    OutputStyle.MUTED
                )
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dong tieu de bang cho attempt.
     */
    private fun buildAttemptHeader(fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> padRight("ID", COL_ID)
                "user", "userid" -> padRight("User ID", COL_NAME)
                "quiz", "quizid" -> padRight("Quiz ID", COL_NAME)
                "score" -> padRight("Diem", COL_SHORT)
                "status" -> padRight("Trang thai", COL_STATUS)
                "start" -> padRight("Bat dau", COL_DATE)
                "end" -> padRight("Ket thuc", COL_DATE)
                "duration" -> padRight("Thoi gian", COL_STATUS)
                else -> padRight(field, COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dong du lieu bang cho mot attempt.
     */
    private fun buildAttemptRow(attempt: Attempt, fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> padRight(attempt.id, COL_ID)
                "user", "userid" -> padRight(truncate(attempt.userId, COL_NAME - 2), COL_NAME)
                "quiz", "quizid" -> padRight(truncate(attempt.quizId, COL_NAME - 2), COL_NAME)
                "score" -> padRight("${attempt.score}/${attempt.totalQuestions}", COL_SHORT)
                "status" -> padRight(if (attempt.endTimeMillis != null) "Hoan thanh" else "Dang lam", COL_STATUS)
                "start" -> padRight(formatTimestamp(attempt.startTimeMillis), COL_DATE)
                "end" -> padRight(
                    if (attempt.endTimeMillis != null) formatTimestamp(attempt.endTimeMillis) else "-",
                    COL_DATE
                )
                "duration" -> {
                    val dur = if (attempt.endTimeMillis != null) {
                        formatDuration((attempt.endTimeMillis - attempt.startTimeMillis) / 1000)
                    } else {
                        "-"
                    }
                    padRight(dur, COL_STATUS)
                }
                else -> padRight("-", COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dau ra attempt dang JSON.
     */
    private fun buildAttemptsJson(
        attempts: List<Attempt>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"type\": \"attempts\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"offset\": ${params.offset},", OutputStyle.CODE))
        lines.add(OutputLine("  \"limit\": ${params.limit},", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${attempts.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"items\": [", OutputStyle.CODE))

        for ((index, attempt) in attempts.withIndex()) {
            val comma = if (index < attempts.size - 1) "," else ""
            val endStr = attempt.endTimeMillis?.toString() ?: "null"
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${escapeJson(attempt.id)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"userId\": \"${escapeJson(attempt.userId)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"quizId\": \"${escapeJson(attempt.quizId)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"score\": ${attempt.score},", OutputStyle.CODE))
            lines.add(OutputLine("      \"totalQuestions\": ${attempt.totalQuestions},", OutputStyle.CODE))
            lines.add(OutputLine("      \"startTimeMillis\": ${attempt.startTimeMillis},", OutputStyle.CODE))
            lines.add(OutputLine("      \"endTimeMillis\": $endStr", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra attempt dang CSV.
     */
    private fun buildAttemptsCsv(attempts: List<Attempt>, params: CommonParams): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val fields = params.requestedFields ?: listOf("id", "user", "quiz", "score", "status")

        if (!params.noHeader) {
            lines.add(OutputLine(fields.joinToString(","), OutputStyle.TABLE_HEADER))
        }

        for (attempt in attempts) {
            val values = fields.map { field ->
                when (field) {
                    "id" -> csvEscape(attempt.id)
                    "user", "userid" -> csvEscape(attempt.userId)
                    "quiz", "quizid" -> csvEscape(attempt.quizId)
                    "score" -> "${attempt.score}/${attempt.totalQuestions}"
                    "status" -> if (attempt.endTimeMillis != null) "completed" else "incomplete"
                    "start" -> attempt.startTimeMillis.toString()
                    "end" -> attempt.endTimeMillis?.toString() ?: ""
                    else -> ""
                }
            }
            lines.add(OutputLine(values.joinToString(","), OutputStyle.TABLE_ROW))
        }

        return CommandResult.success(lines)
    }

    // ====================================================================
    // Pool Items
    // ====================================================================

    /**
     * Liet ke pool item voi bo loc va phan trang.
     *
     * Luu y: API hien tai khong co endpoint "getAllPoolItems". Lenh nay thu thap
     * du lieu qua tat ca nguoi dung da biet hoac loc theo `--contributor` cu the.
     */
    private suspend fun executeListPoolItems(
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val params = parseCommonFlags(flags)
        val contributorFilter = flags["contributor"]
        val tagFilter = flags["tag"]
        val inactiveOnly = "inactive" in flags
        val activeOnly = "active-only" in flags

        val poolRepo = context.repositories.poolRepository
        var items: List<QuestionPoolItem>

        if (contributorFilter != null) {
            val result = poolRepo.getMyContributions(contributorFilter)
            items = result.getOrDefault(emptyList())
        } else {
            // Thu thap tu tat ca nguoi dung — gioi han cua API hien tai
            val adminRepo = context.repositories.adminRepository
            val allUsers = adminRepo.getAllUsers().first()
            val collected = mutableListOf<QuestionPoolItem>()
            for (user in allUsers) {
                val result = poolRepo.getMyContributions(user.id)
                if (result.isSuccess) {
                    collected.addAll(result.getOrDefault(emptyList()))
                }
            }
            items = collected
        }

        // Ap dung bo loc
        if (tagFilter != null) {
            val tagLower = tagFilter.lowercase()
            items = items.filter { item -> item.tags.any { it.lowercase() == tagLower } }
        }
        if (inactiveOnly) {
            items = items.filter { !it.isActive }
        }
        if (activeOnly) {
            items = items.filter { it.isActive }
        }

        val total = items.size

        // Sap xep
        items = sortPoolItems(items, params.sortField, params.sortAsc)

        // Phan trang
        items = items.drop(params.offset).take(params.limit)

        return when (params.outputMode) {
            "count" -> CommandResult.success("Tong so pool item: $total")
            "ids" -> buildIdsOutput(items.map { it.id }, total, params)
            "summary" -> buildPoolSummary(items, total, params)
            else -> buildPoolFullOutput(items, total, params)
        }
    }

    /**
     * Sap xep danh sach pool item theo truong chi dinh.
     */
    private fun sortPoolItems(items: List<QuestionPoolItem>, field: String?, asc: Boolean): List<QuestionPoolItem> {
        val comparator: Comparator<QuestionPoolItem> = when (field) {
            "usage", "usagecount" -> compareBy { it.usageCount }
            "created", "createdat" -> compareBy { it.createdAtMillis }
            "contributor" -> compareBy { it.contributorId ?: "" }
            "active" -> compareBy { it.isActive }
            "id" -> compareBy { it.id }
            else -> compareBy<QuestionPoolItem> { it.createdAtMillis }.reversed()
        }
        return if (asc) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
    }

    /**
     * Xay dung dau ra tom tat cho pool item.
     */
    private fun buildPoolSummary(
        items: List<QuestionPoolItem>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("== Tom tat ngan hang cau hoi ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so          : $total", OutputStyle.NORMAL))

        val activeCount = items.count { it.isActive }
        val inactiveCount = items.size - activeCount
        val uniqueContributors = items.mapNotNull { it.contributorId }.distinct().size
        val totalUsage = items.sumOf { it.usageCount }

        lines.add(OutputLine("  Hoat dong        : $activeCount", OutputStyle.SUCCESS))
        if (inactiveCount > 0) {
            lines.add(OutputLine("  Vo hieu hoa      : $inactiveCount", OutputStyle.WARNING))
        }
        lines.add(OutputLine("  Nguoi dong gop   : $uniqueContributors", OutputStyle.NORMAL))
        lines.add(OutputLine("  Tong luot su dung: $totalUsage", OutputStyle.NORMAL))

        if (items.isNotEmpty()) {
            val allTags = items.flatMap { it.tags }.groupingBy { it.lowercase() }.eachCount()
            val topTags = allTags.entries.sortedByDescending { it.value }.take(5)
            if (topTags.isNotEmpty()) {
                val tagStr = topTags.joinToString(", ") { "${it.key} (${it.value})" }
                lines.add(OutputLine("  Tags pho bien    : $tagStr", OutputStyle.MUTED))
            }
        }

        lines.add(OutputLine("  Hien thi         : ${items.size} / $total", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra day du cho danh sach pool item.
     */
    private fun buildPoolFullOutput(
        items: List<QuestionPoolItem>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        if (params.format == "json") {
            return buildPoolJson(items, total, params)
        }

        if (params.format == "csv") {
            return buildPoolCsv(items, params)
        }

        val lines = mutableListOf<OutputLine>()

        if (!params.quiet) {
            lines.add(OutputLine("== Danh sach pool item ($total) ==", OutputStyle.HEADER))
            lines.add(OutputLine(""))
        }

        if (items.isEmpty()) {
            lines.add(OutputLine("  (Khong co ket qua nao)", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        val defaultFields = listOf("id", "question", "status", "usage", "contributor")
        val fields = params.requestedFields ?: defaultFields

        if (!params.noHeader) {
            val header = buildPoolHeader(fields)
            lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))
        }

        for (item in items) {
            val row = buildPoolRow(item, fields)
            val style = if (item.isActive) OutputStyle.TABLE_ROW else OutputStyle.WARNING
            lines.add(OutputLine(row, style))

            if (params.verbose) {
                lines.add(
                    OutputLine(
                        "  Quiz nguon: ${item.sourceQuizId}",
                        OutputStyle.MUTED
                    )
                )
                if (item.tags.isNotEmpty()) {
                    lines.add(OutputLine("  Tags: ${item.tags.joinToString(", ")}", OutputStyle.MUTED))
                }
                lines.add(
                    OutputLine(
                        "  Tao: ${formatTimestamp(item.createdAtMillis)}",
                        OutputStyle.MUTED
                    )
                )
            }
        }

        if (!params.quiet) {
            lines.add(OutputLine(""))
            lines.add(
                OutputLine(
                    "Hien thi ${items.size} / $total ket qua" +
                        paginationHint(params.offset, params.limit, total),
                    OutputStyle.MUTED
                )
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dong tieu de bang cho pool item.
     */
    private fun buildPoolHeader(fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> padRight("ID", COL_ID)
                "question" -> padRight("Noi dung", COL_TITLE)
                "contributor" -> padRight("Nguoi dong gop", COL_NAME)
                "status" -> padRight("Trang thai", COL_STATUS)
                "usage", "usagecount" -> padRight("Su dung", COL_SHORT)
                "tags" -> padRight("Tags", COL_NAME)
                "source", "sourcequiz" -> padRight("Quiz nguon", COL_ID)
                "created" -> padRight("Ngay tao", COL_DATE)
                else -> padRight(field, COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dong du lieu bang cho mot pool item.
     */
    private fun buildPoolRow(item: QuestionPoolItem, fields: List<String>): String {
        return fields.joinToString("") { field ->
            when (field) {
                "id" -> padRight(item.id, COL_ID)
                "question" -> padRight(truncate(item.question.content, COL_TITLE - 2), COL_TITLE)
                "contributor" -> padRight(truncate(item.contributorId ?: "(an danh)", COL_NAME - 2), COL_NAME)
                "status" -> padRight(if (item.isActive) "Hoat dong" else "Vo hieu", COL_STATUS)
                "usage", "usagecount" -> padRight(item.usageCount.toString(), COL_SHORT)
                "tags" -> padRight(truncate(item.tags.joinToString(","), COL_NAME - 2), COL_NAME)
                "source", "sourcequiz" -> padRight(item.sourceQuizId, COL_ID)
                "created" -> padRight(formatTimestamp(item.createdAtMillis), COL_DATE)
                else -> padRight("-", COL_DEFAULT)
            }
        }
    }

    /**
     * Xay dung dau ra pool item dang JSON.
     */
    private fun buildPoolJson(
        items: List<QuestionPoolItem>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"type\": \"poolItems\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"offset\": ${params.offset},", OutputStyle.CODE))
        lines.add(OutputLine("  \"limit\": ${params.limit},", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${items.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"items\": [", OutputStyle.CODE))

        for ((index, item) in items.withIndex()) {
            val comma = if (index < items.size - 1) "," else ""
            val contributorStr = if (item.contributorId != null) "\"${escapeJson(item.contributorId)}\"" else "null"
            val tagsStr = item.tags.joinToString(", ") { "\"${escapeJson(it)}\"" }
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${escapeJson(item.id)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"questionPreview\": \"${escapeJson(truncate(item.question.content, 80))}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"contributorId\": $contributorStr,", OutputStyle.CODE))
            lines.add(OutputLine("      \"sourceQuizId\": \"${escapeJson(item.sourceQuizId)}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"tags\": [$tagsStr],", OutputStyle.CODE))
            lines.add(OutputLine("      \"isActive\": ${item.isActive},", OutputStyle.CODE))
            lines.add(OutputLine("      \"usageCount\": ${item.usageCount},", OutputStyle.CODE))
            lines.add(OutputLine("      \"createdAtMillis\": ${item.createdAtMillis}", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra pool item dang CSV.
     */
    private fun buildPoolCsv(items: List<QuestionPoolItem>, params: CommonParams): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val fields = params.requestedFields ?: listOf("id", "question", "contributor", "status", "usage")

        if (!params.noHeader) {
            lines.add(OutputLine(fields.joinToString(","), OutputStyle.TABLE_HEADER))
        }

        for (item in items) {
            val values = fields.map { field ->
                when (field) {
                    "id" -> csvEscape(item.id)
                    "question" -> csvEscape(truncate(item.question.content, 80))
                    "contributor" -> csvEscape(item.contributorId ?: "")
                    "status" -> if (item.isActive) "active" else "inactive"
                    "usage", "usagecount" -> item.usageCount.toString()
                    "tags" -> csvEscape(item.tags.joinToString(";"))
                    "source", "sourcequiz" -> csvEscape(item.sourceQuizId)
                    "created" -> item.createdAtMillis.toString()
                    else -> ""
                }
            }
            lines.add(OutputLine(values.joinToString(","), OutputStyle.TABLE_ROW))
        }

        return CommandResult.success(lines)
    }

    // ====================================================================
    // Shared output helpers
    // ====================================================================

    /**
     * Xay dung dau ra che do "ids" — chi hien thi danh sach ID.
     */
    private fun buildIdsOutput(
        ids: List<String>,
        total: Int,
        params: CommonParams
    ): CommandResult {
        if (params.format == "json") {
            val lines = mutableListOf<OutputLine>()
            lines.add(OutputLine("{", OutputStyle.CODE))
            lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
            lines.add(OutputLine("  \"count\": ${ids.size},", OutputStyle.CODE))
            val idsStr = ids.joinToString(", ") { "\"${escapeJson(it)}\"" }
            lines.add(OutputLine("  \"ids\": [$idsStr]", OutputStyle.CODE))
            lines.add(OutputLine("}", OutputStyle.CODE))
            return CommandResult.success(lines)
        }

        val lines = mutableListOf<OutputLine>()
        for (id in ids) {
            lines.add(OutputLine(id, OutputStyle.CODE))
        }
        if (!params.quiet && total > ids.size) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("Hien thi ${ids.size} / $total ID", OutputStyle.MUTED))
        }
        return CommandResult.success(lines)
    }

    // ====================================================================
    // Entity type resolution & permission checks
    // ====================================================================

    /**
     * Xac dinh loai thuc the tu flag.
     */
    private fun resolveEntityType(flags: Map<String, String?>): EntityType? {
        return when {
            "u" in flags -> EntityType.USER
            "q" in flags -> EntityType.QUIZ
            "a" in flags -> EntityType.ATTEMPT
            "p" in flags -> EntityType.POOL
            else -> null
        }
    }

    /**
     * Kiem tra quyen truy cap dua tren loai thuc the.
     *
     * @param entityType Loai thuc the can kiem tra.
     * @param context Context lenh hien tai.
     * @return [CommandResult] loi neu khong du quyen, null neu hop le.
     */
    private fun checkPermission(
        entityType: EntityType,
        context: CommandContext
    ): CommandResult? {
        val user = context.currentUser
        val requiredPerm = when (entityType) {
            EntityType.USER -> AdminPermission.MANAGE_USERS
            EntityType.QUIZ -> AdminPermission.MANAGE_QUIZZES
            EntityType.ATTEMPT -> AdminPermission.MANAGE_QUIZZES
            EntityType.POOL -> AdminPermission.MANAGE_QUIZZES
        }

        if (!user.hasPermission(requiredPerm)) {
            val entityLabel = when (entityType) {
                EntityType.USER -> "nguoi dung"
                EntityType.QUIZ -> "quiz"
                EntityType.ATTEMPT -> "luot lam quiz"
                EntityType.POOL -> "pool item"
            }
            return CommandResult.error(
                "Khong du quyen de liet ke $entityLabel. " +
                    "Yeu cau quyen: ${formatPermission(requiredPerm)}."
            )
        }
        return null
    }

    /**
     * Xay dung thong bao loi khi khong co flag loai thuc the.
     */
    private fun buildNoEntityTypeError(): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Loi: Vui long chi dinh loai thuc the can liet ke.",
                OutputStyle.ERROR
            )
        )
        lines.add(OutputLine(""))
        lines.add(OutputLine("Cac loai thuc the ho tro:", OutputStyle.HEADER))
        lines.add(OutputLine("  -u    Liet ke nguoi dung (yeu cau quyen MANAGE_USERS)", OutputStyle.INFO))
        lines.add(OutputLine("  -q    Liet ke quiz (yeu cau quyen MANAGE_QUIZZES)", OutputStyle.INFO))
        lines.add(OutputLine("  -a    Liet ke luot lam quiz (yeu cau quyen MANAGE_QUIZZES)", OutputStyle.INFO))
        lines.add(OutputLine("  -p    Liet ke pool item (yeu cau quyen MANAGE_QUIZZES)", OutputStyle.INFO))
        lines.add(OutputLine(""))
        lines.add(OutputLine("Vi du:", OutputStyle.HEADER))
        lines.add(OutputLine("  ls -u                          Liet ke nguoi dung", OutputStyle.MUTED))
        lines.add(OutputLine("  ls -q --tag math --limit 10    Liet ke 10 quiz co tag 'math'", OutputStyle.MUTED))
        lines.add(OutputLine("  ls -a --user userId1           Liet ke luot lam cua nguoi dung", OutputStyle.MUTED))
        lines.add(OutputLine("  ls -p --inactive --output ids  Liet ke ID pool item vo hieu", OutputStyle.MUTED))

        return CommandResult(output = lines, isSuccess = false, exitCode = 1)
    }

    // ====================================================================
    // Formatting utilities
    // ====================================================================

    /**
     * Tao goi y phan trang (trang tiep, trang truoc).
     */
    private fun paginationHint(offset: Int, limit: Int, total: Int): String {
        if (total <= limit) return ""

        val currentPage = (offset / limit) + 1
        val totalPages = (total + limit - 1) / limit

        val parts = mutableListOf<String>()
        parts.add(" (trang $currentPage/$totalPages)")

        if (offset + limit < total) {
            val nextPage = currentPage + 1
            parts.add("Trang tiep: --page $nextPage")
        }

        return parts.joinToString(" | ")
    }

    /**
     * Tao nhan trang thai cho quiz.
     */
    private fun quizStatusLabel(quiz: Quiz): String {
        return when {
            quiz.deletedAt != null -> "Da xoa"
            quiz.isDraft -> "Nhap"
            quiz.isPublic -> "Cong khai"
            else -> "Rieng tu"
        }
    }

    /**
     * Dinh dang ten vai tro sang tieng Viet.
     */
    private fun formatRole(role: UserRole): String = when (role) {
        UserRole.GUEST -> "Khach"
        UserRole.USER -> "Nguoi dung"
        UserRole.ADMIN -> "Quan tri vien"
        UserRole.SUPERUSER -> "Sieu quan tri"
    }

    /**
     * Dinh dang ten quyen han de hien thi.
     */
    private fun formatPermission(permission: AdminPermission): String = when (permission) {
        AdminPermission.MANAGE_USERS -> "Quan ly nguoi dung"
        AdminPermission.CHANGE_USER_ROLES -> "Thay doi vai tro"
        AdminPermission.DELETE_USERS -> "Xoa nguoi dung"
        AdminPermission.BAN_USERS -> "Cam nguoi dung"
        AdminPermission.MANAGE_QUIZZES -> "Quan ly quiz"
        AdminPermission.DELETE_QUIZZES -> "Xoa quiz"
        AdminPermission.PUBLISH_QUIZZES -> "Xuat ban quiz"
        AdminPermission.VIEW_REPORTS -> "Xem bao cao"
    }

    /**
     * Dinh dang timestamp thanh chuoi ngay gio doc duoc.
     */
    private fun formatTimestamp(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    /**
     * Dinh dang thoi luong (giay) thanh chuoi doc duoc.
     */
    private fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds < 60) return "${totalSeconds}s"
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        if (minutes < 60) return "${minutes}m ${seconds}s"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return "${hours}h ${remainingMinutes}m ${seconds}s"
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

    /**
     * Thoat gia tri cho CSV (bao quanh bang dau nhay kep neu can).
     */
    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    // ====================================================================
    // Internal types & constants
    // ====================================================================

    /**
     * Cac loai thuc the ma lenh `ls` ho tro.
     */
    private enum class EntityType {
        USER, QUIZ, ATTEMPT, POOL
    }

    /**
     * Tham so chung da phan tich tu flags.
     *
     * @property limit Gioi han so ket qua.
     * @property offset So ket qua bo qua.
     * @property format Dinh dang dau ra (table/json/csv).
     * @property outputMode Che do dau ra (full/count/ids/summary).
     * @property sortField Truong sap xep.
     * @property sortAsc Thu tu sap xep tang dan.
     * @property verbose Hien thi chi tiet.
     * @property quiet Chi hien thi du lieu toi thieu.
     * @property noHeader An dong tieu de bang.
     * @property requestedFields Cac truong cu the can hien thi.
     */
    private data class CommonParams(
        val limit: Int,
        val offset: Int,
        val format: String,
        val outputMode: String,
        val sortField: String?,
        val sortAsc: Boolean,
        val verbose: Boolean,
        val quiet: Boolean,
        val noHeader: Boolean,
        val requestedFields: List<String>?
    )

    companion object {
        /** Gioi han mac dinh so ket qua moi trang. */
        const val DEFAULT_LIMIT = 25

        /** Gioi han toi da so ket qua moi trang. */
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
        private const val COL_DEFAULT = 16
    }
}
