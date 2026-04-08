package com.example.androidapp.ui.screens.admin.quizzes

import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.common.UiError

/**
 * UI state for the admin quiz management screen.
 */
data class AdminQuizManagementUiState(
    val isLoading: Boolean = true,
    val quizzes: List<Quiz> = emptyList(),
    val searchQuery: String = "",
    val error: UiError? = null,
    val isPerformingAction: Boolean = false,
    val actionError: UiError? = null,
    val isOnline: Boolean = true,
    /** Whether more quizzes can be loaded from Firestore. */
    val hasMore: Boolean = true,
    /** Whether a "load more" operation is in progress. */
    val isLoadingMore: Boolean = false,

    // -- Advanced filtering & sorting ----------------------------------------

    /** Currently selected quiz status filter. */
    val statusFilter: QuizStatusFilter = QuizStatusFilter.ALL,
    /** Tag filter text input. */
    val tagFilter: String = "",
    /** Current sort field. */
    val sortField: QuizSortField = QuizSortField.DATE,
    /** Sort direction: true = ascending, false = descending. */
    val sortAscending: Boolean = false,
    /** Current admin user's permissions for conditional action display. */
    val currentPermissions: Set<AdminPermission> = emptySet(),
    /** Whether current user is superuser. */
    val isSuperuser: Boolean = false
)

/**
 * Quiz status filter options for the admin quiz management screen.
 */
enum class QuizStatusFilter {
    /** Show all quizzes regardless of status. */
    ALL,

    /** Show only published public quizzes. */
    PUBLIC,

    /** Show only published private quizzes. */
    PRIVATE,

    /** Show only draft quizzes. */
    DRAFT,

    /** Show only soft-deleted quizzes. */
    DELETED
}

/**
 * Quiz sort field options for the admin quiz management screen.
 */
enum class QuizSortField {
    /** Sort by creation date. */
    DATE,

    /** Sort by quiz title. */
    NAME,

    /** Sort by attempt count. */
    ATTEMPTS,

    /** Sort by question count. */
    QUESTIONS
}
