package com.example.androidapp.ui.screens.admin.quizzes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the admin quiz management screen.
 *
 * @param adminRepository Repository for admin operations.
 */
class AdminQuizManagementViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminQuizManagementUiState())
    val uiState: StateFlow<AdminQuizManagementUiState> = _uiState.asStateFlow()

    private var allQuizzes: List<Quiz> = emptyList()

    init {
        loadQuizzes()
    }

    /**
     * Load all quizzes from the repository.
     */
    fun loadQuizzes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getAllQuizzes()
                .collect { result ->
                    result
                        .onSuccess { quizzes ->
                            allQuizzes = quizzes
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                quizzes = filterQuizzes(
                                    quizzes,
                                    _uiState.value.searchQuery,
                                    _uiState.value.showDeleted
                                ),
                                error = null
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = error.message ?: "Không thể tải danh sách quiz"
                            )
                        }
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
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Không thể cập nhật trạng thái quiz"
                    )
                }
        }
    }

    /**
     * Restore a soft-deleted quiz.
     */
    fun restoreQuiz(quizId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.restoreQuiz(quizId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Không thể khôi phục quiz"
                    )
                }
        }
    }

    /**
     * Delete a quiz permanently.
     */
    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.deleteQuizPermanently(quizId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Không thể xóa quiz"
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
