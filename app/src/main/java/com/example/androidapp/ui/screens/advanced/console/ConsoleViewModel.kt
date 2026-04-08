package com.example.androidapp.ui.screens.advanced.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.service.NetworkService
import com.example.androidapp.domain.console.CommandExecutor
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A single styled line in the console output area.
 *
 * @property text The text content of this output line.
 * @property style The [OutputStyle] controlling color and formatting.
 * @property id Unique identifier for stable Compose keys, generated via [nextId].
 */
data class StyledOutputLine(
    val text: String,
    val style: OutputStyle,
    val id: Long = nextId()
) {
    companion object {
        private val counter = java.util.concurrent.atomic.AtomicLong(0)

        /** Generates a unique, monotonically increasing ID for stable Compose keys. */
        fun nextId(): Long = counter.incrementAndGet()
    }
}

/**
 * UI state for the developer console screen.
 *
 * @property outputLines All rendered output lines currently visible in the console.
 * @property currentInput The text currently in the input field.
 * @property cursorPosition Cursor index within [currentInput].
 * @property suggestions Active autocomplete suggestions from the command engine.
 * @property selectedSuggestionIndex Index of the highlighted suggestion, or -1 if none.
 * @property ghostText Semi-transparent preview text shown after the cursor (first suggestion).
 * @property isExecuting Whether a command is currently being executed.
 * @property networkStatus Current network connectivity state.
 * @property userRole Role of the authenticated user.
 * @property userName Display name or username of the authenticated user.
 * @property showSuggestions Whether the suggestion dropdown is visible.
 * @property prompt Shell-style prompt string displayed before the input cursor.
 */
data class ConsoleUiState(
    val outputLines: List<StyledOutputLine> = emptyList(),
    val currentInput: String = "",
    val cursorPosition: Int = 0,
    val suggestions: List<CompletionSuggestion> = emptyList(),
    val selectedSuggestionIndex: Int = -1,
    val ghostText: String = "",
    val isExecuting: Boolean = false,
    val networkStatus: Boolean = true,
    val userRole: UserRole = UserRole.USER,
    val userName: String = "",
    val showSuggestions: Boolean = false,
    val prompt: String = "[user]$ "
)

/**
 * Events dispatched from the console UI to [ConsoleViewModel].
 */
sealed class ConsoleEvent {
    /**
     * The input text or cursor position changed.
     *
     * @property text Updated input text.
     * @property cursor Updated cursor position.
     */
    data class InputChanged(val text: String, val cursor: Int) : ConsoleEvent()

    /** User pressed Enter / submitted the current input. */
    data object Submit : ConsoleEvent()

    /** User pressed Tab or tapped to accept the current ghost-text / top suggestion. */
    data object AcceptSuggestion : ConsoleEvent()

    /** User pressed Up arrow to navigate command history backwards. */
    data object HistoryUp : ConsoleEvent()

    /** User pressed Down arrow to navigate command history forwards. */
    data object HistoryDown : ConsoleEvent()

    /**
     * User tapped a specific suggestion in the dropdown.
     *
     * @property index Index of the selected suggestion.
     */
    data class SelectSuggestion(val index: Int) : ConsoleEvent()

    /** User dismissed the suggestion dropdown without selecting. */
    data object DismissSuggestions : ConsoleEvent()

    /** User requested clearing all console output. */
    data object Clear : ConsoleEvent()
}

/**
 * ViewModel for the developer console screen.
 *
 * Manages command input, execution via [CommandExecutor], autocomplete suggestions,
 * command history navigation, and output rendering. Observes [AuthRepository] for
 * the current user's role/name and [NetworkService] for connectivity status.
 *
 * Special output conventions:
 * - A line containing `"__CLEAR__"` triggers clearing all output (used by ClearCommand).
 * - A line starting with `"__ALIAS_"` signals alias management (used by AliasCommand).
 *
 * @param commandExecutor The engine that lexes, parses, and executes console commands.
 * @param authRepository Repository for observing the current user.
 * @param networkMonitor Connectivity state provider.
 */
class ConsoleViewModel(
    private val commandExecutor: CommandExecutor,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsoleUiState())

    /** Observable console UI state. */
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    /** In-memory command history for the current session (oldest first). */
    private val commandHistory = mutableListOf<String>()

    /**
     * Current position in the command history stack.
     * -1 means "not browsing history" (showing the live input buffer).
     * 0..lastIndex maps to [commandHistory] entries (0 = oldest).
     */
    private var historyIndex = -1

    /** Snapshot of the input text before the user started navigating history. */
    private var savedInputBeforeHistory = ""

    /** User-defined command aliases persisted for the session. */
    private val aliases = mutableMapOf<String, String>()

    init {
        observeUser()
        observeNetwork()
        appendWelcomeBanner()
    }

    // -- Public API ----------------------------------------------------------------

    /**
     * Single entry point for all UI events.
     *
     * @param event The [ConsoleEvent] dispatched from the composable.
     */
    fun onEvent(event: ConsoleEvent) {
        when (event) {
            is ConsoleEvent.InputChanged -> handleInputChanged(event.text, event.cursor)
            is ConsoleEvent.Submit -> handleSubmit()
            is ConsoleEvent.AcceptSuggestion -> handleAcceptSuggestion()
            is ConsoleEvent.HistoryUp -> handleHistoryUp()
            is ConsoleEvent.HistoryDown -> handleHistoryDown()
            is ConsoleEvent.SelectSuggestion -> handleSelectSuggestion(event.index)
            is ConsoleEvent.DismissSuggestions -> dismissSuggestions()
            is ConsoleEvent.Clear -> handleClear()
        }
    }

    // -- Observers -----------------------------------------------------------------

    /** Observe the current user to update role, name, and prompt. */
    private fun observeUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val role = user?.role ?: UserRole.GUEST
                val name = user?.username?.ifBlank { user.displayName }
                    ?: user?.displayName?.ifBlank { "user" }
                    ?: "user"
                val suffix = if (role >= UserRole.ADMIN) "#" else "$"
                val prompt = "[$name]$suffix "
                _uiState.update {
                    it.copy(
                        userRole = role,
                        userName = name,
                        prompt = prompt
                    )
                }
            }
        }
    }

    /** Observe network connectivity. */
    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(networkStatus = online) }
            }
        }
    }

    // -- Input handling ------------------------------------------------------------

    /**
     * Handles text/cursor changes — updates state and triggers autocomplete.
     */
    private fun handleInputChanged(text: String, cursor: Int) {
        _uiState.update { it.copy(currentInput = text, cursorPosition = cursor) }
        // Reset history browsing when user types manually
        historyIndex = -1
        updateAutocomplete(text, cursor)
    }

    /**
     * Queries the command executor for autocomplete suggestions and updates
     * the ghost text from the first match.
     */
    private fun updateAutocomplete(text: String, cursor: Int) {
        if (text.isBlank()) {
            _uiState.update {
                it.copy(
                    suggestions = emptyList(),
                    ghostText = "",
                    showSuggestions = false,
                    selectedSuggestionIndex = -1
                )
            }
            return
        }

        viewModelScope.launch {
            val suggestions = try {
                commandExecutor.autocomplete(text, cursor)
            } catch (_: Exception) {
                emptyList()
            }

            val ghost = if (suggestions.isNotEmpty()) {
                computeGhostText(text, suggestions.first().text)
            } else {
                ""
            }

            _uiState.update {
                it.copy(
                    suggestions = suggestions,
                    ghostText = ghost,
                    showSuggestions = suggestions.isNotEmpty(),
                    selectedSuggestionIndex = if (suggestions.isNotEmpty()) 0 else -1
                )
            }
        }
    }

    /**
     * Computes the ghost text suffix: the portion of [suggestion] that extends
     * beyond the last token in [currentInput].
     */
    private fun computeGhostText(currentInput: String, suggestion: String): String {
        val lastSpace = currentInput.lastIndexOf(' ')
        val lastToken = if (lastSpace == -1) currentInput else currentInput.substring(lastSpace + 1)
        val lowerToken = lastToken.lowercase()
        val lowerSuggestion = suggestion.lowercase()

        return if (lowerSuggestion.startsWith(lowerToken) && lowerToken.isNotEmpty()) {
            suggestion.substring(lastToken.length)
        } else {
            ""
        }
    }

    // -- Submit / execute ----------------------------------------------------------

    /**
     * Submits the current input for execution. Appends the prompt+input echo
     * to output, runs the command, and processes the result.
     */
    private fun handleSubmit() {
        val input = _uiState.value.currentInput.trim()
        if (input.isEmpty()) return
        if (_uiState.value.isExecuting) return

        // Echo the command in the output
        val prompt = buildPrompt()
        appendOutput(StyledOutputLine("$prompt$input", OutputStyle.MUTED))

        // Push to history
        if (commandHistory.isEmpty() || commandHistory.last() != input) {
            commandHistory.add(input)
        }
        historyIndex = -1

        // Clear input and suggestions
        _uiState.update {
            it.copy(
                currentInput = "",
                cursorPosition = 0,
                suggestions = emptyList(),
                ghostText = "",
                showSuggestions = false,
                selectedSuggestionIndex = -1,
                isExecuting = true
            )
        }

        viewModelScope.launch {
            try {
                val result = commandExecutor.execute(input)
                processCommandOutput(result.output)
            } catch (e: Exception) {
                // TODO: these developer-facing console error messages are hardcoded Vietnamese;
                //  stringResource() is unavailable in ViewModels — acceptable for dev console output.
                appendOutput(
                    StyledOutputLine(
                        "Loi: ${e.message ?: "Loi khong xac dinh"}",
                        OutputStyle.ERROR
                    )
                )
            } finally {
                _uiState.update { it.copy(isExecuting = false) }
            }
        }
    }

    /**
     * Processes command output lines, handling magic values for clear and alias
     * management before appending to the visible output.
     */
    private fun processCommandOutput(
        outputLines: List<com.example.androidapp.domain.console.OutputLine>
    ) {
        for (line in outputLines) {
            when {
                // ClearCommand magic value
                line.text == "__CLEAR__" -> {
                    _uiState.update { it.copy(outputLines = emptyList()) }
                }

                // AliasCommand: set alias (__ALIAS_SET_<name>=<value>)
                line.text.startsWith("__ALIAS_SET_") -> {
                    val payload = line.text.removePrefix("__ALIAS_SET_")
                    val eqIndex = payload.indexOf('=')
                    if (eqIndex > 0) {
                        val name = payload.substring(0, eqIndex)
                        val value = payload.substring(eqIndex + 1)
                        aliases[name] = value
                    }
                }

                // AliasCommand: remove alias (__ALIAS_REMOVE_<name>)
                line.text.startsWith("__ALIAS_REMOVE_") -> {
                    val name = line.text.removePrefix("__ALIAS_REMOVE_")
                    aliases.remove(name)
                }

                // AliasCommand: clear all aliases
                line.text == "__ALIAS_CLEAR__" -> {
                    aliases.clear()
                }

                // Normal line — append to output
                else -> {
                    appendOutput(StyledOutputLine(line.text, line.style))
                }
            }
        }
    }

    // -- Autocomplete accept -------------------------------------------------------

    /**
     * Accepts the currently highlighted suggestion (or the first one) by
     * replacing the last token in the input with the suggestion text.
     */
    private fun handleAcceptSuggestion() {
        val state = _uiState.value
        val suggestions = state.suggestions
        if (suggestions.isEmpty()) return

        val index = if (state.selectedSuggestionIndex in suggestions.indices) {
            state.selectedSuggestionIndex
        } else {
            0
        }

        val suggestion = suggestions[index]
        val currentInput = state.currentInput
        val newInput = applySuggestion(currentInput, suggestion.text)

        _uiState.update {
            it.copy(
                currentInput = newInput,
                cursorPosition = newInput.length,
                suggestions = emptyList(),
                ghostText = "",
                showSuggestions = false,
                selectedSuggestionIndex = -1
            )
        }
    }

    /**
     * Replaces the last whitespace-delimited token in [input] with [suggestionText].
     */
    private fun applySuggestion(input: String, suggestionText: String): String {
        val lastSpace = input.lastIndexOf(' ')
        val prefix = if (lastSpace == -1) "" else input.substring(0, lastSpace + 1)
        return "$prefix$suggestionText "
    }

    /**
     * Handles tapping a specific suggestion from the dropdown.
     */
    private fun handleSelectSuggestion(index: Int) {
        val suggestions = _uiState.value.suggestions
        if (index !in suggestions.indices) return

        val suggestion = suggestions[index]
        val currentInput = _uiState.value.currentInput
        val newInput = applySuggestion(currentInput, suggestion.text)

        _uiState.update {
            it.copy(
                currentInput = newInput,
                cursorPosition = newInput.length,
                suggestions = emptyList(),
                ghostText = "",
                showSuggestions = false,
                selectedSuggestionIndex = -1
            )
        }
    }

    // -- History navigation --------------------------------------------------------

    /**
     * Navigates backward through command history (older commands).
     */
    private fun handleHistoryUp() {
        if (commandHistory.isEmpty()) return

        if (historyIndex == -1) {
            // Entering history — save current input
            savedInputBeforeHistory = _uiState.value.currentInput
            historyIndex = commandHistory.size - 1
        } else if (historyIndex > 0) {
            historyIndex--
        } else {
            // Already at the oldest entry
            return
        }

        val historyEntry = commandHistory[historyIndex]
        _uiState.update {
            it.copy(
                currentInput = historyEntry,
                cursorPosition = historyEntry.length,
                suggestions = emptyList(),
                ghostText = "",
                showSuggestions = false
            )
        }
    }

    /**
     * Navigates forward through command history (newer commands).
     */
    private fun handleHistoryDown() {
        if (historyIndex == -1) return

        if (historyIndex < commandHistory.size - 1) {
            historyIndex++
            val historyEntry = commandHistory[historyIndex]
            _uiState.update {
                it.copy(
                    currentInput = historyEntry,
                    cursorPosition = historyEntry.length,
                    suggestions = emptyList(),
                    ghostText = "",
                    showSuggestions = false
                )
            }
        } else {
            // Past the newest entry — restore the saved input
            historyIndex = -1
            _uiState.update {
                it.copy(
                    currentInput = savedInputBeforeHistory,
                    cursorPosition = savedInputBeforeHistory.length,
                    suggestions = emptyList(),
                    ghostText = "",
                    showSuggestions = false
                )
            }
        }
    }

    // -- Clear / dismiss -----------------------------------------------------------

    /** Clears all console output lines. */
    private fun handleClear() {
        _uiState.update { it.copy(outputLines = emptyList()) }
    }

    /** Hides the suggestion dropdown. */
    private fun dismissSuggestions() {
        _uiState.update {
            it.copy(
                showSuggestions = false,
                suggestions = emptyList(),
                ghostText = "",
                selectedSuggestionIndex = -1
            )
        }
    }

    // -- Helpers -------------------------------------------------------------------

    /**
     * Builds the shell-style prompt string based on the current user.
     * Admins/superusers get `#`, regular users get `$`.
     */
    private fun buildPrompt(): String {
        val state = _uiState.value
        val name = state.userName.ifBlank { "user" }
        val suffix = if (state.userRole == UserRole.ADMIN ||
            state.userRole == UserRole.SUPERUSER
        ) "#" else "$"
        return "[$name]$suffix "
    }

    /**
     * Appends a single styled line to the console output, keeping a maximum
     * buffer of [MAX_OUTPUT_LINES] to prevent unbounded memory growth.
     */
    private fun appendOutput(line: StyledOutputLine) {
        _uiState.update { state ->
            val updated = state.outputLines + line
            val trimmed = if (updated.size > MAX_OUTPUT_LINES) {
                updated.drop(updated.size - MAX_OUTPUT_LINES)
            } else {
                updated
            }
            state.copy(outputLines = trimmed)
        }
    }

    /** Appends the initial welcome banner when the console is first opened. */
    private fun appendWelcomeBanner() {
        // TODO: these developer-facing banner strings are hardcoded Vietnamese;
        //  stringResource() is unavailable in ViewModels — acceptable for dev console output.
        val bannerLines = listOf(
            StyledOutputLine(
                "Quizzez Developer Console v1.0",
                OutputStyle.HEADER
            ),
            StyledOutputLine(
                "Go 'help' de xem danh sach lenh co san.",
                OutputStyle.INFO
            ),
            StyledOutputLine("", OutputStyle.NORMAL)
        )
        _uiState.update { state ->
            state.copy(outputLines = state.outputLines + bannerLines)
        }
    }

    companion object {
        /** Maximum number of output lines retained in the console buffer. */
        private const val MAX_OUTPUT_LINES = 5_000
    }
}
