package com.example.androidapp.ui.screens.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the admin user management screen.
 *
 * Uses cursor-based Firestore pagination instead of loading all users at once.
 * Pages are accumulated in [allUsers] and client-side filtering is applied
 * on the accumulated set.
 *
 * Supports role-based filtering, multi-field sorting, and permission-gated
 * actions (superuser / [AdminPermission] checks).
 *
 * @param adminRepository Repository for admin operations.
 * @param networkMonitor Monitor for observing network connectivity state.
 * @param authRepository Repository for authentication and current-user info.
 */
class AdminUserManagementViewModel(
    private val adminRepository: AdminRepository,
    private val networkMonitor: NetworkMonitor,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUserManagementUiState())
    val uiState: StateFlow<AdminUserManagementUiState> = _uiState.asStateFlow()

    private var allUsers: List<User> = emptyList()

    private companion object {
        /** Number of users to fetch per page from Firestore. */
        const val PAGE_SIZE = 30
    }

    init {
        // Load current user context (permissions + superuser flag)
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            val perms = try {
                adminRepository.getCurrentAdminPermissions()
            } catch (_: Exception) {
                emptySet()
            }
            _uiState.value = _uiState.value.copy(
                currentPermissions = perms,
                isSuperuser = user?.isSuperuser() == true,
                currentUserId = user?.id ?: ""
            )
        }

        loadUsers()

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }
    }

    /**
     * Returns `true` if the device is currently online.
     * Sets [AdminUserManagementUiState.actionError] and returns `false` otherwise.
     */
    private fun requireOnline(): Boolean {
        if (!networkMonitor.isOnline.value) {
            _uiState.value = _uiState.value.copy(
                actionError = UiError.NETWORK_UNAVAILABLE
            )
            return false
        }
        return true
    }

    // -----------------------------------------------------------------------
    // Data loading
    // -----------------------------------------------------------------------

    /**
     * Load the first page of users from the repository.
     * Resets pagination to the beginning.
     */
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasMore = true)
            allUsers = emptyList()

            try {
                val page = adminRepository.getUsersPage(PAGE_SIZE, loadMore = false)
                allUsers = page.items
                val state = _uiState.value
                _uiState.value = state.copy(
                    isLoading = false,
                    users = filterAndSortUsers(
                        allUsers,
                        state.searchQuery,
                        state.roleFilter,
                        state.sortField,
                        state.sortAscending
                    ),
                    hasMore = page.hasMore,
                    error = null
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError.LOAD_USER_LIST_FAILED
                )
            }
        }
    }

    /**
     * Load the next page of users and append to the accumulated list.
     */
    fun loadMoreUsers() {
        if (!_uiState.value.hasMore || _uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                val page = adminRepository.getUsersPage(PAGE_SIZE, loadMore = true)
                allUsers = allUsers + page.items
                val state = _uiState.value
                _uiState.value = state.copy(
                    isLoadingMore = false,
                    users = filterAndSortUsers(
                        allUsers,
                        state.searchQuery,
                        state.roleFilter,
                        state.sortField,
                        state.sortAscending
                    ),
                    hasMore = page.hasMore
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = UiError.LOAD_MORE_USERS_FAILED
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Filter / sort event handlers
    // -----------------------------------------------------------------------

    /**
     * Update search query and re-filter users.
     */
    fun onSearchQueryChanged(query: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            searchQuery = query,
            users = filterAndSortUsers(
                allUsers, query, state.roleFilter, state.sortField, state.sortAscending
            )
        )
    }

    /**
     * Change the active role filter and re-filter users.
     */
    fun onRoleFilterChanged(filter: UserRoleFilter) {
        val state = _uiState.value
        _uiState.value = state.copy(
            roleFilter = filter,
            users = filterAndSortUsers(
                allUsers, state.searchQuery, filter, state.sortField, state.sortAscending
            )
        )
    }

    /**
     * Change the active sort field and re-sort users.
     */
    fun onSortFieldChanged(field: UserSortField) {
        val state = _uiState.value
        _uiState.value = state.copy(
            sortField = field,
            users = filterAndSortUsers(
                allUsers, state.searchQuery, state.roleFilter, field, state.sortAscending
            )
        )
    }

    /**
     * Toggle between ascending and descending sort order.
     */
    fun onToggleSortOrder() {
        val state = _uiState.value
        val newAscending = !state.sortAscending
        _uiState.value = state.copy(
            sortAscending = newAscending,
            users = filterAndSortUsers(
                allUsers, state.searchQuery, state.roleFilter, state.sortField, newAscending
            )
        )
    }

    /**
     * Reset all filters and sort options to their defaults.
     */
    fun clearFilters() {
        val state = _uiState.value
        _uiState.value = state.copy(
            searchQuery = "",
            roleFilter = UserRoleFilter.ALL,
            sortField = UserSortField.NAME,
            sortAscending = true,
            users = filterAndSortUsers(
                allUsers,
                query = "",
                roleFilter = UserRoleFilter.ALL,
                sortField = UserSortField.NAME,
                sortAscending = true
            )
        )
    }

    // -----------------------------------------------------------------------
    // Admin actions (permission-gated)
    // -----------------------------------------------------------------------

    /**
     * Update a user's role.
     *
     * Requires [AdminPermission.CHANGE_USER_ROLES] or superuser status.
     */
    fun updateUserRole(userId: String, newRole: UserRole) {
        if (!requireOnline()) return

        // Prevent self-action
        if (userId == _uiState.value.currentUserId) {
            _uiState.value = _uiState.value.copy(actionError = UiError.SELF_ACTION_NOT_ALLOWED)
            return
        }

        // Prevent non-superuser admin from targeting a superuser
        val targetUser = allUsers.find { it.id == userId }
        if (!_uiState.value.isSuperuser && targetUser?.isSuperuser() == true) {
            _uiState.value = _uiState.value.copy(actionError = UiError.TARGET_IS_SUPERUSER)
            return
        }

        val state = _uiState.value
        if (!state.isSuperuser &&
            !state.currentPermissions.contains(AdminPermission.CHANGE_USER_ROLES)
        ) {
            _uiState.value = state.copy(actionError = UiError.INSUFFICIENT_PERMISSIONS)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.updateUserRole(userId, newRole)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                    loadUsers()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = UiError.UPDATE_USER_ROLE_FAILED
                    )
                }
        }
    }

    /**
     * Ban a user.
     *
     * Requires [AdminPermission.BAN_USERS] or superuser status.
     */
    fun banUser(userId: String) {
        if (!requireOnline()) return

        // Prevent self-action
        if (userId == _uiState.value.currentUserId) {
            _uiState.value = _uiState.value.copy(actionError = UiError.SELF_ACTION_NOT_ALLOWED)
            return
        }

        // Prevent non-superuser admin from targeting a superuser
        val targetUser = allUsers.find { it.id == userId }
        if (!_uiState.value.isSuperuser && targetUser?.isSuperuser() == true) {
            _uiState.value = _uiState.value.copy(actionError = UiError.TARGET_IS_SUPERUSER)
            return
        }

        val state = _uiState.value
        if (!state.isSuperuser &&
            !state.currentPermissions.contains(AdminPermission.BAN_USERS)
        ) {
            _uiState.value = state.copy(actionError = UiError.INSUFFICIENT_PERMISSIONS)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.banUser(userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                    loadUsers()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = UiError.BAN_USER_FAILED
                    )
                }
        }
    }

    /**
     * Unban a user.
     *
     * Requires [AdminPermission.BAN_USERS] or superuser status.
     */
    fun unbanUser(userId: String) {
        if (!requireOnline()) return

        // Prevent self-action
        if (userId == _uiState.value.currentUserId) {
            _uiState.value = _uiState.value.copy(actionError = UiError.SELF_ACTION_NOT_ALLOWED)
            return
        }

        val state = _uiState.value
        if (!state.isSuperuser &&
            !state.currentPermissions.contains(AdminPermission.BAN_USERS)
        ) {
            _uiState.value = state.copy(actionError = UiError.INSUFFICIENT_PERMISSIONS)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.unbanUser(userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                    loadUsers()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = UiError.UNBAN_USER_FAILED
                    )
                }
        }
    }

    /**
     * Delete a user permanently.
     *
     * Requires [AdminPermission.DELETE_USERS] or superuser status.
     */
    fun deleteUser(userId: String) {
        if (!requireOnline()) return

        // Prevent self-action
        if (userId == _uiState.value.currentUserId) {
            _uiState.value = _uiState.value.copy(actionError = UiError.SELF_ACTION_NOT_ALLOWED)
            return
        }

        // Prevent non-superuser admin from targeting a superuser
        val targetUser = allUsers.find { it.id == userId }
        if (!_uiState.value.isSuperuser && targetUser?.isSuperuser() == true) {
            _uiState.value = _uiState.value.copy(actionError = UiError.TARGET_IS_SUPERUSER)
            return
        }

        val state = _uiState.value
        if (!state.isSuperuser &&
            !state.currentPermissions.contains(AdminPermission.DELETE_USERS)
        ) {
            _uiState.value = state.copy(actionError = UiError.INSUFFICIENT_PERMISSIONS)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.deleteUserPermanently(userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                    loadUsers()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = UiError.DELETE_USER_FAILED
                    )
                }
        }
    }

    /**
     * Update the admin permissions for a given user.
     *
     * Only superusers may modify another admin's permissions.
     */
    fun updatePermissions(userId: String, permissions: Set<AdminPermission>) {
        if (!requireOnline()) return

        if (!_uiState.value.isSuperuser) {
            _uiState.value = _uiState.value.copy(actionError = UiError.INSUFFICIENT_PERMISSIONS)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.updateAdminPermissions(userId, permissions)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isPerformingAction = false)
                    loadUsers()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = UiError.UPDATE_PERMISSIONS_FAILED
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

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Apply role filter, text search, and sort to the accumulated user list.
     *
     * @param users The full (unfiltered) accumulated user list.
     * @param query Search text to match against name, email, or username.
     * @param roleFilter Active [UserRoleFilter].
     * @param sortField Active [UserSortField].
     * @param sortAscending Whether to sort ascending (`true`) or descending.
     * @return The filtered and sorted list ready for display.
     */
    private fun filterAndSortUsers(
        users: List<User>,
        query: String,
        roleFilter: UserRoleFilter,
        sortField: UserSortField,
        sortAscending: Boolean
    ): List<User> {
        var filtered = users

        // Role filter
        filtered = when (roleFilter) {
            UserRoleFilter.ALL -> filtered
            UserRoleFilter.SUPERUSER -> filtered.filter { it.role == UserRole.SUPERUSER }
            UserRoleFilter.ADMIN -> filtered.filter { it.role == UserRole.ADMIN }
            UserRoleFilter.USER -> filtered.filter { it.role == UserRole.USER && !it.isBanned }
            UserRoleFilter.BANNED -> filtered.filter { it.isBanned }
        }

        // Search query
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter { user ->
                user.displayName.lowercase().contains(lowerQuery) ||
                        user.email.lowercase().contains(lowerQuery) ||
                        user.username.lowercase().contains(lowerQuery)
            }
        }

        // Sort
        filtered = when (sortField) {
            UserSortField.NAME -> {
                if (sortAscending) filtered.sortedBy { it.displayName.lowercase() }
                else filtered.sortedByDescending { it.displayName.lowercase() }
            }

            UserSortField.EMAIL -> {
                if (sortAscending) filtered.sortedBy { it.email.lowercase() }
                else filtered.sortedByDescending { it.email.lowercase() }
            }

            UserSortField.ROLE -> {
                if (sortAscending) filtered.sortedBy { it.role.ordinal }
                else filtered.sortedByDescending { it.role.ordinal }
            }

            UserSortField.DATE -> filtered // Firestore already sorts by date
        }

        return filtered
    }
}
