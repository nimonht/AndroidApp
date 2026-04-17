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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
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
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true
)

/** Events that can be dispatched to [HistoryViewModel]. */
sealed class HistoryEvent {
    data object Refresh : HistoryEvent()
    data object LoadMore : HistoryEvent()
}

/**
 * ViewModel for the History screen.
 * Loads attempts for the current user with pagination and enriches them with quiz titles.
 *
 * Uses a dynamic LIMIT Room query that increases when the user scrolls near
 * the bottom. Quiz titles are cached in a [HashMap] to avoid the N+1 query
 * problem where each attempt triggers a separate [QuizRepository.getQuizById] call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val attemptRepository: AttemptRepository,
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())

    /** Current UI state for the History screen. */
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    /** Dynamic limit for paginated attempt loading. */
    private val _historyLimit = MutableStateFlow(INITIAL_PAGE_SIZE)

    /** Cache of quiz titles to avoid repeated lookups. Key = quizId. */
    private val quizTitleCache = HashMap<String, Pair<String, Boolean>>()

    companion object {
        /** Initial number of attempts to load. */
        const val INITIAL_PAGE_SIZE = 20

        /** Number of additional attempts to load on each "load more". */
        const val PAGE_SIZE = 20

        /** Fallback title shown when the quiz has been permanently deleted from the database. */
        const val DELETED_QUIZ_FALLBACK_TITLE = "N/A"
    }

    init {
        loadHistory()
    }

    /**
     * Dispatches a [HistoryEvent] to the ViewModel.
     */
    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.Refresh -> {
                quizTitleCache.clear()
                _historyLimit.value = INITIAL_PAGE_SIZE
                loadHistory()
            }

            is HistoryEvent.LoadMore -> handleLoadMore()
        }
    }

    /**
     * Increases the history limit to load more attempts.
     */
    private fun handleLoadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoadingMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        _historyLimit.value += PAGE_SIZE
    }

    /**
     * Loads history using a dynamic LIMIT query.
     * Uses [flatMapLatest] on [_historyLimit] so that increasing the limit
     * triggers a new Room query. Quiz title lookups are cached to avoid N+1.
     */
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, attempts = emptyList()) }
                return@launch
            }

            val totalCount = try {
                attemptRepository.getAttemptCountByUser(user.id)
            } catch (_: Exception) {
                // Count query failed; fall back to limit-based detection below
                -1
            }

            _historyLimit.flatMapLatest { limit ->
                attemptRepository.getAttemptsByUserLimited(user.id, limit)
            }.collectLatest { attempts ->
                val currentLimit = _historyLimit.value
                val enriched = attempts.map { attempt ->
                    val (title, isDeleted) = quizTitleCache.getOrPut(attempt.quizId) {
                        val quiz = quizRepository.getQuizById(attempt.quizId)
                        val deleted = quiz == null || quiz.deletedAt != null
                        val t = quiz?.title ?: DELETED_QUIZ_FALLBACK_TITLE
                        Pair(t, deleted)
                    }
                    AttemptWithQuiz(
                        attempt = attempt,
                        quizTitle = title,
                        isQuizDeleted = isDeleted
                    )
                }
                // If totalCount is available use it; otherwise infer from
                // whether the query returned a full page (limit-based detection).
                val hasMore = if (totalCount >= 0) {
                    attempts.size < totalCount
                } else {
                    attempts.size >= currentLimit
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        attempts = enriched,
                        hasMore = hasMore
                    )
                }
            }
        }
    }
}
