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
 * Admin command to ban one or more users.
 *
 * Supports targeting by email, user ID, role, search query, or regex pattern.
 * Includes dry-run mode to preview which users would be affected without
 * actually performing the ban.
 *
 * Usage examples:
 * ```
 * ban user@example.com
 * ban --role USER --dry-run
 * ban --search "test" --confirm
 * ban user1@ex.com user2@ex.com --reason "Vi pham chinh sach"
 * ```
 */
class BanCommand : Command {

    override val name: String = "ban"

    override val aliases: List<String> = emptyList()

    override val description: String = "Chan nguoi dung theo email, ID hoac tieu chi loc"

    override val usage: String =
        "ban <email|id>... [--role <role>] [--search <query>] [--regex <pattern>] " +
                "[--dry-run] [--confirm] [--reason <ly_do>] [--verbose] [--quiet] [--format <format>]"

    override val requiredPermission: AdminPermission = AdminPermission.BAN_USERS

    override val isDestructive: Boolean = true

    override val minimumRole: UserRole = UserRole.ADMIN

    override val category: String = "admin"

    override val examples: List<Pair<String, String>> = listOf(
        "ban user@example.com" to "Chan mot nguoi dung theo email",
        "ban user@example.com --reason \"Spam\"" to "Chan nguoi dung voi ly do",
        "ban --role USER --dry-run" to "Xem truoc tat ca USER se bi chan",
        "ban --search test --confirm" to "Chan tat ca nguoi dung khop voi 'test'",
        "ban --regex \".*@temp\\.com\" --confirm" to "Chan nguoi dung khop voi regex",
        "ban user1@ex.com user2@ex.com --confirm" to "Chan nhieu nguoi dung cung luc"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--role" to "Loc theo vai tro",
            "--search" to "Tim kiem nguoi dung",
            "--regex" to "Loc theo regex",
            "--dry-run" to "Xem truoc ket qua",
            "--confirm" to "Bo qua xac nhan",
            "--reason" to "Ly do chan",
            "--verbose" to "Hien thi chi tiet",
            "--quiet" to "Chi hien thi so luong",
            "--format" to "Dinh dang ket qua (table|list|csv)"
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
            for (fmt in listOf("table", "list", "csv")) {
                suggestions.add(
                    CompletionSuggestion(
                        text = fmt,
                        description = "Dinh dang $fmt",
                        type = SuggestionType.ARGUMENT
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
        val isDryRun = "dry-run" in flags
        val isVerbose = "verbose" in flags
        val isQuiet = "quiet" in flags
        val confirm = "confirm" in flags
        val reason = flags["reason"]
        val format = flags["format"] ?: "table"
        val roleFilter = flags["role"]
        val searchQuery = flags["search"]
        val regexPattern = flags["regex"]

        if (!isDryRun && !confirm) {
            return CommandResult.error(
                "Thao tac huy diet: cam nguoi dung vinh vien. " +
                        "Su dung --confirm de xac nhan hoac --dry-run de mo phong."
            )
        }

        if (args.isEmpty() && roleFilter == null && searchQuery == null && regexPattern == null) {
            return CommandResult.error(
                "Vui long cung cap email/ID nguoi dung hoac tieu chi loc.\n" +
                        "Su dung: $usage"
            )
        }

        if (roleFilter != null) {
            try {
                UserRole.valueOf(roleFilter.uppercase())
            } catch (e: IllegalArgumentException) {
                return CommandResult.error(
                    "Vai tro khong hop le: $roleFilter. Cac vai tro hop le: ${UserRole.entries.joinToString(", ") { it.name }}"
                )
            }
        }

        if (regexPattern != null) {
            try {
                Regex(regexPattern, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                return CommandResult.error("Bieu thuc chinh quy khong hop le: $regexPattern")
            }
        }

        val allUsers: List<User> = try {
            context.repositories.adminRepository.getAllUsers().first()
        } catch (e: Exception) {
            return CommandResult.error("Khong the tai danh sach nguoi dung: ${e.message}")
        }

        val targetUsers = resolveTargetUsers(args, allUsers, roleFilter, searchQuery, regexPattern)

        if (targetUsers.isEmpty()) {
            return CommandResult.error("Khong tim thay nguoi dung nao khop voi tieu chi da cho.")
        }

        val alreadyBanned = targetUsers.filter { it.isBanned }
        val toBan = targetUsers.filter { !it.isBanned }

        val protectedUsers = toBan.filter { it.role == UserRole.SUPERUSER }
        val effectiveToBan = toBan.filter { it.role != UserRole.SUPERUSER }

        if (effectiveToBan.isEmpty() && alreadyBanned.isEmpty() && protectedUsers.isEmpty()) {
            return CommandResult.error("Khong tim thay nguoi dung nao khop voi tieu chi da cho.")
        }

        if (effectiveToBan.isEmpty()) {
            val lines = mutableListOf<OutputLine>()
            if (alreadyBanned.isNotEmpty()) {
                lines.add(
                    OutputLine(
                        "Da co ${alreadyBanned.size} nguoi dung bi chan truoc do.",
                        OutputStyle.WARNING
                    )
                )
            }
            if (protectedUsers.isNotEmpty()) {
                lines.add(
                    OutputLine(
                        "${protectedUsers.size} nguoi dung la SUPERUSER, khong the chan.",
                        OutputStyle.WARNING
                    )
                )
            }
            lines.add(OutputLine("Khong co nguoi dung nao can chan.", OutputStyle.INFO))
            return CommandResult.success(lines)
        }

        if (isDryRun) {
            return buildDryRunResult(effectiveToBan, alreadyBanned, protectedUsers, format, reason)
        }

        val lines = mutableListOf<OutputLine>()
        var successCount = 0
        var failCount = 0

        for (user in effectiveToBan) {
            val result = context.repositories.adminRepository.banUser(user.id)
            if (result.isSuccess) {
                successCount++
                if (isVerbose) {
                    lines.add(
                        OutputLine(
                            "  Da chan: ${user.email} (${user.displayName})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                failCount++
                if (!isQuiet) {
                    lines.add(
                        OutputLine(
                            "  Loi khi chan ${user.email}: ${result.exceptionOrNull()?.message ?: "Khong ro"}",
                            OutputStyle.ERROR
                        )
                    )
                }
            }
        }

        if (isQuiet) {
            return CommandResult.success("Da chan $successCount nguoi dung.")
        }

        val header = buildString {
            append("Ket qua chan nguoi dung: $successCount thanh cong")
            if (failCount > 0) append(", $failCount that bai")
            if (reason != null) append(" | Ly do: $reason")
        }
        lines.add(0, OutputLine(header, if (failCount > 0) OutputStyle.WARNING else OutputStyle.SUCCESS))

        if (alreadyBanned.isNotEmpty()) {
            lines.add(
                OutputLine(
                    "Bo qua ${alreadyBanned.size} nguoi dung da bi chan truoc do.",
                    OutputStyle.MUTED
                )
            )
        }
        if (protectedUsers.isNotEmpty()) {
            lines.add(
                OutputLine(
                    "Bo qua ${protectedUsers.size} SUPERUSER (khong the chan).",
                    OutputStyle.MUTED
                )
            )
        }

        return CommandResult(
            output = lines,
            isSuccess = failCount == 0,
            exitCode = if (failCount == 0) 0 else 1
        )
    }

    /**
     * Resolves the set of target users from positional arguments and filter flags.
     *
     * @param args Positional email or user-ID arguments.
     * @param allUsers Complete user list from the admin repository.
     * @param roleFilter Optional role name to filter by.
     * @param searchQuery Optional substring search on email/displayName/username.
     * @param regexPattern Optional regex pattern matched against email.
     * @return Distinct list of users matching any of the provided criteria.
     */
    private fun resolveTargetUsers(
        args: List<String>,
        allUsers: List<User>,
        roleFilter: String?,
        searchQuery: String?,
        regexPattern: String?
    ): List<User> {
        val matched = mutableSetOf<String>()
        val result = mutableListOf<User>()

        fun addUser(user: User) {
            if (user.id !in matched) {
                matched.add(user.id)
                result.add(user)
            }
        }

        for (arg in args) {
            val argLower = arg.lowercase()
            for (user in allUsers) {
                if (user.email.lowercase() == argLower || user.id == arg) {
                    addUser(user)
                }
            }
        }

        if (roleFilter != null) {
            val role = try {
                UserRole.valueOf(roleFilter.uppercase())
            } catch (_: IllegalArgumentException) {
                null // Already validated in execute()
            }
            if (role != null) {
                for (user in allUsers) {
                    if (user.role == role) addUser(user)
                }
            }
        }

        if (searchQuery != null) {
            val queryLower = searchQuery.lowercase()
            for (user in allUsers) {
                if (user.email.lowercase().contains(queryLower) ||
                    user.displayName.lowercase().contains(queryLower) ||
                    user.username.lowercase().contains(queryLower)
                ) {
                    addUser(user)
                }
            }
        }

        if (regexPattern != null) {
            val regex = try {
                Regex(regexPattern, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                null // Already validated in execute()
            }
            if (regex != null) {
                for (user in allUsers) {
                    if (regex.containsMatchIn(user.email)) addUser(user)
                }
            }
        }

        return result
    }

    /**
     * Builds the output for a dry-run preview showing which users would be banned.
     *
     * @param toBan Users that would be banned.
     * @param alreadyBanned Users skipped because they are already banned.
     * @param protectedUsers Superusers that cannot be banned.
     * @param format Output format: "table", "list", or "csv".
     * @param reason Optional ban reason.
     * @return A successful [CommandResult] with the preview output.
     */
    private fun buildDryRunResult(
        toBan: List<User>,
        alreadyBanned: List<User>,
        protectedUsers: List<User>,
        format: String,
        reason: String?
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("[DRY-RUN] Xem truoc lenh ban:", OutputStyle.HEADER))
        lines.add(
            OutputLine(
                "Se chan ${toBan.size} nguoi dung" +
                        if (reason != null) " | Ly do: $reason" else "",
                OutputStyle.INFO
            )
        )

        when (format) {
            "csv" -> {
                lines.add(OutputLine("email,displayName,role,id", OutputStyle.TABLE_HEADER))
                for (user in toBan) {
                    lines.add(
                        OutputLine(
                            "${user.email},${user.displayName},${user.role.name},${user.id}",
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }

            "list" -> {
                for (user in toBan) {
                    lines.add(
                        OutputLine(
                            "- ${user.email} (${user.displayName}) [${user.role.name}]",
                            OutputStyle.NORMAL
                        )
                    )
                }
            }

            else -> {
                lines.add(
                    OutputLine(
                        String.format("%-30s %-20s %-10s", "EMAIL", "TEN", "VAI TRO"),
                        OutputStyle.TABLE_HEADER
                    )
                )
                for (user in toBan) {
                    lines.add(
                        OutputLine(
                            String.format(
                                "%-30s %-20s %-10s",
                                user.email.take(28),
                                user.displayName.take(18),
                                user.role.name
                            ),
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }
        }

        if (alreadyBanned.isNotEmpty()) {
            lines.add(
                OutputLine(
                    "Bo qua ${alreadyBanned.size} nguoi dung da bi chan truoc do.",
                    OutputStyle.MUTED
                )
            )
        }
        if (protectedUsers.isNotEmpty()) {
            lines.add(
                OutputLine(
                    "Bo qua ${protectedUsers.size} SUPERUSER (khong the chan).",
                    OutputStyle.MUTED
                )
            )
        }

        lines.add(
            OutputLine(
                "Day la ban xem truoc. Them --confirm de thuc thi.",
                OutputStyle.WARNING
            )
        )

        return CommandResult.success(lines)
    }
}
