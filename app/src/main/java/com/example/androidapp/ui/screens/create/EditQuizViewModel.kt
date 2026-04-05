package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Edit Quiz screen.
 *
 * A quiz can be in one of three states after saving:
 * - **Draft** ([isDraft] = true, [isPublic] = false): saved privately, editable.
 * - **Private** ([isDraft] = false, [isPublic] = false): saved but not publicly listed, editable.
 * - **Public** ([isDraft] = false, [isPublic] = true): published and visible to everyone.
 *
 * @property quizId The ID of the quiz being edited.
 * @property title The quiz title.
 * @property description The quiz description.
 * @property thumbnailUrl Optional URL for the quiz cover image.
 * @property isPublic Whether the quiz is publicly discoverable.
 * @property tags Comma-separated list of tags as raw input text.
 * @property questions The ordered list of question drafts.
 * @property isLoading Whether a save/publish operation is in progress or the quiz is being loaded.
 * @property isSaved Whether the quiz has been successfully saved (triggers navigation).
 * @property isDraft Whether the current version is saved only as a draft (not published).
 * @property isPublished Whether the quiz has been successfully published.
 * @property lastSavedAt Epoch millis of the last draft save, or null if never saved in this session.
 * @property shareToPool Whether to contribute each question to the community pool after publishing.
 * @property error Current error message to display, or null when there is no error.
 */
data class EditQuizUiState(
    val quizId: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val isPublic: Boolean = false,
    val tags: String = "",
    val availableTags: List<String> = emptyList(),
    val questions: List<QuestionDraft> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDraft: Boolean = true,
    val isPublished: Boolean = false,
    val lastSavedAt: Long? = null,
    val shareToPool: Boolean = false,
    val error: String? = null
)

/**
 * Events that can be dispatched to [EditQuizViewModel].
 */
sealed class EditQuizEvent {
    /** Updates the quiz title. */
    data class TitleChanged(val title: String) : EditQuizEvent()

    /** Updates the quiz description. */
    data class DescriptionChanged(val description: String) : EditQuizEvent()

    /** Updates the quiz cover image URL. */
    data class ThumbnailUrlChanged(val thumbnailUrl: String) : EditQuizEvent()

    /** Toggles the public visibility of the quiz. */
    data class IsPublicChanged(val isPublic: Boolean) : EditQuizEvent()

    /** Updates the raw comma-separated tags string. */
    data class TagsChanged(val tags: String) : EditQuizEvent()

    /** Appends a blank question to the end of the question list. */
    data object AddQuestion : EditQuizEvent()

    /** Replaces the question at [index] with [draft]. */
    data class UpdateQuestion(val index: Int, val draft: QuestionDraft) : EditQuizEvent()

    /** Removes the question at [index] if more than one question exists. */
    data class RemoveQuestion(val index: Int) : EditQuizEvent()

    /** Moves the question at [index] one position up in the list. */
    data class MoveQuestionUp(val index: Int) : EditQuizEvent()

    /** Moves the question at [index] one position down in the list. */
    data class MoveQuestionDown(val index: Int) : EditQuizEvent()

    /**
     * Saves the current form as a draft without publishing.
     * Sets [EditQuizUiState.isDraft] to true and records [EditQuizUiState.lastSavedAt].
     * The quiz is saved with whatever [EditQuizUiState.isPublic] the user has set.
     */
    data object SaveDraft : EditQuizEvent()

    /**
     * Validates the quiz and saves it as a published quiz.
     * Sets [EditQuizUiState.isPublished] to true and triggers [EditQuizUiState.isSaved].
     */
    data object PublishQuiz : EditQuizEvent()

    /** Legacy save alias -- behaves identically to [PublishQuiz]. */
    data object SaveQuiz : EditQuizEvent()

    /** Toggles whether each question will be contributed to the community pool after publishing. */
    data class ShareToPoolChanged(val shareToPool: Boolean) : EditQuizEvent()

    /** Clears the current error message from the UI state. */
    data object ClearError : EditQuizEvent()
}

/**
 * ViewModel for the Edit Quiz screen.
 * Pre-populates the form from the repository and saves changes back.
 * Supports draft saving without publishing and explicit publish action.
 *
 * Question CRUD, validation, and domain-mapping logic is delegated to [QuizFormHelper]
 * to avoid duplication with [CreateQuizViewModel].
 *
 * @param quizId The ID of the quiz to load and edit.
 * @param quizRepository Repository for persisting quizzes and questions.
 * @param authRepository Repository for retrieving the currently authenticated user.
 * @param poolRepository Repository for contributing questions to the community pool.
 * @param shareCodeRepository Repository for generating share codes.
 */
class EditQuizViewModel(
    private val quizId: String,
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository,
    private val poolRepository: PoolRepository,
    private val shareCodeRepository: ShareCodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditQuizUiState(quizId = quizId, isLoading = true))

    /** Current UI state for the Edit Quiz screen. */
    val uiState: StateFlow<EditQuizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val tags = quizRepository.getAllTags()
                _uiState.update { it.copy(availableTags = tags) }
            } catch (e: Exception) {
                // Ignore
            }
        }
        loadExistingQuiz()
    }

    /**
     * Dispatches an [EditQuizEvent] to update state or trigger a side effect.
     */
    fun onEvent(event: EditQuizEvent) {
        when (event) {
            is EditQuizEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.title) }

            is EditQuizEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.description) }

            is EditQuizEvent.ThumbnailUrlChanged ->
                _uiState.update { it.copy(thumbnailUrl = event.thumbnailUrl) }

            is EditQuizEvent.IsPublicChanged ->
                _uiState.update { it.copy(isPublic = event.isPublic) }

            is EditQuizEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.tags) }

            is EditQuizEvent.AddQuestion ->
                _uiState.update {
                    it.copy(questions = QuizFormHelper.addQuestion(it.questions))
                }

            is EditQuizEvent.UpdateQuestion ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.updateQuestion(
                            it.questions, event.index, event.draft
                        )
                    )
                }

            is EditQuizEvent.RemoveQuestion ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.removeQuestion(it.questions, event.index)
                    )
                }

            is EditQuizEvent.MoveQuestionUp ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.moveQuestionUp(it.questions, event.index)
                    )
                }

            is EditQuizEvent.MoveQuestionDown ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.moveQuestionDown(it.questions, event.index)
                    )
                }

            is EditQuizEvent.SaveDraft ->
                onSaveQuiz(publishAfterSave = false)

            is EditQuizEvent.PublishQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is EditQuizEvent.SaveQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is EditQuizEvent.ShareToPoolChanged ->
                _uiState.update { it.copy(shareToPool = event.shareToPool) }

            is EditQuizEvent.ClearError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadExistingQuiz() {
        viewModelScope.launch {
            val quiz = quizRepository.getQuizById(quizId)
            if (quiz == null) {
                _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy bài kiểm tra") }
                return@launch
            }
            val questions = quizRepository.getQuestionsForQuizOnce(quizId)
            val drafts = questions.map { question ->
                QuestionDraft(
                    id = question.id,
                    content = question.content,
                    choices = question.choices
                        .sortedBy { it.position }
                        .map { c -> ChoiceDraft(id = c.id, content = c.content) },
                    correctIndices = question.choices
                        .mapIndexedNotNull { idx, c -> if (c.isCorrect) idx else null }
                        .toSet(),
                    isMultiSelect = question.isMultiSelect,
                    explanation = question.explanation ?: "",
                    mediaUrl = question.mediaUrl ?: "",
                    points = question.points
                )
            }.ifEmpty { listOf(QuestionDraft()) }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    title = quiz.title,
                    description = quiz.description ?: "",
                    thumbnailUrl = quiz.thumbnailUrl ?: "",
                    isPublic = quiz.isPublic,
                    isDraft = !quiz.isPublic,
                    tags = quiz.tags.joinToString(", "),
                    questions = drafts
                )
            }
        }
    }

    /**
     * Validates and persists the quiz.
     *
     * When [publishAfterSave] is `true` the quiz is marked as published and
     * [EditQuizUiState.isSaved] is set to trigger back navigation.
     * [EditQuizUiState.isPublic] respects the user's explicit toggle choice,
     * so a published quiz can be either private (share-code only) or public
     * (searchable by anyone). Pool contribution only runs on the publish path.
     *
     * When [publishAfterSave] is `false` the quiz is saved as a draft with
     * [EditQuizUiState.isPublic] forced to `false` (drafts must never be public),
     * and [EditQuizUiState.lastSavedAt] is updated.
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
            val tags = QuizFormHelper.parseTags(state.tags)

            // On publish, respect the user's explicit isPublic toggle choice.
            // On draft save, force isPublic = false -- drafts must never be public.
            val effectiveIsPublic = if (publishAfterSave) state.isPublic else false

            val existingQuiz = quizRepository.getQuizById(quizId)
            var shareCode = existingQuiz?.shareCode

            if (publishAfterSave && shareCode == null) {
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
                questionCount = state.questions.size,
                updatedAt = System.currentTimeMillis()
            )

            val questions = QuizFormHelper.mapQuestionsToEntities(quizId, state.questions)

            val result = quizRepository.updateQuiz(quiz, questions)
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
