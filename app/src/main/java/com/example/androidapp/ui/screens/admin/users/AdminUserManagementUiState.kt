package com.example.androidapp.ui.screens.admin.users

import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.User
import com.example.androidapp.ui.common.UiError

/**
 * Filter options for user roles in the admin user management screen.
 */
enum class UserRoleFilter {
    /** Show all users regardless of role. */
    ALL,

    /** Show only superuser accounts. */
    SUPERUSER,

    /** Show only admin accounts. */
    ADMIN,

    /** Show only regular (non-banned) users. */
    USER,

    /** Show only banned users. */
    BANNED
}

/**
 * Sort field options for the admin user list.
 */
enum class UserSortField {
    /** Sort by display name. */
    NAME,

    /** Sort by email address. */
    EMAIL,

    /** Sort by role ordinal. */
    ROLE,

    /** Sort by creation date (server default). */
    DATE
}

/**
 * UI state for the admin user management screen.
 *
 * Contains loading/error states, the filtered user list, search and filter
 * parameters, pagination flags, and superuser context for permission-gated
 * actions.
 */
data class AdminUserManagementUiState(
    /** Whether the initial user list is loading. */
    val isLoading: Boolean = true,

    /** The currently displayed (filtered + sorted) list of users. */
    val users: List<User> = emptyList(),

    /** Current text in the search bar. */
    val searchQuery: String = "",

    /** Non-null when the initial load or refresh failed. */
    val error: UiError? = null,

    /** Whether a destructive action (role change, ban, delete) is in progress. */
    val isPerformingAction: Boolean = false,

    /** Non-null when the last action failed. */
    val actionError: UiError? = null,

    /** Whether the device is currently online. */
    val isOnline: Boolean = true,

    /** Whether more users can be loaded from Firestore. */
    val hasMore: Boolean = true,

    /** Whether a "load more" operation is in progress. */
    val isLoadingMore: Boolean = false,

    // -- Filter & sort fields ------------------------------------------------

    /** Active role filter for the user list. */
    val roleFilter: UserRoleFilter = UserRoleFilter.ALL,

    /** Active sort field for the user list. */
    val sortField: UserSortField = UserSortField.NAME,

    /** Whether the sort order is ascending (`true`) or descending (`false`). */
    val sortAscending: Boolean = true,

    // -- Superuser context ---------------------------------------------------

    /** Effective permissions of the currently logged-in admin. */
    val currentPermissions: Set<AdminPermission> = emptySet(),

    /** Whether the currently logged-in user holds the SUPERUSER role. */
    val isSuperuser: Boolean = false,

    /** ID of the currently logged-in user (used to hide self-actions). */
    val currentUserId: String = ""
)
