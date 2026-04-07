package com.example.androidapp.ui.screens.admin.users

import com.example.androidapp.domain.model.User

/**
 * UI state for the admin user management screen.
 */
data class AdminUserManagementUiState(
    val isLoading: Boolean = true,
    val users: List<User> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val isPerformingAction: Boolean = false,
    val actionError: String? = null,
    val isOnline: Boolean = true,
    /** Whether more users can be loaded from Firestore. */
    val hasMore: Boolean = true,
    /** Whether a "load more" operation is in progress. */
    val isLoadingMore: Boolean = false
)
