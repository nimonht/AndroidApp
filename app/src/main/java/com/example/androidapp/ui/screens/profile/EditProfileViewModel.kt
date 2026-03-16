package com.example.androidapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.usecase.user.UpdateUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*


data class EditProfileUiState(
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isSaving: Boolean = false,
    val success: Boolean = false
)

class EditProfileViewModel(
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun onDisplayNameChange(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name)
    }

    fun onAvatarChange(url: String) {
        _uiState.value = _uiState.value.copy(avatarUrl = url)
    }

    fun onSaveClick() {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isSaving = true)

            updateUserProfileUseCase(
                displayName = _uiState.value.displayName,
                avatarUrl = _uiState.value.avatarUrl
            )

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                success = true
            )
        }
    }
}

