package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import com.example.androidapp.ui.common.UiError
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
 * @property error Current [UiError] code to display, or null when there is no error.
 * @property errorDetail Optional detail string for parameterised errors or raw validation messages.
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
    val error: UiError? = null,
    val errorDetail: String? = null
)

/**
 * Events for the Edit Quiz screen.
 *
 * The edit flow uses exactly the same set of form events as the create flow,
 * so this is a direct typealias for [QuizFormEvent]. If edit-specific events
 * are ever needed, replace this typealias with a dedicated sealed interface.
 */
typealias EditQuizEvent = QuizFormEvent

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
     * Dispatches a [QuizFormEvent] (aliased as [EditQuizEvent]) to update state
     * or trigger a side effect.
     */
    fun onEvent(event: QuizFormEvent) {
        when (event) {
            is QuizFormEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.title) }

            is QuizFormEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.description) }

            is QuizFormEvent.ThumbnailUrlChanged ->
                _uiState.update { it.copy(thumbnailUrl = event.thumbnailUrl) }

            is QuizFormEvent.IsPublicChanged ->
                _uiState.update { it.copy(isPublic = event.isPublic) }

            is QuizFormEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.tags) }

            is QuizFormEvent.AddQuestion ->
                _uiState.update {
                    it.copy(questions = QuizFormHelper.addQuestion(it.questions))
                }

            is QuizFormEvent.UpdateQuestion ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.updateQuestion(
                            it.questions, event.index, event.draft
                        )
                    )
                }

            is QuizFormEvent.RemoveQuestion ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.removeQuestion(it.questions, event.index)
                    )
                }

            is QuizFormEvent.MoveQuestionUp ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.moveQuestionUp(it.questions, event.index)
                    )
                }

            is QuizFormEvent.MoveQuestionDown ->
                _uiState.update {
                    it.copy(
                        questions = QuizFormHelper.moveQuestionDown(it.questions, event.index)
                    )
                }

            is QuizFormEvent.SaveDraft ->
                onSaveQuiz(publishAfterSave = false)

            is QuizFormEvent.PublishQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is QuizFormEvent.SaveQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is QuizFormEvent.ShareToPoolChanged ->
                _uiState.update { it.copy(shareToPool = event.shareToPool) }

            is QuizFormEvent.ClearError ->
                _uiState.update { it.copy(error = null, errorDetail = null) }
        }
    }

    private fun loadExistingQuiz() {
        viewModelScope.launch {
            val quiz = quizRepository.getQuizById(quizId)
            if (quiz == null) {
                _uiState.update { it.copy(isLoading = false, error = UiError.QUIZ_NOT_FOUND) }
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
                    isDraft = quiz.isDraft,
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
                _uiState.update { it.copy(errorDetail = validationError) }
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
                isDraft = !publishAfterSave,
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
                        it.copy(error = UiError.SAVE_QUIZ_FAILED, errorDetail = e.message)
                    }
                }
            )
        }
    }
}
