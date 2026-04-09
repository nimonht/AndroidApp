package com.example.androidapp.ui.screens.advanced.logviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.LogEntry
import com.example.androidapp.domain.model.LogLevel
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.service.LogService
import com.example.androidapp.ui.theme.QuizzezTheme

// -- Level badge colors as ColorScheme extensions -----------------------------
// Intentional: These colors are fixed semantic values matching standard log-level
// conventions (e.g. green=INFO, red=ERROR). They do not vary by light/dark theme
// because the log viewer rows use alternating surface tones where these hues remain
// legible in both modes. If readability regresses under a future theme, provide
// light/dark variants via the theme.

/** Console/log viewer color extensions for [ColorScheme]. */

/** Gray for VERBOSE-level badges. */
val ColorScheme.logVerbose: Color
    get() = Color(0xFF9E9E9E)

/** Blue for DEBUG-level badges. */
val ColorScheme.logDebug: Color
    get() = Color(0xFF2196F3)

/** Green for INFO-level badges. */
val ColorScheme.logInfo: Color
    get() = Color(0xFF4CAF50)

/** Amber for WARN-level badges. */
val ColorScheme.logWarn: Color
    get() = Color(0xFFFFC107)

/** Red for ERROR-level badges. */
val ColorScheme.logError: Color
    get() = Color(0xFFEF5350)

/** Purple for ASSERT-level badges. */
val ColorScheme.logAssert: Color
    get() = Color(0xFFE040FB)

/**
 * Returns the badge color associated with a given [LogLevel] from the current
 * [ColorScheme].
 *
 * @param level The log severity level.
 * @return A [Color] value for the level badge.
 */
@Composable
private fun levelColor(level: LogLevel): Color {
    val cs = MaterialTheme.colorScheme
    return when (level) {
        LogLevel.VERBOSE -> cs.logVerbose
        LogLevel.DEBUG -> cs.logDebug
        LogLevel.INFO -> cs.logInfo
        LogLevel.WARN -> cs.logWarn
        LogLevel.ERROR -> cs.logError
        LogLevel.ASSERT -> cs.logAssert
    }
}

// -- Main screen --------------------------------------------------------------

/**
 * GCP-Cloud-Logging-style log viewer screen.
 *
 * Presents a real-time, filterable view of the application's logcat output
 * with level filter chips, free-text/regex search, a scrollable log list,
 * and a toolbar for clear/export/pause/scroll-to-bottom actions.
 *
 * Stateless composable; all state is owned by [LogViewerViewModel].
 *
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun LogViewerScreen(
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: LogViewerViewModel = viewModel(
        factory = LogViewerViewModelFactory(
            logCollector = container.logCollector,
            authRepository = container.authRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // Collect scroll-to-bottom one-shot events from the Channel
    LaunchedEffect(Unit) {
        viewModel.scrollToBottom.collect {
            if (uiState.filteredLogs.isNotEmpty()) {
                listState.animateScrollToItem(uiState.filteredLogs.size - 1)
            }
        }
    }

    LogViewerScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        listState = listState,
        modifier = modifier
    )
}

/**
 * Stateless content composable for the log viewer, separated for preview
 * and testing without a real ViewModel.
 *
 * @param uiState Current log viewer state.
 * @param onEvent Callback to dispatch [LogViewerEvent]s.
 * @param listState Optional [LazyListState] for external scroll control.
 * @param modifier Modifier for external layout customisation.
 */
@Composable
fun LogViewerScreenContent(
    uiState: LogViewerUiState,
    onEvent: (LogViewerEvent) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    // Auto-scroll on new logs when not paused and near bottom
    LaunchedEffect(uiState.filteredLogs.size) {
        if (!uiState.isPaused && uiState.filteredLogs.isNotEmpty()) {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = uiState.filteredLogs.size
            // Only auto-scroll if user is near the bottom (within 5 items)
            if (totalItems - lastVisibleItem <= 5) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    // Copy exported logs to clipboard when exportedText is set
    val context = LocalContext.current
    LaunchedEffect(uiState.exportedText) {
        val text = uiState.exportedText ?: return@LaunchedEffect
        if (text.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.log_viewer_export_empty),
                Toast.LENGTH_SHORT
            ).show()
            onEvent(LogViewerEvent.ClearExport)
            return@LaunchedEffect
        }
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Quizzez Logs", text))
        Toast.makeText(
            context,
            context.getString(R.string.log_viewer_export_success),
            Toast.LENGTH_SHORT
        ).show()
        onEvent(LogViewerEvent.ClearExport)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar row
            LogViewerToolbar(
                isPaused = uiState.isPaused,
                onTogglePause = { onEvent(LogViewerEvent.TogglePause) },
                onClear = { onEvent(LogViewerEvent.ClearLogs) },
                onExport = { onEvent(LogViewerEvent.ExportLogs) },
                onScrollToBottom = { onEvent(LogViewerEvent.ScrollToBottom) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Level filter chips
            LogLevelFilterRow(
                availableLevels = uiState.availableLevels,
                selectedLevels = uiState.selectedLevels,
                onToggleLevel = { onEvent(LogViewerEvent.ToggleLevel(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Search bar
            LogSearchBar(
                searchQuery = uiState.searchQuery,
                isRegex = uiState.isRegex,
                tagFilter = uiState.tagFilter,
                onSearchChanged = { onEvent(LogViewerEvent.UpdateSearch(it)) },
                onToggleRegex = { onEvent(LogViewerEvent.ToggleRegex) },
                onTagFilterChanged = { onEvent(LogViewerEvent.UpdateTagFilter(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Log list
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.filteredLogs.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.log_viewer_empty_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.log_viewer_empty_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(
                            items = uiState.filteredLogs,
                            key = { it.id }
                        ) { entry ->
                            LogEntryRow(
                                entry = entry,
                                isExpanded = uiState.expandedLogId == entry.id,
                                onClick = { onEvent(LogViewerEvent.ExpandLog(entry.id)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Status bar
            LogStatusBar(
                filteredCount = uiState.filteredCount,
                totalCount = uiState.logCount,
                isPaused = uiState.isPaused,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -- Toolbar ------------------------------------------------------------------

/**
 * Toolbar with action buttons for the log viewer: pause/resume, clear,
 * export, and scroll-to-bottom.
 *
 * @param isPaused Whether log streaming is currently paused.
 * @param onTogglePause Callback to toggle pause state.
 * @param onClear Callback to clear all logs.
 * @param onExport Callback to export filtered logs.
 * @param onScrollToBottom Callback to scroll to the latest log entry.
 * @param modifier Modifier for external layout.
 */
@Composable
private fun LogViewerToolbar(
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onScrollToBottom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Pause / Resume
        IconButton(onClick = onTogglePause) {
            Icon(
                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (isPaused) stringResource(R.string.log_viewer_resume) else stringResource(R.string.log_viewer_pause),
                tint = if (isPaused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Clear
        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(R.string.log_viewer_clear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Export
        IconButton(onClick = onExport) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.log_viewer_export),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Scroll to bottom
        IconButton(onClick = onScrollToBottom) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = stringResource(R.string.log_viewer_scroll_bottom),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -- Level filter chips -------------------------------------------------------

/**
 * Horizontal row of filter chips, one per available log level.
 *
 * Chips are colored by their respective level color and display the
 * single-character level abbreviation.
 *
 * @param availableLevels The set of log levels visible to the current user.
 * @param selectedLevels The set of currently selected/enabled log levels.
 * @param onToggleLevel Callback when a level chip is toggled.
 * @param modifier Modifier for external layout.
 */
@Composable
private fun LogLevelFilterRow(
    availableLevels: Set<LogLevel>,
    selectedLevels: Set<LogLevel>,
    onToggleLevel: (LogLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedLevels = remember(availableLevels) {
        availableLevels.sortedBy { it.ordinal }
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (level in sortedLevels) {
            val isSelected = level in selectedLevels
            val badgeColor = levelColor(level)

            FilterChip(
                selected = isSelected,
                onClick = { onToggleLevel(level) },
                label = {
                    Text(
                        text = level.abbreviation,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = badgeColor.copy(alpha = 0.2f),
                    selectedLabelColor = badgeColor,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = badgeColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

// -- Search bar ---------------------------------------------------------------

/**
 * Combined search and tag filter bar for the log viewer.
 *
 * Contains a text input for free-text or regex search with a regex toggle
 * button, and a secondary input for tag prefix filtering.
 *
 * @param searchQuery Current search query text.
 * @param isRegex Whether regex mode is enabled.
 * @param tagFilter Current tag prefix filter text.
 * @param onSearchChanged Callback when search text changes.
 * @param onToggleRegex Callback to toggle regex mode.
 * @param onTagFilterChanged Callback when tag filter text changes.
 * @param modifier Modifier for external layout.
 */
@Composable
private fun LogSearchBar(
    searchQuery: String,
    isRegex: Boolean,
    tagFilter: String,
    onSearchChanged: (String) -> Unit,
    onToggleRegex: () -> Unit,
    onTagFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Search input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Search icon
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Search text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.log_viewer_search_logs_hint),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        ),
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Regex toggle
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isRegex) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .clickable(onClick = onToggleRegex)
                    .padding(2.dp)
            ) {
                Text(
                    text = ".*",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isRegex) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Tag filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.log_viewer_tag_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (tagFilter.isEmpty()) {
                    Text(
                        text = stringResource(R.string.log_viewer_tag_filter_hint),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        ),
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = tagFilter,
                    onValueChange = onTagFilterChanged,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -- Log entry row ------------------------------------------------------------

/**
 * A single log entry row in the log list. Tapping expands the row to show
 * full details (timestamp, thread name, complete message).
 *
 * The collapsed view shows: timestamp (HH:mm:ss.SSS), level badge, tag,
 * and truncated message.
 *
 * @param entry The [LogEntry] to render.
 * @param isExpanded Whether the detail view is currently expanded.
 * @param onClick Callback when the row is tapped.
 * @param modifier Modifier for external layout.
 */
@Composable
private fun LogEntryRow(
    entry: LogEntry,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = levelColor(entry.level)
    val timeFormat = remember {
        java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
    }
    val timestamp = remember(entry.timestamp) {
        timeFormat.format(java.util.Date(entry.timestamp))
    }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Collapsed row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Timestamp
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            // Level badge
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = badgeColor.copy(alpha = 0.2f),
                modifier = Modifier.size(width = 20.dp, height = 18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = entry.level.abbreviation,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        ),
                        color = badgeColor
                    )
                }
            }

            // Tag
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(80.dp)
            )

            // Message (truncated)
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Expanded detail
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LogDetailField(
                    label = stringResource(R.string.log_viewer_detail_timestamp),
                    value = remember(entry.timestamp) {
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss.SSS",
                            java.util.Locale.US
                        ).format(java.util.Date(entry.timestamp))
                    }
                )
                LogDetailField(
                    label = stringResource(R.string.log_viewer_detail_level),
                    value = "${entry.level.name} (${entry.level.abbreviation})"
                )
                LogDetailField(
                    label = stringResource(R.string.log_viewer_detail_tag),
                    value = entry.tag
                )
                LogDetailField(
                    label = stringResource(R.string.log_viewer_detail_thread),
                    value = entry.threadName
                )
                LogDetailField(
                    label = stringResource(R.string.log_viewer_detail_message),
                    value = entry.message
                )
            }
        }

        // Subtle separator between rows
        if (!isExpanded) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * A label-value pair used in the expanded log entry detail view.
 *
 * @param label The field label.
 * @param value The field value.
 * @param modifier Modifier for external layout.
 */
@Composable
private fun LogDetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// -- Status bar ---------------------------------------------------------------

/**
 * Status bar at the bottom of the log viewer displaying the count of
 * visible entries vs. total entries and the pause indicator.
 *
 * @param filteredCount Number of entries after filtering.
 * @param totalCount Total number of entries in the buffer.
 * @param isPaused Whether log streaming is paused.
 * @param modifier Modifier for external layout.
 */
@Composable
private fun LogStatusBar(
    filteredCount: Int,
    totalCount: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.log_viewer_status, filteredCount, totalCount),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isPaused) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = stringResource(R.string.log_viewer_paused_badge),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// -- ViewModel Factory --------------------------------------------------------

/**
 * Factory for creating [LogViewerViewModel] instances with manual DI.
 *
 * @param logCollector The application's [LogService] instance.
 * @param authRepository The auth repository for role-based filtering.
 */
class LogViewerViewModelFactory(
    private val logCollector: LogService,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(LogViewerViewModel::class.java)) {
            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}. Expected LogViewerViewModel."
            )
        }
        return LogViewerViewModel(
            logCollector = logCollector,
            authRepository = authRepository
        ) as T
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(
    name = "LogViewer - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun LogViewerScreenLightPreview() {
    QuizzezTheme(darkTheme = false) {
        LogViewerScreenContent(
            uiState = previewUiState(),
            onEvent = {}
        )
    }
}

@Preview(
    name = "LogViewer - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LogViewerScreenDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        LogViewerScreenContent(
            uiState = previewUiState(),
            onEvent = {}
        )
    }
}

@Preview(
    name = "LogViewer Empty - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun LogViewerEmptyLightPreview() {
    QuizzezTheme(darkTheme = false) {
        LogViewerScreenContent(
            uiState = LogViewerUiState(),
            onEvent = {}
        )
    }
}

@Preview(
    name = "LogViewer Empty - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LogViewerEmptyDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        LogViewerScreenContent(
            uiState = LogViewerUiState(),
            onEvent = {}
        )
    }
}

/**
 * Produces a sample [LogViewerUiState] for preview composables.
 */
private fun previewUiState(): LogViewerUiState {
    val sampleLogs = listOf(
        LogEntry(
            id = 1L,
            timestamp = System.currentTimeMillis() - 5000,
            level = LogLevel.INFO,
            tag = "QuizRepo",
            message = "Loaded 42 quizzes from Firestore",
            threadName = "tid-12345"
        ),
        LogEntry(
            id = 2L,
            timestamp = System.currentTimeMillis() - 4000,
            level = LogLevel.WARN,
            tag = "SyncMgr",
            message = "Network timeout, retrying in 5s...",
            threadName = "tid-12346"
        ),
        LogEntry(
            id = 3L,
            timestamp = System.currentTimeMillis() - 3000,
            level = LogLevel.ERROR,
            tag = "AuthRepo",
            message = "Token expired: FirebaseAuthInvalidCredentialsException",
            threadName = "tid-12347"
        ),
        LogEntry(
            id = 4L,
            timestamp = System.currentTimeMillis() - 2000,
            level = LogLevel.DEBUG,
            tag = "NavHost",
            message = "Navigating to route: quiz/detail/abc123",
            threadName = "tid-12348"
        ),
        LogEntry(
            id = 5L,
            timestamp = System.currentTimeMillis() - 1000,
            level = LogLevel.INFO,
            tag = "SyncMgr",
            message = "Sync completed successfully: 3 pending operations processed",
            threadName = "tid-12349"
        )
    )
    return LogViewerUiState(
        allLogs = sampleLogs,
        filteredLogs = sampleLogs,
        availableLevels = LogLevel.ALL_LEVELS,
        selectedLevels = LogLevel.ALL_LEVELS,
        logCount = sampleLogs.size,
        filteredCount = sampleLogs.size,
        isAdmin = true
    )
}
