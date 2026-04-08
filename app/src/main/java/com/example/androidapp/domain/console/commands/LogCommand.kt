package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.LogEntry
import com.example.androidapp.domain.model.LogLevel
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lenh hien thi va loc cac ban ghi nhat ky cua ung dung.
 *
 * Cho phep nguoi dung xem nhat ky, loc theo muc do, tag, noi dung,
 * va xuat nhat ky ra dinh dang van ban. Nguoi dung thuong chi thay
 * muc INFO tro len; ADMIN/SUPERUSER thay tat ca.
 *
 * Cach dung:
 * ```
 * log              # Hien thi 50 dong nhat ky gan nhat
 * log 20           # Hien thi 20 dong gan nhat
 * log -l error     # Chi hien thi loi
 * log -t Network   # Loc theo tag bat dau bang "Network"
 * log --clear      # Xoa bo nho dem nhat ky
 * log --stats      # Thong ke theo muc do
 * ```
 */
class LogCommand : Command {

    override val name: String = "log"

    override val aliases: List<String> = listOf("logs")

    override val description: String = "Xem va loc nhat ky ung dung"

    override val usage: String =
        "log [so_dong] [-l|--level <muc>] [-t|--tag <tag>] [--search <tu_khoa>] " +
            "[--regex <bieu_thuc>] [--not <loai_tru>] [--since <thoi_gian>] " +
            "[--after <thoi_gian>] [--before <thoi_gian>] [--between <bat_dau,ket_thuc>] " +
            "[--thread <ten_luong>] [--format <dinh_dang>] [--fields <truong>] " +
            "[--no-timestamp] [--no-tag] [--clear] [--export] [--count] [--stats]"

    override val category: String = "system"

    override val examples: List<Pair<String, String>> = listOf(
        "log" to "Hien thi 50 ban ghi gan nhat",
        "log 100" to "Hien thi 100 ban ghi gan nhat",
        "log -l error" to "Chi hien thi cac ban ghi loi",
        "log -l W" to "Chi hien thi canh bao (dung viet tat)",
        "log -t Auth" to "Loc ban ghi co tag bat dau bang 'Auth'",
        "log --search timeout" to "Tim cac ban ghi chua 'timeout'",
        "log --regex \"Exception.*null\"" to "Tim bang bieu thuc chinh quy",
        "log --not Debug" to "Loai tru cac ban ghi chua 'Debug'",
        "log --since 5m" to "Ban ghi trong 5 phut gan day",
        "log --since 1h" to "Ban ghi trong 1 gio gan day",
        "log --after 14:30" to "Ban ghi sau 14:30 hom nay",
        "log --between 10:00,12:00" to "Ban ghi tu 10:00 den 12:00",
        "log --thread main" to "Chi ban ghi tu luong chinh",
        "log --format json" to "Xuat dinh dang JSON",
        "log --fields level,tag,message" to "Chi hien thi cac truong chon",
        "log --no-timestamp --no-tag" to "An thoi gian va tag",
        "log --count" to "Dem so ban ghi thay vi hien thi",
        "log --stats" to "Thong ke so luong theo muc do",
        "log --clear" to "Xoa bo nho dem nhat ky",
        "log --export" to "Xuat toan bo nhat ky"
    )

    override fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        if (args.isEmpty()) {
            suggestions.addAll(
                listOf(
                    CompletionSuggestion("--level", description = "Loc theo muc do", type = SuggestionType.FLAG),
                    CompletionSuggestion("--tag", description = "Loc theo tag", type = SuggestionType.FLAG),
                    CompletionSuggestion("--search", description = "Tim kiem noi dung", type = SuggestionType.FLAG),
                    CompletionSuggestion("--regex", description = "Tim bang regex", type = SuggestionType.FLAG),
                    CompletionSuggestion("--since", description = "Loc theo thoi gian", type = SuggestionType.FLAG),
                    CompletionSuggestion("--stats", description = "Thong ke nhat ky", type = SuggestionType.FLAG),
                    CompletionSuggestion("--clear", description = "Xoa nhat ky", type = SuggestionType.FLAG),
                    CompletionSuggestion("--export", description = "Xuat nhat ky", type = SuggestionType.FLAG),
                    CompletionSuggestion("--count", description = "Dem so ban ghi", type = SuggestionType.FLAG),
                    CompletionSuggestion("--format", description = "Dinh dang xuat", type = SuggestionType.FLAG),
                    CompletionSuggestion("50", description = "50 dong gan nhat", type = SuggestionType.ARGUMENT),
                    CompletionSuggestion("100", description = "100 dong gan nhat", type = SuggestionType.ARGUMENT)
                )
            )
        }

        val lastFlag = flags.keys.lastOrNull()
        if (lastFlag == "level" || lastFlag == "l") {
            val isAdmin = context.currentUser.isAdmin()
            val levels = if (isAdmin) LogLevel.entries else LogLevel.USER_VISIBLE_LEVELS.toList()
            levels.forEach { level ->
                suggestions.add(
                    CompletionSuggestion(
                        text = level.name.lowercase(),
                        description = level.abbreviation,
                        type = SuggestionType.ARGUMENT
                    )
                )
            }
        }

        if (lastFlag == "format") {
            suggestions.addAll(
                listOf(
                    CompletionSuggestion("table", description = "Dinh dang bang", type = SuggestionType.ARGUMENT),
                    CompletionSuggestion("json", description = "Dinh dang JSON", type = SuggestionType.ARGUMENT),
                    CompletionSuggestion("compact", description = "Dinh dang gon", type = SuggestionType.ARGUMENT),
                    CompletionSuggestion("raw", description = "Dinh dang tho", type = SuggestionType.ARGUMENT)
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
        val isAdmin = context.currentUser.isAdmin()

        // --clear: xoa bo nho dem
        if (flags.containsKey("clear")) {
            context.services.logCollector.clear()
            return CommandResult.success(
                listOf(OutputLine("Da xoa bo nho dem nhat ky.", OutputStyle.SUCCESS))
            )
        }

        // --export: xuat toan bo
        if (flags.containsKey("export")) {
            val exported = context.services.logCollector.export()
            if (exported.isBlank()) {
                return CommandResult.success(
                    listOf(OutputLine("Khong co ban ghi nao de xuat.", OutputStyle.WARNING))
                )
            }
            val lines = mutableListOf<OutputLine>()
            lines.add(OutputLine("=== XUAT NHAT KY ===", OutputStyle.HEADER))
            exported.lines().forEach { line ->
                lines.add(OutputLine(line, OutputStyle.CODE))
            }
            lines.add(OutputLine("--- Ket thuc xuat ---", OutputStyle.MUTED))
            return CommandResult.success(lines)
        }

        // Thu thap cac ban ghi hien tai
        val allLogs = context.services.logCollector.logs.first()
        if (allLogs.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong co ban ghi nhat ky nao.", OutputStyle.MUTED))
            )
        }

        // Ap dung bo loc muc do dua tren vai tro
        var filtered = if (isAdmin) {
            allLogs
        } else {
            allLogs.filter { it.level in LogLevel.USER_VISIBLE_LEVELS }
        }

        // --level / -l: loc theo muc do
        val levelStr = flags["level"] ?: flags["l"]
        if (levelStr != null) {
            val requestedLevel = LogLevel.fromString(levelStr)
            if (requestedLevel == null) {
                return CommandResult.error("Muc do khong hop le: '$levelStr'. Cac muc hop le: ${availableLevelsForDisplay(isAdmin)}")
            }
            // Nguoi dung thuong khong duoc xem VERBOSE/DEBUG
            if (!isAdmin && requestedLevel !in LogLevel.USER_VISIBLE_LEVELS) {
                // Am tham bo qua, loc theo muc nhin thay duoc
                filtered = filtered.filter { it.level in LogLevel.USER_VISIBLE_LEVELS }
            } else {
                filtered = filtered.filter { it.level == requestedLevel }
            }
        }

        // --tag / -t: loc theo tien to tag
        val tagFilter = flags["tag"] ?: flags["t"]
        if (tagFilter != null) {
            filtered = filtered.filter { it.tag.startsWith(tagFilter, ignoreCase = true) }
        }

        // --search: loc theo tu khoa trong message
        val searchTerm = flags["search"]
        if (searchTerm != null) {
            filtered = filtered.filter { it.message.contains(searchTerm, ignoreCase = true) }
        }

        // --regex: loc bang bieu thuc chinh quy
        val regexPattern = flags["regex"]
        if (regexPattern != null) {
            val regex = try {
                Regex(regexPattern, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                return CommandResult.error("Bieu thuc chinh quy khong hop le: '${regexPattern}' - ${e.message}")
            }
            filtered = filtered.filter { regex.containsMatchIn(it.message) }
        }

        // --not: loai tru cac ban ghi chua chuoi
        val excludeTerm = flags["not"]
        if (excludeTerm != null) {
            filtered = filtered.filter { !it.message.contains(excludeTerm, ignoreCase = true) }
        }

        // --thread: loc theo ten luong
        val threadFilter = flags["thread"]
        if (threadFilter != null) {
            filtered = filtered.filter { it.threadName.contains(threadFilter, ignoreCase = true) }
        }

        // Loc thoi gian: --since, --after, --before, --between
        filtered = applyTimeFilters(filtered, flags)
            ?: return CommandResult.error("Dinh dang thoi gian khong hop le. Dung: 5m, 1h, 30s, hoac HH:mm")

        // --stats: hien thi thong ke
        if (flags.containsKey("stats")) {
            return buildStatsOutput(filtered, isAdmin)
        }

        // --count: chi dem so luong
        if (flags.containsKey("count")) {
            return CommandResult.success(
                listOf(
                    OutputLine("Tong so ban ghi (sau khi loc): ${filtered.size}", OutputStyle.INFO)
                )
            )
        }

        // Xac dinh so dong hien thi
        val limit = if (args.isNotEmpty()) {
            args[0].toIntOrNull() ?: return CommandResult.error(
                "So dong khong hop le: '${args[0]}'. Vui long nhap so nguyen duong."
            )
        } else {
            DEFAULT_LOG_COUNT
        }

        if (limit < 1) {
            return CommandResult.error("So dong phai lon hon 0.")
        }

        // Lay cac ban ghi gan nhat
        val entries = filtered.takeLast(limit)

        if (entries.isEmpty()) {
            return CommandResult.success(
                listOf(OutputLine("Khong co ban ghi nao khop voi bo loc.", OutputStyle.MUTED))
            )
        }

        // Xac dinh dinh dang xuat
        val format = flags["format"] ?: "table"
        val showTimestamp = !flags.containsKey("no-timestamp")
        val showTag = !flags.containsKey("no-tag")
        val fieldsStr = flags["fields"]
        val requestedFields = fieldsStr?.split(",")?.map { it.trim().lowercase() }

        return when (format.lowercase()) {
            "json" -> buildJsonOutput(entries, requestedFields, showTimestamp, showTag)
            "compact" -> buildCompactOutput(entries, showTimestamp, showTag)
            "raw" -> buildRawOutput(entries)
            else -> buildTableOutput(entries, requestedFields, showTimestamp, showTag, filtered.size, limit)
        }
    }

    /**
     * Ap dung cac bo loc thoi gian vao danh sach ban ghi.
     *
     * @param entries Danh sach ban ghi dau vao.
     * @param flags Cac co chua thong so thoi gian.
     * @return Danh sach da loc, hoac null neu dinh dang thoi gian loi.
     */
    private fun applyTimeFilters(
        entries: List<LogEntry>,
        flags: Map<String, String?>
    ): List<LogEntry>? {
        var result = entries
        val now = System.currentTimeMillis()

        // --since: trong khoang thoi gian gan day (vd: 5m, 1h, 30s)
        val sinceStr = flags["since"]
        if (sinceStr != null) {
            val durationMs = parseDuration(sinceStr) ?: return null
            val cutoff = now - durationMs
            result = result.filter { it.timestamp >= cutoff }
        }

        // --after: sau thoi diem cu the (HH:mm hoac epoch ms)
        val afterStr = flags["after"]
        if (afterStr != null) {
            val afterMs = parseTimeReference(afterStr, now) ?: return null
            result = result.filter { it.timestamp >= afterMs }
        }

        // --before: truoc thoi diem cu the
        val beforeStr = flags["before"]
        if (beforeStr != null) {
            val beforeMs = parseTimeReference(beforeStr, now) ?: return null
            result = result.filter { it.timestamp <= beforeMs }
        }

        // --between: giua hai thoi diem (bat_dau,ket_thuc)
        val betweenStr = flags["between"]
        if (betweenStr != null) {
            val parts = betweenStr.split(",")
            if (parts.size != 2) return null
            val startMs = parseTimeReference(parts[0].trim(), now) ?: return null
            val endMs = parseTimeReference(parts[1].trim(), now) ?: return null
            result = result.filter { it.timestamp in startMs..endMs }
        }

        return result
    }

    /**
     * Xay dung xuat thong ke theo muc do nhat ky.
     */
    private fun buildStatsOutput(entries: List<LogEntry>, isAdmin: Boolean): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("=== THONG KE NHAT KY ===", OutputStyle.HEADER))
        lines.add(OutputLine("Tong so ban ghi: ${entries.size}", OutputStyle.INFO))
        lines.add(OutputLine("", OutputStyle.NORMAL))

        val byLevel = entries.groupBy { it.level }
        val levels = if (isAdmin) LogLevel.entries else LogLevel.USER_VISIBLE_LEVELS.toList().sortedBy { it.ordinal }

        lines.add(OutputLine("  Muc do        So luong   Ti le", OutputStyle.TABLE_HEADER))
        lines.add(OutputLine("  ------------- --------- ------", OutputStyle.MUTED))

        for (level in levels) {
            val count = byLevel[level]?.size ?: 0
            val pct = if (entries.isNotEmpty()) (count * 100.0 / entries.size) else 0.0
            val levelName = level.name.padEnd(13)
            val countStr = count.toString().padStart(9)
            val pctStr = String.format(Locale.US, "%5.1f%%", pct)
            lines.add(OutputLine("  $levelName $countStr $pctStr", OutputStyle.TABLE_ROW))
        }

        lines.add(OutputLine("", OutputStyle.NORMAL))

        // Thong ke theo tag (top 10)
        val byTag = entries.groupBy { it.tag }.entries.sortedByDescending { it.value.size }.take(10)
        if (byTag.isNotEmpty()) {
            lines.add(OutputLine("Top tag (theo so luong):", OutputStyle.HEADER))
            lines.add(OutputLine("  Tag                     So luong", OutputStyle.TABLE_HEADER))
            lines.add(OutputLine("  ------------------------ --------", OutputStyle.MUTED))
            for ((tag, tagEntries) in byTag) {
                val tagName = tag.take(24).padEnd(24)
                val countStr = tagEntries.size.toString().padStart(8)
                lines.add(OutputLine("  $tagName $countStr", OutputStyle.TABLE_ROW))
            }
        }

        // Pham vi thoi gian
        if (entries.isNotEmpty()) {
            lines.add(OutputLine("", OutputStyle.NORMAL))
            val oldest = entries.minOf { it.timestamp }
            val newest = entries.maxOf { it.timestamp }
            lines.add(OutputLine("Ban ghi cu nhat: ${formatTimestamp(oldest)}", OutputStyle.MUTED))
            lines.add(OutputLine("Ban ghi moi nhat: ${formatTimestamp(newest)}", OutputStyle.MUTED))
            val spanSec = (newest - oldest) / 1000
            lines.add(OutputLine("Khoang thoi gian: ${formatDurationSeconds(spanSec)}", OutputStyle.MUTED))
        }

        return CommandResult.success(lines)
    }

    /**
     * Xay dung xuat dang bang.
     */
    private fun buildTableOutput(
        entries: List<LogEntry>,
        requestedFields: List<String>?,
        showTimestamp: Boolean,
        showTag: Boolean,
        totalFiltered: Int,
        limit: Int
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()

        if (totalFiltered > limit) {
            lines.add(
                OutputLine(
                    "Hien thi $limit / $totalFiltered ban ghi (dung 'log ${totalFiltered}' de xem tat ca)",
                    OutputStyle.MUTED
                )
            )
            lines.add(OutputLine("", OutputStyle.NORMAL))
        }

        val useFields = requestedFields ?: buildDefaultFields(showTimestamp, showTag)

        // Tao header
        val header = buildFieldHeader(useFields)
        lines.add(OutputLine(header, OutputStyle.TABLE_HEADER))
        lines.add(OutputLine("-".repeat(header.length.coerceAtMost(120)), OutputStyle.MUTED))

        for (entry in entries) {
            val row = buildFieldRow(entry, useFields)
            val style = levelToStyle(entry.level)
            lines.add(OutputLine(row, style))
        }

        lines.add(OutputLine("", OutputStyle.NORMAL))
        lines.add(OutputLine("Tong cong: ${entries.size} ban ghi", OutputStyle.MUTED))

        return CommandResult.success(lines)
    }

    /**
     * Xay dung xuat dinh dang JSON.
     */
    private fun buildJsonOutput(
        entries: List<LogEntry>,
        requestedFields: List<String>?,
        showTimestamp: Boolean,
        showTag: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("[", OutputStyle.CODE))

        for ((index, entry) in entries.withIndex()) {
            val parts = mutableListOf<String>()

            val fields = requestedFields ?: buildDefaultFields(showTimestamp, showTag)

            if ("timestamp" in fields || "time" in fields) {
                parts.add("\"timestamp\": \"${formatTimestamp(entry.timestamp)}\"")
                parts.add("\"timestampMs\": ${entry.timestamp}")
            }
            if ("level" in fields) {
                parts.add("\"level\": \"${entry.level.name}\"")
            }
            if ("tag" in fields) {
                parts.add("\"tag\": \"${escapeJson(entry.tag)}\"")
            }
            if ("message" in fields || "msg" in fields) {
                parts.add("\"message\": \"${escapeJson(entry.message)}\"")
            }
            if ("thread" in fields) {
                parts.add("\"thread\": \"${escapeJson(entry.threadName)}\"")
            }
            if ("id" in fields) {
                parts.add("\"id\": ${entry.id}")
            }

            val comma = if (index < entries.size - 1) "," else ""
            lines.add(OutputLine("  { ${parts.joinToString(", ")} }$comma", OutputStyle.CODE))
        }

        lines.add(OutputLine("]", OutputStyle.CODE))
        return CommandResult.success(lines)
    }

    /**
     * Xay dung xuat gon.
     */
    private fun buildCompactOutput(
        entries: List<LogEntry>,
        showTimestamp: Boolean,
        showTag: Boolean
    ): CommandResult {
        val lines = mutableListOf<OutputLine>()
        for (entry in entries) {
            val parts = mutableListOf<String>()
            if (showTimestamp) {
                parts.add(formatTimestampCompact(entry.timestamp))
            }
            parts.add(entry.level.abbreviation)
            if (showTag) {
                parts.add(entry.tag)
            }
            parts.add(entry.message)
            val style = levelToStyle(entry.level)
            lines.add(OutputLine(parts.joinToString(" "), style))
        }
        return CommandResult.success(lines)
    }

    /**
     * Xay dung xuat tho (giong logcat).
     */
    private fun buildRawOutput(entries: List<LogEntry>): CommandResult {
        val lines = entries.map { entry ->
            val ts = formatTimestamp(entry.timestamp)
            val text = "$ts ${entry.level.abbreviation}/${entry.tag}(${entry.threadName}): ${entry.message}"
            OutputLine(text, OutputStyle.CODE)
        }
        return CommandResult.success(lines)
    }

    // ==================== Tro giup dinh dang ====================

    /**
     * Danh sach truong mac dinh de hien thi.
     */
    private fun buildDefaultFields(showTimestamp: Boolean, showTag: Boolean): List<String> {
        val fields = mutableListOf<String>()
        if (showTimestamp) fields.add("timestamp")
        fields.add("level")
        if (showTag) fields.add("tag")
        fields.add("message")
        return fields
    }

    /**
     * Tao dong header cho bang.
     */
    private fun buildFieldHeader(fields: List<String>): String {
        return fields.joinToString("  ") { field ->
            when (field) {
                "timestamp", "time" -> "Thoi gian".padEnd(19)
                "level" -> "Muc".padEnd(7)
                "tag" -> "Tag".padEnd(20)
                "message", "msg" -> "Noi dung"
                "thread" -> "Luong".padEnd(15)
                "id" -> "ID".padEnd(8)
                else -> field.padEnd(10)
            }
        }
    }

    /**
     * Tao dong du lieu cho mot ban ghi.
     */
    private fun buildFieldRow(entry: LogEntry, fields: List<String>): String {
        return fields.joinToString("  ") { field ->
            when (field) {
                "timestamp", "time" -> formatTimestamp(entry.timestamp).padEnd(19)
                "level" -> entry.level.abbreviation.padEnd(7)
                "tag" -> entry.tag.take(20).padEnd(20)
                "message", "msg" -> entry.message
                "thread" -> entry.threadName.take(15).padEnd(15)
                "id" -> entry.id.toString().padEnd(8)
                else -> ""
            }
        }
    }

    /**
     * Anh xa muc do nhat ky sang kieu xuat tuong ung.
     */
    private fun levelToStyle(level: LogLevel): OutputStyle {
        return when (level) {
            LogLevel.VERBOSE -> OutputStyle.MUTED
            LogLevel.DEBUG -> OutputStyle.MUTED
            LogLevel.INFO -> OutputStyle.INFO
            LogLevel.WARN -> OutputStyle.WARNING
            LogLevel.ERROR -> OutputStyle.ERROR
            LogLevel.ASSERT -> OutputStyle.ERROR
        }
    }

    // ==================== Tien ich thoi gian ====================

    /**
     * Phan tich chuoi thoi luong tuong doi (vd: "5m", "1h", "30s").
     *
     * @param input Chuoi thoi luong.
     * @return So mili giay, hoac null neu khong hop le.
     */
    private fun parseDuration(input: String): Long? {
        val trimmed = input.trim().lowercase()
        if (trimmed.isEmpty()) return null

        val lastChar = trimmed.last()
        val numberPart = trimmed.dropLast(1)

        if (!lastChar.isLetter()) {
            // Co the la so giay thuan tuy
            return trimmed.toLongOrNull()?.times(1000)
        }

        val number = numberPart.toLongOrNull() ?: return null
        if (number < 0) return null

        return when (lastChar) {
            's' -> number * 1000
            'm' -> number * 60 * 1000
            'h' -> number * 60 * 60 * 1000
            'd' -> number * 24 * 60 * 60 * 1000
            else -> null
        }
    }

    /**
     * Phan tich tham chieu thoi gian (HH:mm hoac epoch ms).
     *
     * @param input Chuoi thoi gian.
     * @param now Thoi gian hien tai (epoch ms).
     * @return Epoch ms, hoac null neu khong hop le.
     */
    private fun parseTimeReference(input: String, now: Long): Long? {
        val trimmed = input.trim()

        // Thu phan tich epoch ms
        trimmed.toLongOrNull()?.let { return it }

        // Thu phan tich HH:mm
        if (trimmed.contains(":")) {
            return try {
                val sdf = SimpleDateFormat("HH:mm", Locale.US)
                val todayStart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
                val fullStr = "$todayStart $trimmed"
                val fullSdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                fullSdf.parse(fullStr)?.time
            } catch (_: Exception) {
                null
            }
        }

        // Thu phan tich nhu thoi luong tuong doi
        val durationMs = parseDuration(trimmed)
        if (durationMs != null) {
            return now - durationMs
        }

        return null
    }

    /**
     * Dinh dang timestamp thanh chuoi HH:mm:ss.SSS.
     */
    private fun formatTimestamp(epochMs: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return sdf.format(Date(epochMs))
    }

    /**
     * Dinh dang timestamp gon: HH:mm:ss.
     */
    private fun formatTimestampCompact(epochMs: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        return sdf.format(Date(epochMs))
    }

    /**
     * Dinh dang khoang thoi gian (giay) thanh chuoi doc duoc.
     */
    private fun formatDurationSeconds(totalSec: Long): String {
        if (totalSec < 60) return "${totalSec}s"
        if (totalSec < 3600) {
            val min = totalSec / 60
            val sec = totalSec % 60
            return "${min}m ${sec}s"
        }
        val hours = totalSec / 3600
        val min = (totalSec % 3600) / 60
        return "${hours}h ${min}m"
    }

    /**
     * Tra ve danh sach muc do hop le de hien thi trong thong bao loi.
     */
    private fun availableLevelsForDisplay(isAdmin: Boolean): String {
        val levels = if (isAdmin) LogLevel.entries else LogLevel.USER_VISIBLE_LEVELS.toList()
        return levels.joinToString(", ") { "${it.name.lowercase()} (${it.abbreviation})" }
    }

    /**
     * Thoat ky tu dac biet trong chuoi JSON.
     */
    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private companion object {
        /** So dong nhat ky mac dinh khi khong chi dinh. */
        const val DEFAULT_LOG_COUNT = 50
    }
}
