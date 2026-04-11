package com.example.androidapp.domain.console

import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.UserRole

/**
 * Shared formatting utilities for console command output.
 *
 * Consolidates common string formatting functions previously duplicated
 * across individual command implementations: truncation, padding, JSON
 * escaping, CSV escaping, timestamp formatting, duration formatting,
 * and role/permission display names.
 */
object CommandFormatUtils {

    /**
     * Truncates [text] to [maxLength], appending "..." if it exceeds the limit.
     *
     * @param text The source text.
     * @param maxLength Maximum allowed length (including the "..." suffix).
     *   Must be non-negative.
     * @return The truncated string, or the original if it fits.
     * @throws IllegalArgumentException if [maxLength] is negative.
     */
    fun truncate(text: String, maxLength: Int): String {
        require(maxLength >= 0) { "maxLength must be non-negative, was $maxLength" }
        if (text.length <= maxLength) return text
        if (maxLength <= 3) return text.take(maxLength)
        return text.take(maxLength - 3) + "..."
    }

    /**
     * Pads [text] to [length] with trailing spaces. If the text is longer
     * than [length], it is truncated to fit.
     *
     * @param text The source text.
     * @param length The target fixed width. Must be non-negative.
     * @return A string of exactly [length] characters.
     * @throws IllegalArgumentException if [length] is negative.
     */
    fun padRight(text: String, length: Int): String {
        require(length >= 0) { "length must be non-negative, was $length" }
        return if (text.length >= length) text.take(length) else text.padEnd(length)
    }

    /**
     * Escapes special characters for safe inclusion in a JSON string value.
     *
     * Handles backslash, double-quote, newline, carriage-return, tab,
     * and all other Unicode control characters (U+0000 through U+001F).
     *
     * @param value The raw string to escape.
     * @return The JSON-safe escaped string.
     */
    fun escapeJson(value: String): String {
        val sb = StringBuilder(value.length)
        for (ch in value) {
            when {
                ch == '\\' -> sb.append("\\\\")
                ch == '"' -> sb.append("\\\"")
                ch == '\n' -> sb.append("\\n")
                ch == '\r' -> sb.append("\\r")
                ch == '\t' -> sb.append("\\t")
                ch == '\b' -> sb.append("\\b")
                ch.code == 0x0C -> sb.append("\\f") // form feed
                ch.code in 0x00..0x1F -> {
                    // Other control characters: use unicode escape
                    sb.append("\\u")
                    sb.append(String.format("%04x", ch.code))
                }

                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Escapes a value for CSV output. Wraps the value in double quotes if it
     * contains commas, double-quotes, or newlines.
     *
     * @param value The raw string to escape.
     * @return The CSV-safe string.
     */
    fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Formats an epoch-millisecond timestamp as `"yyyy-MM-dd HH:mm:ss"`.
     * Returns an empty string for 0L (unset timestamp sentinel).
     *
     * Uses [java.util.Locale.ROOT] for deterministic output in the developer console.
     *
     * @param millis Epoch milliseconds.
     * @return Formatted date-time string, or empty string if [millis] is 0.
     */
    fun formatTimestamp(millis: Long): String {
        if (millis == 0L) return ""
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT)
        return sdf.format(java.util.Date(millis))
    }

    /**
     * Formats an epoch-millisecond timestamp as `"yyyy-MM-dd HH:mm"` (no seconds).
     * Returns an empty string for 0L (unset timestamp sentinel).
     *
     * Uses [java.util.Locale.ROOT] for deterministic output in the developer console.
     *
     * @param millis Epoch milliseconds.
     * @return Formatted date-time string without seconds, or empty if [millis] is 0.
     */
    fun formatTimestampShort(millis: Long): String {
        if (millis == 0L) return ""
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ROOT)
        return sdf.format(java.util.Date(millis))
    }

    /**
     * Formats a duration in seconds as a human-readable string.
     *
     * Examples: `"45s"`, `"3m 15s"`, `"1h 30m 0s"`.
     *
     * @param totalSeconds Duration in seconds.
     * @return Formatted duration string.
     */
    fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds < 0) return "N/A"
        if (totalSeconds < 60) return "${totalSeconds}s"
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        if (minutes < 60) return "${minutes}m ${seconds}s"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return "${hours}h ${remainingMinutes}m ${seconds}s"
    }

    /**
     * Formats a [UserRole] as a Vietnamese display name.
     *
     * @param role The user role.
     * @return Vietnamese role name.
     */
    fun formatRole(role: UserRole): String = when (role) {
        UserRole.GUEST -> "Khach"
        UserRole.USER -> "Nguoi dung"
        UserRole.ADMIN -> "Quan tri vien"
        UserRole.SUPERUSER -> "Sieu quan tri"
    }

    /**
     * Formats an [AdminPermission] as a Vietnamese display name.
     *
     * @param permission The admin permission.
     * @return Vietnamese permission name.
     */
    fun formatPermission(permission: AdminPermission): String = when (permission) {
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
