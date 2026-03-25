package com.example.androidapp.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Recycle Bin (Trash) screen.
 */
data class RecycleBinUiState(
    val deletedQuizzes: List<Quiz> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/** Events that can be dispatched to [RecycleBinViewModel]. */
sealed class RecycleBinEvent {
    data class RestoreQuiz(val quizId: String) : RecycleBinEvent()
    data class DeletePermanently(val quizId: String) : RecycleBinEvent()
    data object EmptyTrash : RecycleBinEvent()
    data object ClearMessage : RecycleBinEvent()
    data object ClearError : RecycleBinEvent()
}

/**
 * ViewModel for the Recycle Bin (Trash) screen.
 * Loads soft-deleted quizzes and supports restore and permanent delete actions.
 */
class RecycleBinViewModel(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecycleBinUiState())

    /** Current UI state for the Trash screen. */
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    init {
        loadDeletedQuizzes()
    }

    /**
     * Dispatches a [RecycleBinEvent] to the ViewModel.
     */
    fun onEvent(event: RecycleBinEvent) {
        when (event) {
            is RecycleBinEvent.RestoreQuiz -> onRestoreQuiz(event.quizId)
            is RecycleBinEvent.DeletePermanently -> onDeletePermanently(event.quizId)
            is RecycleBinEvent.EmptyTrash -> onEmptyTrash()
            is RecycleBinEvent.ClearMessage -> _uiState.update { it.copy(successMessage = null) }
            is RecycleBinEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadDeletedQuizzes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            quizRepository.getDeletedQuizzes(user.id).collect { quizzes ->
                _uiState.update { it.copy(isLoading = false, deletedQuizzes = quizzes) }
            }
        }
    }

    private fun onRestoreQuiz(quizId: String) {
        viewModelScope.launch {
            val result = quizRepository.restoreQuiz(quizId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(successMessage = "Đã khôi phục bài kiểm tra") } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    private fun onDeletePermanently(quizId: String) {
        viewModelScope.launch {
            val result = quizRepository.permanentlyDeleteQuiz(quizId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(successMessage = "Đã xóa vĩnh viễn") } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    private fun onEmptyTrash() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Người dùng chưa đăng nhập") }
                return@launch
            }
            val result = quizRepository.emptyTrash(user.id)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, successMessage = "Đã dọn sạch thùng rác") } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }
}

