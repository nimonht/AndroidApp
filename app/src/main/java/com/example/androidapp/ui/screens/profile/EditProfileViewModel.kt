package com.example.androidapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.repository.AuthRepository
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
 * @property error An error message to display, or null when there is no error.
 */
data class EditProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isLoading: Boolean = false,
    val isLoadingAvatar: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
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
                val imageUrl = withContext(Dispatchers.IO) {
                    val url = URL(
                        "https://wallhaven.cc/api/v1/search?categories=010&purity=100&sorting=random&atleast=400x400&ratios=1x1"
                    )
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000

                    try {
                        val responseCode = connection.responseCode
                        if (responseCode != HttpURLConnection.HTTP_OK) {
                            throw Exception("Wallhaven API trả về lỗi HTTP $responseCode")
                        }

                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(responseBody)
                        val dataArray = json.getJSONArray("data")

                        if (dataArray.length() == 0) {
                            throw Exception("Không tìm thấy hình ảnh từ Wallhaven")
                        }

                        val firstItem = dataArray.getJSONObject(0)
                        val thumbs = firstItem.getJSONObject("thumbs")
                        thumbs.getString("small")
                    } finally {
                        connection.disconnect()
                    }
                }

                _uiState.update {
                    it.copy(photoUrl = imageUrl, isLoadingAvatar = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingAvatar = false,
                        error = e.localizedMessage ?: "Không thể tải ảnh đại diện ngẫu nhiên"
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
                _uiState.update { it.copy(error = "Tên hiển thị không được để trống") }
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
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Lưu hồ sơ thất bại"
                        )
                    }
                }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
