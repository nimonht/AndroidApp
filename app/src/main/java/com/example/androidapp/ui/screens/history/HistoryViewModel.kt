package com.example.androidapp.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Combines an [Attempt] with the title of the associated [Quiz].
 *
 * @property attempt The user's quiz attempt.
 * @property quizTitle The title of the quiz, or a fallback if the quiz was deleted.
 * @property isQuizDeleted Whether the associated quiz has been soft-deleted or permanently removed.
 */
data class AttemptWithQuiz(
    val attempt: Attempt,
    val quizTitle: String,
    val isQuizDeleted: Boolean = false
)

/**
 * UI state for the History screen.
 */
data class HistoryUiState(
    val attempts: List<AttemptWithQuiz> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Events that can be dispatched to [HistoryViewModel]. */
sealed class HistoryEvent {
    data object Refresh : HistoryEvent()
    data object ClearError : HistoryEvent()
}

/**
 * ViewModel for the History screen.
 * Loads all attempts for the current user and enriches them with quiz titles.
 */
class HistoryViewModel(
    private val attemptRepository: AttemptRepository,
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())

    /** Current UI state for the History screen. */
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    companion object {
        /** Fallback title shown when the quiz has been permanently deleted from the database. */
        const val DELETED_QUIZ_FALLBACK_TITLE = "N/A"
    }

    /**
     * Dispatches a [HistoryEvent] to the ViewModel.
     */
    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.Refresh -> loadHistory()
            is HistoryEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, attempts = emptyList()) }
                return@launch
            }
            attemptRepository.getAttemptsByUser(user.id).collect { attempts ->
                val enriched = attempts.map { attempt ->
                    val quiz = quizRepository.getQuizById(attempt.quizId)
                    val isDeleted = quiz == null || quiz.deletedAt != null
                    val title = if (quiz != null) quiz.title else DELETED_QUIZ_FALLBACK_TITLE
                    AttemptWithQuiz(
                        attempt = attempt,
                        quizTitle = title,
                        isQuizDeleted = isDeleted
                    )
                }
                _uiState.update { it.copy(isLoading = false, attempts = enriched) }
            }
        }
    }
}
