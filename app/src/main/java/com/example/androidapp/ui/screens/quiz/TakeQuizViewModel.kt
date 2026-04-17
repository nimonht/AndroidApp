package com.example.androidapp.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.util.QuestionShuffler
import com.example.androidapp.domain.util.ScoreCalculator
import com.example.androidapp.ui.common.UiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Possible UI states for the Take Quiz screen.
 */
sealed class TakeQuizUiState {
    data object Loading : TakeQuizUiState()
    data class Active(
        val quizTitle: String,
        val currentQuestion: Question,
        val currentIndex: Int,
        val totalQuestions: Int,
        val selectedAnswers: Set<String>,
        val elapsedSeconds: Int,
        val isSubmitting: Boolean,
        val isMultiSelect: Boolean,
        val showExitDialog: Boolean = false,
        val allAnswers: Map<String, Set<String>> = emptyMap(),
        val shouldNavigateBack: Boolean = false
    ) : TakeQuizUiState()

    data class Finished(val attemptId: String) : TakeQuizUiState()
    data class Error(val error: UiError, val errorDetail: String? = null) : TakeQuizUiState()
}

/** Events that can be dispatched to [TakeQuizViewModel]. */
sealed class TakeQuizEvent {
    data class AnswerSelected(val choiceId: String) : TakeQuizEvent()
    data object NextQuestion : TakeQuizEvent()
    data object PreviousQuestion : TakeQuizEvent()
    data class GoToQuestion(val index: Int) : TakeQuizEvent()
    data object SubmitQuiz : TakeQuizEvent()
    data object RequestExit : TakeQuizEvent()
    data object ConfirmExit : TakeQuizEvent()
    data object DismissExitDialog : TakeQuizEvent()
}

/**
 * ViewModel for the Take Quiz screen.
 * Manages quiz session state: question navigation, answer selection, timer, and submission.
 */
class TakeQuizViewModel(
    private val quizId: String,
    private val quizRepository: QuizRepository,
    private val attemptRepository: AttemptRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TakeQuizUiState>(TakeQuizUiState.Loading)

    /** Current UI state for the Take Quiz screen. */
    val uiState: StateFlow<TakeQuizUiState> = _uiState.asStateFlow()

    private var questions: List<Question> = emptyList()
    private var currentIndex: Int = 0
    private val answers: MutableMap<String, Set<String>> = mutableMapOf()
    private var elapsedSeconds: Int = 0
    private var timerJob: Job? = null
    private var quizTitle: String = ""
    private val attemptId = UUID.randomUUID().toString()
    private val startTimeMillis = System.currentTimeMillis()

    init {
        loadQuiz()
    }

    /**
     * Dispatches a [TakeQuizEvent] to the ViewModel.
     */
    fun onEvent(event: TakeQuizEvent) {
        when (event) {
            is TakeQuizEvent.AnswerSelected -> onAnswerSelected(event.choiceId)
            is TakeQuizEvent.NextQuestion -> onNextQuestion()
            is TakeQuizEvent.PreviousQuestion -> onPreviousQuestion()
            is TakeQuizEvent.GoToQuestion -> onGoToQuestion(event.index)
            is TakeQuizEvent.SubmitQuiz -> onSubmitQuiz()
            is TakeQuizEvent.RequestExit -> onRequestExit()
            is TakeQuizEvent.ConfirmExit -> onConfirmExit()
            is TakeQuizEvent.DismissExitDialog -> onDismissExitDialog()
        }
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            val quiz = quizRepository.getQuizById(quizId)
            if (quiz == null) {
                _uiState.value = TakeQuizUiState.Error(UiError.QUIZ_NOT_FOUND)
                return@launch
            }
            quizTitle = quiz.title
            questions = QuestionShuffler.shuffle(
                questions = quizRepository.getQuestionsForQuizOnce(quizId),
                getChoices = { it.choices },
                copyWithNewChoices = { q, newChoices -> q.copy(choices = newChoices) }
            )
            if (questions.isEmpty()) {
                _uiState.value = TakeQuizUiState.Error(UiError.QUIZ_HAS_NO_QUESTIONS)
                return@launch
            }
            startTimer()
            emitActiveState()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                elapsedSeconds++
                val current = _uiState.value
                if (current is TakeQuizUiState.Active) {
                    _uiState.update { (it as? TakeQuizUiState.Active)?.copy(elapsedSeconds = elapsedSeconds) ?: it }
                }
            }
        }
    }

    private fun onAnswerSelected(choiceId: String) {
        val question = questions.getOrNull(currentIndex) ?: return
        val current = answers[question.id] ?: emptySet()
        val updated = if (question.isMultiSelect) {
            if (current.contains(choiceId)) current - choiceId else current + choiceId
        } else {
            setOf(choiceId)
        }
        answers[question.id] = updated
        emitActiveState()
    }

    private fun onNextQuestion() {
        if (currentIndex < questions.size - 1) {
            currentIndex++
            emitActiveState()
        }
    }

    private fun onPreviousQuestion() {
        if (currentIndex > 0) {
            currentIndex--
            emitActiveState()
        }
    }

    private fun onGoToQuestion(index: Int) {
        if (index in questions.indices) {
            currentIndex = index
            emitActiveState()
        }
    }

    private fun onRequestExit() {
        val current = _uiState.value
        if (current is TakeQuizUiState.Active) {
            _uiState.value = current.copy(showExitDialog = true)
        }
    }

    private fun onConfirmExit() {
        timerJob?.cancel()
        val current = _uiState.value
        if (current is TakeQuizUiState.Active) {
            _uiState.value = current.copy(showExitDialog = false, shouldNavigateBack = true)
        }
    }

    private fun onDismissExitDialog() {
        val current = _uiState.value
        if (current is TakeQuizUiState.Active) {
            _uiState.value = current.copy(showExitDialog = false)
        }
    }

    private fun onSubmitQuiz() {
        viewModelScope.launch {
            val active = _uiState.value as? TakeQuizUiState.Active ?: return@launch
            _uiState.value = active.copy(isSubmitting = true)
            timerJob?.cancel()

            val userAnswers = answers.mapValues { (_, v) -> v.toSet() }
            val scoreResult = ScoreCalculator.calculatePointScore(questions, userAnswers)
            val answerMap = answers.mapValues { (_, v) -> v.toList() }
            val userId = authRepository.getCurrentUser()?.id ?: "guest_${UUID.randomUUID()}"

            val attempt = Attempt(
                id = attemptId,
                userId = userId,
                quizId = quizId,
                score = scoreResult.earnedScore,
                maxScore = scoreResult.maxScore,
                answers = answerMap,
                startTimeMillis = startTimeMillis,
                endTimeMillis = System.currentTimeMillis(),
                questionOrder = questions.map { it.id }
            )

            val result = attemptRepository.saveAttempt(attempt)
            _uiState.value = result.fold(
                onSuccess = {
                    quizRepository.incrementAttemptCount(quizId)
                    TakeQuizUiState.Finished(attemptId)
                },
                onFailure = { e -> TakeQuizUiState.Error(UiError.SAVE_RESULT_FAILED, e.message) }
            )
        }
    }

    private fun emitActiveState() {
        val question = questions.getOrNull(currentIndex) ?: return
        _uiState.value = TakeQuizUiState.Active(
            quizTitle = quizTitle,
            currentQuestion = question,
            currentIndex = currentIndex,
            totalQuestions = questions.size,
            selectedAnswers = answers[question.id] ?: emptySet(),
            elapsedSeconds = elapsedSeconds,
            isSubmitting = false,
            isMultiSelect = question.isMultiSelect,
            allAnswers = answers.toMap(),
            shouldNavigateBack = false
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
