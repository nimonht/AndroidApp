package com.example.androidapp.data.logging

import android.os.Process
import com.example.androidapp.domain.model.LogEntry
import com.example.androidapp.domain.model.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Singleton-style ring-buffer log collector that captures logcat output
 * from the current application process.
 *
 * Reads `logcat -v threadtime` filtered to the current PID and parses
 * each line into a [LogEntry]. Maintains an in-memory ring buffer capped
 * at [maxEntries] items, dropping the oldest entries when the limit is
 * exceeded.
 *
 * Exposes a [StateFlow] of the current log entries for reactive UI
 * consumption (e.g. the Log Viewer screen).
 *
 * **Lifecycle**: call [install] once at application startup (typically
 * from the DI container). The reader coroutine runs on [Dispatchers.IO]
 * within the provided [scope] and is cancelled when the scope is cancelled
 * or when the logcat process terminates.
 *
 * @param scope The [CoroutineScope] used to launch the logcat reader coroutine.
 *   Typically the application-scoped coroutine scope from the DI container.
 * @param maxEntries Maximum number of log entries to retain in the ring buffer.
 *   Defaults to 10,000.
 */
class LogCollector(
    private val scope: CoroutineScope,
    private val maxEntries: Int = 10_000
) {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())

    /**
     * Reactive stream of captured log entries, ordered from oldest to newest.
     * The list is replaced (not mutated) on each update for safe snapshot reads.
     */
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val nextId = AtomicLong(0)
    private var readerJob: Job? = null

    /**
     * Regex pattern matching the logcat `threadtime` format:
     *
     * ```
     * 04-08 10:30:15.123  1234  5678 I TagName  : Message here
     * ```
     *
     * Groups:
     * 1. date     — `MM-dd`
     * 2. time     — `HH:mm:ss.SSS`
     * 3. pid      — process ID
     * 4. tid      — thread ID
     * 5. level    — single char (V/D/I/W/E/F/A)
     * 6. tag      — logger tag (may have trailing spaces)
     * 7. message  — log message body
     */
    private val logcatPattern = Regex(
        """^(\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFA])\s+(.+?)\s*:\s(.*)$"""
    )

    /**
     * Date format used to parse the logcat timestamp into epoch milliseconds.
     * Logcat outputs `MM-dd HH:mm:ss.SSS`; we prepend the current year.
     */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * Starts the logcat reader coroutine. Safe to call multiple times;
     * subsequent calls are no-ops if a reader is already active.
     *
     * The reader spawns a `logcat` process filtered to the current PID
     * and continuously parses its stdout line by line. Parsed entries
     * are appended to the ring buffer and emitted via [logs].
     */
    fun install() {
        if (readerJob?.isActive == true) return

        readerJob = scope.launch(Dispatchers.IO) {
            val pid = Process.myPid()
            val process: java.lang.Process = try {
                Runtime.getRuntime().exec(
                    arrayOf("logcat", "-v", "threadtime", "--pid", pid.toString())
                )
            } catch (e: Exception) {
                // If logcat is unavailable (rare on real devices), bail out silently.
                return@launch
            }

            val reader = BufferedReader(InputStreamReader(process.inputStream))

            try {
                var line: String?
                while (isActive) {
                    line = reader.readLine() ?: break
                    val entry = parseLine(line) ?: continue
                    appendEntry(entry)
                }
            } catch (_: Exception) {
                // Stream closed or process killed — stop gracefully.
            } finally {
                try {
                    reader.close()
                } catch (_: Exception) { /* best-effort */ }
                try {
                    process.destroy()
                } catch (_: Exception) { /* best-effort */ }
            }
        }
    }

    /**
     * Clears all captured log entries and resets the buffer.
     */
    fun clear() {
        _logs.value = emptyList()
    }

    /**
     * Exports all currently captured log entries as a single formatted
     * text string suitable for sharing or saving to a file.
     *
     * Each line follows the format:
     * ```
     * [YYYY-MM-dd HH:mm:ss.SSS] LEVEL/Tag (thread): Message
     * ```
     *
     * @return A newline-separated string of all log entries, or an empty
     *   string if no entries are captured.
     */
    fun export(): String {
        val exportFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return _logs.value.joinToString("\n") { entry ->
            val timestamp = exportFormat.format(entry.timestamp)
            val level = entry.level.abbreviation
            "[$timestamp] $level/${entry.tag} (${entry.threadName}): ${entry.message}"
        }
    }

    /**
     * Parses a single logcat threadtime-formatted line into a [LogEntry].
     *
     * @param line Raw line from logcat stdout.
     * @return A [LogEntry] if the line matches the expected format, or `null`
     *   if the line is a header, blank, or otherwise unparseable.
     */
    private fun parseLine(line: String): LogEntry? {
        val match = logcatPattern.matchEntire(line) ?: return null

        val datePart = match.groupValues[1]   // MM-dd
        val timePart = match.groupValues[2]   // HH:mm:ss.SSS
        val tid = match.groupValues[4]        // thread ID
        val levelChar = match.groupValues[5].first()
        val tag = match.groupValues[6].trim()
        val message = match.groupValues[7]

        val year = Calendar.getInstance().get(Calendar.YEAR)
        val timestamp = try {
            dateFormat.parse("$year-$datePart $timePart")?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        val level = LogLevel.fromLogcatChar(levelChar)

        // Use the TID as the thread name since logcat threadtime does not
        // provide the actual thread name. The TID is still useful for
        // distinguishing which thread produced the log.
        val threadName = "tid-$tid"

        return LogEntry(
            id = nextId.getAndIncrement(),
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message,
            threadName = threadName
        )
    }

    /**
     * Appends a [LogEntry] to the ring buffer, dropping the oldest entries
     * if the buffer exceeds [maxEntries].
     *
     * This method replaces the entire list in the [MutableStateFlow] to
     * ensure atomic snapshot semantics for collectors.
     */
    private fun appendEntry(entry: LogEntry) {
        val current = _logs.value
        val updated = if (current.size >= maxEntries) {
            // Drop the oldest entries to maintain the ring buffer size.
            // Drop enough to avoid repeated trimming on every single entry
            // when running at capacity — drop 5% at a time.
            val dropCount = (maxEntries * 0.05).toInt().coerceAtLeast(1)
            current.drop(dropCount) + entry
        } else {
            current + entry
        }
        _logs.value = updated
    }
}
