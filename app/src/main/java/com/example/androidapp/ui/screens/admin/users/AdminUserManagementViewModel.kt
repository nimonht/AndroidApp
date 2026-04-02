package com.example.androidapp.ui.screens.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the admin user management screen.
 *
 * @param adminRepository Repository for admin operations.
 */
class AdminUserManagementViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUserManagementUiState())
    val uiState: StateFlow<AdminUserManagementUiState> = _uiState.asStateFlow()

    private var allUsers: List<User> = emptyList()

    init {
        loadUsers()
    }

    /**
     * Load all users from the repository.
     */
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getAllUsers()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Không thể tải danh sách người dùng"
                    )
                }
                .collect { users ->
                    allUsers = users
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        users = filterUsers(users, _uiState.value.searchQuery),
                        error = null
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.updateUserRole(userId, newRole)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Không thể cập nhật vai trò"
                    )
                }
        }
    }

    /**
     * Ban a user.
     */
    fun banUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.banUser(userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Không thể cấm người dùng"
                    )
                }
        }
    }

    /**
     * Unban a user.
     */
    fun unbanUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.unbanUser(userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Không thể bỏ cấm người dùng"
                    )
                }
        }
    }

    /**
     * Delete a user permanently.
     */
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPerformingAction = true, actionError = null)

            adminRepository.deleteUserPermanently(userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = error.message ?: "Không thể xóa người dùng"
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
