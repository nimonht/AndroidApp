package com.example.androidapp.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the review state for a single question.
 */
data class QuestionReview(
    val question: Question,
    val selectedAnswerIds: List<String>,
    val isCorrect: Boolean
)

/**
 * Possible UI states for the Answer Review screen.
 */
sealed class AnswerReviewUiState {
    data object Loading : AnswerReviewUiState()
    data class Success(
        val quiz: Quiz,
        val attempt: Attempt,
        val reviews: List<QuestionReview>,
        val correctCount: Int,
        val totalCount: Int
    ) : AnswerReviewUiState()
    data class Error(val message: String) : AnswerReviewUiState()
}

/**
 * ViewModel for the Answer Review screen.
 * Loads questions and the user's answers to show correct/wrong answers.
 */
class AnswerReviewViewModel(
    private val quizId: String,
    private val attemptId: String,
    private val quizRepository: QuizRepository,
    private val attemptRepository: AttemptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnswerReviewUiState>(AnswerReviewUiState.Loading)

    /** Current UI state for the Answer Review screen. */
    val uiState: StateFlow<AnswerReviewUiState> = _uiState.asStateFlow()

    init {
        loadReview()
    }

    /**
     * Retries loading the review data.
     */
    fun onRetry() {
        loadReview()
    }

    private fun loadReview() {
        viewModelScope.launch {
            _uiState.value = AnswerReviewUiState.Loading
            val quiz = quizRepository.getQuizById(quizId)
            val attempt = attemptRepository.getAttemptById(attemptId)
            if (quiz == null || attempt == null) {
                _uiState.value = AnswerReviewUiState.Error("Không tìm thấy kết quả")
                return@launch
            }
            val allQuestions = quizRepository.getQuestionsForQuizOnce(quizId)
            // Use the persisted question order from the attempt, or fall back to repository order
            val questions = if (attempt.questionOrder.isNotEmpty()) {
                val questionMap = allQuestions.associateBy { it.id }
                attempt.questionOrder.mapNotNull { questionMap[it] }
            } else {
                allQuestions
            }
            val reviews = questions.map { question ->
                val selectedIds = attempt.answers[question.id] ?: emptyList()
                val correctIds = question.choices.filter { it.isCorrect }.map { it.id }.toSet()
                QuestionReview(
                    question = question,
                    selectedAnswerIds = selectedIds,
                    isCorrect = selectedIds.toSet() == correctIds
                )
            }
            val correctCount = reviews.count { it.isCorrect }
            _uiState.value = AnswerReviewUiState.Success(
                quiz = quiz,
                attempt = attempt,
                reviews = reviews,
                correctCount = correctCount,
                totalCount = questions.size
            )
        }
    }
}
