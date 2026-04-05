package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.QuestionPoolItem
import android.util.Log
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * UI state for the Create Quiz screen.
 *
 * A quiz can be in one of three states after saving:
 * - **Draft** ([isDraft] = true, [isPublic] = false): saved privately, editable.
 * - **Private** ([isDraft] = false, [isPublic] = false): saved but not publicly listed, editable.
 * - **Public** ([isDraft] = false, [isPublic] = true): published and visible to everyone.
 *
 * @property title The quiz title.
 * @property description The quiz description.
 * @property thumbnailUrl Optional URL for the quiz cover image.
 * @property isPublic Whether the quiz is publicly discoverable.
 * @property tags Comma-separated list of tags as raw input text.
 * @property questions The ordered list of question drafts.
 * @property isLoading Whether a save/publish operation is in progress.
 * @property isSaved Whether the quiz has been successfully saved (triggers navigation).
 * @property isDraft Whether the current version is saved only as a draft (not published).
 * @property isPublished Whether the quiz has been successfully published.
 * @property lastSavedAt Epoch millis of the last draft save, or null if never saved.
 * @property shareToPool Whether to contribute each question to the community pool after publishing.
 * @property error Current error message to display, or null when there is no error.
 */
data class CreateQuizUiState(
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val isPublic: Boolean = false,
    val tags: String = "",
    val availableTags: List<String> = emptyList(),
    val questions: List<QuestionDraft> = listOf(QuestionDraft()),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDraft: Boolean = true,
    val isPublished: Boolean = false,
    val lastSavedAt: Long? = null,
    val shareToPool: Boolean = false,
    val error: String? = null,
    val showPoolDialog: Boolean = false,
    val poolSearchTags: String = "",
    val poolResults: List<QuestionPoolItem> = emptyList(),
    val selectedPoolItemIds: Set<String> = emptySet(),
    val isPoolLoading: Boolean = false,
    val poolError: String? = null
)

/**
 * Events that can be dispatched to [CreateQuizViewModel].
 */
sealed class CreateQuizEvent {
    /** Updates the quiz title. */
    data class TitleChanged(val title: String) : CreateQuizEvent()

    /** Updates the quiz description. */
    data class DescriptionChanged(val description: String) : CreateQuizEvent()

    /** Updates the quiz cover image URL. */
    data class ThumbnailUrlChanged(val thumbnailUrl: String) : CreateQuizEvent()

    /** Toggles the public visibility of the quiz. */
    data class IsPublicChanged(val isPublic: Boolean) : CreateQuizEvent()

    /** Updates the raw comma-separated tags string. */
    data class TagsChanged(val tags: String) : CreateQuizEvent()

    /** Appends a blank question to the end of the question list. */
    data object AddQuestion : CreateQuizEvent()

    /** Replaces the question at [index] with [draft]. */
    data class UpdateQuestion(val index: Int, val draft: QuestionDraft) : CreateQuizEvent()

    /** Removes the question at [index] if more than one question exists. */
    data class RemoveQuestion(val index: Int) : CreateQuizEvent()

    /** Moves the question at [index] one position up in the list. */
    data class MoveQuestionUp(val index: Int) : CreateQuizEvent()

    /** Moves the question at [index] one position down in the list. */
    data class MoveQuestionDown(val index: Int) : CreateQuizEvent()

    /**
     * Saves the current form as a draft without publishing.
     * Sets [CreateQuizUiState.isDraft] to true and records [CreateQuizUiState.lastSavedAt].
     * The quiz is saved with whatever [CreateQuizUiState.isPublic] the user has set.
     */
    data object SaveDraft : CreateQuizEvent()

    /**
     * Validates the quiz and saves it as a published quiz.
     * Sets [CreateQuizUiState.isPublished] to true and triggers [CreateQuizUiState.isSaved].
     */
    data object PublishQuiz : CreateQuizEvent()

    /** Legacy save alias -- behaves identically to [PublishQuiz]. */
    data object SaveQuiz : CreateQuizEvent()

    /** Toggles whether each question will be contributed to the community pool after publishing. */
    data class ShareToPoolChanged(val shareToPool: Boolean) : CreateQuizEvent()

    /** Clears the current error message from the UI state. */
    data object ClearError : CreateQuizEvent()

    /** Imports a list of questions, appending them to the current list or replacing the initial blank question. */
    data class ImportQuestions(val questions: List<QuestionDraft>) : CreateQuizEvent()

    /** Opens the pool import dialog. */
    data object ShowPoolDialog : CreateQuizEvent()

    /** Dismisses the pool import dialog and resets pool-related state. */
    data object DismissPoolDialog : CreateQuizEvent()

    /** Updates the comma-separated tag string used to search the community pool. */
    data class PoolSearchTagsChanged(val tags: String) : CreateQuizEvent()

    /** Triggers a search of the community pool using [CreateQuizUiState.poolSearchTags]. */
    data object SearchPool : CreateQuizEvent()

    /** Toggles the selection state of a pool item by its [poolItemId]. */
    data class TogglePoolItemSelection(val poolItemId: String) : CreateQuizEvent()

    /** Imports the currently selected pool items as [QuestionDraft] entries and closes the dialog. */
    data object ImportFromPool : CreateQuizEvent()
}

/**
 * ViewModel for the Create Quiz screen.
 * Owns the multi-step form state and coordinates draft saving and publishing via the repository.
 *
 * Question CRUD, validation, and domain-mapping logic is delegated to [QuizFormHelper]
 * to avoid duplication with [EditQuizViewModel].
 *
 * @param quizRepository Repository for persisting quizzes and questions.
 * @param authRepository Repository for retrieving the currently authenticated user.
 * @param poolRepository Repository for contributing questions to the community pool.
 * @param shareCodeRepository Repository for generating share codes.
 */
class CreateQuizViewModel(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository,
    private val poolRepository: PoolRepository,
    private val shareCodeRepository: ShareCodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())

    /** Current UI state for the Create Quiz screen. */
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val tags = quizRepository.getAllTags()
                _uiState.update { it.copy(availableTags = tags) }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Dispatches a [CreateQuizEvent] to update state or trigger a side effect.
     */
    fun onEvent(event: CreateQuizEvent) {
        when (event) {
            is CreateQuizEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.title) }

            is CreateQuizEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.description) }

            is CreateQuizEvent.ThumbnailUrlChanged ->
                _uiState.update { it.copy(thumbnailUrl = event.thumbnailUrl) }

            is CreateQuizEvent.IsPublicChanged ->
                _uiState.update { it.copy(isPublic = event.isPublic) }

            is CreateQuizEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.tags) }

            is CreateQuizEvent.AddQuestion ->
                _uiState.update {
                    it.copy(questions = QuizFormHelper.addQuestion(it.questions))
                }

            is CreateQuizEvent.UpdateQuestion ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.updateQuestion(
                            it.questions, event.index, event.draft
                        )
                    )
                }

            is CreateQuizEvent.RemoveQuestion ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.removeQuestion(it.questions, event.index)
                    )
                }

            is CreateQuizEvent.MoveQuestionUp ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.moveQuestionUp(it.questions, event.index)
                    )
                }

            is CreateQuizEvent.MoveQuestionDown ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.moveQuestionDown(it.questions, event.index)
                    )
                }

            is CreateQuizEvent.SaveDraft ->
                onSaveQuiz(publishAfterSave = false)

            is CreateQuizEvent.PublishQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is CreateQuizEvent.SaveQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is CreateQuizEvent.ShareToPoolChanged ->
                _uiState.update { it.copy(shareToPool = event.shareToPool) }

            is CreateQuizEvent.ClearError ->
                _uiState.update { it.copy(error = null) }

            is CreateQuizEvent.ImportQuestions ->
                _uiState.update { state ->
                    state.copy(
                        questions = QuizFormHelper.mergeImportedQuestions(
                            state.questions, event.questions
                        )
                    )
                }

            is CreateQuizEvent.ShowPoolDialog ->
                _uiState.update { it.copy(showPoolDialog = true) }

            is CreateQuizEvent.DismissPoolDialog ->
                _uiState.update {
                    it.copy(
                        showPoolDialog = false,
                        poolSearchTags = "",
                        poolResults = emptyList(),
                        selectedPoolItemIds = emptySet(),
                        isPoolLoading = false,
                        poolError = null
                    )
                }

            is CreateQuizEvent.PoolSearchTagsChanged ->
                _uiState.update { it.copy(poolSearchTags = event.tags) }

            is CreateQuizEvent.SearchPool -> onSearchPool()

            is CreateQuizEvent.TogglePoolItemSelection ->
                _uiState.update { state ->
                    val updated = if (event.poolItemId in state.selectedPoolItemIds) {
                        state.selectedPoolItemIds - event.poolItemId
                    } else {
                        state.selectedPoolItemIds + event.poolItemId
                    }
                    state.copy(selectedPoolItemIds = updated)
                }

            is CreateQuizEvent.ImportFromPool -> onImportFromPool()
        }
    }

    /**
     * Searches the community pool using the tags entered in [CreateQuizUiState.poolSearchTags].
     *
     * Tags are parsed as a comma-separated string, trimmed, and passed to
     * [PoolRepository.getPoolQuestionsByTags]. Results are stored in
     * [CreateQuizUiState.poolResults].
     */
    private fun onSearchPool() {
        viewModelScope.launch {
            val tags = QuizFormHelper.parseTags(_uiState.value.poolSearchTags)

            if (tags.isEmpty()) {
                _uiState.update { it.copy(poolError = "Vui lòng nhập từ khóa tìm kiếm") }
                return@launch
            }

            _uiState.update { it.copy(isPoolLoading = true, poolError = null) }

            poolRepository.getPoolQuestionsByTags(tags, activeOnly = true)
                .fold(
                    onSuccess = { items ->
                        _uiState.update {
                            it.copy(
                                poolResults = items,
                                isPoolLoading = false,
                                selectedPoolItemIds = emptySet()
                            )
                        }
                    },
                    onFailure = { e ->
                        Log.e("CreateQuizVM", "Pool search failed", e)
                        _uiState.update {
                            it.copy(
                                isPoolLoading = false,
                                poolError = e.message ?: "Không thể tìm kiếm kho câu hỏi"
                            )
                        }
                    }
                )
        }
    }

    /**
     * Maps the selected [QuestionPoolItem]s to [QuestionDraft] entries, dispatches
     * [CreateQuizEvent.ImportQuestions], increments usage counts, and closes the dialog.
     */
    private fun onImportFromPool() {
        val state = _uiState.value
        val selectedItems = state.poolResults.filter { it.id in state.selectedPoolItemIds }
        if (selectedItems.isEmpty()) return

        val drafts = QuizFormHelper.mapPoolItemsToDrafts(selectedItems)

        // Append imported questions via the existing ImportQuestions handler.
        onEvent(CreateQuizEvent.ImportQuestions(drafts))

        // Increment usage counts in the background.
        viewModelScope.launch {
            for (item in selectedItems) {
                poolRepository.incrementUsageCount(item.id)
            }
        }

        // Close the dialog and reset pool state.
        onEvent(CreateQuizEvent.DismissPoolDialog)
    }

    /**
     * Validates and persists the quiz.
     *
     * When [publishAfterSave] is `true` the quiz is marked as published and
     * [CreateQuizUiState.isSaved] is set to `true` to trigger back navigation.
     * The [CreateQuizUiState.isPublic] toggle is respected so the user can
     * publish a quiz as either **private** (share-code only) or **public**
     * (searchable by anyone). Pool contribution only runs on the publish path.
     *
     * When [publishAfterSave] is `false` the quiz is saved as a draft --
     * [CreateQuizUiState.isPublic] is forced to `false` (drafts must never be
     * public) and [CreateQuizUiState.lastSavedAt] is updated.
     *
     * @param publishAfterSave `true` to publish, `false` to save as draft.
     */
    private fun onSaveQuiz(publishAfterSave: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value

            val validationError = QuizFormHelper.validateQuizForm(state.title, state.questions)
            if (validationError != null) {
                _uiState.update { it.copy(error = validationError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val user = authRepository.getCurrentUser()
            val quizId = UUID.randomUUID().toString()
            val tags = QuizFormHelper.parseTags(state.tags)

            // On publish, respect the user's explicit isPublic toggle choice.
            // On draft save, force isPublic = false -- drafts must never be public.
            val effectiveIsPublic = if (publishAfterSave) state.isPublic else false

            var shareCode: String? = null
            if (publishAfterSave) {
                shareCodeRepository.generateShareCode(quizId).onSuccess { code ->
                    shareCode = code
                }
            }

            val quiz = QuizFormHelper.buildQuizFromForm(
                quizId = quizId,
                ownerId = user?.id ?: "",
                title = state.title,
                description = state.description,
                thumbnailUrl = state.thumbnailUrl,
                authorName = user?.displayName ?: "",
                tags = tags,
                isPublic = effectiveIsPublic,
                shareCode = shareCode,
                questionCount = state.questions.size
            )

            val questions = QuizFormHelper.mapQuestionsToEntities(quizId, state.questions)

            val result = quizRepository.saveQuiz(quiz, questions)
            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = {
                    if (publishAfterSave) {
                        // Contribute each question to the community pool if opted in.
                        // Pool contribution only runs on publish, never on draft saves.
                        if (state.shareToPool) {
                            val contributions = QuizFormHelper.buildPoolContributions(
                                questions = questions,
                                contributorId = user?.id,
                                sourceQuizId = quizId,
                                tags = tags
                            )
                            contributions.forEach { poolItem ->
                                poolRepository.contributeQuestion(poolItem)
                            }
                        }
                        _uiState.update {
                            it.copy(
                                isSaved = true,
                                isPublished = true,
                                isDraft = false,
                                isPublic = effectiveIsPublic
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isDraft = true,
                                isPublished = false,
                                lastSavedAt = System.currentTimeMillis()
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Không thể lưu bài kiểm tra")
                    }
                }
            )
        }
    }
}
