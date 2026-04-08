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
 * Lenh noi bo `del-user` — xoa nguoi dung vinh vien.
 *
 * Duoc goi tu [DeleteCommand] khi co flag `-u`/`--user` hoac khi doi so
 * dau tien khop email. Ho tro xoa theo email hoac ID, loc chi nguoi dung
 * bi cam, chay thu (dry-run), va xac nhan truoc khi thuc hien.
 *
 * Cac flag ho tro:
 * - `--with-data`: ghi nhan yeu cau xoa ca du lieu lien quan (quiz, attempt).
 * - `--banned-only`: chi xoa nguoi dung da bi cam.
 * - `--dry-run`: mo phong thao tac, khong thuc su xoa.
 * - `--confirm`: xac nhan thao tac huy diet (bat buoc neu khong co dry-run).
 * - `--format <table|json>`: dinh dang dau ra.
 * - `--verbose`: hien thi chi tiet.
 * - `--quiet`: chi hien thi ket qua tom tat.
 */
class DeleteUserCommand : Command {

    override val name: String = "del-user"

    override val description: String = "Xoa vinh vien nguoi dung theo email hoac ID"

    override val usage: String =
        "del -u <email|id> [...] [--with-data] [--banned-only] [--dry-run] [--confirm] [--format <table|json>] [--verbose] [--quiet]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    override val requiredPermission: AdminPermission = AdminPermission.DELETE_USERS

    override val isDestructive: Boolean = true

    override val examples: List<Pair<String, String>> = listOf(
        "del -u user@example.com --confirm" to "Xoa nguoi dung theo email",
        "del -u abc123 def456 --confirm" to "Xoa nhieu nguoi dung theo ID",
        "del -u user@example.com --dry-run" to "Mo phong xoa (khong thuc su xoa)",
        "del -u --banned-only --confirm" to "Xoa tat ca nguoi dung bi cam",
        "del -u user@example.com --with-data --confirm" to "Xoa nguoi dung va ghi nhan xoa du lieu lien quan",
        "del -u user@example.com --format json --verbose" to "Xoa va hien thi ket qua dang JSON chi tiet"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val availableFlags = listOf(
            "--with-data" to "Ghi nhan xoa du lieu lien quan (quiz, attempt)",
            "--banned-only" to "Chi xoa nguoi dung da bi cam",
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
        val withData = "with-data" in flags
        val bannedOnly = "banned-only" in flags
        val verbose = "verbose" in flags || "v" in flags
        val quiet = "quiet" in flags || "q" in flags
        val format = flags["format"]?.lowercase() ?: "table"

        if (!dryRun && !confirm) {
            return CommandResult.error(
                "Thao tac huy diet: xoa nguoi dung vinh vien. " +
                        "Su dung --confirm de xac nhan hoac --dry-run de mo phong."
            )
        }

        val adminRepo = context.repositories.adminRepository

        val usersToDelete = mutableListOf<User>()

        if (bannedOnly && args.isEmpty()) {
            val allUsers = adminRepo.getAllUsers().first()
            usersToDelete.addAll(allUsers.filter { it.isBanned })
            if (usersToDelete.isEmpty()) {
                return CommandResult.success("Khong tim thay nguoi dung bi cam nao.")
            }
        } else if (args.isEmpty()) {
            return CommandResult.error(
                "Vui long cung cap email hoac ID nguoi dung can xoa, " +
                        "hoac su dung --banned-only de xoa tat ca nguoi dung bi cam."
            )
        } else {
            val allUsers = adminRepo.getAllUsers().first()
            for (identifier in args) {
                val user = findUser(allUsers, identifier)
                if (user == null) {
                    if (!quiet) {
                        return CommandResult.error("Khong tim thay nguoi dung: '$identifier'")
                    }
                    continue
                }
                if (bannedOnly && !user.isBanned) {
                    if (!quiet) {
                        return CommandResult.error(
                            "Nguoi dung '${user.displayName}' chua bi cam. " +
                                    "Su dung --banned-only chi ap dung cho nguoi dung bi cam."
                        )
                    }
                    continue
                }
                if (user.role == UserRole.SUPERUSER) {
                    return CommandResult.error(
                        "Khong the xoa tai khoan superuser '${user.displayName}'."
                    )
                }
                if (user.id == context.currentUser.id) {
                    return CommandResult.error("Khong the tu xoa tai khoan cua ban.")
                }
                usersToDelete.add(user)
            }
        }

        if (usersToDelete.isEmpty()) {
            return CommandResult.success("Khong co nguoi dung nao can xoa.")
        }

        if (dryRun) {
            return buildDryRunOutput(usersToDelete, withData, verbose, quiet, format)
        }

        return executeDelete(usersToDelete, withData, verbose, quiet, format, adminRepo)
    }

    /**
     * Tim nguoi dung theo email hoac ID trong danh sach.
     *
     * @param users Danh sach nguoi dung.
     * @param identifier Email hoac ID can tim.
     * @return [User] neu tim thay, null neu khong.
     */
    private fun findUser(users: List<User>, identifier: String): User? {
        return users.find { it.email.equals(identifier, ignoreCase = true) }
            ?: users.find { it.id == identifier }
            ?: users.find { it.username.equals(identifier, ignoreCase = true) }
    }

    /**
     * Xay dung dau ra mo phong (dry-run) cho thao tac xoa.
     */
    private fun buildDryRunOutput(
        users: List<User>,
        withData: Boolean,
        verbose: Boolean,
        quiet: Boolean,
        format: String
    ): CommandResult {
        if (format == "json") {
            return buildDryRunJson(users, withData)
        }

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[DRY-RUN] Mo phong xoa nguoi dung", OutputStyle.WARNING))
        lines.add(OutputLine(""))

        if (!quiet) {
            lines.add(
                OutputLine(
                    CommandFormatUtils.padRight("Email", 30) + CommandFormatUtils.padRight(
                        "Ten",
                        20
                    ) + CommandFormatUtils.padRight("Vai tro", 15) +
                            CommandFormatUtils.padRight("Trang thai", 12),
                    OutputStyle.TABLE_HEADER
                )
            )

            for (user in users) {
                val status = if (user.isBanned) "Bi cam" else "Hoat dong"
                val roleName = formatRole(user.role)
                lines.add(
                    OutputLine(
                        CommandFormatUtils.padRight(user.email, 30) + CommandFormatUtils.padRight(
                            user.displayName,
                            20
                        ) +
                                CommandFormatUtils.padRight(roleName, 15) + CommandFormatUtils.padRight(status, 12),
                        OutputStyle.TABLE_ROW
                    )
                )

                if (verbose) {
                    lines.add(OutputLine("  ID: ${user.id}", OutputStyle.MUTED))
                    if (user.username.isNotBlank()) {
                        lines.add(OutputLine("  Username: @${user.username}", OutputStyle.MUTED))
                    }
                }
            }
        }

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "[DRY-RUN] Se xoa ${users.size} nguoi dung." +
                        if (withData) " (bao gom du lieu lien quan)" else "",
                OutputStyle.WARNING
            )
        )
        lines.add(OutputLine("Them --confirm va bo --dry-run de thuc hien.", OutputStyle.MUTED))

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra dry-run dinh dang JSON.
     */
    private fun buildDryRunJson(users: List<User>, withData: Boolean): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"dryRun\": true,", OutputStyle.CODE))
        lines.add(OutputLine("  \"withData\": $withData,", OutputStyle.CODE))
        lines.add(OutputLine("  \"count\": ${users.size},", OutputStyle.CODE))
        lines.add(OutputLine("  \"users\": [", OutputStyle.CODE))

        for ((index, user) in users.withIndex()) {
            val comma = if (index < users.size - 1) "," else ""
            lines.add(OutputLine("    {", OutputStyle.CODE))
            lines.add(OutputLine("      \"id\": \"${CommandFormatUtils.escapeJson(user.id)}\",", OutputStyle.CODE))
            lines.add(
                OutputLine(
                    "      \"email\": \"${CommandFormatUtils.escapeJson(user.email)}\",",
                    OutputStyle.CODE
                )
            )
            lines.add(
                OutputLine(
                    "      \"displayName\": \"${CommandFormatUtils.escapeJson(user.displayName)}\",",
                    OutputStyle.CODE
                )
            )
            lines.add(OutputLine("      \"role\": \"${user.role.name}\",", OutputStyle.CODE))
            lines.add(OutputLine("      \"isBanned\": ${user.isBanned}", OutputStyle.CODE))
            lines.add(OutputLine("    }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("  ]", OutputStyle.CODE))
        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Thuc hien xoa nguoi dung that su.
     */
    private suspend fun executeDelete(
        users: List<User>,
        withData: Boolean,
        verbose: Boolean,
        quiet: Boolean,
        format: String,
        adminRepo: com.example.androidapp.domain.repository.AdminRepository
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        for (user in users) {
            if (verbose && !quiet) {
                lines.add(
                    OutputLine("Dang xoa: ${user.email} (${user.id})...", OutputStyle.INFO)
                )
            }

            if (withData && verbose && !quiet) {
                lines.add(
                    OutputLine(
                        "  Ghi nhan: du lieu lien quan (quiz, attempt) se duoc xu ly boi backend.",
                        OutputStyle.MUTED
                    )
                )
            }

            val result = adminRepo.deleteUserPermanently(user.id)
            if (result.isSuccess) {
                successCount++
                if (!quiet) {
                    lines.add(
                        OutputLine(
                            "Da xoa: ${user.email} (${user.displayName})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                failCount++
                val errorMsg = result.exceptionOrNull()?.message ?: "Loi khong xac dinh"
                errors.add("${user.email}: $errorMsg")
                if (!quiet) {
                    lines.add(
                        OutputLine(
                            "Loi khi xoa ${user.email}: $errorMsg",
                            OutputStyle.ERROR
                        )
                    )
                }
            }
        }

        if (format == "json") {
            return buildDeleteResultJson(users.size, successCount, failCount, errors, withData)
        }

        lines.add(OutputLine(""))
        lines.add(OutputLine("== Ket qua xoa nguoi dung ==", OutputStyle.HEADER))
        lines.add(OutputLine("  Tong so     : ${users.size}", OutputStyle.NORMAL))
        lines.add(OutputLine("  Thanh cong  : $successCount", OutputStyle.SUCCESS))

        if (failCount > 0) {
            lines.add(OutputLine("  That bai    : $failCount", OutputStyle.ERROR))
        }

        if (withData) {
            lines.add(
                OutputLine(
                    "  Ghi chu: Du lieu lien quan duoc xu ly boi backend (cascade delete).",
                    OutputStyle.MUTED
                )
            )
        }

        val isSuccess = failCount == 0
        return CommandResult(output = lines, isSuccess = isSuccess, exitCode = if (isSuccess) 0 else 1)
    }

    /**
     * Xay dung ket qua xoa dinh dang JSON.
     */
    private fun buildDeleteResultJson(
        total: Int,
        success: Int,
        failed: Int,
        errors: List<String>,
        withData: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"operation\": \"deleteUsers\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"total\": $total,", OutputStyle.CODE))
        lines.add(OutputLine("  \"success\": $success,", OutputStyle.CODE))
        lines.add(OutputLine("  \"failed\": $failed,", OutputStyle.CODE))
        lines.add(OutputLine("  \"withData\": $withData,", OutputStyle.CODE))

        if (errors.isNotEmpty()) {
            lines.add(OutputLine("  \"errors\": [", OutputStyle.CODE))
            for ((index, err) in errors.withIndex()) {
                val comma = if (index < errors.size - 1) "," else ""
                lines.add(OutputLine("    \"${CommandFormatUtils.escapeJson(err)}\"$comma", OutputStyle.CODE))
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
     * Dinh dang ten vai tro sang tieng Viet.
     */
    private fun formatRole(role: UserRole): String = when (role) {
        UserRole.GUEST -> "Khach"
        UserRole.USER -> "Nguoi dung"
        UserRole.ADMIN -> "Quan tri"
        UserRole.SUPERUSER -> "Sieu QT"
    }
}
