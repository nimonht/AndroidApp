package com.example.androidapp.domain.service

import com.example.androidapp.domain.model.LogEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain-layer interface for accessing the application log buffer.
 *
 * Provides a reactive stream of log entries and a clear operation
 * without coupling to the concrete
 * [com.example.androidapp.data.logging.LogCollector].
 */
interface LogService {
    /** Reactive stream of captured log entries, oldest to newest. */
    val logs: StateFlow<List<LogEntry>>

    /** Clears all entries from the log buffer. */
    fun clear()

    /**
     * Exports all captured log entries as a single formatted text block.
     *
     * Each entry is rendered on its own line in a human-readable format
     * suitable for sharing or pasting. Returns an empty string if the
     * buffer is empty.
     *
     * @return Formatted multi-line string of all log entries.
     */
    fun export(): String
}
