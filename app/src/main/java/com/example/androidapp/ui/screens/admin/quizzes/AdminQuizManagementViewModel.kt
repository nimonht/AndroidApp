package com.example.androidapp.ui.screens.admin.quizzes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.ui.common.UiError
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
 * Loads the current admin's permissions on init so the UI can conditionally
 * display destructive actions (delete, publish, restore). Actions that require
 * a missing permission are rejected with [UiError.INSUFFICIENT_PERMISSIONS].
 *
 * @param adminRepository Repository for admin operations.
 * @param authRepository Repository for authentication and current-user queries.
 * @param networkMonitor Monitor for observing network connectivity state.
 */
class AdminQuizManagementViewModel(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository,
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
        // Load permissions first, then gate screen access with MANAGE_QUIZZES.
        viewModelScope.launch {
            loadPermissions()

            // Screen-level gate: MANAGE_QUIZZES required to view the quiz list.
            val state = _uiState.value
            if (!state.isSuperuser &&
                !state.currentPermissions.contains(AdminPermission.MANAGE_QUIZZES)
            ) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = UiError.INSUFFICIENT_PERMISSIONS
                )
                return@launch
            }

            loadQuizzesInternal()
        }

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }
    }

    /**
     * Loads the current admin user's permissions and superuser status into UI state.
     */
    private suspend fun loadPermissions() {
        try {
            val user = authRepository.getCurrentUser()
            val perms = adminRepository.getCurrentAdminPermissions()
            _uiState.value = _uiState.value.copy(
                currentPermissions = perms,
                isSuperuser = user?.isSuperuser() == true
            )
        } catch (_: Exception) {
            // Non-critical: permissions default to empty, actions will be rejected.
        }
    }

    /**
     * Returns `true` if the device is currently online.
     * Sets [AdminQuizManagementUiState.actionError] and returns `false` otherwise.
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
     * Returns `true` if the current user holds [permission] or is a superuser.
     * Sets [UiError.INSUFFICIENT_PERMISSIONS] and returns `false` otherwise.
     */
    private fun requirePermission(permission: AdminPermission): Boolean {
        val state = _uiState.value
        if (state.isSuperuser || state.currentPermissions.contains(permission)) {
            return true
        }
        _uiState.value = state.copy(actionError = UiError.INSUFFICIENT_PERMISSIONS)
        return false
    }

    /**
     * Public reload trigger. Re-checks [AdminPermission.MANAGE_QUIZZES] before
     * fetching, in case the screen is being retried after a permission error.
     */
    fun loadQuizzes() {
        val state = _uiState.value
        if (!state.isSuperuser && !state.currentPermissions.contains(AdminPermission.MANAGE_QUIZZES)) {
            _uiState.value = state.copy(
                isLoading = false,
                error = UiError.INSUFFICIENT_PERMISSIONS
            )
            return
        }
        loadQuizzesInternal()
    }

    /**
     * Load the first page of quizzes from the repository.
     * Resets pagination to the beginning.
     */
    private fun loadQuizzesInternal() {
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
                    quizzes = applyFilters(allQuizzes),
                    hasMore = page.hasMore,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError.LOAD_QUIZ_LIST_FAILED
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
                    quizzes = applyFilters(allQuizzes),
                    hasMore = page.hasMore
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = UiError.LOAD_MORE_QUIZZES_FAILED
                )
            }
        }
    }

    // -- Filter / sort event handlers -------------------------------------------

    /**
     * Update search query and re-filter quizzes.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        refilter()
    }

    /**
     * Update the quiz status filter and re-filter quizzes.
     *
     * @param filter The new [QuizStatusFilter] to apply.
     */
    fun onStatusFilterChanged(filter: QuizStatusFilter) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
        refilter()
    }

    /**
     * Update the tag filter text and re-filter quizzes.
     *
     * @param tag The tag substring to filter by.
     */
    fun onTagFilterChanged(tag: String) {
        _uiState.value = _uiState.value.copy(tagFilter = tag)
        refilter()
    }

    /**
     * Update the sort field and re-sort quizzes.
     *
     * @param field The new [QuizSortField] to sort by.
     */
    fun onSortFieldChanged(field: QuizSortField) {
        _uiState.value = _uiState.value.copy(sortField = field)
        refilter()
    }

    /**
     * Toggle the sort direction (ascending / descending) and re-sort quizzes.
     */
    fun onToggleSortOrder() {
        _uiState.value = _uiState.value.copy(sortAscending = !_uiState.value.sortAscending)
        refilter()
    }

    /**
     * Reset all filters and sort options to their default values.
     */
    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            statusFilter = QuizStatusFilter.ALL,
            tagFilter = "",
            sortField = QuizSortField.DATE,
            sortAscending = false
        )
        refilter()
    }

    // -- Quiz actions -----------------------------------------------------------

    /**
     * Publish or unpublish a quiz.
     *
     * Requires [AdminPermission.PUBLISH_QUIZZES].
     */
    fun togglePublishQuiz(quizId: String, currentlyPublic: Boolean) {
        if (!requireOnline()) return
        if (!requirePermission(AdminPermission.PUBLISH_QUIZZES)) return

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
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = UiError.UPDATE_QUIZ_STATUS_FAILED
                    )
                }
        }
    }

    /**
     * Restore a soft-deleted quiz.
     *
     * Requires [AdminPermission.MANAGE_QUIZZES].
     */
    fun restoreQuiz(quizId: String) {
        if (!requireOnline()) return
        if (!requirePermission(AdminPermission.MANAGE_QUIZZES)) return

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
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = UiError.RESTORE_QUIZ_FAILED
                    )
                }
        }
    }

    /**
     * Delete a quiz permanently.
     *
     * Requires [AdminPermission.DELETE_QUIZZES].
     */
    fun deleteQuiz(quizId: String) {
        if (!requireOnline()) return
        if (!requirePermission(AdminPermission.DELETE_QUIZZES)) return

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
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPerformingAction = false,
                        actionError = UiError.ADMIN_DELETE_QUIZ_FAILED
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

    // -- Internal helpers -------------------------------------------------------

    /**
     * Convenience wrapper that reads filter/sort params from the current UI state.
     */
    private fun applyFilters(quizzes: List<Quiz>): List<Quiz> {
        val state = _uiState.value
        return filterAndSortQuizzes(
            quizzes = quizzes,
            query = state.searchQuery,
            statusFilter = state.statusFilter,
            tagFilter = state.tagFilter,
            sortField = state.sortField,
            sortAscending = state.sortAscending
        )
    }

    /**
     * Re-applies the current filters and sort to [allQuizzes] and updates UI state.
     */
    private fun refilter() {
        _uiState.value = _uiState.value.copy(quizzes = applyFilters(allQuizzes))
    }

    /**
     * Filter and sort quizzes based on all active filter criteria.
     *
     * @param quizzes The full accumulated list of quizzes from Firestore.
     * @param query Free-text search query matched against title, author name, and tags.
     * @param statusFilter Restricts quizzes to a specific publication status.
     * @param tagFilter Substring matched against quiz tags.
     * @param sortField The field to sort by.
     * @param sortAscending `true` for ascending order, `false` for descending.
     * @return The filtered and sorted list.
     */
    private fun filterAndSortQuizzes(
        quizzes: List<Quiz>,
        query: String,
        statusFilter: QuizStatusFilter,
        tagFilter: String,
        sortField: QuizSortField,
        sortAscending: Boolean
    ): List<Quiz> {
        var filtered = quizzes

        // Status filter
        filtered = when (statusFilter) {
            QuizStatusFilter.ALL -> filtered
            QuizStatusFilter.PUBLIC -> filtered.filter { it.isPublic && it.deletedAt == null && !it.isDraft }
            QuizStatusFilter.PRIVATE -> filtered.filter { !it.isPublic && it.deletedAt == null && !it.isDraft }
            QuizStatusFilter.DRAFT -> filtered.filter { it.isDraft && it.deletedAt == null }
            QuizStatusFilter.DELETED -> filtered.filter { it.deletedAt != null }
        }

        // Tag filter
        if (tagFilter.isNotBlank()) {
            val lowerTag = tagFilter.lowercase()
            filtered = filtered.filter { quiz ->
                quiz.tags.any { it.lowercase().contains(lowerTag) }
            }
        }

        // Search query
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter { quiz ->
                quiz.title.lowercase().contains(lowerQuery) ||
                        quiz.authorName.lowercase().contains(lowerQuery) ||
                        quiz.tags.any { it.lowercase().contains(lowerQuery) }
            }
        }

        // Sort
        filtered = when (sortField) {
            QuizSortField.DATE -> {
                if (sortAscending) filtered.sortedBy { it.createdAt }
                else filtered.sortedByDescending { it.createdAt }
            }

            QuizSortField.NAME -> {
                if (sortAscending) filtered.sortedBy { it.title.lowercase() }
                else filtered.sortedByDescending { it.title.lowercase() }
            }

            QuizSortField.ATTEMPTS -> {
                if (sortAscending) filtered.sortedBy { it.attemptCount }
                else filtered.sortedByDescending { it.attemptCount }
            }

            QuizSortField.QUESTIONS -> {
                if (sortAscending) filtered.sortedBy { it.questionCount }
                else filtered.sortedByDescending { it.questionCount }
            }
        }

        return filtered
    }
}
