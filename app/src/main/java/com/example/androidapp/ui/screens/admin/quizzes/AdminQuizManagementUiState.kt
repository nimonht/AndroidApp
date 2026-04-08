package com.example.androidapp.ui.screens.admin.quizzes

import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.common.UiError

/**
 * UI state for the admin quiz management screen.
 */
data class AdminQuizManagementUiState(
    val isLoading: Boolean = true,
    val quizzes: List<Quiz> = emptyList(),
    val searchQuery: String = "",
    val showDeleted: Boolean = false,
    val error: UiError? = null,
    val isPerformingAction: Boolean = false,
    val actionError: UiError? = null,
    val isOnline: Boolean = true,
    /** Whether more quizzes can be loaded from Firestore. */
    val hasMore: Boolean = true,
    /** Whether a "load more" operation is in progress. */
    val isLoadingMore: Boolean = false
)
