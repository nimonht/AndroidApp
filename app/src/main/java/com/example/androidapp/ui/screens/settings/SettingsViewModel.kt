package com.example.androidapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val autoSyncEnabled: Boolean = true
)

/** Events that can be dispatched to [SettingsViewModel]. */
sealed class SettingsEvent {
    data class AutoSyncToggled(val enabled: Boolean) : SettingsEvent()
}

/**
 * ViewModel for the Settings screen.
 * Persists settings in-memory (a SharedPreferences integration can be added later).
 */
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    /** Current UI state for the Settings screen. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Dispatches a [SettingsEvent] to the ViewModel.
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.AutoSyncToggled ->
                _uiState.update { it.copy(autoSyncEnabled = event.enabled) }
        }
    }
}

