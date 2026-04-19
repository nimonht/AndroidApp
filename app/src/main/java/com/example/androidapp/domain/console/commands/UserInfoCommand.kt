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
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Console command that displays detailed information about a user.
 *
 * Looks up a user by email or user ID and renders their profile data,
 * role, permissions, ban status, and optionally their quizzes and attempts.
 *
 * Usage:
 * ```
 * userinfo <email_or_id>
 * userinfo <email_or_id> --quizzes
 * userinfo <email_or_id> --attempts
 * userinfo <email_or_id> --all
 * ```
 *
 * Aliases: `ui`, `whois`
 */
class UserInfoCommand : Command {

    /** @inheritDoc */
    override val name: String = "userinfo"

    /** @inheritDoc */
    override val aliases: List<String> = listOf("ui", "whois")

    /** @inheritDoc */
    override val description: String = "Hien thi thong tin chi tiet cua nguoi dung"

    /** @inheritDoc */
    override val usage: String =
        "userinfo <email_hoac_id> [--quizzes] [--attempts] [--all] [--format <table|compact|full>] [--verbose] [--fields <field1,field2,...>]"

    /** @inheritDoc */
    override val requiredPermission: AdminPermission = AdminPermission.MANAGE_USERS

    /** @inheritDoc */
    override val minimumRole: UserRole = UserRole.ADMIN

    /** @inheritDoc */
    override val category: String = "admin"

    /** @inheritDoc */
    override val examples: List<Pair<String, String>> = listOf(
        "userinfo user@example.com" to "Hien thi thong tin co ban cua nguoi dung",
        "userinfo user@example.com --quizzes" to "Hien thi thong tin kem danh sach quiz",
        "userinfo user@example.com --attempts" to "Hien thi thong tin kem lich su lam bai",
        "userinfo user@example.com --all" to "Hien thi tat ca thong tin chi tiet",
        "userinfo user@example.com --fields id,email,role" to "Chi hien thi cac truong duoc chon",
        "whois abc123 --verbose --format full" to "Tra cuu theo ID voi dinh dang day du"
    )

    /** @inheritDoc */
    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        if (args.size <= 1) {
            suggestions.add(
                CompletionSuggestion(
                    text = "<email_hoac_id>",
                    description = "Email hoac ID cua nguoi dung",
                    type = SuggestionType.USER
                )
            )
        }

        if ("quizzes" !in flags) {
            suggestions.add(
                CompletionSuggestion(
                    text = "--quizzes",
                    description = "Hien thi danh sach quiz cua nguoi dung",
                    type = SuggestionType.FLAG
                )
            )
        }
        if ("attempts" !in flags) {
            suggestions.add(
                CompletionSuggestion(
                    text = "--attempts",
                    description = "Hien thi lich su lam bai",
                    type = SuggestionType.FLAG
                )
            )
        }
        if ("all" !in flags) {
            suggestions.add(
                CompletionSuggestion(
                    text = "--all",
                    description = "Hien thi tat ca thong tin",
                    type = SuggestionType.FLAG
                )
            )
        }
        if ("format" !in flags) {
            suggestions.add(
                CompletionSuggestion(
                    text = "--format",
                    description = "Dinh dang ket qua (table|compact|full)",
                    type = SuggestionType.FLAG
                )
            )
        }
        if ("verbose" !in flags) {
            suggestions.add(
                CompletionSuggestion(
                    text = "--verbose",
                    description = "Hien thi thong tin chi tiet hon",
                    type = SuggestionType.FLAG
                )
            )
        }
        if ("fields" !in flags) {
            suggestions.add(
                CompletionSuggestion(
                    text = "--fields",
                    description = "Chi dinh cac truong can hien thi",
                    type = SuggestionType.FLAG
                )
            )
        }

        return suggestions
    }

    /** @inheritDoc */
    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error("Thieu tham so. Su dung: $usage")
        }

        val query = args[0]
        val showQuizzes = "quizzes" in flags || "all" in flags
        val showAttempts = "attempts" in flags || "all" in flags
        val verbose = "verbose" in flags
        val format = flags["format"] ?: "table"
        val selectedFields = flags["fields"]?.split(",")?.map { it.trim().lowercase() }

        val allUsers = try {
            context.repositories.adminRepository.getAllUsers().first()
        } catch (e: Exception) {
            return CommandResult.error("Khong the tai danh sach nguoi dung: ${e.message}")
        }

        val user = findUser(allUsers, query)
            ?: return CommandResult.error("Khong tim thay nguoi dung: $query")

        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("=== Thong tin nguoi dung ===", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val allFieldData = buildUserFields(user, verbose)

        val fieldsToShow = if (selectedFields != null) {
            allFieldData.filter { (key, _) -> key.lowercase() in selectedFields }
        } else {
            allFieldData
        }

        when (format) {
            "compact" -> renderCompact(fieldsToShow, lines)
            "full" -> renderFull(fieldsToShow, lines, user)
            else -> renderTable(fieldsToShow, lines)
        }

        if (verbose) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("--- Quyen han ---", OutputStyle.HEADER))
            val effectivePerms = user.effectivePermissions()
            if (effectivePerms.isEmpty()) {
                lines.add(OutputLine("  (khong co quyen admin)", OutputStyle.MUTED))
            } else {
                effectivePerms.forEach { perm ->
                    lines.add(OutputLine("  - $perm", OutputStyle.TABLE_ROW))
                }
            }
        }

        if (showQuizzes) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("--- Quiz cua nguoi dung ---", OutputStyle.HEADER))
            try {
                val quizzes = context.repositories.quizRepository
                    .getMyQuizzes(user.id).first()
                if (quizzes.isEmpty()) {
                    lines.add(OutputLine("  Khong co quiz nao.", OutputStyle.MUTED))
                } else {
                    lines.add(
                        OutputLine(
                            padEnd("ID", 24) + padEnd("Tieu de", 32) +
                                    padEnd("Cong khai", 12) + padEnd("Luot thi", 10),
                            OutputStyle.TABLE_HEADER
                        )
                    )
                    quizzes.forEach { quiz ->
                        val visibility = if (quiz.isPublic) "Co" else "Khong"
                        val draft = if (quiz.isDraft) " [Nhap]" else ""
                        lines.add(
                            OutputLine(
                                padEnd(quiz.id, 24) +
                                        padEnd(CommandFormatUtils.truncate(quiz.title, 28) + draft, 32) +
                                        padEnd(visibility, 12) +
                                        padEnd(quiz.attemptCount.toString(), 10),
                                OutputStyle.TABLE_ROW
                            )
                        )
                    }
                    lines.add(
                        OutputLine(
                            "Tong cong: ${quizzes.size} quiz",
                            OutputStyle.INFO
                        )
                    )
                }
            } catch (e: Exception) {
                lines.add(
                    OutputLine(
                        "Loi khi tai danh sach quiz: ${e.message}",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        if (showAttempts) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("--- Lich su lam bai ---", OutputStyle.HEADER))
            try {
                val attempts = context.repositories.attemptRepository
                    .getAttemptsByUser(user.id).first()
                if (attempts.isEmpty()) {
                    lines.add(OutputLine("  Chua co luot thi nao.", OutputStyle.MUTED))
                } else {
                    lines.add(
                        OutputLine(
                            padEnd("ID", 24) + padEnd("Quiz ID", 24) +
                                    padEnd("Diem", 12) + padEnd("Thoi gian (ms)", 16),
                            OutputStyle.TABLE_HEADER
                        )
                    )
                    attempts.forEach { attempt ->
                        val duration = if (attempt.endTimeMillis != null) {
                            (attempt.endTimeMillis - attempt.startTimeMillis).toString()
                        } else {
                            "Dang lam"
                        }
                        lines.add(
                            OutputLine(
                                padEnd(attempt.id, 24) +
                                        padEnd(attempt.quizId, 24) +
                                        padEnd(
                                            "${attempt.score}/${attempt.maxScore}",
                                            12
                                        ) +
                                        padEnd(duration, 16),
                                OutputStyle.TABLE_ROW
                            )
                        )
                    }
                    lines.add(
                        OutputLine(
                            "Tong cong: ${attempts.size} luot thi",
                            OutputStyle.INFO
                        )
                    )

                    if (verbose && attempts.isNotEmpty()) {
                        val totalScore = attempts.sumOf { it.score }
                        val totalMaxScore = attempts.sumOf { it.maxScore }
                        val avgPercent = if (totalMaxScore > 0) {
                            (totalScore.toDouble() / totalMaxScore * 100)
                        } else {
                            0.0
                        }
                        lines.add(
                            OutputLine(
                                "Diem trung binh: ${"%.1f".format(avgPercent)}%",
                                OutputStyle.INFO
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                lines.add(
                    OutputLine(
                        "Loi khi tai lich su lam bai: ${e.message}",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Finds a user by matching email, ID, or username.
     *
     * @param users The full list of users to search.
     * @param query The email, ID, or username to match.
     * @return The matched [User], or null if not found.
     */
    private fun findUser(users: List<User>, query: String): User? {
        return users.find { it.email.equals(query, ignoreCase = true) }
            ?: users.find { it.id == query }
            ?: users.find { it.username.equals(query, ignoreCase = true) }
    }

    /**
     * Builds the ordered list of user field key-value pairs for display.
     *
     * @param user The user whose fields to build.
     * @param verbose Whether to include extended fields.
     * @return Ordered list of (field name, value) pairs.
     */
    private fun buildUserFields(user: User, verbose: Boolean): List<Pair<String, String>> {
        val fields = mutableListOf<Pair<String, String>>()

        fields.add("ID" to user.id)
        fields.add("Email" to user.email)
        fields.add("Ten hien thi" to user.displayName)
        fields.add("Username" to user.username.ifEmpty { "(chua dat)" })
        fields.add("Vai tro" to formatRole(user.role))
        fields.add("Trang thai" to if (user.isBanned) "Da bi cam" else "Hoat dong")

        if (verbose) {
            fields.add("Anh dai dien" to (user.photoUrl ?: "(khong co)"))
            fields.add("La admin" to if (user.isAdmin()) "Co" else "Khong")
            fields.add("La superuser" to if (user.isSuperuser()) "Co" else "Khong")
            val permCount = user.effectivePermissions().size
            fields.add("So quyen" to "$permCount/${AdminPermission.entries.size}")
        }

        return fields
    }

    /**
     * Renders fields in table format with aligned columns.
     *
     * @param fields The field key-value pairs.
     * @param lines The output line list to append to.
     */
    private fun renderTable(
        fields: List<Pair<String, String>>,
        lines: MutableList<OutputLine>
    ) {
        val maxKeyLen = fields.maxOfOrNull { it.first.length } ?: 0
        fields.forEach { (key, value) ->
            val paddedKey = key.padEnd(maxKeyLen + 2)
            val style = when {
                key == "Trang thai" && value == "Da bi cam" -> OutputStyle.WARNING
                key == "Vai tro" && value.contains("SUPERUSER") -> OutputStyle.INFO
                else -> OutputStyle.TABLE_ROW
            }
            lines.add(OutputLine("  $paddedKey: $value", style))
        }
    }

    /**
     * Renders fields in compact single-line format.
     *
     * @param fields The field key-value pairs.
     * @param lines The output line list to append to.
     */
    private fun renderCompact(
        fields: List<Pair<String, String>>,
        lines: MutableList<OutputLine>
    ) {
        val summary = fields.joinToString(" | ") { "${it.first}=${it.second}" }
        lines.add(OutputLine(summary, OutputStyle.TABLE_ROW))
    }

    /**
     * Renders fields in full verbose format with section separators.
     *
     * @param fields The field key-value pairs.
     * @param lines The output line list to append to.
     * @param user The user being displayed (for extra context).
     */
    private fun renderFull(
        fields: List<Pair<String, String>>,
        lines: MutableList<OutputLine>,
        user: User
    ) {
        lines.add(OutputLine("Nguoi dung: ${user.displayName} (${user.email})", OutputStyle.INFO))
        lines.add(OutputLine("-".repeat(50), OutputStyle.MUTED))
        val maxKeyLen = fields.maxOfOrNull { it.first.length } ?: 0
        fields.forEach { (key, value) ->
            val paddedKey = key.padEnd(maxKeyLen + 2)
            lines.add(OutputLine("  $paddedKey: $value", OutputStyle.TABLE_ROW))
        }
        lines.add(OutputLine("-".repeat(50), OutputStyle.MUTED))
    }

    /**
     * Formats a [UserRole] for Vietnamese display.
     *
     * @param role The role to format.
     * @return Formatted string with role name and Vietnamese label.
     */
    private fun formatRole(role: UserRole): String {
        return when (role) {
            UserRole.GUEST -> "GUEST (Khach)"
            UserRole.USER -> "USER (Nguoi dung)"
            UserRole.ADMIN -> "ADMIN (Quan tri vien)"
            UserRole.SUPERUSER -> "SUPERUSER (Sieu quan tri)"
        }
    }

}
