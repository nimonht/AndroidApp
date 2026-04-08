package com.example.androidapp.ui.screens.advanced.logviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.LogEntry
import com.example.androidapp.domain.model.LogLevel
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.service.LogService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI state for the log viewer screen.
 *
 * @property allLogs The complete unfiltered log buffer from [LogService].
 * @property filteredLogs Logs after applying level, search, and tag filters.
 * @property availableLevels Log levels the current user is allowed to see.
 * @property selectedLevels Log levels currently selected for display.
 * @property searchQuery Free-text or regex search applied to log messages.
 * @property isRegex Whether [searchQuery] is interpreted as a regular expression.
 * @property tagFilter Optional tag prefix filter (case-insensitive).
 * @property isPaused Whether live log streaming is paused.
 * @property expandedLogId The [LogEntry.id] of the currently expanded row, or null.
 * @property logCount Total number of logs in the buffer.
 * @property filteredCount Number of logs after filtering.
 * @property isAdmin Whether the current user has admin privileges.
 * @property exportedText The most recent export result text, or null if no export pending.
 */
data class LogViewerUiState(
    val allLogs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val availableLevels: Set<LogLevel> = LogLevel.USER_VISIBLE_LEVELS,
    val selectedLevels: Set<LogLevel> = LogLevel.USER_VISIBLE_LEVELS,
    val searchQuery: String = "",
    val isRegex: Boolean = false,
    val tagFilter: String = "",
    val isPaused: Boolean = false,
    val expandedLogId: Long? = null,
    val logCount: Int = 0,
    val filteredCount: Int = 0,
    val isAdmin: Boolean = false,
    val exportedText: String? = null
)

/**
 * Events dispatched from the log viewer UI to [LogViewerViewModel].
 */
sealed class LogViewerEvent {
    /** Toggle a specific log level on or off in the filter chip bar. */
    data class ToggleLevel(val level: LogLevel) : LogViewerEvent()

    /** Update the free-text / regex search query. */
    data class UpdateSearch(val query: String) : LogViewerEvent()

    /** Toggle between plain-text and regex search mode. */
    data object ToggleRegex : LogViewerEvent()

    /** Update the tag prefix filter. */
    data class UpdateTagFilter(val tag: String) : LogViewerEvent()

    /** Pause or resume live log streaming. */
    data object TogglePause : LogViewerEvent()

    /** Clear all buffered logs. */
    data object ClearLogs : LogViewerEvent()

    /** Export the current filtered logs as formatted text. */
    data object ExportLogs : LogViewerEvent()

    /** Expand or collapse a log entry detail view. Pass null to collapse. */
    data class ExpandLog(val id: Long?) : LogViewerEvent()

    /** Scroll the log list to the most recent entry. */
    data object ScrollToBottom : LogViewerEvent()

    /** Acknowledge and clear the exported text result. */
    data object ClearExport : LogViewerEvent()
}

/**
 * ViewModel for the GCP-Cloud-Logging-style log viewer.
 *
 * Collects the live log stream from [LogService], applies role-based
 * level visibility and user-chosen filters (level chips, search query,
 * tag prefix), and exposes the resulting list via [uiState].
 *
 * @param logCollector The application-wide log service (ring-buffer collector).
 * @param authRepository Repository for resolving the current user and role.
 */
class LogViewerViewModel(
    private val logCollector: LogService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogViewerUiState())

    /** Observable UI state for the log viewer screen. */
    val uiState: StateFlow<LogViewerUiState> = _uiState.asStateFlow()

    /** Channel-based one-shot signal to scroll the list to the bottom. */
    private val _scrollToBottom = Channel<Unit>(Channel.CONFLATED)

    /** Flow that emits a [Unit] each time the UI should scroll to the bottom. */
    val scrollToBottom = _scrollToBottom.receiveAsFlow()

    init {
        observeCurrentUser()
        observeLogs()
    }

    /**
     * Central event dispatcher. All UI interactions funnel through this method.
     *
     * @param event The [LogViewerEvent] to handle.
     */
    fun onEvent(event: LogViewerEvent) {
        when (event) {
            is LogViewerEvent.ToggleLevel -> handleToggleLevel(event.level)
            is LogViewerEvent.UpdateSearch -> handleUpdateSearch(event.query)
            is LogViewerEvent.ToggleRegex -> handleToggleRegex()
            is LogViewerEvent.UpdateTagFilter -> handleUpdateTagFilter(event.tag)
            is LogViewerEvent.TogglePause -> handleTogglePause()
            is LogViewerEvent.ClearLogs -> handleClearLogs()
            is LogViewerEvent.ExportLogs -> handleExportLogs()
            is LogViewerEvent.ExpandLog -> handleExpandLog(event.id)
            is LogViewerEvent.ScrollToBottom -> handleScrollToBottom()
            is LogViewerEvent.ClearExport -> _uiState.update { it.copy(exportedText = null) }
        }
    }

    // -- Observers --------------------------------------------------------------

    /**
     * Observes the current user to determine admin status and available log levels.
     */
    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val isAdmin = user?.isAdmin() == true
                val availableLevels = if (isAdmin) {
                    LogLevel.ALL_LEVELS
                } else {
                    LogLevel.USER_VISIBLE_LEVELS
                }
                // When switching roles, clamp selected levels to the available set
                _uiState.update { current ->
                    val clampedSelected = current.selectedLevels.intersect(availableLevels)
                    val newSelected = clampedSelected.ifEmpty { availableLevels }
                    current.copy(
                        isAdmin = isAdmin,
                        availableLevels = availableLevels,
                        selectedLevels = newSelected
                    ).let { applyFilters(it) }
                }
            }
        }
    }

    /**
     * Observes the live log stream from [LogService] and applies filters.
     * When paused, incoming updates are silently discarded; the frozen
     * snapshot is retained until the user resumes.
     */
    private fun observeLogs() {
        viewModelScope.launch {
            logCollector.logs.collect { logs ->
                _uiState.update { current ->
                    if (current.isPaused) {
                        // While paused, only update the total count for the status bar
                        current.copy(logCount = logs.size)
                    } else {
                        val updated = current.copy(
                            allLogs = logs,
                            logCount = logs.size
                        )
                        applyFilters(updated)
                    }
                }
            }
        }
    }

    // -- Event handlers ---------------------------------------------------------

    /**
     * Toggles a log level in the selected set. At least one level must remain.
     */
    private fun handleToggleLevel(level: LogLevel) {
        _uiState.update { current ->
            if (level !in current.availableLevels) return@update current
            val newSelected = if (level in current.selectedLevels) {
                // Don't allow deselecting the last remaining level
                val candidate = current.selectedLevels - level
                if (candidate.isEmpty()) current.selectedLevels else candidate
            } else {
                current.selectedLevels + level
            }
            applyFilters(current.copy(selectedLevels = newSelected))
        }
    }

    /**
     * Updates the search query and re-applies filters.
     */
    private fun handleUpdateSearch(query: String) {
        _uiState.update { current ->
            applyFilters(current.copy(searchQuery = query))
        }
    }

    /**
     * Toggles regex mode for the search query.
     */
    private fun handleToggleRegex() {
        _uiState.update { current ->
            applyFilters(current.copy(isRegex = !current.isRegex))
        }
    }

    /**
     * Updates the tag prefix filter.
     */
    private fun handleUpdateTagFilter(tag: String) {
        _uiState.update { current ->
            applyFilters(current.copy(tagFilter = tag))
        }
    }

    /**
     * Pauses or resumes live streaming. On pause, the current log list is
     * frozen in [uiState]. On resume, the live stream replaces the snapshot.
     */
    private fun handleTogglePause() {
        _uiState.update { current ->
            val nowPaused = !current.isPaused
            if (nowPaused) {
                // Freeze the current log list
                current.copy(isPaused = true)
            } else {
                // Resume: reload from the collector's live buffer
                val liveLogs = logCollector.logs.value
                applyFilters(
                    current.copy(
                        isPaused = false,
                        allLogs = liveLogs,
                        logCount = liveLogs.size
                    )
                )
            }
        }
    }

    /**
     * Clears all logs from the collector's ring buffer.
     */
    private fun handleClearLogs() {
        logCollector.clear()
        _uiState.update { current ->
            current.copy(
                allLogs = emptyList(),
                filteredLogs = emptyList(),
                logCount = 0,
                filteredCount = 0,
                expandedLogId = null,
                isPaused = false
            )
        }
    }

    /**
     * Exports the currently filtered logs as a formatted text block.
     */
    private fun handleExportLogs() {
        val state = _uiState.value
        val logsToExport = state.filteredLogs
        if (logsToExport.isEmpty()) {
            _uiState.update { it.copy(exportedText = "") }
            return
        }
        val text = buildString {
            appendLine("=== Xuat Nhat Ky Quizzez ===")
            appendLine("Tong muc: ${logsToExport.size}")
            appendLine(
                "Bo loc: muc do=${state.selectedLevels.joinToString(",") { it.abbreviation }}" +
                        ", tim kiem=\"${state.searchQuery}\"" +
                        ", tag=\"${state.tagFilter}\""
            )
            appendLine()
            for (entry in logsToExport) {
                appendLine(formatLogEntry(entry))
            }
        }
        _uiState.update { it.copy(exportedText = text) }
    }

    /**
     * Expands or collapses a log detail row. If [id] matches the currently
     * expanded entry, it collapses; otherwise it expands the new entry.
     */
    private fun handleExpandLog(id: Long?) {
        _uiState.update { current ->
            val newId = if (current.expandedLogId == id) null else id
            current.copy(expandedLogId = newId)
        }
    }

    /**
     * Signals the UI to scroll to the bottom of the log list via a
     * conflated [Channel], avoiding the timing-hack approach.
     */
    private fun handleScrollToBottom() {
        viewModelScope.launch {
            _scrollToBottom.send(Unit)
        }
    }

    // -- Filtering logic --------------------------------------------------------

    /**
     * Applies all active filters (level, search, tag) to the log list in [state]
     * and returns a new state with [LogViewerUiState.filteredLogs] and
     * [LogViewerUiState.filteredCount] updated.
     *
     * @param state The current state containing filter parameters and raw logs.
     * @return A copy of [state] with filtered results applied.
     */
    private fun applyFilters(state: LogViewerUiState): LogViewerUiState {
        val logs = state.allLogs
        val selectedLevels = state.selectedLevels
        val searchQuery = state.searchQuery.trim()
        val tagFilter = state.tagFilter.trim()
        val isRegex = state.isRegex

        // Compile regex once if needed (fall back to literal on invalid patterns)
        val searchRegex: Regex? = if (isRegex && searchQuery.isNotEmpty()) {
            try {
                Regex(searchQuery, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                null // Invalid regex -- skip search filtering
            }
        } else {
            null
        }

        val filtered = logs.filter { entry ->
            // Level filter
            if (entry.level !in selectedLevels) return@filter false

            // Tag prefix filter
            if (tagFilter.isNotEmpty()) {
                if (!entry.tag.startsWith(tagFilter, ignoreCase = true)) return@filter false
            }

            // Search query filter
            if (searchQuery.isNotEmpty()) {
                if (isRegex) {
                    if (searchRegex == null) return@filter true // invalid regex => show all
                    val matchesMessage = searchRegex.containsMatchIn(entry.message)
                    val matchesTag = searchRegex.containsMatchIn(entry.tag)
                    if (!matchesMessage && !matchesTag) return@filter false
                } else {
                    val matchesMessage = entry.message.contains(searchQuery, ignoreCase = true)
                    val matchesTag = entry.tag.contains(searchQuery, ignoreCase = true)
                    if (!matchesMessage && !matchesTag) return@filter false
                }
            }

            true
        }

        return state.copy(
            filteredLogs = filtered,
            filteredCount = filtered.size
        )
    }

    // -- Formatting helpers -----------------------------------------------------

    /**
     * Formats a single [LogEntry] into a human-readable text line for export.
     *
     * @param entry The log entry to format.
     * @return A single-line string representation.
     */
    private fun formatLogEntry(entry: LogEntry): String {
        val time = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.US
        ).format(Date(entry.timestamp))
        return "$time ${entry.level.abbreviation}/${entry.tag} [${entry.threadName}]: ${entry.message}"
    }
}
