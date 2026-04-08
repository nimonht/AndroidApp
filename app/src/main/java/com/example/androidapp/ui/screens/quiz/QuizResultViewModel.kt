package com.example.androidapp.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.util.ScoreCalculator
import com.example.androidapp.domain.util.ScoreUtil
import com.example.androidapp.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Possible UI states for the Quiz Result screen.
 */
sealed class QuizResultUiState {
    data object Loading : QuizResultUiState()
    data class Success(
        val quiz: Quiz,
        val attempt: Attempt,
        val percentage: Int,
        val starRating: Int,
        /** Sum of points the user earned (correct questions * their point values). */
        val earnedScore: Int,
        /** Sum of all question point values (maximum possible score). */
        val maxScore: Int,
        /** Number of questions answered correctly. */
        val correctCount: Int,
        /** Number of questions answered incorrectly. */
        val wrongCount: Int
    ) : QuizResultUiState()

    data class Error(val error: UiError, val errorDetail: String? = null) : QuizResultUiState()
}

/**
 * ViewModel for the Quiz Result screen.
 * Loads the attempt result and quiz metadata.
 */
class QuizResultViewModel(
    private val quizId: String,
    private val attemptId: String,
    private val quizRepository: QuizRepository,
    private val attemptRepository: AttemptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizResultUiState>(QuizResultUiState.Loading)

    /** Current UI state for the Quiz Result screen. */
    val uiState: StateFlow<QuizResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    /**
     * Retries loading the result data.
     */
    fun onRetry() {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch {
            _uiState.value = QuizResultUiState.Loading
            val quiz = quizRepository.getQuizById(quizId)
            val attempt = attemptRepository.getAttemptById(attemptId)
            if (quiz == null || attempt == null) {
                _uiState.value = QuizResultUiState.Error(UiError.RESULT_NOT_FOUND)
                return@launch
            }

            val questions = quizRepository.getQuestionsForQuizOnce(quizId)
            val userAnswers = attempt.answers.mapValues { (_, v) -> v.toSet() }
            val scoreResult = ScoreCalculator.calculatePointScore(questions, userAnswers)

            val percentage = if (scoreResult.maxScore > 0) (scoreResult.earnedScore * 100) / scoreResult.maxScore else 0
            val starRating = ScoreUtil.calculateStarRating(percentage)
            _uiState.value = QuizResultUiState.Success(
                quiz = quiz,
                attempt = attempt,
                percentage = percentage,
                starRating = starRating,
                earnedScore = scoreResult.earnedScore,
                maxScore = scoreResult.maxScore,
                correctCount = scoreResult.correctCount,
                wrongCount = scoreResult.wrongCount
            )
        }
    }

}
