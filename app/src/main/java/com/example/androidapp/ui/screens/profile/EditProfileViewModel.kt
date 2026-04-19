package com.example.androidapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.ui.common.UiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * UI state for the Edit Profile screen.
 *
 * @property displayName The current value of the display-name text field.
 * @property email The user's email address (read-only; shown but not editable).
 * @property photoUrl The URL of the user's current avatar image, or null if none is set.
 * @property isLoading Whether a save operation is in progress.
 * @property isLoadingAvatar Whether a random avatar fetch is in progress.
 * @property isSaved Whether the profile was saved successfully; used to trigger navigation back.
 * @property error A [UiError] code to display, or null when there is no error.
 * @property errorDetail Optional dynamic detail for parameterised errors (e.g. HTTP status code).
 */
data class EditProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isLoading: Boolean = false,
    val isLoadingAvatar: Boolean = false,
    val isSaved: Boolean = false,
    val error: UiError? = null,
    val errorDetail: String? = null
)

/**
 * Events that can be dispatched to [EditProfileViewModel].
 */
sealed class EditProfileEvent {
    /**
     * Fired whenever the user edits the display-name field.
     * @property name The updated display name string.
     */
    data class DisplayNameChanged(val name: String) : EditProfileEvent()

    /**
     * Fired when the user manually enters or pastes an avatar image URL.
     * @property url The image URL string.
     */
    data class AvatarUrlChanged(val url: String) : EditProfileEvent()

    /**
     * Fired when the user taps the "Random Avatar" button.
     * Fetches a random anime image from the Wallhaven API.
     */
    data object FetchRandomAvatar : EditProfileEvent()

    /**
     * Fired when the user taps the "Save" button.
     * Persists the display name and photo URL via [AuthRepository.updateProfile].
     */
    data object SaveProfile : EditProfileEvent()

    /**
     * Fired to dismiss any visible error snackbar / dialog.
     */
    data object ClearError : EditProfileEvent()
}

/**
 * ViewModel for the Edit Profile screen.
 *
 * Loads the current user on init, allows manual URL entry or random avatar
 * fetching via the Wallhaven API, and persists display-name / photo-URL
 * changes via [AuthRepository.updateProfile].
 *
 * @param authRepository Repository for authentication and user-profile operations.
 */
class EditProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val WALLHAVEN_RANDOM_AVATAR_URL =
            "https://wallhaven.cc/api/v1/search?categories=010&purity=100&sorting=random&atleast=400x400&ratios=1x1"
    }

    private val _uiState = MutableStateFlow(EditProfileUiState())

    /** Current UI state for the Edit Profile screen. */
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    /**
     * Dispatches an [EditProfileEvent] to the ViewModel.
     */
    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.DisplayNameChanged -> onDisplayNameChanged(event.name)
            is EditProfileEvent.AvatarUrlChanged -> onAvatarUrlChanged(event.url)
            is EditProfileEvent.FetchRandomAvatar -> onFetchRandomAvatar()
            is EditProfileEvent.SaveProfile -> onSaveProfile()
            is EditProfileEvent.ClearError -> clearError()
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.update {
                    it.copy(
                        displayName = user.displayName,
                        email = user.email,
                        photoUrl = user.photoUrl
                    )
                }
            }
        }
    }

    private fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    private fun onAvatarUrlChanged(url: String) {
        _uiState.update { it.copy(photoUrl = url.ifBlank { null }) }
    }

    /**
     * Fetches a random anime/artwork image from the Wallhaven API and sets it
     * as the avatar URL.
     *
     * API: https://wallhaven.cc/api/v1/search?categories=010&purity=100&sorting=random&atleast=400x400&ratios=1x1
     * Parses the JSON response and picks the first result's small thumbnail.
     */
    private fun onFetchRandomAvatar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAvatar = true) }

            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL(WALLHAVEN_RANDOM_AVATAR_URL)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000

                    try {
                        val responseCode = connection.responseCode
                        if (responseCode != HttpURLConnection.HTTP_OK) {
                            return@withContext AvatarFetchResult.ApiError(responseCode)
                        }

                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(responseBody)
                        val dataArray = json.getJSONArray("data")

                        if (dataArray.length() == 0) {
                            return@withContext AvatarFetchResult.NoImages
                        }

                        val firstItem = dataArray.getJSONObject(0)
                        val thumbs = firstItem.getJSONObject("thumbs")
                        AvatarFetchResult.Success(thumbs.getString("small"))
                    } finally {
                        connection.disconnect()
                    }
                }

                when (result) {
                    is AvatarFetchResult.Success -> _uiState.update {
                        it.copy(photoUrl = result.url, isLoadingAvatar = false)
                    }

                    is AvatarFetchResult.ApiError -> _uiState.update {
                        it.copy(
                            isLoadingAvatar = false,
                            error = UiError.WALLHAVEN_API_ERROR,
                            errorDetail = result.httpCode.toString()
                        )
                    }

                    is AvatarFetchResult.NoImages -> _uiState.update {
                        it.copy(
                            isLoadingAvatar = false,
                            error = UiError.WALLHAVEN_NO_IMAGES
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingAvatar = false,
                        error = UiError.RANDOM_AVATAR_FAILED
                    )
                }
            }
        }
    }

    private fun onSaveProfile() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val displayName = currentState.displayName.trim()

            if (displayName.isBlank()) {
                _uiState.update { it.copy(error = UiError.DISPLAY_NAME_BLANK) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            authRepository.updateProfile(
                displayName = displayName,
                photoUrl = currentState.photoUrl
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError.SAVE_PROFILE_FAILED
                        )
                    }
                }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null, errorDetail = null) }
    }

    // -------------------------------------------------------------------------
    // Internal result type
    // -------------------------------------------------------------------------

    /**
     * Internal result type for the Wallhaven avatar fetch.
     * Avoids raw exceptions for expected (non-exceptional) conditions so that
     * each outcome maps cleanly to a [UiError].
     */
    private sealed interface AvatarFetchResult {
        data class Success(val url: String) : AvatarFetchResult
        data class ApiError(val httpCode: Int) : AvatarFetchResult
        data object NoImages : AvatarFetchResult
    }
}
