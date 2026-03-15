package com.example.androidapp.ui.screens.attempt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.util.ScoreUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Possible UI states for the Attempt Detail screen.
 */
sealed class AttemptDetailUiState {
    data object Loading : AttemptDetailUiState()
    data class Success(
        val attempt: Attempt,
        val quiz: Quiz,
        val percentage: Int,
        val starRating: Int,
        val timeTakenMs: Long
    ) : AttemptDetailUiState()
    data class Error(val message: String) : AttemptDetailUiState()
}

/**
 * ViewModel for the Attempt Detail screen.
 * Loads attempt data and the associated quiz information.
 */
class AttemptDetailViewModel(
    private val attemptId: String,
    private val attemptRepository: AttemptRepository,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AttemptDetailUiState>(AttemptDetailUiState.Loading)

    /** Current UI state for the Attempt Detail screen. */
    val uiState: StateFlow<AttemptDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    /**
     * Retries loading the attempt detail data.
     */
    fun onRetry() {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = AttemptDetailUiState.Loading
            val attempt = attemptRepository.getAttemptById(attemptId)
            if (attempt == null) {
                _uiState.value = AttemptDetailUiState.Error("Không tìm thấy lượt làm")
                return@launch
            }
            val quiz = quizRepository.getQuizById(attempt.quizId)
            if (quiz == null) {
                _uiState.value = AttemptDetailUiState.Error("Không tìm thấy bài kiểm tra")
                return@launch
            }
            val percentage = ScoreUtil.calculatePercentage(attempt.score, attempt.totalQuestions)
            val starRating = ScoreUtil.calculateStarRating(percentage)
            val timeTakenMs = if (attempt.endTimeMillis != null) {
                maxOf(0L, attempt.endTimeMillis - attempt.startTimeMillis)
            } else {
                0L
            }

            _uiState.value = AttemptDetailUiState.Success(
                attempt = attempt,
                quiz = quiz,
                percentage = percentage,
                starRating = starRating,
                timeTakenMs = timeTakenMs
            )
        }
    }
}
