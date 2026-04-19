package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
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
 * Admin command to unban previously banned users.
 *
 * Supports targeting by email/user-ID, search query, regex pattern, and role
 * filter. Only users whose [User.isBanned] flag is `true` are eligible.
 *
 * A `--dry-run` flag previews which users would be unbanned without performing
 * the operation.
 *
 * The `--all` flag unbans every currently banned user (requires `--confirm`).
 *
 * The `--banned-before` and `--banned-after` flags are recognized but not yet
 * functional because the [User] domain model lacks a `bannedAt` field.
 *
 * Usage examples:
 * ```
 * unban user@example.com
 * unban --search "test" --dry-run
 * unban --role USER --confirm
 * unban --regex ".*@temp\.com" --confirm
 * unban --all --confirm
 * ```
 */
class UnbanCommand : Command {

    override val name: String = "unban"

    override val aliases: List<String> = emptyList()

    override val description: String = "Bo cam nguoi dung da bi cam"

    override val usage: String =
        "unban <email|userId> [...] [--search <query>] [--regex <pattern>] " +
                "[--role <role>] [--all] [--banned-before <date>] [--banned-after <date>] " +
                "[--dry-run] [--confirm] [--reason <text>] [--verbose] [--quiet] [--format <plain|table>]"

    override val requiredPermission: AdminPermission = AdminPermission.BAN_USERS

    override val isDestructive: Boolean = true

    override val minimumRole: UserRole = UserRole.ADMIN

    override val category: String = "admin"

    override val valueFlags: Set<String> = setOf(
        "search", "regex", "role", "reason", "format", "banned-before", "banned-after"
    )

    override val examples: List<Pair<String, String>> = listOf(
        "unban user@example.com" to "Bo cam mot nguoi dung cu the",
        "unban user1@a.com user2@b.com" to "Bo cam nhieu nguoi dung",
        "unban --search test --dry-run" to "Xem truoc nguoi dung bi cam khop voi 'test'",
        "unban --role USER --confirm" to "Bo cam tat ca nguoi dung co role USER",
        "unban --regex \".*@temp\\.com\" --confirm" to "Bo cam nguoi dung khop regex",
        "unban --all --confirm" to "Bo cam tat ca nguoi dung dang bi cam"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--search" to "Tim kiem nguoi dung bi cam",
            "--regex" to "Loc bang bieu thuc chinh quy",
            "--role" to "Loc theo vai tro",
            "--all" to "Bo cam tat ca nguoi dung dang bi cam",
            "--banned-before" to "Loc theo thoi gian cam truoc ngay",
            "--banned-after" to "Loc theo thoi gian cam sau ngay",
            "--dry-run" to "Xem truoc ket qua ma khong thuc hien",
            "--confirm" to "Bo qua xac nhan",
            "--reason" to "Ly do bo cam",
            "--verbose" to "Hien thi chi tiet",
            "--quiet" to "Chi hien thi so luong",
            "--format" to "Dinh dang ket qua (plain|table)"
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

        if ("role" in flags && flags["role"] == null) {
            for (role in UserRole.entries) {
                suggestions.add(
                    CompletionSuggestion(
                        text = role.name,
                        description = "Vai tro ${role.name}",
                        type = SuggestionType.ARGUMENT
                    )
                )
            }
        }

        if ("format" in flags && flags["format"] == null) {
            suggestions.add(
                CompletionSuggestion(text = "plain", description = "Van ban thuan", type = SuggestionType.ARGUMENT)
            )
            suggestions.add(
                CompletionSuggestion(text = "table", description = "Dang bang", type = SuggestionType.ARGUMENT)
            )
        }

        return suggestions
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val isDryRun = "dry-run" in flags
        val isVerbose = "verbose" in flags
        val isQuiet = "quiet" in flags
        val isAll = "all" in flags
        val confirm = "confirm" in flags
        val reason = flags["reason"]
        val format = flags["format"] ?: "plain"
        val searchQuery = flags["search"]
        val regexPattern = flags["regex"]
        val roleFilter = flags["role"]
        val bannedBefore = flags["banned-before"]
        val bannedAfter = flags["banned-after"]

        // Handle unsupported --banned-before / --banned-after flags early
        if (bannedBefore != null || "banned-before" in flags) {
            return CommandResult.error(
                "Tuy chon --banned-before chua duoc ho tro vi du lieu thoi gian cam chua co san."
            )
        }
        if (bannedAfter != null || "banned-after" in flags) {
            return CommandResult.error(
                "Tuy chon --banned-after chua duoc ho tro vi du lieu thoi gian cam chua co san."
            )
        }

        // Validate that at least one targeting mechanism is provided
        if (!isAll && args.isEmpty() && searchQuery == null && regexPattern == null && roleFilter == null) {
            return CommandResult.error(
                "Vui long cung cap email/userId, hoac su dung --all, --search, --regex, --role de chi dinh nguoi dung can bo cam."
            )
        }

        // --all requires --confirm (unless dry-run)
        if (isAll && !confirm && !isDryRun) {
            return CommandResult.error(
                "Tuy chon --all yeu cau --confirm de xac nhan bo cam tat ca nguoi dung. " +
                        "Su dung --dry-run de xem truoc."
            )
        }

        val allUsers: List<User> = try {
            context.repositories.adminRepository.getAllUsers().first()
        } catch (e: Exception) {
            return CommandResult.error("Khong the tai danh sach nguoi dung: ${e.message}")
        }

        val bannedUsers = allUsers.filter { it.isBanned }

        if (bannedUsers.isEmpty()) {
            return CommandResult.success("Khong co nguoi dung nao dang bi cam trong he thong.")
        }

        val warnings = mutableListOf<OutputLine>()

        var targetUsers = when {
            isAll -> {
                bannedUsers
            }

            args.isNotEmpty() -> {
                val matched = mutableListOf<User>()
                val notFound = mutableListOf<String>()
                for (identifier in args) {
                    val user = bannedUsers.find {
                        it.email.equals(identifier, ignoreCase = true) || it.id == identifier
                    }
                    if (user != null) {
                        matched.add(user)
                    } else {
                        val unbannedMatch = allUsers.find {
                            !it.isBanned && (it.email.equals(identifier, ignoreCase = true) || it.id == identifier)
                        }
                        if (unbannedMatch != null) {
                            notFound.add("$identifier (nguoi dung nay khong bi cam)")
                        } else {
                            notFound.add(identifier)
                        }
                    }
                }
                if (notFound.isNotEmpty() && matched.isEmpty()) {
                    return CommandResult.error(
                        "Khong tim thay nguoi dung bi cam: ${notFound.joinToString(", ")}"
                    )
                }
                if (notFound.isNotEmpty()) {
                    warnings.add(
                        OutputLine(
                            "Canh bao: Khong tim thay mot so nguoi dung bi cam: ${notFound.joinToString(", ")}",
                            OutputStyle.WARNING
                        )
                    )
                    if (matched.isEmpty()) {
                        return CommandResult.error(
                            "Khong tim thay nguoi dung bi cam nao khop voi tieu chi."
                        )
                    }
                }
                matched
            }

            searchQuery != null -> {
                bannedUsers.filter { user ->
                    user.email.contains(searchQuery, ignoreCase = true) ||
                            user.displayName.contains(searchQuery, ignoreCase = true) ||
                            user.username.contains(searchQuery, ignoreCase = true)
                }
            }

            regexPattern != null -> {
                val regex = try {
                    Regex(regexPattern, RegexOption.IGNORE_CASE)
                } catch (e: Exception) {
                    return CommandResult.error("Bieu thuc chinh quy khong hop le: ${e.message}")
                }
                bannedUsers.filter { user ->
                    regex.containsMatchIn(user.email) ||
                            regex.containsMatchIn(user.displayName) ||
                            regex.containsMatchIn(user.username)
                }
            }

            roleFilter != null -> {
                val role = try {
                    UserRole.valueOf(roleFilter.uppercase())
                } catch (e: IllegalArgumentException) {
                    return CommandResult.error(
                        "Vai tro khong hop le: $roleFilter. Cac vai tro hop le: ${UserRole.entries.joinToString(", ") { it.name }}"
                    )
                }
                bannedUsers.filter { it.role == role }
            }

            else -> bannedUsers
        }

        if (targetUsers.isEmpty()) {
            return CommandResult.success("Khong tim thay nguoi dung bi cam nao khop voi tieu chi.")
        }

        // Prevent unbanning yourself
        targetUsers = targetUsers.filter { it.id != context.currentUser.id }

        if (targetUsers.isEmpty()) {
            return CommandResult.error("Ban khong the tu bo cam chinh minh.")
        }

        // Dry-run mode: preview only
        if (isDryRun) {
            return buildDryRunResult(targetUsers, format, isVerbose)
        }

        // Execute unban operations
        val output = mutableListOf<OutputLine>()
        output.addAll(warnings)
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()

        for (user in targetUsers) {
            val result = context.repositories.adminRepository.unbanUser(user.id)
            if (result.isSuccess) {
                successCount++
                if (isVerbose && !isQuiet) {
                    output.add(
                        OutputLine(
                            "  Da bo cam: ${user.email} (${user.displayName})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                failureCount++
                val errorMsg = result.exceptionOrNull()?.message ?: "Loi khong xac dinh"
                errors.add("${user.email}: $errorMsg")
                if (!isQuiet) {
                    output.add(
                        OutputLine(
                            "  That bai: ${user.email} - $errorMsg",
                            OutputStyle.ERROR
                        )
                    )
                }
            }
        }

        // Summary
        if (isQuiet) {
            output.add(OutputLine("$successCount", OutputStyle.NORMAL))
        } else {
            if (reason != null) {
                output.add(0, OutputLine("Ly do: $reason", OutputStyle.MUTED))
            }

            output.add(OutputLine("", OutputStyle.NORMAL))

            val summaryText = buildString {
                append("Ket qua: Bo cam thanh cong $successCount")
                append("/${targetUsers.size} nguoi dung")
                if (failureCount > 0) {
                    append(" ($failureCount that bai)")
                }
            }
            val summaryStyle = when {
                failureCount == 0 -> OutputStyle.SUCCESS
                successCount == 0 -> OutputStyle.ERROR
                else -> OutputStyle.WARNING
            }
            output.add(OutputLine(summaryText, summaryStyle))
        }

        return CommandResult(
            output = output,
            isSuccess = failureCount == 0,
            exitCode = if (failureCount == 0) 0 else 1
        )
    }

    /**
     * Builds a preview result listing which users would be unbanned.
     *
     * @param users The list of users targeted for unbanning.
     * @param format Output format (`"plain"` or `"table"`).
     * @param verbose Whether to include extra details per user.
     * @return A [CommandResult] describing the dry-run preview.
     */
    private fun buildDryRunResult(
        users: List<User>,
        format: String,
        verbose: Boolean
    ): CommandResult {
        val output = mutableListOf<OutputLine>()
        output.add(
            OutputLine(
                "[DRY-RUN] Se bo cam ${users.size} nguoi dung:",
                OutputStyle.WARNING
            )
        )
        output.add(OutputLine("", OutputStyle.NORMAL))

        if (format == "table") {
            if (verbose) {
                output.add(
                    OutputLine(
                        String.format("%-30s %-25s %-10s %-12s", "EMAIL", "TEN", "VAI TRO", "ID"),
                        OutputStyle.TABLE_HEADER
                    )
                )
                output.add(
                    OutputLine("-".repeat(80), OutputStyle.MUTED)
                )
                for (user in users) {
                    output.add(
                        OutputLine(
                            String.format(
                                "%-30s %-25s %-10s %-12s",
                                user.email.take(29),
                                user.displayName.take(24),
                                user.role.name,
                                user.id.take(11)
                            ),
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            } else {
                output.add(
                    OutputLine(
                        String.format("%-35s %-25s %-10s", "EMAIL", "TEN", "VAI TRO"),
                        OutputStyle.TABLE_HEADER
                    )
                )
                output.add(
                    OutputLine("-".repeat(72), OutputStyle.MUTED)
                )
                for (user in users) {
                    output.add(
                        OutputLine(
                            String.format(
                                "%-35s %-25s %-10s",
                                user.email.take(34),
                                user.displayName.take(24),
                                user.role.name
                            ),
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }
        } else {
            for ((index, user) in users.withIndex()) {
                val line = buildString {
                    append("  ${index + 1}. ${user.email}")
                    if (verbose) {
                        append(" (${user.displayName}, vai tro: ${user.role.name}, id: ${user.id})")
                    }
                }
                output.add(OutputLine(line, OutputStyle.INFO))
            }
        }

        output.add(OutputLine("", OutputStyle.NORMAL))
        output.add(
            OutputLine(
                "Su dung --confirm de thuc hien bo cam.",
                OutputStyle.MUTED
            )
        )

        return CommandResult.success(output)
    }
}
