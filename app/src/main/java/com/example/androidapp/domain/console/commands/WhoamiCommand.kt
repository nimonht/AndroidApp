package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.UserRole

/**
 * Lenh `whoami` — hien thi thong tin nguoi dung hien tai.
 *
 * Hien thi ten, email, vai tro, quyen han va cac chi tiet tai khoan.
 * Ho tro dinh dang dau ra theo bang hoac JSON.
 */
class WhoamiCommand : Command {

    override val name: String = "whoami"

    override val aliases: List<String> = listOf("me", "user")

    override val description: String = "Hien thi thong tin nguoi dung hien tai"

    override val usage: String = "whoami [--verbose] [--permissions] [--format <table|json>] [--session]"

    override val category: String = "system"

    override val examples: List<Pair<String, String>> = listOf(
        "whoami" to "Hien thi thong tin co ban cua nguoi dung",
        "whoami --verbose" to "Hien thi chi tiet day du tai khoan",
        "whoami --permissions" to "Hien thi ma tran quyen han",
        "whoami --format json" to "Xuat thong tin duoi dang JSON",
        "whoami --session" to "Hien thi thong tin phien dang nhap"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()
        val availableFlags = listOf(
            "--verbose" to "Hien thi chi tiet day du",
            "--permissions" to "Hien thi ma tran quyen han",
            "--format" to "Dinh dang dau ra (table/json)",
            "--session" to "Hien thi thong tin phien dang nhap"
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
            suggestions.add(CompletionSuggestion(text = "table", description = "Dinh dang bang", type = SuggestionType.ARGUMENT))
            suggestions.add(CompletionSuggestion(text = "json", description = "Dinh dang JSON", type = SuggestionType.ARGUMENT))
        }

        return suggestions
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val user = context.currentUser
        val isVerbose = "verbose" in flags || "v" in flags
        val showPermissions = "permissions" in flags || "p" in flags
        val showSession = "session" in flags
        val format = flags["format"] ?: "table"

        return when (format.lowercase()) {
            "json" -> buildJsonOutput(user, isVerbose, showPermissions, showSession, context)
            else -> buildTableOutput(user, isVerbose, showPermissions, showSession, context)
        }
    }

    /**
     * Xay dung dau ra dinh dang bang.
     */
    private fun buildTableOutput(
        user: com.example.androidapp.domain.model.User,
        verbose: Boolean,
        showPermissions: Boolean,
        showSession: Boolean,
        context: CommandContext
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("== Thong tin nguoi dung ==", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        val roleBadge = formatRoleBadge(user.role)
        lines.add(OutputLine("  Ten hien thi : ${user.displayName}", OutputStyle.NORMAL))
        lines.add(OutputLine("  Email        : ${user.email}", OutputStyle.NORMAL))

        if (user.username.isNotBlank()) {
            lines.add(OutputLine("  Ten nguoi dung: @${user.username}", OutputStyle.NORMAL))
        }

        lines.add(OutputLine("  Vai tro      : $roleBadge", OutputStyle.INFO))

        if (user.isBanned) {
            lines.add(OutputLine("  Trang thai   : BI CAM", OutputStyle.ERROR))
        } else {
            lines.add(OutputLine("  Trang thai   : Hoat dong", OutputStyle.SUCCESS))
        }

        if (verbose) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("-- Chi tiet tai khoan --", OutputStyle.HEADER))
            lines.add(OutputLine("  ID           : ${user.id}", OutputStyle.MUTED))
            lines.add(OutputLine("  Anh dai dien : ${user.photoUrl ?: "(khong co)"}", OutputStyle.MUTED))
            lines.add(OutputLine("  La admin     : ${if (user.isAdmin()) "Co" else "Khong"}", OutputStyle.NORMAL))
            lines.add(OutputLine("  La superuser : ${if (user.isSuperuser()) "Co" else "Khong"}", OutputStyle.NORMAL))

            val effectivePerms = user.effectivePermissions()
            lines.add(OutputLine("  So quyen     : ${effectivePerms.size}/${AdminPermission.entries.size}", OutputStyle.NORMAL))
        }

        if (showPermissions) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("-- Ma tran quyen han --", OutputStyle.HEADER))

            if (user.isSuperuser()) {
                lines.add(OutputLine("  * Superuser: co TAT CA quyen han", OutputStyle.SUCCESS))
                lines.add(OutputLine(""))
            }

            val effectivePerms = user.effectivePermissions()
            val maxLen = AdminPermission.entries.maxOfOrNull { formatPermissionName(it).length } ?: 0

            lines.add(
                OutputLine(
                    "  ${"Quyen han".padEnd(maxLen + 2)}Trang thai",
                    OutputStyle.TABLE_HEADER
                )
            )

            for (perm in AdminPermission.entries) {
                val name = formatPermissionName(perm).padEnd(maxLen + 2)
                val status = if (perm in effectivePerms) "[V]" else "[ ]"
                val style = if (perm in effectivePerms) OutputStyle.SUCCESS else OutputStyle.MUTED
                lines.add(OutputLine("  $name$status", style))
            }
        }

        if (showSession) {
            lines.add(OutputLine(""))
            lines.add(OutputLine("-- Phien dang nhap --", OutputStyle.HEADER))

            val isOnline = context.services.networkMonitor.isOnline.value
            val networkStatus = if (isOnline) "Truc tuyen" else "Ngoai tuyen"
            val networkStyle = if (isOnline) OutputStyle.SUCCESS else OutputStyle.WARNING

            lines.add(OutputLine("  Mang         : $networkStatus", networkStyle))

            val syncState = context.services.syncManager.syncState.value
            lines.add(OutputLine("  Dong bo      : ${formatSyncState(syncState)}", OutputStyle.NORMAL))
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung dau ra dinh dang JSON.
     */
    private fun buildJsonOutput(
        user: com.example.androidapp.domain.model.User,
        verbose: Boolean,
        showPermissions: Boolean,
        showSession: Boolean,
        context: CommandContext
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("{", OutputStyle.CODE))
        lines.add(OutputLine("  \"displayName\": \"${escapeJson(user.displayName)}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"email\": \"${escapeJson(user.email)}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"username\": \"${escapeJson(user.username)}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"role\": \"${user.role.name}\",", OutputStyle.CODE))
        lines.add(OutputLine("  \"isBanned\": ${user.isBanned},", OutputStyle.CODE))

        if (verbose) {
            lines.add(OutputLine("  \"id\": \"${escapeJson(user.id)}\",", OutputStyle.CODE))
            lines.add(OutputLine("  \"photoUrl\": ${if (user.photoUrl != null) "\"${escapeJson(user.photoUrl)}\"" else "null"},", OutputStyle.CODE))
            lines.add(OutputLine("  \"isAdmin\": ${user.isAdmin()},", OutputStyle.CODE))
            lines.add(OutputLine("  \"isSuperuser\": ${user.isSuperuser()},", OutputStyle.CODE))
        }

        if (showPermissions) {
            val perms = user.effectivePermissions().joinToString(", ") { "\"${it.name}\"" }
            lines.add(OutputLine("  \"permissions\": [$perms],", OutputStyle.CODE))
        }

        if (showSession) {
            val isOnline = context.services.networkMonitor.isOnline.value
            val syncState = context.services.syncManager.syncState.value
            lines.add(OutputLine("  \"network\": \"${if (isOnline) "online" else "offline"}\",", OutputStyle.CODE))
            lines.add(OutputLine("  \"syncState\": \"${syncState.name}\",", OutputStyle.CODE))
        }

        // Remove trailing comma from last property
        val lastIndex = lines.lastIndex
        if (lastIndex > 0) {
            val lastLine = lines[lastIndex]
            if (lastLine.text.endsWith(",")) {
                lines[lastIndex] = lastLine.copy(text = lastLine.text.dropLast(1))
            }
        }

        lines.add(OutputLine("}", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Dinh dang ten vai tro sang tieng Viet.
     */
    private fun formatRoleBadge(role: UserRole): String = when (role) {
        UserRole.GUEST -> "Khach"
        UserRole.USER -> "Nguoi dung"
        UserRole.ADMIN -> "Quan tri vien"
        UserRole.SUPERUSER -> "Sieu quan tri"
    }

    /**
     * Dinh dang ten quyen han de hien thi.
     */
    private fun formatPermissionName(permission: AdminPermission): String = when (permission) {
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
     * Dinh dang trang thai dong bo sang tieng Viet.
     */
    private fun formatSyncState(state: com.example.androidapp.data.sync.SyncState): String = when (state) {
        com.example.androidapp.data.sync.SyncState.IDLE -> "Ranh roi"
        com.example.androidapp.data.sync.SyncState.SYNCING -> "Dang dong bo..."
        com.example.androidapp.data.sync.SyncState.PENDING -> "Cho xu ly"
        com.example.androidapp.data.sync.SyncState.ERROR -> "Loi"
    }

    /**
     * Thoat ky tu dac biet cho chuoi JSON.
     */
    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
