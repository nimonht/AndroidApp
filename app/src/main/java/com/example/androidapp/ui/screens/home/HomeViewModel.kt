package com.example.androidapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val joinCodeError: String? = null,
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
 */
class HomeViewModel(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /** Current UI state for the Home screen. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Handle for the long-lived home-data collection coroutine.
     * Cancelled and relaunched on every [loadHomeData] call so that:
     * 1. Only one collector exists at a time (no zombie coroutines).
     * 2. A fresh Room subscription always emits the current snapshot
     *    immediately, guaranteeing `isRefreshing` is reset to false.
     */
    private var homeDataJob: Job? = null

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
                    loadHomeData(user.id)
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
            _uiState.update { it.copy(joinCodeError = "Mã không hợp lệ. Vui lòng nhập 6 ký tự chữ hoặc số.") }
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
                            joinCodeError = "Không tìm thấy bài kiểm tra với mã này. Vui lòng kiểm tra lại mã."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isJoining = false,
                        joinCodeError = "Không thể tìm kiếm bài kiểm tra. Vui lòng thử lại."
                    )
                }
            }
        }
    }

    /**
     * Handles pull-to-refresh. Sets the refreshing flag immediately and
     * relaunches the home-data collector which cancels the previous one.
     */
    private fun onRefresh() {
        val userId = _uiState.value.userId
        if (userId.isNotBlank()) {
            _uiState.update { it.copy(isRefreshing = true) }
            loadHomeData(userId)
        }
    }

    /**
     * Cancels any in-flight home-data collection and starts a fresh one.
     *
     * Cancelling the previous [homeDataJob] is critical: Room/Firestore cold
     * Flows always emit current data on first subscription, so a fresh
     * `.collect` is guaranteed to fire immediately. This ensures
     * `isRefreshing` is reset to `false` without relying on data changes.
     */
    private fun loadHomeData(userId: String) {
        homeDataJob?.cancel()
        homeDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            quizRepository.getHomeQuizzes(userId).collect { homeQuizzes ->
                val removedCount = homeQuizzes.myQuizzes.count { it.isRemovedFromCloud }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
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
