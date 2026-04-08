package com.example.androidapp.ui.screens.pool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.ui.common.UiError
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
 * @property isLoadingMore Whether a "load more" operation is in progress.
 * @property hasMoreContributions Whether more contributions can be loaded.
 * @property hasMoreBrowse Whether more browse results can be loaded.
 * @property error Current error code, or null.
 * @property successMessage Transient success message, or null.
 */
data class QuestionPoolUiState(
    val selectedTab: Int = 0,
    val myContributions: List<QuestionPoolItem> = emptyList(),
    val browseResults: List<QuestionPoolItem> = emptyList(),
    val searchTags: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreContributions: Boolean = true,
    val hasMoreBrowse: Boolean = true,
    val error: UiError? = null,
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

    /** Loads more items for the My Contributions tab. */
    data object LoadMoreContributions : QuestionPoolEvent()

    /** Loads more items for the Browse tab. */
    data object LoadMoreBrowse : QuestionPoolEvent()
}

/**
 * ViewModel for the Question Pool screen.
 * Manages browsing the community question pool and managing user contributions
 * with cursor-based Firestore pagination.
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

    private companion object {
        /** Number of pool items per page. */
        const val PAGE_SIZE = 20
    }

    init {
        loadMyContributions(loadMore = false)
    }

    /**
     * Dispatches a [QuestionPoolEvent] to the ViewModel.
     */
    fun onEvent(event: QuestionPoolEvent) {
        when (event) {
            is QuestionPoolEvent.TabSelected -> {
                _uiState.update { it.copy(selectedTab = event.index) }
                if (event.index == 0) loadMyContributions(loadMore = false)
            }

            is QuestionPoolEvent.SearchTagsChanged ->
                _uiState.update { it.copy(searchTags = event.tags) }

            is QuestionPoolEvent.SearchPool -> searchPool(loadMore = false)

            is QuestionPoolEvent.RevokeContribution -> revokeContribution(event.poolItemId)

            is QuestionPoolEvent.ClearError ->
                _uiState.update { it.copy(error = null) }

            is QuestionPoolEvent.ClearSuccess ->
                _uiState.update { it.copy(successMessage = null) }

            is QuestionPoolEvent.Refresh -> {
                if (_uiState.value.selectedTab == 0) {
                    loadMyContributions(loadMore = false)
                } else {
                    searchPool(loadMore = false)
                }
            }

            is QuestionPoolEvent.LoadMoreContributions ->
                loadMyContributions(loadMore = true)

            is QuestionPoolEvent.LoadMoreBrowse ->
                searchPool(loadMore = true)
        }
    }

    /**
     * Loads contributions with cursor-based pagination.
     *
     * @param loadMore If true, appends next page; if false, resets to first page.
     */
    private fun loadMyContributions(loadMore: Boolean) {
        if (loadMore && (!_uiState.value.hasMoreContributions || _uiState.value.isLoadingMore)) return

        viewModelScope.launch {
            if (loadMore) {
                _uiState.update { it.copy(isLoadingMore = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = UiError.LOGIN_REQUIRED) }
                return@launch
            }

            val result = poolRepository.getMyContributionsPaged(
                userId = user.id,
                pageSize = PAGE_SIZE,
                loadMore = loadMore
            )

            result.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        val updatedContributions = if (loadMore) {
                            state.myContributions + page.items
                        } else {
                            page.items
                        }
                        state.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            myContributions = updatedContributions,
                            hasMoreContributions = page.hasMore
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = UiError.LOAD_DATA_FAILED
                        )
                    }
                }
            )
        }
    }

    /**
     * Searches pool by tags with cursor-based pagination.
     *
     * @param loadMore If true, appends next page; if false, resets to first page.
     */
    private fun searchPool(loadMore: Boolean) {
        val tags = _uiState.value.searchTags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (tags.isEmpty()) {
            _uiState.update { it.copy(error = UiError.POOL_SEARCH_TAGS_EMPTY) }
            return
        }

        if (loadMore && (!_uiState.value.hasMoreBrowse || _uiState.value.isLoadingMore)) return

        viewModelScope.launch {
            if (loadMore) {
                _uiState.update { it.copy(isLoadingMore = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            val result = poolRepository.getPoolQuestionsByTagsPaged(
                tags = tags,
                activeOnly = true,
                pageSize = PAGE_SIZE,
                loadMore = loadMore
            )

            result.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        val updatedResults = if (loadMore) {
                            state.browseResults + page.items
                        } else {
                            page.items
                        }
                        state.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            browseResults = updatedResults,
                            hasMoreBrowse = page.hasMore
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = UiError.SEARCH_FAILED
                        )
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
                    _uiState.update { it.copy(successMessage = "Da thu hoi cau hoi") }
                    // Reload from Firestore to confirm the write persisted
                    loadMyContributions(loadMore = false)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(error = UiError.POOL_REVOKE_FAILED)
                    }
                }
            )
        }
    }
}
