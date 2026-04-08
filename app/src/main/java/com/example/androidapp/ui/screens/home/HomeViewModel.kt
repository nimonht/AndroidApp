package com.example.androidapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.ui.common.UiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val recentQuizzes: List<Quiz> = emptyList(),
    val myQuizzes: List<Quiz> = emptyList(),
    val trendingQuizzes: List<Quiz> = emptyList(),
    val joinCode: String = "",
    val joinCodeError: UiError? = null,
    val isJoining: Boolean = false,
    val joinedQuizId: String? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val displayName: String = "",
    val photoUrl: String? = null,
    val userId: String = "",
    /**
     * Number of the current user's quizzes that were removed from Firestore
     * (e.g. by an admin) but still exist locally. When greater than zero the
     * UI shows a warning banner.
     */
    val adminRemovedQuizCount: Int = 0
)

/** Events that can be dispatched to [HomeViewModel]. */
sealed class HomeEvent {
    data class JoinCodeChanged(val code: String) : HomeEvent()
    data class JoinQuiz(val code: String) : HomeEvent()
    data object Refresh : HomeEvent()
    data object ClearError : HomeEvent()
    data object ClearJoinResult : HomeEvent()
}

/**
 * ViewModel for the Home screen.
 * Loads recent, owned, and trending quizzes using the local-first pattern.
 *
 * Data observation and refresh are intentionally decoupled:
 * - [observeHomeData] sets up a long-lived collector that continuously emits
 *   Room snapshots. It is started once per user and never cancelled for refresh.
 * - [onRefresh] triggers a separate, short-lived Firestore sync with a timeout.
 *   When the sync writes to Room, the existing observer picks up the changes.
 *
 * This guarantees the refresh indicator is always cleared (via `finally`) and
 * eliminates spinner-stuck bugs caused by Flow emission delays.
 */
class HomeViewModel(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /** Current UI state for the Home screen. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Handle for the long-lived home-data observation coroutine.
     * Cancelled only when the user changes (login/logout), never for refresh.
     */
    private var homeDataJob: Job? = null

    /**
     * Handle for the refresh coroutine. Cancelled and relaunched on each
     * pull-to-refresh so only the latest refresh runs.
     */
    private var refreshJob: Job? = null

    private companion object {
        /**
         * Maximum time (ms) the Firestore refresh is allowed to take.
         * The `finally` block ensures the spinner stops even on timeout.
         */
        const val REFRESH_TIMEOUT_MS = 8_000L
    }

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        displayName = user?.displayName ?: "",
                        photoUrl = user?.photoUrl,
                        userId = user?.id ?: ""
                    )
                }
                if (user != null) {
                    observeHomeData(user.id)
                }
            }
        }
    }

    /**
     * Dispatches a [HomeEvent] to the ViewModel.
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.JoinCodeChanged -> _uiState.update {
                it.copy(joinCode = event.code, joinCodeError = null)
            }

            is HomeEvent.JoinQuiz -> onJoinQuiz(event.code)
            is HomeEvent.Refresh -> onRefresh()
            is HomeEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is HomeEvent.ClearJoinResult -> _uiState.update { it.copy(joinedQuizId = null) }
        }
    }

    private fun onJoinQuiz(code: String) {
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.length != 6 || !trimmedCode.all { it.isLetterOrDigit() }) {
            _uiState.update { it.copy(joinCodeError = UiError.INVALID_JOIN_CODE) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, joinCodeError = null) }
            try {
                val quiz = quizRepository.getQuizByShareCode(trimmedCode)
                if (quiz != null) {
                    _uiState.update { it.copy(isJoining = false, joinedQuizId = quiz.id) }
                } else {
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            joinCodeError = UiError.JOIN_QUIZ_NOT_FOUND
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isJoining = false,
                        joinCodeError = UiError.JOIN_QUIZ_FAILED
                    )
                }
            }
        }
    }

    /**
     * Handles pull-to-refresh.
     *
     * Launches a **separate** coroutine (independent of [homeDataJob]) that:
     * 1. Sets `isRefreshing = true`.
     * 2. Calls [QuizRepository.refreshHomeData] with a timeout.
     * 3. Always clears `isRefreshing` in `finally` — even on error or timeout.
     *
     * The existing [observeHomeData] collector picks up Room changes written by
     * the refresh, so the UI updates naturally without restarting the observer.
     */
    private fun onRefresh() {
        val userId = _uiState.value.userId
        if (userId.isBlank()) return

        // Cancel any in-flight refresh so rapid pulls don't pile up.
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                withTimeoutOrNull(REFRESH_TIMEOUT_MS) {
                    quizRepository.refreshHomeData(userId)
                }
            } catch (_: Exception) {
                // Network error, cancellation, etc. — just stop the spinner.
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Sets up a long-lived collector for home screen data.
     *
     * Cancelled and restarted only when the logged-in user changes — never
     * for pull-to-refresh. The underlying Room Flows continuously emit
     * snapshots, and the `onStart` block in the repository triggers an
     * initial Firestore sync on first subscription.
     */
    private fun observeHomeData(userId: String) {
        homeDataJob?.cancel()
        homeDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            quizRepository.getHomeQuizzes(userId).collect { homeQuizzes ->
                val removedCount = homeQuizzes.myQuizzes.count { it.isRemovedFromCloud }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        recentQuizzes = homeQuizzes.recentAttemptQuizzes,
                        myQuizzes = homeQuizzes.myQuizzes,
                        trendingQuizzes = homeQuizzes.trendingQuizzes,
                        adminRemovedQuizCount = removedCount
                    )
                }
            }
        }
    }
}
