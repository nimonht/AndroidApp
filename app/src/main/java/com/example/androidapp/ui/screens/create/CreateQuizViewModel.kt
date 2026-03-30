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
import com.example.androidapp.domain.util.QuizValidator
import com.example.androidapp.domain.util.UserInputSanitizer
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
    data class TitleChanged(val title: String) : CreateQuizEvent()
    data class DescriptionChanged(val description: String) : CreateQuizEvent()
    data class ThumbnailUrlChanged(val thumbnailUrl: String) : CreateQuizEvent()
    data class IsPublicChanged(val isPublic: Boolean) : CreateQuizEvent()
    data class TagsChanged(val tags: String) : CreateQuizEvent()
    data object AddQuestion : CreateQuizEvent()
    data class UpdateQuestion(val index: Int, val draft: QuestionDraft) : CreateQuizEvent()
    data class RemoveQuestion(val index: Int) : CreateQuizEvent()
    data class MoveQuestionUp(val index: Int) : CreateQuizEvent()
    data class MoveQuestionDown(val index: Int) : CreateQuizEvent()
    data object SaveDraft : CreateQuizEvent()
    data object PublishQuiz : CreateQuizEvent()
    data object SaveQuiz : CreateQuizEvent()
    data class ShareToPoolChanged(val shareToPool: Boolean) : CreateQuizEvent()
    data object ClearError : CreateQuizEvent()
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
    private val poolRepository: PoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    fun onEvent(event: CreateQuizEvent) {
        when (event) {
            is CreateQuizEvent.TitleChanged -> _uiState.update { it.copy(title = event.title) }
            is CreateQuizEvent.DescriptionChanged -> _uiState.update { it.copy(description = event.description) }
            is CreateQuizEvent.ThumbnailUrlChanged -> _uiState.update { it.copy(thumbnailUrl = event.thumbnailUrl) }
            is CreateQuizEvent.IsPublicChanged -> _uiState.update { it.copy(isPublic = event.isPublic) }
            is CreateQuizEvent.TagsChanged -> _uiState.update { it.copy(tags = event.tags) }
            is CreateQuizEvent.AddQuestion -> _uiState.update { it.copy(questions = it.questions + QuestionDraft()) }
            is CreateQuizEvent.UpdateQuestion -> _uiState.update { state ->
                state.copy(questions = state.questions.toMutableList().apply { this[event.index] = event.draft })
            }
            is CreateQuizEvent.RemoveQuestion -> _uiState.update { state ->
                if (state.questions.size > 1) {
                    state.copy(questions = state.questions.toMutableList().apply { removeAt(event.index) })
                } else state
            }
            is CreateQuizEvent.MoveQuestionUp -> _uiState.update { state ->
                val idx = event.index
                if (idx <= 0 || idx >= state.questions.size) return@update state
                val list = state.questions.toMutableList()
                val temp = list[idx - 1]
                list[idx - 1] = list[idx]
                list[idx] = temp
                state.copy(questions = list)
            }
            is CreateQuizEvent.MoveQuestionDown -> _uiState.update { state ->
                val idx = event.index
                if (idx < 0 || idx >= state.questions.size - 1) return@update state
                val list = state.questions.toMutableList()
                val temp = list[idx + 1]
                list[idx + 1] = list[idx]
                list[idx] = temp
                state.copy(questions = list)
            }
            is CreateQuizEvent.SaveDraft -> onSaveQuiz(publishAfterSave = false)
            is CreateQuizEvent.PublishQuiz -> onSaveQuiz(publishAfterSave = true)
            is CreateQuizEvent.SaveQuiz -> onSaveQuiz(publishAfterSave = true)
            is CreateQuizEvent.ShareToPoolChanged -> _uiState.update { it.copy(shareToPool = event.shareToPool) }
            is CreateQuizEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    /**
     * Tích hợp Nhiệm vụ 45: Validates and normalizes the raw tags string.
     * Rules: Max 5 tags, max 20 characters per tag, lowercase, no duplicates.
     * * @return A Pair where the first element is the valid list of tags,
     * and the second element is the error message (if any).
     */
    private fun validateAndNormalizeTags(rawTags: String): Pair<List<String>, String?> {
        if (rawTags.isBlank()) return Pair(emptyList(), null)

        val parsedTags = rawTags.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (parsedTags.size > 5) {
            return Pair(emptyList(), "Chỉ được nhập tối đa 5 thẻ (tags).")
        }

        val invalidTag = parsedTags.find { it.length > 20 }
        if (invalidTag != null) {
            return Pair(emptyList(), "Thẻ '$invalidTag' vượt quá 20 ký tự.")
        }

        return Pair(parsedTags, null)
    }

    private fun onSaveQuiz(publishAfterSave: Boolean) {
        viewModelScope.launch {
            val currentState = _uiState.value

            // 1. Sanitize inputs (Nhiệm vụ 44)
            val sanitizedTitle = UserInputSanitizer.sanitize(currentState.title)
            val sanitizedDescription = UserInputSanitizer.sanitize(currentState.description)
            val sanitizedThumbnailUrl = UserInputSanitizer.sanitize(currentState.thumbnailUrl)
            val sanitizedTags = UserInputSanitizer.sanitize(currentState.tags)

            val sanitizedQuestions = currentState.questions.map { questionDraft ->
                questionDraft.copy(
                    content = UserInputSanitizer.sanitize(questionDraft.content),
                    explanation = UserInputSanitizer.sanitize(questionDraft.explanation),
                    mediaUrl = UserInputSanitizer.sanitize(questionDraft.mediaUrl),
                    choices = questionDraft.choices.map { choiceDraft ->
                        choiceDraft.copy(content = UserInputSanitizer.sanitize(choiceDraft.content))
                    }
                )
            }

            // Cập nhật lại UI state để form hiển thị dữ liệu đã được làm sạch
            _uiState.update {
                it.copy(
                    title = sanitizedTitle,
                    description = sanitizedDescription,
                    thumbnailUrl = sanitizedThumbnailUrl,
                    tags = sanitizedTags,
                    questions = sanitizedQuestions
                )
            }

            // 2. Validate & Normalize Tags (Nhiệm vụ 45)
            val (parsedTags, tagError) = validateAndNormalizeTags(sanitizedTags)
            if (tagError != null) {
                _uiState.update { it.copy(error = tagError) }
                return@launch
            }

            // 3. Tiến hành kiểm tra tính hợp lệ tiêu đề và câu hỏi (Nhiệm vụ 42, 43)
            if (sanitizedTitle.isBlank()) {
                _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề bài kiểm tra") }
                return@launch
            }

            val validationResult = QuizValidator.validate(
                questions = sanitizedQuestions,
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
            val effectiveIsPublic = if (publishAfterSave) true else currentState.isPublic

            val quiz = Quiz(
                id = quizId,
                ownerId = user?.id ?: "",
                title = sanitizedTitle,
                description = sanitizedDescription.takeIf { it.isNotBlank() },
                thumbnailUrl = sanitizedThumbnailUrl.takeIf { it.isNotBlank() },
                authorName = user?.displayName ?: "",
                tags = parsedTags, // Sử dụng tags đã được chuẩn hóa và kiểm tra hợp lệ
                isPublic = effectiveIsPublic,
                questionCount = sanitizedQuestions.size
            )

            val domainQuestions = sanitizedQuestions.mapIndexed { idx, draft ->
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

            val result = quizRepository.saveQuiz(quiz, domainQuestions)
            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = {
                    if (publishAfterSave) {
                        if (currentState.shareToPool) {
                            domainQuestions.forEach { question ->
                                poolRepository.contributeQuestion(
                                    QuestionPoolItem(
                                        id = question.id,
                                        question = question,
                                        contributorId = user?.id,
                                        sourceQuizId = quizId,
                                        tags = parsedTags,
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