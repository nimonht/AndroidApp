package com.example.androidapp.ui.screens.advanced

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state for the [AdvancedScreen] container.
 *
 * @property selectedTab Currently selected tab index (0 = Console, 1 = Logs).
 */
data class AdvancedUiState(
    val selectedTab: Int = 0
)

/**
 * Events dispatched from [AdvancedScreen] to [AdvancedViewModel].
 */
sealed class AdvancedEvent {
    /** Select the tab at the given [index] (0 = Console, 1 = Logs). */
    data class SelectTab(val index: Int) : AdvancedEvent()
}

/**
 * ViewModel for the [AdvancedScreen] container.
 *
 * Tracks which tab (Console or Logs) is currently selected using the
 * standard `_uiState`/`uiState` + `onEvent()` pattern.
 */
class AdvancedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedUiState())

    /** Observable UI state for the Advanced screen. */
    val uiState: StateFlow<AdvancedUiState> = _uiState.asStateFlow()

    /**
     * Central event dispatcher.
     *
     * @param event The [AdvancedEvent] to handle.
     */
    fun onEvent(event: AdvancedEvent) {
        when (event) {
            is AdvancedEvent.SelectTab -> {
                _uiState.update { it.copy(selectedTab = event.index.coerceIn(0, 1)) }
            }
        }
    }
}
