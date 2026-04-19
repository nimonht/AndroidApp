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
 * Lenh quan ly quyen admin cho superuser.
 *
 * Cho phep xem, cap, thu hoi quyen admin cua nguoi dung.
 * Chi superuser moi co the thuc hien lenh nay.
 *
 * Cac che do hoat dong:
 * - `perm <email>` — Xem quyen hien tai cua nguoi dung
 * - `perm list` — Liet ke tat ca quyen co san
 * - `perm grant <email> <permission>` — Cap quyen cho nguoi dung
 * - `perm revoke <email> <permission>` — Thu hoi quyen cua nguoi dung
 * - `perm grant --all <email>` — Cap tat ca quyen
 * - `perm revoke --all <email>` — Thu hoi tat ca quyen
 */
class PermCommand : Command {

    override val name: String = "perm"

    override val aliases: List<String> = listOf("permissions")

    override val description: String = "Quan ly quyen admin cua nguoi dung (chi superuser)"

    override val usage: String =
        "perm [list | grant | revoke] [<email>] [<permission>] [--all] [--dry-run] [--verbose]"

    override val minimumRole: UserRole = UserRole.SUPERUSER

    override val category: String = "admin"

    override val isDestructive: Boolean = true

    override val examples: List<Pair<String, String>> = listOf(
        "perm admin@example.com" to "Xem quyen hien tai cua nguoi dung",
        "perm list" to "Liet ke tat ca quyen co san trong he thong",
        "perm grant admin@example.com MANAGE_USERS" to "Cap quyen MANAGE_USERS",
        "perm revoke admin@example.com BAN_USERS" to "Thu hoi quyen BAN_USERS",
        "perm grant --all admin@example.com" to "Cap tat ca quyen cho nguoi dung",
        "perm revoke --all admin@example.com" to "Thu hoi tat ca quyen cua nguoi dung",
        "perm grant admin@example.com MANAGE_USERS --dry-run" to "Xem truoc thay doi ma khong ap dung"
    )

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val subcommands = listOf("list", "grant", "revoke")

        return when {
            args.isEmpty() -> {
                subcommands.map { sub ->
                    CompletionSuggestion(
                        text = sub,
                        description = when (sub) {
                            "list" -> "Liet ke tat ca quyen"
                            "grant" -> "Cap quyen cho nguoi dung"
                            "revoke" -> "Thu hoi quyen cua nguoi dung"
                            else -> ""
                        },
                        type = SuggestionType.SUBCOMMAND
                    )
                }
            }

            args.size == 1 && args[0] !in subcommands -> {
                subcommands.filter { it.startsWith(args[0], ignoreCase = true) }.map { sub ->
                    CompletionSuggestion(
                        text = sub,
                        description = when (sub) {
                            "list" -> "Liet ke tat ca quyen"
                            "grant" -> "Cap quyen cho nguoi dung"
                            "revoke" -> "Thu hoi quyen cua nguoi dung"
                            else -> ""
                        },
                        type = SuggestionType.SUBCOMMAND
                    )
                }
            }

            args[0] in listOf("grant", "revoke") && args.size == 2 -> {
                emptyList()
            }

            args[0] in listOf("grant", "revoke") && args.size == 3 -> {
                val partial = args[2]
                AdminPermission.entries
                    .map { it.name }
                    .filter { it.startsWith(partial, ignoreCase = true) }
                    .map { perm ->
                        CompletionSuggestion(
                            text = perm,
                            description = describePermission(perm),
                            type = SuggestionType.ARGUMENT
                        )
                    }
            }

            else -> {
                listOf(
                    CompletionSuggestion("--all", description = "Tat ca quyen", type = SuggestionType.FLAG),
                    CompletionSuggestion("--dry-run", description = "Xem truoc thay doi", type = SuggestionType.FLAG),
                    CompletionSuggestion("--verbose", description = "Hien thi chi tiet", type = SuggestionType.FLAG)
                ).filter { it.text !in flags }
            }
        }
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error("Thieu tham so. Su dung: $usage")
        }

        val verbose = "verbose" in flags || "v" in flags
        val dryRun = "dry-run" in flags
        val grantAll = "all" in flags

        return when (args[0].lowercase()) {
            "list" -> executeList(verbose)
            "grant" -> executeGrant(args.drop(1), grantAll, dryRun, verbose, context)
            "revoke" -> executeRevoke(args.drop(1), grantAll, dryRun, verbose, context)
            else -> executeShow(args[0], verbose, context)
        }
    }

    /**
     * Liet ke tat ca quyen co san trong he thong.
     *
     * @param verbose Hien thi mo ta chi tiet cho moi quyen.
     * @return Ket qua chua danh sach quyen.
     */
    private fun executeList(verbose: Boolean): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Danh sach quyen admin:", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        AdminPermission.entries.forEach { perm ->
            if (verbose) {
                lines.add(OutputLine("  ${perm.name}", OutputStyle.INFO))
                lines.add(OutputLine("    ${describePermission(perm.name)}", OutputStyle.MUTED))
            } else {
                lines.add(OutputLine("  ${perm.name} - ${describePermission(perm.name)}", OutputStyle.TABLE_ROW))
            }
        }

        lines.add(OutputLine(""))
        lines.add(OutputLine("Tong cong: ${AdminPermission.entries.size} quyen", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Hien thi quyen hien tai cua mot nguoi dung.
     *
     * @param identifier Email hoac ID cua nguoi dung can xem.
     * @param verbose Hien thi them thong tin chi tiet.
     * @param context Ngu canh lenh chua repository.
     * @return Ket qua chua thong tin quyen nguoi dung.
     */
    private suspend fun executeShow(
        identifier: String,
        verbose: Boolean,
        context: CommandContext
    ): CommandResult {
        val user = findUser(identifier, context)
            ?: return CommandResult.error("Khong tim thay nguoi dung: $identifier")

        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("Thong tin quyen - ${user.displayName} (${user.email}):", OutputStyle.HEADER))
        lines.add(OutputLine("  Vai tro: ${user.role.name}", OutputStyle.INFO))

        when (user.role) {
            UserRole.SUPERUSER -> {
                lines.add(OutputLine("  Trang thai: Co tat ca quyen (Superuser)", OutputStyle.SUCCESS))
                if (verbose) {
                    lines.add(OutputLine(""))
                    lines.add(OutputLine("  Quyen hieu luc:", OutputStyle.INFO))
                    AdminPermission.entries.forEach { perm ->
                        lines.add(OutputLine("    [x] ${perm.name}", OutputStyle.SUCCESS))
                    }
                }
            }

            UserRole.ADMIN -> {
                val effective = user.effectivePermissions()
                lines.add(
                    OutputLine(
                        "  Quyen duoc cap: ${effective.size}/${AdminPermission.entries.size}",
                        OutputStyle.INFO
                    )
                )
                lines.add(OutputLine(""))

                if (effective.isEmpty()) {
                    lines.add(OutputLine("  Chua co quyen nao duoc cap.", OutputStyle.WARNING))
                } else {
                    lines.add(OutputLine("  Quyen hien tai:", OutputStyle.INFO))
                    AdminPermission.entries.forEach { perm ->
                        if (perm in effective) {
                            lines.add(OutputLine("    [x] ${perm.name}", OutputStyle.SUCCESS))
                        } else if (verbose) {
                            lines.add(OutputLine("    [ ] ${perm.name}", OutputStyle.MUTED))
                        }
                    }
                }
            }

            else -> {
                lines.add(OutputLine("  Nguoi dung khong phai admin, khong co quyen admin.", OutputStyle.WARNING))
                if (verbose) {
                    lines.add(OutputLine("  Hay nang vai tro len ADMIN truoc khi cap quyen.", OutputStyle.MUTED))
                }
            }
        }

        return CommandResult.success(lines)
    }

    /**
     * Cap quyen admin cho nguoi dung.
     *
     * @param subArgs Tham so con (email va ten quyen).
     * @param grantAll Cap tat ca quyen neu true.
     * @param dryRun Chi xem truoc thay doi ma khong ap dung.
     * @param verbose Hien thi chi tiet.
     * @param context Ngu canh lenh chua repository.
     * @return Ket qua thao tac.
     */
    private suspend fun executeGrant(
        subArgs: List<String>,
        grantAll: Boolean,
        dryRun: Boolean,
        verbose: Boolean,
        context: CommandContext
    ): CommandResult {
        if (subArgs.isEmpty()) {
            return CommandResult.error("Thieu email nguoi dung. Su dung: perm grant <email> <permission>")
        }

        val identifier = subArgs[0]
        val user = findUser(identifier, context)
            ?: return CommandResult.error("Khong tim thay nguoi dung: $identifier")

        if (user.role == UserRole.SUPERUSER) {
            return CommandResult.error("Khong the thay doi quyen cua superuser.")
        }

        if (user.role != UserRole.ADMIN) {
            return CommandResult.error(
                "Nguoi dung '${user.email}' co vai tro ${user.role.name}. " +
                        "Chi co the cap quyen cho nguoi dung co vai tro ADMIN."
            )
        }

        val currentPerms = user.permissions.toMutableSet()

        val permissionsToGrant: Set<AdminPermission> = if (grantAll) {
            AdminPermission.entries.toSet()
        } else {
            if (subArgs.size < 2) {
                return CommandResult.error(
                    "Thieu ten quyen. Su dung: perm grant <email> <permission>\n" +
                            "Hoac dung --all de cap tat ca quyen."
                )
            }
            val permName = subArgs[1].uppercase()
            val perm = AdminPermission.fromString(permName)
                ?: return CommandResult.error(
                    "Quyen khong hop le: $permName\n" +
                            "Cac quyen hop le: ${AdminPermission.entries.joinToString(", ") { it.name }}"
                )
            setOf(perm)
        }

        val alreadyGranted = permissionsToGrant.filter { it in currentPerms }
        val newlyGranted = permissionsToGrant.filter { it !in currentPerms }

        if (newlyGranted.isEmpty()) {
            val lines = mutableListOf<OutputLine>()
            lines.add(OutputLine("Khong co thay doi.", OutputStyle.WARNING))
            if (alreadyGranted.isNotEmpty()) {
                lines.add(
                    OutputLine(
                        "Nguoi dung da co quyen: ${alreadyGranted.joinToString(", ") { it.name }}",
                        OutputStyle.MUTED
                    )
                )
            }
            return CommandResult.success(lines)
        }

        val updatedPerms = currentPerms + newlyGranted

        if (dryRun) {
            return buildDryRunResult("CAP QUYEN", user, newlyGranted, alreadyGranted, updatedPerms, verbose)
        }

        val result = context.repositories.adminRepository.updateAdminPermissions(
            user.id,
            updatedPerms
        )

        return if (result.isSuccess) {
            val lines = mutableListOf<OutputLine>()
            lines.add(
                OutputLine(
                    "Da cap ${newlyGranted.size} quyen cho ${user.email}.",
                    OutputStyle.SUCCESS
                )
            )
            if (verbose) {
                newlyGranted.forEach { perm ->
                    lines.add(OutputLine("  + ${perm.name}", OutputStyle.SUCCESS))
                }
                if (alreadyGranted.isNotEmpty()) {
                    lines.add(OutputLine(""))
                    lines.add(
                        OutputLine(
                            "Bo qua ${alreadyGranted.size} quyen da co:",
                            OutputStyle.MUTED
                        )
                    )
                    alreadyGranted.forEach { perm ->
                        lines.add(OutputLine("    ${perm.name}", OutputStyle.MUTED))
                    }
                }
                lines.add(OutputLine(""))
                lines.add(
                    OutputLine(
                        "Tong quyen hien tai: ${updatedPerms.size}/${AdminPermission.entries.size}",
                        OutputStyle.INFO
                    )
                )
            }
            CommandResult.success(lines)
        } else {
            CommandResult.error(
                "Loi khi cap quyen: ${result.exceptionOrNull()?.message ?: "Khong xac dinh"}"
            )
        }
    }

    /**
     * Thu hoi quyen admin cua nguoi dung.
     *
     * @param subArgs Tham so con (email va ten quyen).
     * @param revokeAll Thu hoi tat ca quyen neu true.
     * @param dryRun Chi xem truoc thay doi ma khong ap dung.
     * @param verbose Hien thi chi tiet.
     * @param context Ngu canh lenh chua repository.
     * @return Ket qua thao tac.
     */
    private suspend fun executeRevoke(
        subArgs: List<String>,
        revokeAll: Boolean,
        dryRun: Boolean,
        verbose: Boolean,
        context: CommandContext
    ): CommandResult {
        if (subArgs.isEmpty()) {
            return CommandResult.error("Thieu email nguoi dung. Su dung: perm revoke <email> <permission>")
        }

        val identifier = subArgs[0]
        val user = findUser(identifier, context)
            ?: return CommandResult.error("Khong tim thay nguoi dung: $identifier")

        if (user.role == UserRole.SUPERUSER) {
            return CommandResult.error("Khong the thay doi quyen cua superuser.")
        }

        if (user.role != UserRole.ADMIN) {
            return CommandResult.error(
                "Nguoi dung '${user.email}' co vai tro ${user.role.name}. " +
                        "Chi co the thu hoi quyen cua nguoi dung co vai tro ADMIN."
            )
        }

        val currentPerms = user.permissions.toMutableSet()

        val permissionsToRevoke: Set<AdminPermission> = if (revokeAll) {
            currentPerms.toSet()
        } else {
            if (subArgs.size < 2) {
                return CommandResult.error(
                    "Thieu ten quyen. Su dung: perm revoke <email> <permission>\n" +
                            "Hoac dung --all de thu hoi tat ca quyen."
                )
            }
            val permName = subArgs[1].uppercase()
            val perm = AdminPermission.fromString(permName)
                ?: return CommandResult.error(
                    "Quyen khong hop le: $permName\n" +
                            "Cac quyen hop le: ${AdminPermission.entries.joinToString(", ") { it.name }}"
                )
            setOf(perm)
        }

        val actuallyRevoked = permissionsToRevoke.filter { it in currentPerms }
        val notPresent = permissionsToRevoke.filter { it !in currentPerms }

        if (actuallyRevoked.isEmpty()) {
            val lines = mutableListOf<OutputLine>()
            lines.add(OutputLine("Khong co thay doi.", OutputStyle.WARNING))
            if (notPresent.isNotEmpty()) {
                lines.add(
                    OutputLine(
                        "Nguoi dung khong co quyen: ${notPresent.joinToString(", ") { it.name }}",
                        OutputStyle.MUTED
                    )
                )
            }
            return CommandResult.success(lines)
        }

        val updatedPerms = currentPerms - permissionsToRevoke

        if (dryRun) {
            return buildDryRunResult("THU HOI QUYEN", user, actuallyRevoked, notPresent, updatedPerms, verbose)
        }

        val result = context.repositories.adminRepository.updateAdminPermissions(
            user.id,
            updatedPerms
        )

        return if (result.isSuccess) {
            val lines = mutableListOf<OutputLine>()
            lines.add(
                OutputLine(
                    "Da thu hoi ${actuallyRevoked.size} quyen cua ${user.email}.",
                    OutputStyle.SUCCESS
                )
            )
            if (verbose) {
                actuallyRevoked.forEach { perm ->
                    lines.add(OutputLine("  - ${perm.name}", OutputStyle.WARNING))
                }
                if (notPresent.isNotEmpty()) {
                    lines.add(OutputLine(""))
                    lines.add(
                        OutputLine(
                            "Bo qua ${notPresent.size} quyen khong ton tai:",
                            OutputStyle.MUTED
                        )
                    )
                    notPresent.forEach { perm ->
                        lines.add(OutputLine("    ${perm.name}", OutputStyle.MUTED))
                    }
                }
                lines.add(OutputLine(""))
                lines.add(
                    OutputLine(
                        "Tong quyen con lai: ${updatedPerms.size}/${AdminPermission.entries.size}",
                        OutputStyle.INFO
                    )
                )
            }
            CommandResult.success(lines)
        } else {
            CommandResult.error(
                "Loi khi thu hoi quyen: ${result.exceptionOrNull()?.message ?: "Khong xac dinh"}"
            )
        }
    }

    /**
     * Tao ket qua xem truoc (dry-run) cho thao tac cap/thu hoi quyen.
     *
     * @param actionLabel Nhan hanh dong (vd: "CAP QUYEN", "THU HOI QUYEN").
     * @param user Nguoi dung muc tieu.
     * @param affected Danh sach quyen bi anh huong.
     * @param skipped Danh sach quyen bi bo qua.
     * @param finalPerms Tap quyen sau khi ap dung thay doi.
     * @param verbose Hien thi chi tiet.
     * @return Ket qua xem truoc.
     */
    private fun buildDryRunResult(
        actionLabel: String,
        user: User,
        affected: List<AdminPermission>,
        skipped: List<AdminPermission>,
        finalPerms: Set<AdminPermission>,
        verbose: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[DRY-RUN] $actionLabel - ${user.email}", OutputStyle.WARNING))
        lines.add(OutputLine(""))

        lines.add(OutputLine("Quyen bi anh huong (${affected.size}):", OutputStyle.INFO))
        affected.forEach { perm ->
            val symbol = if (actionLabel.contains("CAP")) "+" else "-"
            val style = if (actionLabel.contains("CAP")) OutputStyle.SUCCESS else OutputStyle.WARNING
            lines.add(OutputLine("  $symbol ${perm.name}", style))
        }

        if (skipped.isNotEmpty()) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("Bo qua (${skipped.size}):", OutputStyle.MUTED))
            skipped.forEach { perm ->
                lines.add(OutputLine("    ${perm.name}", OutputStyle.MUTED))
            }
        }

        if (verbose) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("Quyen sau khi ap dung:", OutputStyle.HEADER))
            AdminPermission.entries.forEach { perm ->
                if (perm in finalPerms) {
                    lines.add(OutputLine("  [x] ${perm.name}", OutputStyle.SUCCESS))
                } else {
                    lines.add(OutputLine("  [ ] ${perm.name}", OutputStyle.MUTED))
                }
            }
        }

        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "Tong quyen sau thay doi: ${finalPerms.size}/${AdminPermission.entries.size}",
                OutputStyle.INFO
            )
        )
        lines.add(OutputLine("Khong co thay doi nao duoc ap dung (dry-run).", OutputStyle.MUTED))
        return CommandResult.success(lines)
    }

    /**
     * Tim nguoi dung theo email hoac ID tu danh sach tat ca nguoi dung.
     *
     * @param identifier Email hoac ID cua nguoi dung can tim.
     * @param context Ngu canh lenh chua repository.
     * @return [User] neu tim thay, null neu khong.
     */
    private suspend fun findUser(identifier: String, context: CommandContext): User? {
        val allUsers = context.repositories.adminRepository.getAllUsers().first()
        return allUsers.find { user ->
            user.email.equals(identifier, ignoreCase = true) ||
                    user.id == identifier ||
                    user.username.equals(identifier, ignoreCase = true)
        }
    }


    /**
     * Tra ve mo ta tieng Viet cho mot quyen admin.
     *
     * @param permName Ten quyen.
     * @return Mo ta ngan gon.
     */
    private fun describePermission(permName: String): String {
        return when (permName) {
            "MANAGE_USERS" -> "Quan ly nguoi dung (xem, tim kiem)"
            "CHANGE_USER_ROLES" -> "Thay doi vai tro nguoi dung"
            "DELETE_USERS" -> "Xoa nguoi dung vinh vien"
            "BAN_USERS" -> "Chan/bo chan nguoi dung"
            "MANAGE_QUIZZES" -> "Quan ly quiz (xem, tim kiem)"
            "DELETE_QUIZZES" -> "Xoa quiz vinh vien"
            "PUBLISH_QUIZZES" -> "Xuat ban/go xuat ban quiz"
            "VIEW_REPORTS" -> "Xem bao cao va thong ke he thong"
            else -> "Khong co mo ta"
        }
    }
}
