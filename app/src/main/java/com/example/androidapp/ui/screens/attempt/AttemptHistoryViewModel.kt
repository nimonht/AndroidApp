package com.example.androidapp.ui.screens.attempt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.usecase.GetAttemptHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AttemptHistoryUiState(
    val attempts: List<Attempt> = emptyList(),
    val isLoading: Boolean = true
)

class AttemptHistoryViewModel(
    private val getAttemptHistoryUseCase: GetAttemptHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttemptHistoryUiState())
    val uiState: StateFlow<AttemptHistoryUiState> = _uiState.asStateFlow()

    fun loadAttempts(userId: String) {
        viewModelScope.launch {
            getAttemptHistoryUseCase(userId).collect { list ->
                _uiState.value = AttemptHistoryUiState(
                    attempts = list,
                    isLoading = false
                )
            }
        }
    }
}

