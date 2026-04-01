package com.example.androidapp.ui.screens.pool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Question Pool screen.
 *
 * @property selectedTab The currently selected tab index (0 = My Contributions, 1 = Browse).
 * @property myContributions The user's contributed questions.
 * @property browseResults Questions found by tag search in the pool.
 * @property searchTags Comma-separated tag input for browsing.
 * @property isLoading Whether a loading operation is in progress.
 * @property error Current error message, or null.
 * @property successMessage Transient success message, or null.
 */
data class QuestionPoolUiState(
    val selectedTab: Int = 0,
    val myContributions: List<QuestionPoolItem> = emptyList(),
    val browseResults: List<QuestionPoolItem> = emptyList(),
    val searchTags: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/** Events that can be dispatched to [QuestionPoolViewModel]. */
sealed class QuestionPoolEvent {
    /** Switches to the specified tab. */
    data class TabSelected(val index: Int) : QuestionPoolEvent()

    /** Updates the tag search input. */
    data class SearchTagsChanged(val tags: String) : QuestionPoolEvent()

    /** Triggers a search of the pool by tags. */
    data object SearchPool : QuestionPoolEvent()

    /** Revokes a contributed question from the pool. */
    data class RevokeContribution(val poolItemId: String) : QuestionPoolEvent()

    /** Clears the current error message. */
    data object ClearError : QuestionPoolEvent()

    /** Clears the current success message. */
    data object ClearSuccess : QuestionPoolEvent()

    /** Refreshes the current tab data. */
    data object Refresh : QuestionPoolEvent()
}

/**
 * ViewModel for the Question Pool screen.
 * Manages browsing the community question pool and managing user contributions.
 *
 * @param poolRepository Repository for question pool operations.
 * @param authRepository Repository for retrieving the currently authenticated user.
 */
class QuestionPoolViewModel(
    private val poolRepository: PoolRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionPoolUiState())

    /** Current UI state for the Question Pool screen. */
    val uiState: StateFlow<QuestionPoolUiState> = _uiState.asStateFlow()

    init {
        loadMyContributions()
    }

    /**
     * Dispatches a [QuestionPoolEvent] to the ViewModel.
     */
    fun onEvent(event: QuestionPoolEvent) {
        when (event) {
            is QuestionPoolEvent.TabSelected -> {
                _uiState.update { it.copy(selectedTab = event.index) }
                if (event.index == 0) loadMyContributions()
            }

            is QuestionPoolEvent.SearchTagsChanged ->
                _uiState.update { it.copy(searchTags = event.tags) }

            is QuestionPoolEvent.SearchPool -> searchPool()

            is QuestionPoolEvent.RevokeContribution -> revokeContribution(event.poolItemId)

            is QuestionPoolEvent.ClearError ->
                _uiState.update { it.copy(error = null) }

            is QuestionPoolEvent.ClearSuccess ->
                _uiState.update { it.copy(successMessage = null) }

            is QuestionPoolEvent.Refresh -> {
                if (_uiState.value.selectedTab == 0) {
                    loadMyContributions()
                } else {
                    searchPool()
                }
            }
        }
    }

    private fun loadMyContributions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Vui lòng đăng nhập") }
                return@launch
            }
            val result = poolRepository.getMyContributions(user.id)
            result.fold(
                onSuccess = { contributions ->
                    _uiState.update {
                        it.copy(isLoading = false, myContributions = contributions)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Không thể tải dữ liệu")
                    }
                }
            )
        }
    }

    private fun searchPool() {
        viewModelScope.launch {
            val tags = _uiState.value.searchTags
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (tags.isEmpty()) {
                _uiState.update { it.copy(error = "Vui lòng nhập ít nhất một từ khóa") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result = poolRepository.getPoolQuestionsByTags(tags, activeOnly = true)
            result.fold(
                onSuccess = { items ->
                    _uiState.update { it.copy(isLoading = false, browseResults = items) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Không thể tìm kiếm")
                    }
                }
            )
        }
    }

    private fun revokeContribution(poolItemId: String) {
        viewModelScope.launch {
            val result = poolRepository.revokeContribution(poolItemId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Đã thu hồi câu hỏi") }
                    // Reload from Firestore to confirm the write persisted
                    // instead of relying on an optimistic local update.
                    loadMyContributions()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Không thể thu hồi câu hỏi")
                    }
                }
            )
        }
    }
}
