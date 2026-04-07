package com.example.androidapp.ui.screens.admin.quizzes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the admin quiz management screen.
 *
 * Uses cursor-based Firestore pagination instead of loading all quizzes at once.
 * Pages are accumulated in [allQuizzes] and client-side filtering is applied
 * on the accumulated set.
 *
 * @param adminRepository Repository for admin operations.
 * @param networkMonitor Monitor for observing network connectivity state.
 */
class AdminQuizManagementViewModel(
    private val adminRepository: AdminRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminQuizManagementUiState())
    val uiState: StateFlow<AdminQuizManagementUiState> = _uiState.asStateFlow()

    private var allQuizzes: List<Quiz> = emptyList()

    private companion object {
        /** Number of quizzes to fetch per page from Firestore. */
        const val PAGE_SIZE = 30
    }

    init {
        loadQuizzes()
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }
    }

    /**
     * Returns `true` if the device is currently online.
     * Sets [AdminQuizManagementUiState.actionError] and returns `false` otherwise.
     */
    private fun requireOnline(): Boolean {
        if (!networkMonitor.isOnline.value) {
            _uiState.value = _uiState.value.copy(
                actionError = "Khong co ket noi mang. Vui long ket noi internet de thuc hien thao tac quan tri."
            )
            return false
        }
        return true
    }

    /**
     * Load the first page of quizzes from the repository.
     * Resets pagination to the beginning.
     */
    fun loadQuizzes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasMore = true)
            allQuizzes = emptyList()

            try {
                val page = adminRepository.getQuizzesPage(
                    pageSize = PAGE_SIZE,
                    includeDeleted = true,
                    loadMore = false
                )
                allQuizzes = page.items
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quizzes = filterQuizzes(
                        allQuizzes,
                        _uiState.value.searchQuery,
                        _uiState.value.showDeleted
                    ),
                    hasMore = page.hasMore,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Khong the tai danh sach quiz"
                )
            }
        }
    }

    /**
     * Load the next page of quizzes and append to the accumulated list.
     */
    fun loadMoreQuizzes() {
        if (!_uiState.value.hasMore || _uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                val page = adminRepository.getQuizzesPage(
                    pageSize = PAGE_SIZE,
                    includeDeleted = true,
                    loadMore = true
                )
                allQuizzes = allQuizzes + page.items
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    quizzes = filterQuizzes(
                        allQuizzes,
                        _uiState.value.searchQuery,
                        _uiState.value.showDeleted
                    ),
                    hasMore = page.hasMore
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Khong the tai them quiz"
                )
            }
        }
    }

    /**
     * Update search query and filter quizzes.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            quizzes = filterQuizzes(allQuizzes, query, _uiState.value.showDeleted)
        )
    }

    /**
     * Toggle show deleted quizzes filter.
     */
    fun toggleShowDeleted() {
        val newShowDeleted = !_uiState.value.showDeleted
        _uiState.value = _uiState.value.copy(
            showDeleted = newShowDeleted,
            quizzes = filterQuizzes(allQuizzes, _uiState.value.searchQuery, newShowDeleted)
        )
    }

    /**
     * Publish or unpublish a quiz.
     */
    fun togglePublishQuiz(quizId: String, currentlyPublic: Boolean) {
        if (!requireOnline()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            val result = if (currentlyPublic) {
                adminRepository.unpublishQuiz(quizId)
            } else {
                adminRepository.forcePublishQuiz(quizId)
            }

            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                    loadQuizzes()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Khong the cap nhat trang thai quiz"
                    )
                }
        }
    }

    /**
     * Restore a soft-deleted quiz.
     */
    fun restoreQuiz(quizId: String) {
        if (!requireOnline()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.restoreQuiz(quizId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                    loadQuizzes()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Khong the khoi phuc quiz"
                    )
                }
        }
    }

    /**
     * Delete a quiz permanently.
     */
    fun deleteQuiz(quizId: String) {
        if (!requireOnline()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.deleteQuizPermanently(quizId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                    loadQuizzes()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Khong the xoa quiz"
                    )
                }
        }
    }

    /**
     * Clear action error.
     */
    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }

    /**
     * Filter quizzes based on search query and deleted status.
     */
    private fun filterQuizzes(
        quizzes: List<Quiz>,
        query: String,
        showDeleted: Boolean
    ): List<Quiz> {
        var filtered = quizzes

        // Filter by deleted status
        filtered = if (showDeleted) {
            filtered.filter { it.deletedAt != null }
        } else {
            filtered.filter { it.deletedAt == null }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter { quiz ->
                quiz.title.lowercase().contains(lowerQuery) ||
                        quiz.authorName.lowercase().contains(lowerQuery) ||
                        quiz.tags.any { it.lowercase().contains(lowerQuery) }
            }
        }

        return filtered
    }
}
