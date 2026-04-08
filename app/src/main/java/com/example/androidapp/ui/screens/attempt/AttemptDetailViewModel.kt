package com.example.androidapp.ui.screens.attempt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.util.ScoreCalculator
import com.example.androidapp.domain.util.ScoreUtil
import com.example.androidapp.domain.util.TimeFormatter
import com.example.androidapp.ui.common.UiError
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
        val quiz: Quiz?,
        val percentage: Int,
        val starRating: Int,
        val timeTakenMs: Long,
        val timeTakenFormatted: String,
        /** Number of questions answered correctly. */
        val correctCount: Int,
        /** Number of questions answered incorrectly. */
        val wrongCount: Int
    ) : AttemptDetailUiState()

    data class Error(val error: UiError) : AttemptDetailUiState()
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
                _uiState.value = AttemptDetailUiState.Error(UiError.ATTEMPT_NOT_FOUND)
                return@launch
            }
            val quiz = quizRepository.getQuizById(attempt.quizId)
            val percentage = ScoreUtil.calculatePercentage(attempt.score, attempt.totalQuestions)
            val starRating = ScoreUtil.calculateStarRating(percentage)
            val timeTakenMs = if (attempt.endTimeMillis != null) {
                maxOf(0L, attempt.endTimeMillis - attempt.startTimeMillis)
            } else {
                0L
            }
            val timeTakenFormatted = TimeFormatter.formatDuration(timeTakenMs / 1000L)

            // Calculate actual correct/incorrect question counts from the answers map
            val questions = quiz?.let { quizRepository.getQuestionsForQuizOnce(it.id) } ?: emptyList()
            val userAnswers = attempt.answers.mapValues { (_, v) -> v.toSet() }
            val scoreResult = ScoreCalculator.calculatePointScore(questions, userAnswers)

            _uiState.value = AttemptDetailUiState.Success(
                attempt = attempt,
                quiz = quiz,
                percentage = percentage,
                starRating = starRating,
                timeTakenMs = timeTakenMs,
                timeTakenFormatted = timeTakenFormatted,
                correctCount = scoreResult.correctCount,
                wrongCount = scoreResult.wrongCount
            )
        }
    }

}
