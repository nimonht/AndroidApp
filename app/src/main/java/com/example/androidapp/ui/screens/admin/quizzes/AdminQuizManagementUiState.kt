package com.example.androidapp.ui.screens.admin.quizzes

import com.example.androidapp.domain.model.Quiz

/**
 * UI state for the admin quiz management screen.
 */
data class AdminQuizManagementUiState(
    val isLoading: Boolean = true,
    val quizzes: List<Quiz> = emptyList(),
    val searchQuery: String = "",
    val showDeleted: Boolean = false,
    val error: String? = null,
    val isPerformingAction: Boolean = false,
    val actionError: String? = null
)
