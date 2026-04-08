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
 * Lenh `del` — meta-dispatcher dinh tuyen den cac lenh xoa chuyen biet.
 *
 * Day la diem vao duy nhat cho moi thao tac xoa trong console admin.
 * Dua tren flag loai thuc the (`-u`, `-q`, `-a`, `-p`), lenh nay uy quyen
 * thuc thi cho [DeleteUserCommand], [DeleteQuizCommand], [DeleteAttemptCommand],
 * hoac [DeletePoolItemCommand] tuong ung.
 *
 * Cac flag dinh tuyen:
 * - `-u` / `--user`:    Xoa nguoi dung -> [DeleteUserCommand]
 * - `-q` / `--quiz`:    Xoa quiz -> [DeleteQuizCommand]
 * - `-a` / `--attempt`: Xoa luot lam -> [DeleteAttemptCommand]
 * - `-p` / `--pool`:    Xoa pool item -> [DeletePoolItemCommand]
 *
 * Neu khong co flag loai thuc the, lenh tra ve loi yeu cau chi dinh loai.
 * Cac thuoc tinh [requiredPermission] va [isDestructive] duoc uy quyen cho
 * tung lenh con; ban than lenh nay khong yeu cau quyen cu the nao.
 *
 * @property deleteUserCommand Lenh xoa nguoi dung.
 * @property deleteQuizCommand Lenh xoa quiz.
 * @property deleteAttemptCommand Lenh xoa luot lam quiz.
 * @property deletePoolItemCommand Lenh xoa pool item.
 */
class DeleteCommand(
    private val deleteUserCommand: DeleteUserCommand,
    private val deleteQuizCommand: DeleteQuizCommand,
    private val deleteAttemptCommand: DeleteAttemptCommand,
    private val deletePoolItemCommand: DeletePoolItemCommand
) : Command {

    override val name: String = "del"

    override val aliases: List<String> = listOf("delete")

    override val description: String = "Xoa thuc the (nguoi dung, quiz, luot lam, pool item)"

    override val usage: String =
        "del <-u|-q|-a|-p> <id|filter> [...] [--dry-run] [--confirm] [--format <table|json>] [--verbose]"

    override val category: String = "admin"

    override val minimumRole: UserRole = UserRole.ADMIN

    /**
     * Quyen duoc uy quyen cho lenh con tuong ung. Ban than meta-dispatcher
     * khong yeu cau quyen cu the — kiem tra quyen nam o lenh con.
     */
    override val requiredPermission: AdminPermission? = null

    /**
     * Tinh huy diet duoc uy quyen cho lenh con. Tat ca lenh con deu
     * la huy diet, nhung meta-dispatcher khong tu danh dau.
     */
    override val isDestructive: Boolean = false

    override val examples: List<Pair<String, String>> = listOf(
        "del -u user@example.com --confirm" to "Xoa nguoi dung theo email",
        "del -u --banned-only --confirm" to "Xoa tat ca nguoi dung bi cam",
        "del -q quizId123 --confirm" to "Xoa quiz theo ID",
        "del -q --draft --owner userId1 --confirm" to "Xoa tat ca quiz nhap cua mot nguoi dung",
        "del -a attemptId123 --confirm" to "Xoa luot lam quiz theo ID",
        "del -a --incomplete --dry-run" to "Mo phong xoa luot lam chua hoan thanh",
        "del -p poolId123 --confirm" to "Vo hieu hoa pool item theo ID",
        "del -p --inactive --dry-run" to "Mo phong xoa pool item da vo hieu",
        "del -q --no-attempts --dry-run" to "Mo phong xoa quiz chua co ai lam",
        "del -u userId1 --with-data --confirm" to "Xoa nguoi dung va ghi nhan xoa du lieu lien quan"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        // Neu da co flag loai thuc the, uy quyen cho lenh con
        val subCommand = resolveSubCommand(flags)
        if (subCommand != null) {
            return subCommand.autocomplete(args, stripEntityFlags(flags), context)
        }

        // Chua co flag loai — goi y cac flag loai thuc the
        val suggestions = mutableListOf<CompletionSuggestion>()
        val usedFlags = flags.keys.map { "--$it" }.toSet()

        val entityFlags = listOf(
            "-u" to "Xoa nguoi dung",
            "-q" to "Xoa quiz",
            "-a" to "Xoa luot lam quiz",
            "-p" to "Xoa pool item"
        )

        for ((flag, desc) in entityFlags) {
            val longForm = shortFlagToLong(flag)
            if (flag !in usedFlags && longForm !in usedFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = flag,
                        description = desc,
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        // Cac flag chung
        val commonFlags = listOf(
            "--dry-run" to "Mo phong thao tac, khong thuc su xoa",
            "--confirm" to "Xac nhan thao tac huy diet",
            "--format" to "Dinh dang dau ra (table/json)",
            "--verbose" to "Hien thi chi tiet"
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

        if ("format" in flags && flags["format"] == null) {
            suggestions.clear()
            suggestions.add(
                CompletionSuggestion(
                    text = "table",
                    description = "Dinh dang bang",
                    type = SuggestionType.ARGUMENT
                )
            )
            suggestions.add(
                CompletionSuggestion(
                    text = "json",
                    description = "Dinh dang JSON",
                    type = SuggestionType.ARGUMENT
                )
            )
        }

        return suggestions
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val subCommand = resolveSubCommand(flags)
            ?: return buildNoEntityTypeError()

        // Kiem tra quyen cua lenh con
        val permissionError = checkSubCommandPermission(subCommand, context)
        if (permissionError != null) {
            return permissionError
        }

        // Loai bo entity flags va uy quyen cho lenh con
        val cleanedFlags = stripEntityFlags(flags)
        return subCommand.execute(args, cleanedFlags, context)
    }

    /**
     * Xac dinh lenh con dua tren flag loai thuc the.
     *
     * Uu tien kiem tra: `-u`/`--user` > `-q`/`--quiz` > `-a`/`--attempt` > `-p`/`--pool`.
     *
     * @param flags Tap hop flag da phan tich.
     * @return Lenh con tuong ung, hoac null neu khong co flag loai thuc the.
     */
    private fun resolveSubCommand(flags: Map<String, String?>): Command? {
        return when {
            "u" in flags || "user" in flags -> deleteUserCommand
            "q" in flags || "quiz" in flags -> deleteQuizCommand
            "a" in flags || "attempt" in flags -> deleteAttemptCommand
            "p" in flags || "pool" in flags -> deletePoolItemCommand
            else -> null
        }
    }

    /**
     * Kiem tra quyen truy cap cua nguoi dung hien tai doi voi lenh con.
     *
     * @param subCommand Lenh con can kiem tra quyen.
     * @param context Context lenh hien tai.
     * @return [CommandResult] loi neu khong du quyen, null neu hop le.
     */
    private fun checkSubCommandPermission(
        subCommand: Command,
        context: CommandContext
    ): CommandResult? {
        val user = context.currentUser

        // Kiem tra vai tro toi thieu
        if (user.role.ordinal < subCommand.minimumRole.ordinal) {
            return CommandResult.error(
                "Khong du quyen. Lenh '${subCommand.name}' yeu cau vai tro toi thieu: " +
                    "${formatRole(subCommand.minimumRole)}."
            )
        }

        // Kiem tra quyen cu the
        val requiredPerm = subCommand.requiredPermission
        if (requiredPerm != null && !user.hasPermission(requiredPerm)) {
            return CommandResult.error(
                "Khong du quyen. Lenh '${subCommand.name}' yeu cau quyen: " +
                    "${formatPermission(requiredPerm)}."
            )
        }

        return null
    }

    /**
     * Loai bo cac flag loai thuc the (`u`, `user`, `q`, `quiz`, `a`, `attempt`,
     * `p`, `pool`) khoi tap hop flag truoc khi chuyen tiep cho lenh con.
     *
     * @param flags Tap hop flag goc.
     * @return Tap hop flag da loai bo cac flag loai thuc the.
     */
    private fun stripEntityFlags(flags: Map<String, String?>): Map<String, String?> {
        val entityKeys = setOf("u", "user", "q", "quiz", "a", "attempt", "p", "pool")
        return flags.filterKeys { it !in entityKeys }
    }

    /**
     * Xay dung thong bao loi khi khong co flag loai thuc the.
     *
     * @return [CommandResult] loi voi huong dan su dung.
     */
    private fun buildNoEntityTypeError(): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Loi: Vui long chi dinh loai thuc the can xoa.",
                OutputStyle.ERROR
            )
        )
        lines.add(OutputLine(""))
        lines.add(OutputLine("Cac loai thuc the ho tro:", OutputStyle.HEADER))
        lines.add(
            OutputLine(
                "  -u, --user      Xoa nguoi dung (yeu cau quyen DELETE_USERS)",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  -q, --quiz      Xoa quiz (yeu cau quyen DELETE_QUIZZES)",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  -a, --attempt   Xoa luot lam quiz (yeu cau quyen MANAGE_QUIZZES)",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  -p, --pool      Xoa pool item (yeu cau quyen MANAGE_QUIZZES)",
                OutputStyle.INFO
            )
        )
        lines.add(OutputLine(""))
        lines.add(OutputLine("Vi du:", OutputStyle.HEADER))
        lines.add(OutputLine("  del -u user@example.com --confirm", OutputStyle.MUTED))
        lines.add(OutputLine("  del -q quizId123 --dry-run", OutputStyle.MUTED))
        lines.add(OutputLine("  del -a --incomplete --confirm", OutputStyle.MUTED))
        lines.add(OutputLine("  del -p poolId123 --confirm", OutputStyle.MUTED))

        return CommandResult(output = lines, isSuccess = false, exitCode = 1)
    }

    /**
     * Chuyen doi flag ngan thanh dang dai.
     *
     * @param shortFlag Flag ngan (vd: "-u").
     * @return Flag dang dai tuong ung (vd: "--user").
     */
    private fun shortFlagToLong(shortFlag: String): String = when (shortFlag) {
        "-u" -> "--user"
        "-q" -> "--quiz"
        "-a" -> "--attempt"
        "-p" -> "--pool"
        else -> shortFlag
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
}
