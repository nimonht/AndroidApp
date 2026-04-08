package com.example.androidapp.ui.screens.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AdminRepository
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
 * @param adminRepository Repository for admin operations.
 * @param networkMonitor Monitor for observing network connectivity state.
 */
class AdminUserManagementViewModel(
    private val adminRepository: AdminRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUserManagementUiState())
    val uiState: StateFlow<AdminUserManagementUiState> = _uiState.asStateFlow()

    private var allUsers: List<User> = emptyList()

    private companion object {
        /** Number of users to fetch per page from Firestore. */
        const val PAGE_SIZE = 30
    }

    init {
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    users = filterUsers(allUsers, _uiState.value.searchQuery),
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
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    users = filterUsers(allUsers, _uiState.value.searchQuery),
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

    /**
     * Update search query and filter users.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            users = filterUsers(allUsers, query)
        )
    }

    /**
     * Update a user's role.
     */
    fun updateUserRole(userId: String, newRole: UserRole) {
        if (!requireOnline()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.updateUserRole(userId, newRole)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                    // Refresh to get updated data
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
     */
    fun banUser(userId: String) {
        if (!requireOnline()) return
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
     */
    fun unbanUser(userId: String) {
        if (!requireOnline()) return
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
     */
    fun deleteUser(userId: String) {
        if (!requireOnline()) return
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
     * Clear action error.
     */
    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }

    /**
     * Filter users based on search query.
     */
    private fun filterUsers(users: List<User>, query: String): List<User> {
        if (query.isBlank()) return users

        val lowerQuery = query.lowercase()
        return users.filter { user ->
            user.displayName.lowercase().contains(lowerQuery) ||
                    user.email.lowercase().contains(lowerQuery)
        }
    }
}
