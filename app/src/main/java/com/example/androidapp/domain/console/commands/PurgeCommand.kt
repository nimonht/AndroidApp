package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.first

/**
 * Lenh `purge` — thao tac don dep hang loat danh cho SUPERUSER.
 *
 * Ho tro xoa vinh vien nhieu loai du lieu cung luc dua tren cac flag muc tieu:
 * - `--trash`: Xoa vinh vien tat ca quiz da nam trong thung rac (deletedAt != null).
 * - `--inactive-users <days>`: Xoa nguoi dung khong hoat dong trong N+ ngay.
 * - `--old-attempts <days>`: Xoa cac luot lam quiz cu hon N ngay.
 * - `--orphan-attempts`: Xoa cac luot lam quiz ma quiz tuong ung khong con ton tai.
 * - `--revoked-pool`: Stub — ghi nhan nhung API hien tai con han che.
 * - `--banned-users`: Xoa vinh vien tat ca tai khoan nguoi dung bi cam.
 * - `--empty-quizzes`: Xoa quiz co 0 cau hoi.
 *
 * Cac flag dieu khien:
 * - `--dry-run`: Xem truoc so luong va danh sach se bi anh huong ma khong thuc thi.
 * - `--confirm`: Bat buoc de thuc thi thuc su (lenh huy diet).
 * - `--verbose`: Hien thi chi tiet tung thuc the bi xoa.
 * - `--format <table|csv|json>`: Dinh dang dau ra (mac dinh: table).
 * - `--output <count|full>`: Che do dau ra — chi so luong hoac day du (mac dinh: full).
 *
 * Phai chi dinh it nhat mot flag muc tieu. Lenh nay chi danh cho SUPERUSER
 * va duoc danh dau la huy diet ([isDestructive] = true).
 *
 * @see com.example.androidapp.domain.console.Command
 */
class PurgeCommand : Command {

    override val name: String = "purge"

    override val aliases: List<String> = emptyList()

    override val description: String =
        "Don dep hang loat: xoa quiz rac, nguoi dung bi cam, luot lam cu, v.v."

    override val usage: String =
        "purge [--trash] [--inactive-users <days>] [--old-attempts <days>] " +
                "[--orphan-attempts] [--revoked-pool] [--banned-users] [--empty-quizzes] " +
                "[--dry-run] [--confirm] [--verbose] [--format <table|csv|json>] [--output <count|full>]"

    override val isDestructive: Boolean = true

    override val minimumRole: UserRole = UserRole.SUPERUSER

    override val requiredPermission: Nothing? = null

    override val category: String = "admin"

    override val examples: List<Pair<String, String>> = listOf(
        "purge --dry-run --trash" to "Xem truoc cac quiz da xoa se bi xoa vinh vien",
        "purge --trash --confirm" to "Xoa vinh vien tat ca quiz trong thung rac",
        "purge --banned-users --dry-run" to "Xem truoc nguoi dung bi cam se bi xoa",
        "purge --banned-users --confirm --verbose" to "Xoa tat ca nguoi dung bi cam (chi tiet)",
        "purge --old-attempts 90 --confirm" to "Xoa luot lam quiz cu hon 90 ngay",
        "purge --inactive-users 180 --dry-run" to "Xem truoc nguoi dung khong hoat dong 180+ ngay",
        "purge --orphan-attempts --confirm" to "Xoa luot lam cua quiz khong con ton tai",
        "purge --empty-quizzes --dry-run --format csv" to "Xem truoc quiz rong, xuat CSV",
        "purge --trash --banned-users --confirm" to "Don dep nhieu loai cung luc"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        val targetFlags = listOf(
            "--trash" to "Xoa vinh vien quiz trong thung rac",
            "--inactive-users" to "Xoa nguoi dung khong hoat dong (chi dinh so ngay)",
            "--old-attempts" to "Xoa luot lam cu (chi dinh so ngay)",
            "--orphan-attempts" to "Xoa luot lam cua quiz khong ton tai",
            "--revoked-pool" to "Don dep pool item da thu hoi",
            "--banned-users" to "Xoa vinh vien nguoi dung bi cam",
            "--empty-quizzes" to "Xoa quiz co 0 cau hoi"
        )

        val controlFlags = listOf(
            "--dry-run" to "Xem truoc ket qua ma khong thuc thi",
            "--confirm" to "Xac nhan thuc thi thao tac huy diet",
            "--verbose" to "Hien thi chi tiet tung thuc the",
            "--format" to "Dinh dang dau ra (table|csv|json)",
            "--output" to "Che do dau ra (count|full)"
        )

        val usedFlags = flags.keys

        for ((flag, desc) in targetFlags + controlFlags) {
            val flagName = flag.removePrefix("--")
            if (flagName !in usedFlags) {
                suggestions.add(
                    CompletionSuggestion(
                        text = flag,
                        description = desc,
                        type = SuggestionType.FLAG
                    )
                )
            }
        }

        if ("format" in usedFlags && flags["format"] == null) {
            suggestions.clear()
            for (fmt in listOf("table", "csv", "json")) {
                suggestions.add(
                    CompletionSuggestion(
                        text = fmt,
                        description = "Dinh dang $fmt",
                        type = SuggestionType.ARGUMENT
                    )
                )
            }
        }

        if ("output" in usedFlags && flags["output"] == null) {
            suggestions.clear()
            for (mode in listOf("count", "full")) {
                suggestions.add(
                    CompletionSuggestion(
                        text = mode,
                        description = if (mode == "count") "Chi hien thi so luong" else "Hien thi day du",
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
        val isConfirmed = "confirm" in flags
        val isVerbose = "verbose" in flags
        val format = flags["format"] ?: "table"
        val outputMode = flags["output"] ?: "full"

        if (format !in listOf("table", "csv", "json")) {
            return CommandResult.error(
                "Dinh dang khong hop le: '$format'. Chon: table, csv, json."
            )
        }

        if (outputMode !in listOf("count", "full")) {
            return CommandResult.error(
                "Che do dau ra khong hop le: '$outputMode'. Chon: count, full."
            )
        }

        val hasTrash = "trash" in flags
        val hasInactiveUsers = "inactive-users" in flags
        val hasOldAttempts = "old-attempts" in flags
        val hasOrphanAttempts = "orphan-attempts" in flags
        val hasRevokedPool = "revoked-pool" in flags
        val hasBannedUsers = "banned-users" in flags
        val hasEmptyQuizzes = "empty-quizzes" in flags

        val hasAnyTarget = hasTrash || hasInactiveUsers || hasOldAttempts ||
                hasOrphanAttempts || hasRevokedPool || hasBannedUsers || hasEmptyQuizzes

        if (!hasAnyTarget) {
            return buildNoTargetError()
        }

        if (!isDryRun && !isConfirmed) {
            return CommandResult.error(
                "Thao tac huy diet yeu cau --confirm de thuc thi, " +
                        "hoac su dung --dry-run de xem truoc."
            )
        }

        val allLines = mutableListOf<OutputLine>()
        var totalAffected = 0
        var totalSuccess = 0
        var totalFail = 0
        var hasError = false

        if (isDryRun) {
            allLines.add(OutputLine("[DRY-RUN] Xem truoc lenh purge:", OutputStyle.HEADER))
        } else {
            allLines.add(OutputLine("Dang thuc thi don dep...", OutputStyle.HEADER))
        }
        allLines.add(OutputLine(""))

        if (hasTrash) {
            val result = executePurgeTrash(context, isDryRun, isVerbose, format, outputMode)
            allLines.addAll(result.lines)
            totalAffected += result.affected
            totalSuccess += result.success
            totalFail += result.fail
            if (result.fail > 0) hasError = true
            allLines.add(OutputLine(""))
        }

        if (hasBannedUsers) {
            val result = executePurgeBannedUsers(context, isDryRun, isVerbose, format, outputMode)
            allLines.addAll(result.lines)
            totalAffected += result.affected
            totalSuccess += result.success
            totalFail += result.fail
            if (result.fail > 0) hasError = true
            allLines.add(OutputLine(""))
        }

        if (hasInactiveUsers) {
            val daysStr = flags["inactive-users"]
            val days = daysStr?.toIntOrNull()
            if (days == null || days < 1) {
                allLines.add(
                    OutputLine(
                        "[inactive-users] Loi: Vui long chi dinh so ngay hop le (vd: --inactive-users 90).",
                        OutputStyle.ERROR
                    )
                )
                hasError = true
            } else {
                val result = executePurgeInactiveUsers(
                    context, days, isDryRun, isVerbose, format, outputMode
                )
                allLines.addAll(result.lines)
                totalAffected += result.affected
                totalSuccess += result.success
                totalFail += result.fail
                if (result.fail > 0) hasError = true
            }
            allLines.add(OutputLine(""))
        }

        if (hasOldAttempts) {
            val daysStr = flags["old-attempts"]
            val days = daysStr?.toIntOrNull()
            if (days == null || days < 1) {
                allLines.add(
                    OutputLine(
                        "[old-attempts] Loi: Vui long chi dinh so ngay hop le (vd: --old-attempts 90).",
                        OutputStyle.ERROR
                    )
                )
                hasError = true
            } else {
                val result = executePurgeOldAttempts(
                    context, days, isDryRun, isVerbose, format, outputMode
                )
                allLines.addAll(result.lines)
                totalAffected += result.affected
                totalSuccess += result.success
                totalFail += result.fail
                if (result.fail > 0) hasError = true
            }
            allLines.add(OutputLine(""))
        }

        if (hasOrphanAttempts) {
            val result = executePurgeOrphanAttempts(
                context, isDryRun, isVerbose, format, outputMode
            )
            allLines.addAll(result.lines)
            totalAffected += result.affected
            totalSuccess += result.success
            totalFail += result.fail
            if (result.fail > 0) hasError = true
            allLines.add(OutputLine(""))
        }

        if (hasEmptyQuizzes) {
            val result = executePurgeEmptyQuizzes(
                context, isDryRun, isVerbose, format, outputMode
            )
            allLines.addAll(result.lines)
            totalAffected += result.affected
            totalSuccess += result.success
            totalFail += result.fail
            if (result.fail > 0) hasError = true
            allLines.add(OutputLine(""))
        }

        if (hasRevokedPool) {
            allLines.add(
                OutputLine(
                    "[revoked-pool] Chuc nang nay hien bi han che boi API. " +
                            "Chua the thuc hien don dep pool item da thu hoi.",
                    OutputStyle.WARNING
                )
            )
            allLines.add(OutputLine(""))
        }

        // --- Tong ket ---
        allLines.add(OutputLine("=== TONG KET ===", OutputStyle.HEADER))
        if (isDryRun) {
            allLines.add(
                OutputLine(
                    "Tong cong se bi anh huong: $totalAffected thuc the",
                    OutputStyle.INFO
                )
            )
            allLines.add(
                OutputLine(
                    "Day la ban xem truoc. Them --confirm (bo --dry-run) de thuc thi.",
                    OutputStyle.WARNING
                )
            )
        } else {
            allLines.add(
                OutputLine(
                    "Thanh cong: $totalSuccess | That bai: $totalFail | Tong: $totalAffected",
                    if (totalFail > 0) OutputStyle.WARNING else OutputStyle.SUCCESS
                )
            )
        }

        return CommandResult(
            output = allLines,
            isSuccess = !hasError,
            exitCode = if (hasError) 1 else 0
        )
    }

    // ====================================================================
    // Purge: quiz da xoa (thung rac)
    // ====================================================================

    /**
     * Xoa vinh vien tat ca quiz co [Quiz.deletedAt] != null.
     *
     * @param context Context lenh hien tai.
     * @param isDryRun Neu true, chi dem va liet ke ma khong xoa.
     * @param isVerbose Hien thi chi tiet tung quiz bi xoa.
     * @param format Dinh dang dau ra.
     * @param outputMode Che do dau ra (count/full).
     * @return [PurgeResult] chua ket qua thao tac.
     */
    private suspend fun executePurgeTrash(
        context: CommandContext,
        isDryRun: Boolean,
        isVerbose: Boolean,
        format: String,
        outputMode: String
    ): PurgeResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[trash] Xoa quiz trong thung rac:", OutputStyle.HEADER))

        val allQuizzes: List<Quiz> = try {
            context.repositories.adminRepository.getAllQuizzes(includeDeleted = true).first()
        } catch (e: Exception) {
            lines.add(OutputLine("Loi khi tai danh sach quiz: ${e.message}", OutputStyle.ERROR))
            return PurgeResult(lines, 0, 0, 0)
        }

        val trashedQuizzes = allQuizzes.filter { it.deletedAt != null }

        if (trashedQuizzes.isEmpty()) {
            lines.add(OutputLine("Khong co quiz nao trong thung rac.", OutputStyle.MUTED))
            return PurgeResult(lines, 0, 0, 0)
        }

        if (isDryRun) {
            lines.add(
                OutputLine(
                    "Tim thay ${trashedQuizzes.size} quiz trong thung rac.",
                    OutputStyle.INFO
                )
            )
            if (outputMode == "full") {
                lines.addAll(formatQuizList(trashedQuizzes, format))
            }
            return PurgeResult(lines, trashedQuizzes.size, 0, 0)
        }

        var success = 0
        var fail = 0
        for (quiz in trashedQuizzes) {
            val result = context.repositories.adminRepository.deleteQuizPermanently(quiz.id)
            if (result.isSuccess) {
                success++
                if (isVerbose) {
                    lines.add(
                        OutputLine(
                            "  Da xoa: ${quiz.title} (${quiz.id})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                fail++
                lines.add(
                    OutputLine(
                        "  Loi khi xoa quiz ${quiz.id}: ${result.exceptionOrNull()?.message ?: "Khong ro"}",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        lines.add(
            OutputLine(
                "Ket qua: $success thanh cong, $fail that bai / ${trashedQuizzes.size} quiz.",
                if (fail > 0) OutputStyle.WARNING else OutputStyle.SUCCESS
            )
        )

        return PurgeResult(lines, trashedQuizzes.size, success, fail)
    }

    // ====================================================================
    // Purge: nguoi dung bi cam
    // ====================================================================

    /**
     * Xoa vinh vien tat ca nguoi dung co [User.isBanned] == true.
     *
     * @param context Context lenh hien tai.
     * @param isDryRun Neu true, chi dem va liet ke ma khong xoa.
     * @param isVerbose Hien thi chi tiet tung nguoi dung bi xoa.
     * @param format Dinh dang dau ra.
     * @param outputMode Che do dau ra (count/full).
     * @return [PurgeResult] chua ket qua thao tac.
     */
    private suspend fun executePurgeBannedUsers(
        context: CommandContext,
        isDryRun: Boolean,
        isVerbose: Boolean,
        format: String,
        outputMode: String
    ): PurgeResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[banned-users] Xoa nguoi dung bi cam:", OutputStyle.HEADER))

        val allUsers: List<User> = try {
            context.repositories.adminRepository.getAllUsers().first()
        } catch (e: Exception) {
            lines.add(
                OutputLine("Loi khi tai danh sach nguoi dung: ${e.message}", OutputStyle.ERROR)
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        val bannedUsers = allUsers.filter { it.isBanned }

        if (bannedUsers.isEmpty()) {
            lines.add(OutputLine("Khong co nguoi dung nao bi cam.", OutputStyle.MUTED))
            return PurgeResult(lines, 0, 0, 0)
        }

        if (isDryRun) {
            lines.add(
                OutputLine(
                    "Tim thay ${bannedUsers.size} nguoi dung bi cam.",
                    OutputStyle.INFO
                )
            )
            if (outputMode == "full") {
                lines.addAll(formatUserList(bannedUsers, format))
            }
            return PurgeResult(lines, bannedUsers.size, 0, 0)
        }

        var success = 0
        var fail = 0
        for (user in bannedUsers) {
            val result = context.repositories.adminRepository.deleteUserPermanently(user.id)
            if (result.isSuccess) {
                success++
                if (isVerbose) {
                    lines.add(
                        OutputLine(
                            "  Da xoa: ${user.email} (${user.displayName})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                fail++
                lines.add(
                    OutputLine(
                        "  Loi khi xoa nguoi dung ${user.email}: " +
                                "${result.exceptionOrNull()?.message ?: "Khong ro"}",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        lines.add(
            OutputLine(
                "Ket qua: $success thanh cong, $fail that bai / ${bannedUsers.size} nguoi dung.",
                if (fail > 0) OutputStyle.WARNING else OutputStyle.SUCCESS
            )
        )

        return PurgeResult(lines, bannedUsers.size, success, fail)
    }

    // ====================================================================
    // Purge: nguoi dung khong hoat dong
    // ====================================================================

    /**
     * Xoa nguoi dung khong hoat dong trong [days]+ ngay.
     *
     * Heuristic: su dung truong nao co san (createdAt) de uoc tinh
     * thoi gian hoat dong cuoi cung. Day la phep xap xi — domain model
     * hien tai khong co truong lastActiveAt rieng.
     *
     * @param context Context lenh hien tai.
     * @param days So ngay khong hoat dong toi thieu.
     * @param isDryRun Neu true, chi dem va liet ke.
     * @param isVerbose Hien thi chi tiet.
     * @param format Dinh dang dau ra.
     * @param outputMode Che do dau ra (count/full).
     * @return [PurgeResult] chua ket qua.
     */
    private suspend fun executePurgeInactiveUsers(
        context: CommandContext,
        days: Int,
        isDryRun: Boolean,
        isVerbose: Boolean,
        format: String,
        outputMode: String
    ): PurgeResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "[inactive-users] Xoa nguoi dung khong hoat dong $days+ ngay:",
                OutputStyle.HEADER
            )
        )

        lines.add(
            OutputLine(
                "Luu y: Heuristic dua tren du lieu hien co. " +
                        "Domain model khong co truong lastActiveAt rieng.",
                OutputStyle.MUTED
            )
        )

        val allUsers: List<User> = try {
            context.repositories.adminRepository.getAllUsers().first()
        } catch (e: Exception) {
            lines.add(
                OutputLine("Loi khi tai danh sach nguoi dung: ${e.message}", OutputStyle.ERROR)
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        // Lay tat ca attempt de xac dinh hoat dong gan nhat
        val allAttempts: List<Attempt> = try {
            context.repositories.adminRepository.getAllAttempts().first()
        } catch (_: Exception) {
            emptyList()
        }

        val now = System.currentTimeMillis()
        val cutoffMillis = now - (days.toLong() * MILLIS_PER_DAY)

        // Xay dung map: userId -> timestamp hoat dong gan nhat
        val lastActivity = mutableMapOf<String, Long>()
        for (attempt in allAttempts) {
            val ts = attempt.endTimeMillis ?: attempt.startTimeMillis
            val current = lastActivity[attempt.userId] ?: 0L
            if (ts > current) {
                lastActivity[attempt.userId] = ts
            }
        }

        // Loc nguoi dung khong hoat dong: khong co attempt hoac attempt cuoi truoc cutoff
        // Bo qua SUPERUSER va ADMIN de tranh xoa nham
        val inactiveUsers = allUsers.filter { user ->
            user.role != UserRole.SUPERUSER &&
                    user.role != UserRole.ADMIN &&
                    (lastActivity[user.id] ?: 0L) < cutoffMillis
        }

        if (inactiveUsers.isEmpty()) {
            lines.add(
                OutputLine(
                    "Khong tim thay nguoi dung khong hoat dong trong $days+ ngay.",
                    OutputStyle.MUTED
                )
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        if (isDryRun) {
            lines.add(
                OutputLine(
                    "Tim thay ${inactiveUsers.size} nguoi dung khong hoat dong.",
                    OutputStyle.INFO
                )
            )
            if (outputMode == "full") {
                lines.addAll(formatUserList(inactiveUsers, format))
            }
            return PurgeResult(lines, inactiveUsers.size, 0, 0)
        }

        var success = 0
        var fail = 0
        for (user in inactiveUsers) {
            val result = context.repositories.adminRepository.deleteUserPermanently(user.id)
            if (result.isSuccess) {
                success++
                if (isVerbose) {
                    lines.add(
                        OutputLine(
                            "  Da xoa: ${user.email} (${user.displayName})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                fail++
                lines.add(
                    OutputLine(
                        "  Loi khi xoa nguoi dung ${user.email}: " +
                                "${result.exceptionOrNull()?.message ?: "Khong ro"}",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        lines.add(
            OutputLine(
                "Ket qua: $success thanh cong, $fail that bai / ${inactiveUsers.size} nguoi dung.",
                if (fail > 0) OutputStyle.WARNING else OutputStyle.SUCCESS
            )
        )

        return PurgeResult(lines, inactiveUsers.size, success, fail)
    }

    // ====================================================================
    // Purge: luot lam cu
    // ====================================================================

    /**
     * Xoa cac luot lam quiz co [Attempt.startTimeMillis] cu hon [days] ngay.
     *
     * @param context Context lenh hien tai.
     * @param days So ngay toi thieu.
     * @param isDryRun Neu true, chi dem va liet ke.
     * @param isVerbose Hien thi chi tiet.
     * @param format Dinh dang dau ra.
     * @param outputMode Che do dau ra (count/full).
     * @return [PurgeResult] chua ket qua.
     */
    private suspend fun executePurgeOldAttempts(
        context: CommandContext,
        days: Int,
        isDryRun: Boolean,
        isVerbose: Boolean,
        format: String,
        outputMode: String
    ): PurgeResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "[old-attempts] Xoa luot lam cu hon $days ngay:",
                OutputStyle.HEADER
            )
        )

        val allAttempts: List<Attempt> = try {
            context.repositories.adminRepository.getAllAttempts().first()
        } catch (e: Exception) {
            lines.add(
                OutputLine("Loi khi tai danh sach luot lam: ${e.message}", OutputStyle.ERROR)
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        val now = System.currentTimeMillis()
        val cutoffMillis = now - (days.toLong() * MILLIS_PER_DAY)

        val oldAttempts = allAttempts.filter { it.startTimeMillis < cutoffMillis }

        if (oldAttempts.isEmpty()) {
            lines.add(
                OutputLine(
                    "Khong co luot lam nao cu hon $days ngay.",
                    OutputStyle.MUTED
                )
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        if (isDryRun) {
            lines.add(
                OutputLine(
                    "Tim thay ${oldAttempts.size} luot lam cu hon $days ngay.",
                    OutputStyle.INFO
                )
            )
            if (outputMode == "full") {
                lines.addAll(formatAttemptList(oldAttempts, format))
            }
            return PurgeResult(lines, oldAttempts.size, 0, 0)
        }

        var success = 0
        var fail = 0
        for (attempt in oldAttempts) {
            val result = context.repositories.adminRepository.deleteAttempt(attempt.id)
            if (result.isSuccess) {
                success++
                if (isVerbose) {
                    lines.add(
                        OutputLine(
                            "  Da xoa: attempt ${attempt.id} (quiz: ${attempt.quizId})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                fail++
                lines.add(
                    OutputLine(
                        "  Loi khi xoa attempt ${attempt.id}: " +
                                "${result.exceptionOrNull()?.message ?: "Khong ro"}",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        lines.add(
            OutputLine(
                "Ket qua: $success thanh cong, $fail that bai / ${oldAttempts.size} luot lam.",
                if (fail > 0) OutputStyle.WARNING else OutputStyle.SUCCESS
            )
        )

        return PurgeResult(lines, oldAttempts.size, success, fail)
    }

    // ====================================================================
    // Purge: luot lam mo coi (orphan attempts)
    // ====================================================================

    /**
     * Xoa cac luot lam quiz ma quiz tuong ung khong con ton tai.
     *
     * @param context Context lenh hien tai.
     * @param isDryRun Neu true, chi dem va liet ke.
     * @param isVerbose Hien thi chi tiet.
     * @param format Dinh dang dau ra.
     * @param outputMode Che do dau ra (count/full).
     * @return [PurgeResult] chua ket qua.
     */
    private suspend fun executePurgeOrphanAttempts(
        context: CommandContext,
        isDryRun: Boolean,
        isVerbose: Boolean,
        format: String,
        outputMode: String
    ): PurgeResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "[orphan-attempts] Xoa luot lam cua quiz khong ton tai:",
                OutputStyle.HEADER
            )
        )

        val allAttempts: List<Attempt> = try {
            context.repositories.adminRepository.getAllAttempts().first()
        } catch (e: Exception) {
            lines.add(
                OutputLine("Loi khi tai danh sach luot lam: ${e.message}", OutputStyle.ERROR)
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        val allQuizzes: List<Quiz> = try {
            context.repositories.adminRepository.getAllQuizzes(includeDeleted = true).first()
        } catch (e: Exception) {
            lines.add(
                OutputLine("Loi khi tai danh sach quiz: ${e.message}", OutputStyle.ERROR)
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        val existingQuizIds = allQuizzes.map { it.id }.toSet()
        val orphanAttempts = allAttempts.filter { it.quizId !in existingQuizIds }

        if (orphanAttempts.isEmpty()) {
            lines.add(
                OutputLine(
                    "Khong co luot lam mo coi nao.",
                    OutputStyle.MUTED
                )
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        if (isDryRun) {
            lines.add(
                OutputLine(
                    "Tim thay ${orphanAttempts.size} luot lam mo coi " +
                            "(quiz khong con ton tai).",
                    OutputStyle.INFO
                )
            )
            if (outputMode == "full") {
                lines.addAll(formatAttemptList(orphanAttempts, format))
            }
            return PurgeResult(lines, orphanAttempts.size, 0, 0)
        }

        var success = 0
        var fail = 0
        for (attempt in orphanAttempts) {
            val result = context.repositories.adminRepository.deleteAttempt(attempt.id)
            if (result.isSuccess) {
                success++
                if (isVerbose) {
                    lines.add(
                        OutputLine(
                            "  Da xoa: attempt ${attempt.id} (quiz: ${attempt.quizId})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                fail++
                lines.add(
                    OutputLine(
                        "  Loi khi xoa attempt ${attempt.id}: " +
                                "${result.exceptionOrNull()?.message ?: "Khong ro"}",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        lines.add(
            OutputLine(
                "Ket qua: $success thanh cong, $fail that bai / ${orphanAttempts.size} luot lam.",
                if (fail > 0) OutputStyle.WARNING else OutputStyle.SUCCESS
            )
        )

        return PurgeResult(lines, orphanAttempts.size, success, fail)
    }

    // ====================================================================
    // Purge: quiz rong (0 cau hoi)
    // ====================================================================

    /**
     * Xoa quiz co [Quiz.questionCount] == 0.
     *
     * @param context Context lenh hien tai.
     * @param isDryRun Neu true, chi dem va liet ke.
     * @param isVerbose Hien thi chi tiet.
     * @param format Dinh dang dau ra.
     * @param outputMode Che do dau ra (count/full).
     * @return [PurgeResult] chua ket qua.
     */
    private suspend fun executePurgeEmptyQuizzes(
        context: CommandContext,
        isDryRun: Boolean,
        isVerbose: Boolean,
        format: String,
        outputMode: String
    ): PurgeResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[empty-quizzes] Xoa quiz co 0 cau hoi:", OutputStyle.HEADER))

        val allQuizzes: List<Quiz> = try {
            context.repositories.adminRepository.getAllQuizzes(includeDeleted = false).first()
        } catch (e: Exception) {
            lines.add(
                OutputLine("Loi khi tai danh sach quiz: ${e.message}", OutputStyle.ERROR)
            )
            return PurgeResult(lines, 0, 0, 0)
        }

        val emptyQuizzes = allQuizzes.filter { it.questionCount == 0 }

        if (emptyQuizzes.isEmpty()) {
            lines.add(OutputLine("Khong co quiz nao co 0 cau hoi.", OutputStyle.MUTED))
            return PurgeResult(lines, 0, 0, 0)
        }

        if (isDryRun) {
            lines.add(
                OutputLine(
                    "Tim thay ${emptyQuizzes.size} quiz co 0 cau hoi.",
                    OutputStyle.INFO
                )
            )
            if (outputMode == "full") {
                lines.addAll(formatQuizList(emptyQuizzes, format))
            }
            return PurgeResult(lines, emptyQuizzes.size, 0, 0)
        }

        var success = 0
        var fail = 0
        for (quiz in emptyQuizzes) {
            val result = context.repositories.adminRepository.deleteQuizPermanently(quiz.id)
            if (result.isSuccess) {
                success++
                if (isVerbose) {
                    lines.add(
                        OutputLine(
                            "  Da xoa: ${quiz.title} (${quiz.id})",
                            OutputStyle.SUCCESS
                        )
                    )
                }
            } else {
                fail++
                lines.add(
                    OutputLine(
                        "  Loi khi xoa quiz ${quiz.id}: " +
                                "${result.exceptionOrNull()?.message ?: "Khong ro"}",
                        OutputStyle.ERROR
                    )
                )
            }
        }

        lines.add(
            OutputLine(
                "Ket qua: $success thanh cong, $fail that bai / ${emptyQuizzes.size} quiz.",
                if (fail > 0) OutputStyle.WARNING else OutputStyle.SUCCESS
            )
        )

        return PurgeResult(lines, emptyQuizzes.size, success, fail)
    }

    // ====================================================================
    // Dinh dang danh sach thuc the
    // ====================================================================

    /**
     * Dinh dang danh sach quiz theo format duoc chi dinh.
     *
     * @param quizzes Danh sach quiz can dinh dang.
     * @param format Dinh dang dau ra: "table", "csv", "json".
     * @return Danh sach [OutputLine] da dinh dang.
     */
    private fun formatQuizList(quizzes: List<Quiz>, format: String): List<OutputLine> {
        val lines = mutableListOf<OutputLine>()
        when (format) {
            "csv" -> {
                lines.add(
                    OutputLine("id,title,ownerId,questionCount,deletedAt", OutputStyle.TABLE_HEADER)
                )
                for (quiz in quizzes) {
                    lines.add(
                        OutputLine(
                            "${csvEscape(quiz.id)},${csvEscape(quiz.title)}," +
                                    "${csvEscape(quiz.ownerId)},${quiz.questionCount}," +
                                    "${quiz.deletedAt ?: ""}",
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }
            "json" -> {
                lines.add(OutputLine("[", OutputStyle.CODE))
                for ((index, quiz) in quizzes.withIndex()) {
                    val comma = if (index < quizzes.size - 1) "," else ""
                    lines.add(
                        OutputLine(
                            "  {\"id\":\"${escapeJson(quiz.id)}\",\"title\":\"${escapeJson(quiz.title)}\"," +
                                    "\"ownerId\":\"${escapeJson(quiz.ownerId)}\"," +
                                    "\"questionCount\":${quiz.questionCount}," +
                                    "\"deletedAt\":${quiz.deletedAt ?: "null"}}$comma",
                            OutputStyle.CODE
                        )
                    )
                }
                lines.add(OutputLine("]", OutputStyle.CODE))
            }
            else -> {
                lines.add(
                    OutputLine(
                        String.format(
                            "  %-24s %-28s %-24s %s",
                            "ID", "TIEU DE", "CHU SO HUU", "SO CAU"
                        ),
                        OutputStyle.TABLE_HEADER
                    )
                )
                for (quiz in quizzes) {
                    lines.add(
                        OutputLine(
                            String.format(
                                "  %-24s %-28s %-24s %d",
                                truncate(quiz.id, 22),
                                truncate(quiz.title, 26),
                                truncate(quiz.ownerId, 22),
                                quiz.questionCount
                            ),
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }
        }
        return lines
    }

    /**
     * Dinh dang danh sach nguoi dung theo format duoc chi dinh.
     *
     * @param users Danh sach nguoi dung can dinh dang.
     * @param format Dinh dang dau ra: "table", "csv", "json".
     * @return Danh sach [OutputLine] da dinh dang.
     */
    private fun formatUserList(users: List<User>, format: String): List<OutputLine> {
        val lines = mutableListOf<OutputLine>()
        when (format) {
            "csv" -> {
                lines.add(
                    OutputLine("id,email,displayName,role,isBanned", OutputStyle.TABLE_HEADER)
                )
                for (user in users) {
                    lines.add(
                        OutputLine(
                            "${csvEscape(user.id)},${csvEscape(user.email)}," +
                                    "${csvEscape(user.displayName)},${user.role.name},${user.isBanned}",
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }
            "json" -> {
                lines.add(OutputLine("[", OutputStyle.CODE))
                for ((index, user) in users.withIndex()) {
                    val comma = if (index < users.size - 1) "," else ""
                    lines.add(
                        OutputLine(
                            "  {\"id\":\"${escapeJson(user.id)}\",\"email\":\"${escapeJson(user.email)}\"," +
                                    "\"displayName\":\"${escapeJson(user.displayName)}\"," +
                                    "\"role\":\"${user.role.name}\",\"isBanned\":${user.isBanned}}$comma",
                            OutputStyle.CODE
                        )
                    )
                }
                lines.add(OutputLine("]", OutputStyle.CODE))
            }
            else -> {
                lines.add(
                    OutputLine(
                        String.format(
                            "  %-24s %-28s %-18s %-10s %s",
                            "ID", "EMAIL", "TEN", "VAI TRO", "BI CAM"
                        ),
                        OutputStyle.TABLE_HEADER
                    )
                )
                for (user in users) {
                    lines.add(
                        OutputLine(
                            String.format(
                                "  %-24s %-28s %-18s %-10s %s",
                                truncate(user.id, 22),
                                truncate(user.email, 26),
                                truncate(user.displayName, 16),
                                user.role.name,
                                if (user.isBanned) "Co" else "Khong"
                            ),
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }
        }
        return lines
    }

    /**
     * Dinh dang danh sach luot lam theo format duoc chi dinh.
     *
     * @param attempts Danh sach luot lam can dinh dang.
     * @param format Dinh dang dau ra: "table", "csv", "json".
     * @return Danh sach [OutputLine] da dinh dang.
     */
    private fun formatAttemptList(attempts: List<Attempt>, format: String): List<OutputLine> {
        val lines = mutableListOf<OutputLine>()
        when (format) {
            "csv" -> {
                lines.add(
                    OutputLine(
                        "id,userId,quizId,score,totalQuestions,startTimeMillis",
                        OutputStyle.TABLE_HEADER
                    )
                )
                for (attempt in attempts) {
                    lines.add(
                        OutputLine(
                            "${csvEscape(attempt.id)},${csvEscape(attempt.userId)}," +
                                    "${csvEscape(attempt.quizId)},${attempt.score}," +
                                    "${attempt.totalQuestions},${attempt.startTimeMillis}",
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }
            "json" -> {
                lines.add(OutputLine("[", OutputStyle.CODE))
                for ((index, attempt) in attempts.withIndex()) {
                    val comma = if (index < attempts.size - 1) "," else ""
                    lines.add(
                        OutputLine(
                            "  {\"id\":\"${escapeJson(attempt.id)}\"," +
                                    "\"userId\":\"${escapeJson(attempt.userId)}\"," +
                                    "\"quizId\":\"${escapeJson(attempt.quizId)}\"," +
                                    "\"score\":${attempt.score}," +
                                    "\"totalQuestions\":${attempt.totalQuestions}," +
                                    "\"startTimeMillis\":${attempt.startTimeMillis}}$comma",
                            OutputStyle.CODE
                        )
                    )
                }
                lines.add(OutputLine("]", OutputStyle.CODE))
            }
            else -> {
                lines.add(
                    OutputLine(
                        String.format(
                            "  %-24s %-24s %-24s %-8s %-8s %s",
                            "ID", "NGUOI DUNG", "QUIZ", "DIEM", "TONG", "THOI GIAN"
                        ),
                        OutputStyle.TABLE_HEADER
                    )
                )
                for (attempt in attempts) {
                    lines.add(
                        OutputLine(
                            String.format(
                                "  %-24s %-24s %-24s %-8d %-8d %s",
                                truncate(attempt.id, 22),
                                truncate(attempt.userId, 22),
                                truncate(attempt.quizId, 22),
                                attempt.score,
                                attempt.totalQuestions,
                                formatTimestamp(attempt.startTimeMillis)
                            ),
                            OutputStyle.TABLE_ROW
                        )
                    )
                }
            }
        }
        return lines
    }

    // ====================================================================
    // Thong bao loi khi khong co muc tieu
    // ====================================================================

    /**
     * Xay dung thong bao loi khi khong co flag muc tieu nao duoc chi dinh.
     *
     * @return [CommandResult] loi voi huong dan su dung.
     */
    private fun buildNoTargetError(): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(
            OutputLine(
                "Loi: Vui long chi dinh it nhat mot muc tieu don dep.",
                OutputStyle.ERROR
            )
        )
        lines.add(OutputLine(""))
        lines.add(OutputLine("Cac muc tieu ho tro:", OutputStyle.HEADER))
        lines.add(
            OutputLine(
                "  --trash             Xoa vinh vien quiz trong thung rac",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  --banned-users      Xoa vinh vien nguoi dung bi cam",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  --inactive-users N  Xoa nguoi dung khong hoat dong N+ ngay",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  --old-attempts N    Xoa luot lam cu hon N ngay",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  --orphan-attempts   Xoa luot lam cua quiz khong ton tai",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  --empty-quizzes     Xoa quiz co 0 cau hoi",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  --revoked-pool      Don dep pool item da thu hoi (han che)",
                OutputStyle.INFO
            )
        )
        lines.add(OutputLine(""))
        lines.add(OutputLine("Tuy chon dieu khien:", OutputStyle.HEADER))
        lines.add(
            OutputLine(
                "  --dry-run           Xem truoc ket qua ma khong thuc thi",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  --confirm           Xac nhan thuc thi (bat buoc khi khong dung --dry-run)",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "  --verbose           Hien thi chi tiet tung thuc the",
                OutputStyle.INFO
            )
        )
        lines.add(OutputLine(""))
        lines.add(OutputLine("Vi du:", OutputStyle.HEADER))
        lines.add(
            OutputLine("  purge --trash --dry-run", OutputStyle.MUTED)
        )
        lines.add(
            OutputLine("  purge --banned-users --confirm --verbose", OutputStyle.MUTED)
        )
        lines.add(
            OutputLine("  purge --old-attempts 90 --confirm", OutputStyle.MUTED)
        )

        return CommandResult(output = lines, isSuccess = false, exitCode = 1)
    }

    // ====================================================================
    // Tien ich dinh dang
    // ====================================================================

    /**
     * Dinh dang timestamp thanh chuoi ngay gio doc duoc.
     *
     * @param millis Epoch milliseconds.
     * @return Chuoi dinh dang "yyyy-MM-dd HH:mm".
     */
    private fun formatTimestamp(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    /**
     * Cat ngan chuoi va them "..." neu qua dai.
     *
     * @param text Chuoi goc.
     * @param maxLength Do dai toi da.
     * @return Chuoi da cat ngan hoac giu nguyen.
     */
    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text else text.take(maxLength - 3) + "..."
    }

    /**
     * Thoat ky tu dac biet trong chuoi JSON.
     *
     * @param value Chuoi goc.
     * @return Chuoi da thoat an toan cho JSON.
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
     *
     * @param value Chuoi goc.
     * @return Chuoi an toan cho CSV.
     */
    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    // ====================================================================
    // Kieu du lieu noi bo
    // ====================================================================

    /**
     * Ket qua cua mot thao tac purge don le.
     *
     * @property lines Cac dong dau ra da dinh dang.
     * @property affected Tong so thuc the bi anh huong (hoac se bi anh huong trong dry-run).
     * @property success So thuc the da xoa thanh cong (0 trong dry-run).
     * @property fail So thuc the xoa that bai (0 trong dry-run).
     */
    private data class PurgeResult(
        val lines: List<OutputLine>,
        val affected: Int,
        val success: Int,
        val fail: Int
    )

    private companion object {
        /** So millisecond trong mot ngay. */
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
