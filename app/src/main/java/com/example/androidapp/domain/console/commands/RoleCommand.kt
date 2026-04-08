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
 * Admin command to view and change user roles.
 *
 * Supports single-user role assignment (`role <email> <role>`) and bulk
 * operations (`role --from <oldRole> --to <newRole>`). Includes dry-run
 * mode for previewing changes without committing them.
 *
 * Only users with [UserRole.ADMIN] or higher **and** the
 * [AdminPermission.CHANGE_USER_ROLES] permission may execute this command.
 *
 * Usage examples:
 * ```
 * role user@example.com ADMIN
 * role --from USER --to ADMIN --dry-run
 * role --search "test" --to ADMIN
 * ```
 */
class RoleCommand : Command {

    override val name: String = "role"

    override val description: String =
        "Xem hoac thay doi vai tro (role) cua nguoi dung"

    override val usage: String =
        "role <email> <role> | role --from <oldRole> --to <newRole> [--search <query>] [--dry-run] [--confirm] [--verbose] [--format <format>]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.CHANGE_USER_ROLES

    override val isDestructive: Boolean = true

    override val examples: List<Pair<String, String>> = listOf(
        "role user@example.com ADMIN" to "Gan vai tro ADMIN cho nguoi dung",
        "role user@example.com USER" to "Ha cap nguoi dung ve USER",
        "role --from USER --to ADMIN --dry-run" to "Xem truoc chuyen tat ca USER thanh ADMIN",
        "role --from ADMIN --to USER --confirm" to "Chuyen tat ca ADMIN thanh USER (bo qua xac nhan)",
        "role --search \"test\" --to ADMIN" to "Gan ADMIN cho nguoi dung khop voi \"test\"",
        "role --search \"@company.com\" --to ADMIN --dry-run" to "Xem truoc gan ADMIN cho nguoi dung cua cong ty"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        // If user typed one arg already, suggest role names as second arg
        if (args.size == 1) {
            val validRoles = UserRole.entries.filter { it != UserRole.GUEST }
            validRoles.forEach { role ->
                suggestions.add(
                    CompletionSuggestion(
                        text = role.name,
                        description = "Vai tro ${role.name}",
                        type = SuggestionType.ARGUMENT
                    )
                )
            }
        }

        // Suggest flags
        if (args.isEmpty()) {
            val availableFlags = listOf(
                "--from" to "Vai tro nguon (bulk)",
                "--to" to "Vai tro dich (bulk)",
                "--search" to "Tim kiem nguoi dung",
                "--dry-run" to "Xem truoc thay doi",
                "--confirm" to "Bo qua xac nhan",
                "--verbose" to "Hien thi chi tiet",
                "--format" to "Dinh dang (table/list/csv)"
            )
            availableFlags.forEach { (flag, desc) ->
                if (flag !in flags) {
                    suggestions.add(
                        CompletionSuggestion(
                            text = flag,
                            description = desc,
                            type = SuggestionType.FLAG
                        )
                    )
                }
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
        val format = flags["format"] ?: "table"
        val searchQuery = flags["search"] ?: flags["s"]

        val fromRoleStr = flags["from"]
        val toRoleStr = flags["to"] ?: flags.getOrDefault("t", null)

        // Bulk mode: --from / --to
        if (fromRoleStr != null || (toRoleStr != null && args.isEmpty())) {
            return executeBulk(
                fromRoleStr = fromRoleStr,
                toRoleStr = toRoleStr,
                searchQuery = searchQuery,
                isDryRun = isDryRun,
                isVerbose = isVerbose,
                format = format,
                context = context
            )
        }

        // Single-user mode: role <email> <role>
        if (args.isEmpty()) {
            return CommandResult.error(
                "Thieu tham so. Su dung: role <email> <role> hoac role --from <role> --to <role>"
            )
        }

        val targetIdentifier = args[0]

        // If only one arg and no --to, show user's current role
        if (args.size < 2 && toRoleStr == null) {
            return showUserRole(targetIdentifier, isVerbose, context)
        }

        val newRoleStr = if (args.size >= 2) args[1] else toRoleStr
        if (newRoleStr == null) {
            return CommandResult.error(
                "Thieu vai tro dich. Su dung: role <email> <role>"
            )
        }

        val newRole = parseRole(newRoleStr)
            ?: return CommandResult.error(
                "Vai tro khong hop le: \"$newRoleStr\". " +
                        "Cac vai tro hop le: ${validRoleNames().joinToString(", ")}"
            )

        return executeSingle(
            targetIdentifier = targetIdentifier,
            newRole = newRole,
            isDryRun = isDryRun,
            isVerbose = isVerbose,
            context = context
        )
    }

    /**
     * Displays the current role of a single user.
     */
    private suspend fun showUserRole(
        identifier: String,
        isVerbose: Boolean,
        context: CommandContext
    ): CommandResult {
        val allUsers = context.repositories.adminRepository.getAllUsers().first()
        val user = findUser(allUsers, identifier)
            ?: return CommandResult.error(
                "Khong tim thay nguoi dung: \"$identifier\""
            )

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Thong tin vai tro", OutputStyle.HEADER))
        lines.add(OutputLine("  Nguoi dung : ${user.displayName} (${user.email})", OutputStyle.NORMAL))
        lines.add(OutputLine("  Vai tro    : ${user.role.name}", OutputStyle.INFO))

        if (isVerbose) {
            lines.add(OutputLine("  ID         : ${user.id}", OutputStyle.MUTED))
            lines.add(OutputLine("  Bi cam     : ${if (user.isBanned) "Co" else "Khong"}", OutputStyle.NORMAL))
            if (user.isAdmin()) {
                val perms = user.effectivePermissions()
                lines.add(
                    OutputLine(
                        "  Quyen      : ${if (perms.isEmpty()) "(khong co)" else perms.joinToString(", ") { it.name }}",
                        OutputStyle.NORMAL
                    )
                )
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Changes the role of a single user identified by email or user ID.
     */
    private suspend fun executeSingle(
        targetIdentifier: String,
        newRole: UserRole,
        isDryRun: Boolean,
        isVerbose: Boolean,
        context: CommandContext
    ): CommandResult {
        val allUsers = context.repositories.adminRepository.getAllUsers().first()
        val user = findUser(allUsers, targetIdentifier)
            ?: return CommandResult.error(
                "Khong tim thay nguoi dung: \"$targetIdentifier\""
            )

        // Prevent changing own role
        if (user.id == context.currentUser.id) {
            return CommandResult.error("Khong the thay doi vai tro cua chinh minh.")
        }

        // Prevent non-superusers from assigning SUPERUSER
        if (newRole == UserRole.SUPERUSER && !context.currentUser.isSuperuser()) {
            return CommandResult.error(
                "Chi SUPERUSER moi co the gan vai tro SUPERUSER cho nguoi khac."
            )
        }

        // Prevent non-superusers from changing a SUPERUSER's role
        if (user.isSuperuser() && !context.currentUser.isSuperuser()) {
            return CommandResult.error(
                "Khong the thay doi vai tro cua SUPERUSER."
            )
        }

        if (user.role == newRole) {
            return CommandResult.success(
                "Nguoi dung ${user.email} da co vai tro ${newRole.name} roi."
            )
        }

        val lines = mutableListOf<OutputLine>()

        if (isDryRun) {
            lines.add(OutputLine("[DRY-RUN] Se thay doi vai tro:", OutputStyle.WARNING))
            lines.add(
                OutputLine(
                    "  ${user.displayName} (${user.email}): ${user.role.name} -> ${newRole.name}",
                    OutputStyle.INFO
                )
            )
            lines.add(OutputLine("Su dung khong co --dry-run de thuc hien.", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        val result = context.repositories.adminRepository.updateUserRole(user.id, newRole)

        return result.fold(
            onSuccess = {
                lines.add(
                    OutputLine(
                        "Da thay doi vai tro cua ${user.email}: ${user.role.name} -> ${newRole.name}",
                        OutputStyle.SUCCESS
                    )
                )
                if (isVerbose) {
                    lines.add(OutputLine("  User ID: ${user.id}", OutputStyle.MUTED))
                    lines.add(OutputLine("  Ten    : ${user.displayName}", OutputStyle.MUTED))
                }
                CommandResult.success(lines)
            },
            onFailure = { error ->
                CommandResult.error(
                    "Loi khi thay doi vai tro cua ${user.email}: ${error.message ?: "Loi khong xac dinh"}"
                )
            }
        )
    }

    /**
     * Performs a bulk role change for all users matching the criteria.
     *
     * When `--from` is specified, only users with that role are affected.
     * When `--search` is specified, users are further filtered by the query.
     *
     * @param fromRoleStr Source role filter (optional for --search mode).
     * @param toRoleStr Target role (required).
     * @param searchQuery Text search filter (optional).
     * @param isDryRun If true, preview changes without committing.
     * @param isVerbose If true, show per-user detail lines.
     * @param format Output format: "table", "list", or "csv".
     * @param context The command execution context.
     * @return The result of the bulk operation.
     */
    private suspend fun executeBulk(
        fromRoleStr: String?,
        toRoleStr: String?,
        searchQuery: String?,
        isDryRun: Boolean,
        isVerbose: Boolean,
        format: String,
        context: CommandContext
    ): CommandResult {
        if (toRoleStr == null) {
            return CommandResult.error(
                "Thieu vai tro dich (--to). Su dung: role --from <role> --to <role>"
            )
        }

        val toRole = parseRole(toRoleStr)
            ?: return CommandResult.error(
                "Vai tro dich khong hop le: \"$toRoleStr\". " +
                        "Cac vai tro hop le: ${validRoleNames().joinToString(", ")}"
            )

        val fromRole = if (fromRoleStr != null) {
            parseRole(fromRoleStr)
                ?: return CommandResult.error(
                    "Vai tro nguon khong hop le: \"$fromRoleStr\". " +
                            "Cac vai tro hop le: ${validRoleNames().joinToString(", ")}"
                )
        } else {
            null
        }

        // Prevent non-superusers from assigning/removing SUPERUSER
        if (toRole == UserRole.SUPERUSER && !context.currentUser.isSuperuser()) {
            return CommandResult.error(
                "Chi SUPERUSER moi co the gan vai tro SUPERUSER."
            )
        }
        if (fromRole == UserRole.SUPERUSER && !context.currentUser.isSuperuser()) {
            return CommandResult.error(
                "Khong the thay doi vai tro cua SUPERUSER."
            )
        }

        val allUsers = context.repositories.adminRepository.getAllUsers().first()

        var candidates = allUsers.filter { it.id != context.currentUser.id }

        // Protect SUPERUSER accounts from accidental bulk role changes.
        // To change a SUPERUSER's role, target them explicitly by ID/email.
        if (fromRole == null) {
            candidates = candidates.filter { it.role != UserRole.SUPERUSER }
        }

        if (fromRole != null) {
            candidates = candidates.filter { it.role == fromRole }
        }

        if (searchQuery != null) {
            val query = searchQuery.lowercase()
            candidates = candidates.filter { user ->
                user.email.lowercase().contains(query) ||
                        user.displayName.lowercase().contains(query) ||
                        user.username.lowercase().contains(query)
            }
        }

        // Filter out users already at the target role
        candidates = candidates.filter { it.role != toRole }

        if (candidates.isEmpty()) {
            val filterDesc = buildString {
                if (fromRole != null) append("vai tro ${fromRole.name}")
                if (searchQuery != null) {
                    if (isNotEmpty()) append(", ")
                    append("tim kiem \"$searchQuery\"")
                }
            }
            return CommandResult.error(
                "Khong tim thay nguoi dung nao can thay doi" +
                        if (filterDesc.isNotEmpty()) " ($filterDesc)." else "."
            )
        }

        val lines = mutableListOf<OutputLine>()

        if (isDryRun) {
            lines.add(
                OutputLine(
                    "[DRY-RUN] Se thay doi vai tro cua ${candidates.size} nguoi dung sang ${toRole.name}:",
                    OutputStyle.WARNING
                )
            )
            lines.add(OutputLine(""))

            when (format.lowercase()) {
                "csv" -> formatCsv(candidates, toRole, lines)
                "list" -> formatList(candidates, toRole, lines, isVerbose)
                else -> formatTable(candidates, toRole, lines, isVerbose)
            }

            lines.add(OutputLine(""))
            lines.add(OutputLine("Su dung khong co --dry-run de thuc hien.", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        // Execute bulk change
        lines.add(
            OutputLine(
                "Dang thay doi vai tro cua ${candidates.size} nguoi dung sang ${toRole.name}...",
                OutputStyle.INFO
            )
        )

        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        for (user in candidates) {
            val result = context.repositories.adminRepository.updateUserRole(user.id, toRole)
            result.fold(
                onSuccess = {
                    successCount++
                    if (isVerbose) {
                        lines.add(
                            OutputLine(
                                "  [OK] ${user.email}: ${user.role.name} -> ${toRole.name}",
                                OutputStyle.SUCCESS
                            )
                        )
                    }
                },
                onFailure = { error ->
                    failCount++
                    val msg = "${user.email}: ${error.message ?: "Loi khong xac dinh"}"
                    errors.add(msg)
                    if (isVerbose) {
                        lines.add(OutputLine("  [LOI] $msg", OutputStyle.ERROR))
                    }
                }
            )
        }

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "Hoan tat: $successCount thanh cong, $failCount that bai (tong: ${candidates.size})",
                if (failCount == 0) OutputStyle.SUCCESS else OutputStyle.WARNING
            )
        )

        if (!isVerbose && errors.isNotEmpty()) {
            lines.add(OutputLine("Loi:", OutputStyle.ERROR))
            errors.forEach { lines.add(OutputLine("  - $it", OutputStyle.ERROR)) }
        }

        return CommandResult(
            output = lines,
            isSuccess = failCount == 0,
            exitCode = if (failCount == 0) 0 else 1
        )
    }

    // ==================== Formatting helpers ====================

    /**
     * Formats the candidate list as a table for dry-run output.
     */
    private fun formatTable(
        candidates: List<User>,
        toRole: UserRole,
        lines: MutableList<OutputLine>,
        isVerbose: Boolean
    ) {
        val header = if (isVerbose) {
            String.format("%-30s %-20s %-12s %-12s %-10s", "EMAIL", "TEN", "VAI TRO CU", "VAI TRO MOI", "BI CAM")
        } else {
            String.format("%-30s %-12s %-12s", "EMAIL", "VAI TRO CU", "VAI TRO MOI")
        }
        lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))

        for (user in candidates) {
            val row = if (isVerbose) {
                String.format(
                    "%-30s %-20s %-12s %-12s %-10s",
                    truncate(user.email, 28),
                    truncate(user.displayName, 18),
                    user.role.name,
                    toRole.name,
                    if (user.isBanned) "Co" else "Khong"
                )
            } else {
                String.format(
                    "%-30s %-12s %-12s",
                    truncate(user.email, 28),
                    user.role.name,
                    toRole.name
                )
            }
            lines.add(OutputLine(row, OutputStyle.TABLE_ROW))
        }
    }

    /**
     * Formats the candidate list as a simple list for dry-run output.
     */
    private fun formatList(
        candidates: List<User>,
        toRole: UserRole,
        lines: MutableList<OutputLine>,
        isVerbose: Boolean
    ) {
        for ((index, user) in candidates.withIndex()) {
            lines.add(
                OutputLine(
                    "${index + 1}. ${user.email} (${user.role.name} -> ${toRole.name})",
                    OutputStyle.NORMAL
                )
            )
            if (isVerbose) {
                lines.add(OutputLine("   Ten: ${user.displayName}", OutputStyle.MUTED))
                lines.add(OutputLine("   ID : ${user.id}", OutputStyle.MUTED))
            }
        }
    }

    /**
     * Formats the candidate list as CSV for dry-run output.
     */
    private fun formatCsv(
        candidates: List<User>,
        toRole: UserRole,
        lines: MutableList<OutputLine>
    ) {
        lines.add(OutputLine("email,display_name,old_role,new_role", OutputStyle.TABLE_HEADER))
        for (user in candidates) {
            lines.add(
                OutputLine(
                    "${user.email},${user.displayName},${user.role.name},${toRole.name}",
                    OutputStyle.TABLE_ROW
                )
            )
        }
    }

    // ==================== Utility helpers ====================

    /**
     * Finds a user by email, username, or user ID (case-insensitive for email/username).
     */
    private fun findUser(users: List<User>, identifier: String): User? {
        val lower = identifier.lowercase()
        return users.find { it.email.lowercase() == lower }
            ?: users.find { it.username.lowercase() == lower }
            ?: users.find { it.id == identifier }
    }

    /**
     * Parses a role string into a [UserRole], returning null if invalid.
     * Accepts case-insensitive names and rejects [UserRole.GUEST].
     */
    private fun parseRole(value: String): UserRole? {
        return try {
            val role = UserRole.valueOf(value.uppercase())
            if (role == UserRole.GUEST) null else role
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Returns the list of valid assignable role names (excludes GUEST).
     */
    private fun validRoleNames(): List<String> =
        UserRole.entries.filter { it != UserRole.GUEST }.map { it.name }

    /**
     * Truncates a string to [maxLen], appending ".." if truncated.
     */
    private fun truncate(text: String, maxLen: Int): String =
        if (text.length <= maxLen) text else text.take(maxLen - 2) + ".."
}
