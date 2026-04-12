package com.example.androidapp.domain.model

/**
 * Domain model representing a single log entry captured from the application process.
 *
 * This is a pure domain model with no Android dependencies. The actual log
 * collection mechanism lives in the data layer (`LogCollector`).
 *
 * @property id Auto-increment sequence number for ordering.
 * @property timestamp Epoch milliseconds when the log was recorded.
 * @property level Severity level of the log entry.
 * @property tag Logger tag identifying the source component.
 * @property message The log message content.
 * @property threadName Name of the thread that produced the log entry.
 */
data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val threadName: String
)

/**
 * Log severity levels matching the standard Android log levels.
 *
 * Ordered from least to most severe. The ordinal values can be used
 * for comparison: `LogLevel.ERROR.ordinal > LogLevel.INFO.ordinal`.
 *
 * Role-based visibility:
 * - Regular USERs see only [INFO], [WARN], [ERROR], and [ASSERT].
 * - ADMINs and SUPERUSERs see all levels including [VERBOSE] and [DEBUG].
 */
enum class LogLevel {
    /** Verbose: finest-grained informational events. */
    VERBOSE,

    /** Debug: fine-grained informational events useful for debugging. */
    DEBUG,

    /** Info: informational messages highlighting application progress. */
    INFO,

    /** Warn: potentially harmful situations. */
    WARN,

    /** Error: error events that might still allow the app to continue. */
    ERROR,

    /** Assert: severe error events that will presumably lead the app to abort. */
    ASSERT;

    /**
     * Single-character abbreviation used in log display badges.
     */
    val abbreviation: String
        get() = when (this) {
            VERBOSE -> "V"
            DEBUG -> "D"
            INFO -> "I"
            WARN -> "W"
            ERROR -> "E"
            ASSERT -> "A"
        }

    companion object {
        /**
         * Set of log levels visible to regular (non-admin) users.
         * Excludes [VERBOSE] and [DEBUG] which may contain sensitive
         * internal details such as Firebase tokens or implementation specifics.
         */
        val USER_VISIBLE_LEVELS: Set<LogLevel> = setOf(INFO, WARN, ERROR, ASSERT)

        /**
         * All log levels, visible to admin and superuser roles.
         */
        val ALL_LEVELS: Set<LogLevel> = entries.toSet()

        /**
         * Parse a log level from its priority character as used by logcat.
         *
         * @param char The priority character: V, D, I, W, E, or A/F.
         * @return The corresponding [LogLevel], or [VERBOSE] as fallback.
         */
        fun fromLogcatChar(char: Char): LogLevel = when (char.uppercaseChar()) {
            'V' -> VERBOSE
            'D' -> DEBUG
            'I' -> INFO
            'W' -> WARN
            'E' -> ERROR
            'A', 'F' -> ASSERT
            else -> VERBOSE
        }

        /**
         * Parse a log level from a string name (case-insensitive).
         * Accepts both full names ("error") and abbreviations ("e").
         *
         * @param value The string to parse.
         * @return The corresponding [LogLevel], or null if unrecognised.
         */
        fun fromString(value: String): LogLevel? {
            val normalized = value.trim().uppercase()
            // Accept "warning" as a common alias for the WARN level
            if (normalized == "WARNING") return WARN
            return entries.find { it.name == normalized }
                ?: entries.find { it.abbreviation == normalized }
        }
    }
}
