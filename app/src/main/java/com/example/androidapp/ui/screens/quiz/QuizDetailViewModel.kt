package com.example.androidapp.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Possible UI states for the Quiz Detail screen.
 */
sealed class QuizDetailUiState {
    data object Loading : QuizDetailUiState()
    data class Success(
        val quiz: Quiz,
        val questions: List<Question>,
        val isRefreshing: Boolean = false
    ) : QuizDetailUiState()

    data class Error(val message: String) : QuizDetailUiState()
}

/**
 * ViewModel for the Quiz Detail screen.
 * Loads quiz metadata and question list from the repository.
 *
 * Implements a remote refresh fallback: when questions loaded from Room are
 * empty but the quiz advertises [Quiz.questionCount] > 0, the ViewModel
 * automatically fetches the full quiz (with questions and choices) from
 * Firestore and re-populates Room. The user can also trigger a manual
 * refresh via [onRefresh].
 */
class QuizDetailViewModel(
    private val quizId: String,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizDetailUiState>(QuizDetailUiState.Loading)

    /** Current UI state for the Quiz Detail screen. */
    val uiState: StateFlow<QuizDetailUiState> = _uiState.asStateFlow()

    /** Tracks whether a remote refresh has already been attempted for the current load. */
    private var remoteRefreshAttempted = false

    /** Active collection job so we can cancel it before starting a new one. */
    private var collectJob: Job? = null

    init {
        loadQuizDetail()
    }

    /**
     * Reloads the quiz detail data from local storage.
     * If local questions are still missing, triggers a remote refresh automatically.
     */
    fun onRetry() {
        remoteRefreshAttempted = false
        loadQuizDetail()
    }

    /**
     * Explicitly refreshes quiz data from Firestore, re-populating Room.
     * Useful when the user suspects local data is stale or incomplete.
     */
    fun onRefresh() {
        refreshFromRemote()
    }

    private fun loadQuizDetail() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            _uiState.value = QuizDetailUiState.Loading

            val quiz = quizRepository.getQuizById(quizId)
            if (quiz == null) {
                // Quiz not in Room — attempt to fetch from remote before giving up
                if (!remoteRefreshAttempted) {
                    remoteRefreshAttempted = true
                    val result = quizRepository.refreshQuizFromRemote(quizId)
                    if (result.isSuccess) {
                        // Restart load now that Room should have the data
                        loadQuizDetail()
                        return@launch
                    }
                }
                _uiState.value = QuizDetailUiState.Error("Khong tim thay bai kiem tra")
                return@launch
            }

            quizRepository.getQuestionsForQuiz(quizId).collect { questions ->
                if (questions.isEmpty() && quiz.questionCount > 0 && !remoteRefreshAttempted) {
                    // Questions missing locally but the quiz claims to have some — refresh
                    remoteRefreshAttempted = true
                    refreshFromRemote(quiz)
                } else {
                    _uiState.value = QuizDetailUiState.Success(quiz, questions)
                }
            }
        }
    }

    /**
     * Fetches the full quiz (with questions and choices) from Firestore,
     * inserts into Room, and restarts the local flow collection.
     *
     * @param currentQuiz Optional quiz already loaded from Room; used to show
     *   a "refreshing" indicator in [QuizDetailUiState.Success] while the
     *   network call is in progress.
     */
    private fun refreshFromRemote(currentQuiz: Quiz? = null) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            // Show a refreshing indicator if we already have partial data
            if (currentQuiz != null) {
                val currentState = _uiState.value
                val currentQuestions = (currentState as? QuizDetailUiState.Success)?.questions
                    ?: emptyList()
                _uiState.value = QuizDetailUiState.Success(
                    quiz = currentQuiz,
                    questions = currentQuestions,
                    isRefreshing = true
                )
            } else {
                _uiState.value = QuizDetailUiState.Loading
            }

            val result = quizRepository.refreshQuizFromRemote(quizId)

            if (result.isFailure) {
                // If we already have a quiz in the success state, keep it visible
                val current = _uiState.value
                if (current is QuizDetailUiState.Success) {
                    _uiState.value = current.copy(isRefreshing = false)
                } else {
                    _uiState.value = QuizDetailUiState.Error(
                        result.exceptionOrNull()?.message
                            ?: "Khong the tai du lieu tu may chu"
                    )
                }
                return@launch
            }

            // Room has been re-populated — restart the local flow collection
            loadQuizDetail()
        }
    }
}
