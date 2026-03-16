package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.androidapp.ui.screens.create.DraftSaver





/**
 * Draft model for creating/editing a question.
 * Kept in the ViewModel layer, not in the domain.
 */
data class QuestionDraft(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val choices: List<ChoiceDraft> = List(4) { ChoiceDraft() },
    val correctIndices: Set<Int> = setOf(0),
    val isMultiSelect: Boolean = false,
    val explanation: String = "",
    val mediaUrl: String? = null,
    val points: Int = 1,
)

/**
 * Draft model for a choice within a [QuestionDraft].
 */
data class ChoiceDraft(
    val id: String = UUID.randomUUID().toString(),
    val content: String = ""
)


/**
 * UI state for the Create Quiz screen.
 */
data class CreateQuizUiState(
    val title: String = "",
    val description: String = "",
    val isPublic: Boolean = false,
    val tags: String = "",
    val questions: List<QuestionDraft> = listOf(QuestionDraft()),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val shareToPool: Boolean = false

)

/** Events that can be dispatched to [CreateQuizViewModel]. */
sealed class CreateQuizEvent {
    data class TitleChanged(val title: String) : CreateQuizEvent()
    data class DescriptionChanged(val description: String) : CreateQuizEvent()
    data class IsPublicChanged(val isPublic: Boolean) : CreateQuizEvent()
    data class TagsChanged(val tags: String) : CreateQuizEvent()
    data object AddQuestion : CreateQuizEvent()
    data class UpdateQuestion(val index: Int, val draft: QuestionDraft) : CreateQuizEvent()
    data class RemoveQuestion(val index: Int) : CreateQuizEvent()
    data object SaveQuiz : CreateQuizEvent()
    data object ClearError : CreateQuizEvent()

    data class UpdateQuestionContent(val content: String) : CreateQuizEvent()

    data class UpdateQuestionMedia(val media: String) : CreateQuizEvent()

    data class UpdateQuestionPoints(val points: Int) : CreateQuizEvent()

    data class UpdateExplanation(val explanation: String) : CreateQuizEvent()

    object SaveQuestion : CreateQuizEvent()

    data class OnShareToPoolToggle(val enabled: Boolean) : CreateQuizEvent()
}



/**
 * ViewModel for the Create Quiz screen.
 * Manages multi-step form state and saves to the repository.
 */
class CreateQuizViewModel(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())

    /** Current UI state for the Create Quiz screen. */
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    /**
     * Dispatches a [CreateQuizEvent] to the ViewModel.
     */
    /**
     * Thêm câu hỏi
     */
    private val _questionDraft = MutableStateFlow(QuestionDraft())
    val questionDraft: StateFlow<QuestionDraft> = _questionDraft.asStateFlow()

    fun onEvent(event: CreateQuizEvent) {
        when (event) {

            is CreateQuizEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.title) }

            is CreateQuizEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.description) }

            is CreateQuizEvent.IsPublicChanged ->
                _uiState.update { it.copy(isPublic = event.isPublic) }

            is CreateQuizEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.tags) }

            is CreateQuizEvent.AddQuestion ->
                _uiState.update { it.copy(questions = it.questions + QuestionDraft()) }

            is CreateQuizEvent.UpdateQuestion -> _uiState.update { state ->
                state.copy(
                    questions = state.questions.toMutableList().apply {
                        this[event.index] = event.draft
                    }
                )
            }

            is CreateQuizEvent.RemoveQuestion -> _uiState.update { state ->
                if (state.questions.size > 1) {
                    state.copy(
                        questions = state.questions.toMutableList().apply {
                            removeAt(event.index)
                        }
                    )
                } else state
            }

            is CreateQuizEvent.UpdateQuestionContent -> {
                _questionDraft.update { it.copy(content = event.content) }
            }

            is CreateQuizEvent.UpdateQuestionMedia -> {
                _questionDraft.update { it.copy(mediaUrl = event.media) }
            }

            is CreateQuizEvent.UpdateQuestionPoints -> {
                _questionDraft.update { it.copy(points = event.points) }
            }

            is CreateQuizEvent.UpdateExplanation -> {
                _questionDraft.update { it.copy(explanation = event.explanation) }
            }

            CreateQuizEvent.SaveQuestion -> {
                val question = _questionDraft.value

                _uiState.update { state ->
                    state.copy(
                        questions = state.questions + question
                    )
                }

                _questionDraft.value = QuestionDraft()
            }

            is CreateQuizEvent.OnShareToPoolToggle -> {
                _uiState.update {
                    it.copy(shareToPool = event.enabled)
                }
            }

            is CreateQuizEvent.SaveQuiz -> onSaveQuiz()

            is CreateQuizEvent.ClearError ->
                _uiState.update { it.copy(error = null) }
        }
    }
    /// choice editor
    private val _choices = MutableStateFlow(
        listOf(
            ChoiceDraft(),
            ChoiceDraft()
        )
    )
    val choices: StateFlow<List<ChoiceDraft>> = _choices.asStateFlow()

    ////--------------------------------------------------------------------
    private val draftSaver = DraftSaver<CreateQuizUiState>(
        scope = viewModelScope,
        delayMillis = 2000
    ) { draft ->
        saveDraftInternal(draft)
    }


    fun addChoice() {

        _questionDraft.update { draft ->

            if (draft.choices.size >= 10) return

            draft.copy(
                choices = draft.choices + ChoiceDraft()
            )
        }
    }
    fun removeChoice(index: Int) {

        _questionDraft.update { draft ->

            if (draft.choices.size <= 2) return

            val updated = draft.choices.toMutableList()

            updated.removeAt(index)

            draft.copy(
                choices = updated
            )
        }
    }
    fun updateChoice(index: Int, text: String) {

        _questionDraft.update { draft ->

            val updated = draft.choices.toMutableList()

            updated[index] = updated[index].copy(
                content = text
            )

            draft.copy(
                choices = updated
            )
        }
    }
    fun markCorrect(index: Int) {

        _questionDraft.update { draft ->
            draft.copy(
                correctIndices = setOf(index)
            )
        }
    }
    fun moveChoiceUp(index: Int) {

        _questionDraft.update { draft ->

            if (index == 0) return

            val list = draft.choices.toMutableList()

            val item = list[index]

            list[index] = list[index - 1]
            list[index - 1] = item

            draft.copy(
                choices = list
            )
        }
    }
    fun moveChoiceDown(index: Int) {

        _questionDraft.update { draft ->

            val list = draft.choices.toMutableList()

            if (index == list.lastIndex) return

            val item = list[index]

            list[index] = list[index + 1]
            list[index + 1] = item

            draft.copy(
                choices = list
            )
        }
    }
    ////

    fun moveQuestion(from: Int, to: Int) {

        val list = _uiState.value.questions.toMutableList()

        val item = list.removeAt(from)

        list.add(to, item)

        _uiState.update {
            it.copy(questions = list)
        }
    }

    /// draft saver
    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        scheduleAutoSave()
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
        scheduleAutoSave()
    }
    fun onSaveDraftClick() {
        viewModelScope.launch {
            saveDraftInternal(_uiState.value)
        }
    }

    private suspend fun saveDraftInternal(draft: CreateQuizUiState) {

        // TODO sau này có thể lưu Room
        println("Draft saved: $draft")

        _draftSaved.value = true
    }


    ///// lưu bản nháp
    private val _draftSaved = MutableStateFlow(false)
    val draftSaved: StateFlow<Boolean> = _draftSaved.asStateFlow()

    private fun scheduleAutoSave() {
        draftSaver.scheduleSave(_uiState.value)
    }

    ////

    private fun onSaveQuiz() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.title.isBlank()) {
                _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề bài kiểm tra") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            val quizId = UUID.randomUUID().toString()
            val tags = state.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val quiz = Quiz(
                id = quizId,
                ownerId = user?.id ?: "",
                title = state.title,
                description = state.description.takeIf { it.isNotBlank() },
                authorName = user?.displayName ?: "",
                tags = tags,
                isPublic = state.isPublic,
                questionCount = state.questions.size
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
                    position = idx
                )
            }
            val result = quizRepository.saveQuiz(quiz, questions)
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaved = true) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Không thể lưu bài kiểm tra") } }
            )
        }
    }
}

