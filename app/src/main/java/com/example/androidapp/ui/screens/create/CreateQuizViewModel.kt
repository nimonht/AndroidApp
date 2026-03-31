package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import com.example.androidapp.domain.util.QuizValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Draft model for creating/editing a question.
 * Kept in the ViewModel layer, not in the domain.
 *
 * @property id Unique identifier for this draft, used to track identity across recompositions.
 * @property content The question text.
 * @property choices The list of answer choices (min 2, max 10).
 * @property correctIndices Set of indices that mark the correct choice(s).
 * @property isMultiSelect Whether the question allows multiple correct answers.
 * @property explanation Optional explanation shown after answering.
 * @property mediaUrl Optional URL for an image or video attached to the question.
 * @property points The point value awarded for a correct answer (1–10).
 */
data class QuestionDraft(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val choices: List<ChoiceDraft> = List(4) { ChoiceDraft() },
    val correctIndices: Set<Int> = setOf(0),
    val isMultiSelect: Boolean = false,
    val explanation: String = "",
    val mediaUrl: String = "",
    val points: Int = 1
)

/**
 * Draft model for a choice within a [QuestionDraft].
 *
 * @property id Unique identifier for this choice draft.
 * @property content The display text for this choice.
 */
data class ChoiceDraft(
    val id: String = UUID.randomUUID().toString(),
    val content: String = ""
)

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
    val error: String? = null
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

    /** Legacy save alias — behaves identically to [PublishQuiz]. */
    data object SaveQuiz : CreateQuizEvent()

    /** Toggles whether each question will be contributed to the community pool after publishing. */
    data class ShareToPoolChanged(val shareToPool: Boolean) : CreateQuizEvent()

    /** Clears the current error message from the UI state. */
    data object ClearError : CreateQuizEvent()

    /** Imports a list of questions, appending them to the current list or replacing the initial blank question. */
    data class ImportQuestions(val questions: List<QuestionDraft>) : CreateQuizEvent()
}

/**
 * ViewModel for the Create Quiz screen.
 * Owns the multi-step form state and coordinates draft saving and publishing via the repository.
 *
 * @param quizRepository Repository for persisting quizzes and questions.
 * @param authRepository Repository for retrieving the currently authenticated user.
 * @param poolRepository Repository for contributing questions to the community pool.
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
                _uiState.update { it.copy(questions = it.questions + QuestionDraft()) }

            is CreateQuizEvent.UpdateQuestion ->
                _uiState.update { state ->
                    state.copy(questions = state.questions.toMutableList().apply {
                        this[event.index] = event.draft
                    })
                }

            is CreateQuizEvent.RemoveQuestion ->
                _uiState.update { state ->
                    if (state.questions.size > 1) {
                        state.copy(
                            questions = state.questions.toMutableList().apply {
                                removeAt(event.index)
                            }
                        )
                    } else state
                }

            is CreateQuizEvent.MoveQuestionUp ->
                _uiState.update { state ->
                    val idx = event.index
                    if (idx <= 0 || idx >= state.questions.size) return@update state
                    val list = state.questions.toMutableList()
                    val temp = list[idx - 1]
                    list[idx - 1] = list[idx]
                    list[idx] = temp
                    state.copy(questions = list)
                }

            is CreateQuizEvent.MoveQuestionDown ->
                _uiState.update { state ->
                    val idx = event.index
                    if (idx < 0 || idx >= state.questions.size - 1) return@update state
                    val list = state.questions.toMutableList()
                    val temp = list[idx + 1]
                    list[idx + 1] = list[idx]
                    list[idx] = temp
                    state.copy(questions = list)
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
                    val current = state.questions
                    val merged = if (current.size == 1 && current.first().content.isBlank()) {
                        event.questions
                    } else {
                        current + event.questions
                    }
                    state.copy(questions = merged)
                }
        }
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
     * When [publishAfterSave] is `false` the quiz is saved as a draft —
     * [CreateQuizUiState.isPublic] is forced to `false` (drafts must never be
     * public) and [CreateQuizUiState.lastSavedAt] is updated.
     *
     * @param publishAfterSave `true` to publish, `false` to save as draft.
     */
    private fun onSaveQuiz(publishAfterSave: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.title.isBlank()) {
                _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề bài kiểm tra") }
                return@launch
            }

            val validationResult = QuizValidator.validate(
                questions = state.questions,
                getChoices = { draft ->
                    draft.choices.mapIndexed { idx, choice ->
                        Pair(choice, idx in draft.correctIndices)
                    }
                },
                isCorrect = { (_, correct) -> correct }
            )
            if (!validationResult.isValid) {
                _uiState.update { it.copy(error = validationResult.errorMessage) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val user = authRepository.getCurrentUser()
            val quizId = UUID.randomUUID().toString()
            val tags = state.tags
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // On publish, respect the user's explicit isPublic toggle choice.
            // On draft save, force isPublic = false — drafts must never be public.
            val effectiveIsPublic = if (publishAfterSave) state.isPublic else false

            var shareCode: String? = null
            if (publishAfterSave) {
                shareCodeRepository.generateShareCode(quizId).onSuccess { code ->
                    shareCode = code
                }
            }

            val quiz = Quiz(
                id = quizId,
                ownerId = user?.id ?: "",
                title = state.title,
                description = state.description.takeIf { it.isNotBlank() },
                thumbnailUrl = state.thumbnailUrl.takeIf { it.isNotBlank() },
                authorName = user?.displayName ?: "",
                tags = tags,
                isPublic = effectiveIsPublic,
                questionCount = state.questions.size,
                shareCode = shareCode
            )

            val questions = state.questions.mapIndexed { idx, draft ->
                Question(
                    id = draft.id,
                    quizId = quizId,
                    content = draft.content,
                    choices = draft.choices.mapIndexed { cIdx, choice ->
                        Choice(
                            id = choice.id,
                            content = choice.content,
                            isCorrect = cIdx in draft.correctIndices,
                            position = cIdx
                        )
                    },
                    isMultiSelect = draft.isMultiSelect,
                    explanation = draft.explanation.takeIf { it.isNotBlank() },
                    mediaUrl = draft.mediaUrl.takeIf { it.isNotBlank() },
                    points = draft.points,
                    position = idx
                )
            }

            val result = quizRepository.saveQuiz(quiz, questions)
            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = {
                    if (publishAfterSave) {
                        // Contribute each question to the community pool if opted in.
                        // Pool contribution only runs on publish, never on draft saves.
                        if (state.shareToPool) {
                            questions.forEach { question ->
                                poolRepository.contributeQuestion(
                                    QuestionPoolItem(
                                        id = question.id,
                                        question = question,
                                        contributorId = user?.id,
                                        sourceQuizId = quizId,
                                        tags = tags,
                                        usageCount = 0
                                    )
                                )
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
