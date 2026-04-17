package com.example.androidapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.preferences.SettingsPreferences
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Settings screen.
 *
 * @property autoSyncEnabled Whether automatic background sync is enabled.
 * @property wifiOnlySync Whether sync is restricted to WiFi connections (data-saving mode).
 * @property darkThemeMode Theme mode: 0 = system, 1 = light, 2 = dark.
 * @property isLoggedIn Whether a user is currently authenticated.
 * @property showDeleteAccountDialog Whether the delete-account confirmation dialog is visible.
 * @property isDeleting Whether a delete-account operation is in progress.
 * @property deleteError Error message from a failed delete-account attempt, or null.
 * @property accountDeleted Whether the account was successfully deleted (triggers navigation).
 */
data class SettingsUiState(
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val darkThemeMode: Int = SettingsPreferences.THEME_MODE_SYSTEM,
    val isLoggedIn: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: UiError? = null,
    val accountDeleted: Boolean = false
)

/**
 * Events that can be dispatched to [SettingsViewModel].
 */
sealed class SettingsEvent {
    /** Toggle automatic background sync. */
    data class AutoSyncToggled(val enabled: Boolean) : SettingsEvent()

    /** Toggle WiFi-only sync (data-saving mode). */
    data class WifiOnlySyncToggled(val enabled: Boolean) : SettingsEvent()

    /** Change the dark theme mode (0 = system, 1 = light, 2 = dark). */
    data class DarkThemeModeChanged(val mode: Int) : SettingsEvent()

    /** User tapped the delete-account button. */
    data object DeleteAccountRequested : SettingsEvent()

    /** User confirmed account deletion in the dialog. */
    data object DeleteAccountConfirmed : SettingsEvent()

    /** User dismissed the delete-account dialog. */
    data object DeleteAccountDismissed : SettingsEvent()

    /** Clear the delete-account error message. */
    data object ClearDeleteError : SettingsEvent()
}

/**
 * ViewModel for the Settings screen.
 *
 * Reads and writes settings to [SettingsPreferences] (DataStore-backed)
 * and delegates account deletion to [AuthRepository].
 *
 * @param settingsPreferences DataStore wrapper for persisted settings.
 * @param authRepository Repository handling authentication and account operations.
 */
class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    /** Current UI state for the Settings screen. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Observe persisted preferences and project into UI state.
        viewModelScope.launch {
            settingsPreferences.autoSyncEnabled.collect { enabled ->
                _uiState.update { it.copy(autoSyncEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.wifiOnlySync.collect { enabled ->
                _uiState.update { it.copy(wifiOnlySync = enabled) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.darkThemeMode.collect { mode ->
                _uiState.update { it.copy(darkThemeMode = mode) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(isLoggedIn = user != null) }
            }
        }
    }

    /**
     * Dispatches a [SettingsEvent] to the ViewModel.
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.AutoSyncToggled -> viewModelScope.launch {
                settingsPreferences.setAutoSyncEnabled(event.enabled)
            }

            is SettingsEvent.WifiOnlySyncToggled -> viewModelScope.launch {
                settingsPreferences.setWifiOnlySync(event.enabled)
            }

            is SettingsEvent.DarkThemeModeChanged -> viewModelScope.launch {
                settingsPreferences.setDarkThemeMode(event.mode)
            }

            is SettingsEvent.DeleteAccountRequested -> {
                _uiState.update { it.copy(showDeleteAccountDialog = true) }
            }

            is SettingsEvent.DeleteAccountConfirmed -> {
                _uiState.update {
                    it.copy(showDeleteAccountDialog = false, isDeleting = true, deleteError = null)
                }
                viewModelScope.launch {
                    val result = authRepository.deleteAccount()
                    result.fold(
                        onSuccess = {
                            _uiState.update { it.copy(isDeleting = false, accountDeleted = true) }
                        },
                        onFailure = {
                            _uiState.update {
                                it.copy(
                                    isDeleting = false,
                                    deleteError = UiError.DELETE_USER_FAILED
                                )
                            }
                        }
                    )
                }
            }

            is SettingsEvent.DeleteAccountDismissed -> {
                _uiState.update { it.copy(showDeleteAccountDialog = false) }
            }

            is SettingsEvent.ClearDeleteError -> {
                _uiState.update { it.copy(deleteError = null) }
            }
        }
    }
}
