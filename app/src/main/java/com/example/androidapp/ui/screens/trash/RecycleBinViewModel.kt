package com.example.androidapp.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Recycle Bin (Trash) screen.
 */
data class RecycleBinUiState(
    val deletedQuizzes: List<Quiz> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

/** Events that can be dispatched to [RecycleBinViewModel]. */
sealed class RecycleBinEvent {
    data class RestoreQuiz(val quizId: String) : RecycleBinEvent()
    data class DeletePermanently(val quizId: String) : RecycleBinEvent()
    data object EmptyTrash : RecycleBinEvent()
    data object ClearMessage : RecycleBinEvent()
    data object ClearError : RecycleBinEvent()
    data object LoadMore : RecycleBinEvent()
}

/**
 * ViewModel for the Recycle Bin (Trash) screen.
 * Loads soft-deleted quizzes with pagination and supports restore and permanent delete actions.
 *
 * Uses a dynamic LIMIT Room query that increases when the user scrolls near
 * the bottom. Room re-emits the full list up to the new limit whenever data changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecycleBinViewModel(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecycleBinUiState())

    /** Current UI state for the Trash screen. */
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    /** Dynamic limit for paginated loading. */
    private val _trashLimit = MutableStateFlow(INITIAL_PAGE_SIZE)

    /** Cached user ID to avoid repeated auth lookups. */
    private var cachedUserId: String? = null

    private companion object {
        /** Initial number of deleted quizzes to load. */
        const val INITIAL_PAGE_SIZE = 20

        /** Number of additional quizzes to load on each "load more". */
        const val PAGE_SIZE = 20
    }

    init {
        loadDeletedQuizzes()
    }

    /**
     * Dispatches a [RecycleBinEvent] to the ViewModel.
     */
    fun onEvent(event: RecycleBinEvent) {
        when (event) {
            is RecycleBinEvent.RestoreQuiz -> onRestoreQuiz(event.quizId)
            is RecycleBinEvent.DeletePermanently -> onDeletePermanently(event.quizId)
            is RecycleBinEvent.EmptyTrash -> onEmptyTrash()
            is RecycleBinEvent.ClearMessage -> _uiState.update { it.copy(successMessage = null) }
            is RecycleBinEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is RecycleBinEvent.LoadMore -> handleLoadMore()
        }
    }

    /**
     * Increases the trash limit to load more deleted quizzes.
     */
    private fun handleLoadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoadingMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        _trashLimit.value += PAGE_SIZE
    }

    /**
     * Loads deleted quizzes using a dynamic LIMIT query.
     * Uses [flatMapLatest] on [_trashLimit] so that increasing the limit
     * triggers a new Room query automatically.
     */
    private fun loadDeletedQuizzes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            cachedUserId = user.id

            _trashLimit.flatMapLatest { limit ->
                quizRepository.getDeletedQuizzesLimited(user.id, limit)
            }.collectLatest { quizzes ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        deletedQuizzes = quizzes,
                        hasMore = quizzes.size >= _trashLimit.value
                    )
                }
            }
        }
    }

    /**
     * Restores a soft-deleted quiz from the recycle bin.
     *
     * @param quizId The ID of the quiz to restore.
     */
    private fun onRestoreQuiz(quizId: String) {
        viewModelScope.launch {
            val result = quizRepository.restoreQuiz(quizId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Da khoi phuc bai kiem tra") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    /**
     * Permanently deletes a quiz from both Room and Firestore.
     *
     * @param quizId The ID of the quiz to permanently delete.
     */
    private fun onDeletePermanently(quizId: String) {
        viewModelScope.launch {
            val result = quizRepository.permanentlyDeleteQuiz(quizId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Da xoa vinh vien") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    /**
     * Permanently deletes all soft-deleted quizzes for the current user.
     */
    private fun onEmptyTrash() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = cachedUserId ?: authRepository.getCurrentUser()?.id
            if (userId == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Nguoi dung chua dang nhap")
                }
                return@launch
            }
            val result = quizRepository.emptyTrash(userId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "Da don sach thung rac")
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }
}
