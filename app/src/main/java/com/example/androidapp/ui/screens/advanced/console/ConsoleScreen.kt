package com.example.androidapp.ui.screens.advanced.console

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.ui.screens.advanced.console.components.ConsoleInputField
import com.example.androidapp.ui.screens.advanced.console.components.ConsoleOutputLine
import com.example.androidapp.ui.screens.advanced.console.components.SuggestionDropdown
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Terminal-emulator-style console screen for executing in-app commands.
 *
 * Displays a scrollable output area with styled lines, a suggestion dropdown
 * for autocomplete, a custom input field with token highlighting, and a
 * status bar showing network state and user role.
 *
 * Stateless composable; all state is owned by [ConsoleViewModel].
 *
 * @param modifier Modifier for styling and layout.
 */
@Composable
fun ConsoleScreen(
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: ConsoleViewModel = viewModel(
        factory = ConsoleViewModelFactory(
            commandExecutor = container.commandExecutor,
            authRepository = container.authRepository,
            networkMonitor = container.networkMonitor,
            logCollector = container.logCollector,
            syncManager = container.syncManager,
            settingsPreferences = container.settingsPreferences,
            adminRepository = container.adminRepository,
            quizRepository = container.quizRepository,
            attemptRepository = container.attemptRepository,
            shareCodeRepository = container.shareCodeRepository,
            poolRepository = container.poolRepository
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ConsoleScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

/**
 * Stateless content composable for the console screen.
 *
 * Separated from [ConsoleScreen] to enable preview and testing without
 * a real ViewModel.
 *
 * @param uiState The current console UI state.
 * @param onEvent Callback to dispatch [ConsoleEvent]s.
 * @param modifier Modifier for styling and layout.
 */
@Composable
fun ConsoleScreenContent(
    uiState: ConsoleUiState,
    onEvent: (ConsoleEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new output is added
    LaunchedEffect(uiState.outputLines.size) {
        if (uiState.outputLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.outputLines.size - 1)
        }
    }

    val prompt = remember(uiState.userName, uiState.userRole) {
        val suffix = if (uiState.userRole >= UserRole.ADMIN) "#" else "$"
        val name = uiState.userName.ifEmpty { "guest" }
        "[$name]$suffix "
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Output area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.outputLines.isEmpty()) {
                    // Welcome message
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            // TODO: move to strings.xml
                            text = "Quizzez Console",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            // TODO: move to strings.xml
                            text = "Go 'help' de xem danh sach lenh",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        items(
                            items = uiState.outputLines,
                            key = { it.id }
                        ) { line ->
                            ConsoleOutputLine(
                                line = line,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Suggestion dropdown (shown above input)
            if (uiState.showSuggestions && uiState.suggestions.isNotEmpty()) {
                SuggestionDropdown(
                    suggestions = uiState.suggestions,
                    selectedIndex = uiState.selectedSuggestionIndex,
                    onSelect = { index -> onEvent(ConsoleEvent.SelectSuggestion(index)) },
                    onDismiss = { onEvent(ConsoleEvent.DismissSuggestions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Input area
            ConsoleInputField(
                value = uiState.currentInput,
                onValueChange = { text, cursor ->
                    onEvent(ConsoleEvent.InputChanged(text, cursor))
                },
                ghostText = uiState.ghostText,
                prompt = prompt,
                onSubmit = { onEvent(ConsoleEvent.Submit) },
                onTabPress = { onEvent(ConsoleEvent.AcceptSuggestion) },
                onUpPress = { onEvent(ConsoleEvent.HistoryUp) },
                onDownPress = { onEvent(ConsoleEvent.HistoryDown) },
                isExecuting = uiState.isExecuting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Status bar
            ConsoleStatusBar(
                networkStatus = uiState.networkStatus,
                userRole = uiState.userRole,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Status bar at the bottom of the console showing network state and user role.
 *
 * @param networkStatus Whether the device is currently online.
 * @param userRole The current user's role for display.
 * @param modifier Modifier for styling and layout.
 */
@Composable
fun ConsoleStatusBar(
    networkStatus: Boolean,
    userRole: UserRole,
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
            // Network indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (networkStatus) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    // TODO: move to strings.xml
                    text = if (networkStatus) "Truc tuyen" else "Ngoai tuyen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            // Role badge
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = when (userRole) {
                    UserRole.SUPERUSER -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ) {
                Text(
                    text = userRole.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (userRole) {
                        UserRole.SUPERUSER -> MaterialTheme.colorScheme.error
                        UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// -- Previews -------------------------------------------------------------------

@Preview(name = "Console - Light", showBackground = true)
@Preview(
    name = "Console - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleScreenContentPreview() {
    QuizzezTheme {
        ConsoleScreenContent(
            uiState = ConsoleUiState(
                outputLines = listOf(
                    StyledOutputLine(
                        text = "[user]\$ help",
                        style = OutputStyle.MUTED,
                        id = 1L
                    ),
                    StyledOutputLine(
                        text = "DANH SACH LENH",
                        style = OutputStyle.HEADER,
                        id = 2L
                    ),
                    StyledOutputLine(
                        text = "  help     - Hien thi danh sach lenh",
                        style = OutputStyle.NORMAL,
                        id = 3L
                    ),
                    StyledOutputLine(
                        text = "  clear    - Xoa man hinh console",
                        style = OutputStyle.NORMAL,
                        id = 4L
                    ),
                    StyledOutputLine(
                        text = "Lenh da thuc thi thanh cong.",
                        style = OutputStyle.SUCCESS,
                        id = 5L
                    )
                ),
                userName = "user",
                userRole = UserRole.USER,
                networkStatus = true
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Console Empty - Light", showBackground = true)
@Preview(
    name = "Console Empty - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleScreenEmptyPreview() {
    QuizzezTheme {
        ConsoleScreenContent(
            uiState = ConsoleUiState(),
            onEvent = {}
        )
    }
}

@Preview(name = "Status Bar - Light", showBackground = true)
@Preview(
    name = "Status Bar - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleStatusBarPreview() {
    QuizzezTheme {
        Column {
            ConsoleStatusBar(
                networkStatus = true,
                userRole = UserRole.ADMIN
            )
            ConsoleStatusBar(
                networkStatus = false,
                userRole = UserRole.USER
            )
            ConsoleStatusBar(
                networkStatus = true,
                userRole = UserRole.SUPERUSER
            )
        }
    }
}
