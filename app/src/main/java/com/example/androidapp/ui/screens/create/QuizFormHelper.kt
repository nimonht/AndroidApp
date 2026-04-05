package com.example.androidapp.ui.screens.create

import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.util.QuizValidator
import java.util.UUID

/**
 * Draft model for creating/editing a question.
 * Kept in the UI layer, not in the domain.
 *
 * @property id Unique identifier for this draft, used to track identity across recompositions.
 * @property content The question text.
 * @property choices The list of answer choices (min 2, max 10).
 * @property correctIndices Set of indices that mark the correct choice(s).
 * @property isMultiSelect Whether the question allows multiple correct answers.
 * @property explanation Optional explanation shown after answering.
 * @property mediaUrl Optional URL for an image or video attached to the question.
 * @property points The point value awarded for a correct answer (1-10).
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
 * Pure-Kotlin helper that centralises quiz-form logic shared by
 * [CreateQuizViewModel] and [EditQuizViewModel].
 *
 * All functions are stateless and side-effect-free so that ViewModels remain
 * the sole owners of mutable state and coroutine scopes.
 */
object QuizFormHelper {

    // -- Question CRUD operations ------------------------------------------------

    /**
     * Returns a new list with a blank [QuestionDraft] appended.
     *
     * @param current The current list of question drafts.
     * @return A new list with one additional blank draft at the end.
     */
    fun addQuestion(current: List<QuestionDraft>): List<QuestionDraft> =
        current + QuestionDraft()

    /**
     * Replaces the draft at [index] with [draft].
     *
     * @param current The current list of question drafts.
     * @param index   The zero-based position of the draft to replace.
     * @param draft   The replacement draft.
     * @return A new list with the updated draft, or the original list if [index] is out of bounds.
     */
    fun updateQuestion(
        current: List<QuestionDraft>,
        index: Int,
        draft: QuestionDraft
    ): List<QuestionDraft> {
        if (index < 0 || index >= current.size) return current
        return current.toMutableList().apply { this[index] = draft }
    }

    /**
     * Removes the draft at [index], provided the list has more than one element.
     *
     * @param current The current list of question drafts.
     * @param index   The zero-based position of the draft to remove.
     * @return A new list without the removed draft, or the original list if removal is not allowed.
     */
    fun removeQuestion(
        current: List<QuestionDraft>,
        index: Int
    ): List<QuestionDraft> {
        if (current.size <= 1) return current
        if (index < 0 || index >= current.size) return current
        return current.toMutableList().apply { removeAt(index) }
    }

    /**
     * Swaps the draft at [index] with the one directly above it (index - 1).
     *
     * @param current The current list of question drafts.
     * @param index   The zero-based position of the draft to move up.
     * @return A new list with the two drafts swapped, or the original list if the move is invalid.
     */
    fun moveQuestionUp(
        current: List<QuestionDraft>,
        index: Int
    ): List<QuestionDraft> {
        if (index <= 0 || index >= current.size) return current
        val list = current.toMutableList()
        val temp = list[index - 1]
        list[index - 1] = list[index]
        list[index] = temp
        return list
    }

    /**
     * Swaps the draft at [index] with the one directly below it (index + 1).
     *
     * @param current The current list of question drafts.
     * @param index   The zero-based position of the draft to move down.
     * @return A new list with the two drafts swapped, or the original list if the move is invalid.
     */
    fun moveQuestionDown(
        current: List<QuestionDraft>,
        index: Int
    ): List<QuestionDraft> {
        if (index < 0 || index >= current.size - 1) return current
        val list = current.toMutableList()
        val temp = list[index + 1]
        list[index + 1] = list[index]
        list[index] = temp
        return list
    }

    /**
     * Merges [imported] questions into [current].
     *
     * If the current list contains only a single blank question (empty content),
     * it is replaced entirely by [imported]. Otherwise [imported] is appended.
     *
     * @param current  The existing list of question drafts.
     * @param imported The newly imported question drafts.
     * @return The merged list.
     */
    fun mergeImportedQuestions(
        current: List<QuestionDraft>,
        imported: List<QuestionDraft>
    ): List<QuestionDraft> {
        return if (current.size == 1 && current.first().content.isBlank()) {
            imported
        } else {
            current + imported
        }
    }

    // -- Validation --------------------------------------------------------------

    /**
     * Validates the quiz form and returns an error message or `null` when valid.
     *
     * Checks performed:
     * 1. Title must not be blank.
     * 2. Delegates structural validation to [QuizValidator] (question count,
     *    choice count, and correct-answer presence).
     *
     * @param title     The quiz title entered by the user.
     * @param questions The current list of question drafts.
     * @return An error message string, or `null` if the form is valid.
     */
    fun validateQuizForm(title: String, questions: List<QuestionDraft>): String? {
        if (title.isBlank()) {
            return "Vui lòng nhập tiêu đề bài kiểm tra"
        }

        val result = QuizValidator.validate(
            questions = questions,
            getChoices = { draft ->
                draft.choices.mapIndexed { idx, choice ->
                    Pair(choice, idx in draft.correctIndices)
                }
            },
            isCorrect = { (_, correct) -> correct },
            getQuestionContent = { draft -> draft.content },
            getChoiceContent = { (choice, _) -> choice.content }
        )

        return if (result.isValid) null else result.errorMessage
    }

    // -- Mapping helpers ---------------------------------------------------------

    /**
     * Converts a list of [QuestionDraft] objects to domain [Question] objects.
     *
     * Each draft is mapped to a [Question] with properly positioned [Choice] entries.
     * The [Question.position] is set to the draft's index in the list.
     *
     * @param quizId    The ID of the parent quiz.
     * @param questions The drafts to convert.
     * @return An ordered list of domain [Question] objects.
     */
    fun mapQuestionsToEntities(
        quizId: String,
        questions: List<QuestionDraft>
    ): List<Question> = questions.mapIndexed { idx, draft ->
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

    /**
     * Constructs a [Quiz] domain object from the form fields.
     *
     * @param quizId           The unique quiz identifier.
     * @param ownerId          The ID of the quiz owner / creator.
     * @param title            The quiz title.
     * @param description      The quiz description (blank values become `null`).
     * @param thumbnailUrl     The cover image URL (blank values become `null`).
     * @param authorName       The display name of the author.
     * @param tags             Parsed list of tag strings.
     * @param isPublic         Whether the quiz is publicly discoverable.
     * @param shareCode        Optional share code for the quiz.
     * @param questionCount    Number of questions in the quiz.
     * @param updatedAt        Epoch millis for the update timestamp; defaults to current time.
     * @return A fully constructed [Quiz] domain object.
     */
    fun buildQuizFromForm(
        quizId: String,
        ownerId: String,
        title: String,
        description: String,
        thumbnailUrl: String,
        authorName: String,
        tags: List<String>,
        isPublic: Boolean,
        shareCode: String?,
        questionCount: Int,
        updatedAt: Long = System.currentTimeMillis()
    ): Quiz = Quiz(
        id = quizId,
        ownerId = ownerId,
        title = title,
        description = description.takeIf { it.isNotBlank() },
        thumbnailUrl = thumbnailUrl.takeIf { it.isNotBlank() },
        authorName = authorName,
        tags = tags,
        isPublic = isPublic,
        shareCode = shareCode,
        questionCount = questionCount,
        updatedAt = updatedAt
    )

    // -- Tag parsing -------------------------------------------------------------

    /**
     * Parses a raw comma-separated tags string into a trimmed, non-blank list.
     *
     * @param raw The raw input string (e.g. "kotlin, android, ").
     * @return A list of trimmed non-blank tag strings.
     */
    fun parseTags(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotBlank() }

    // -- Pool import helpers -----------------------------------------------------

    /**
     * Converts a list of [QuestionPoolItem] objects into [QuestionDraft] entries
     * suitable for appending to the quiz form.
     *
     * Each pool item receives fresh UUIDs for its draft and choice IDs so that
     * they do not collide with the originals.
     *
     * @param poolItems The pool items to convert.
     * @return A list of [QuestionDraft] objects ready for import.
     */
    fun mapPoolItemsToDrafts(poolItems: List<QuestionPoolItem>): List<QuestionDraft> =
        poolItems.map { poolItem ->
            QuestionDraft(
                id = UUID.randomUUID().toString(),
                content = poolItem.question.content,
                choices = poolItem.question.choices.map { choice ->
                    ChoiceDraft(
                        id = UUID.randomUUID().toString(),
                        content = choice.content
                    )
                },
                correctIndices = poolItem.question.choices
                    .mapIndexedNotNull { idx, c -> if (c.isCorrect) idx else null }
                    .toSet(),
                isMultiSelect = poolItem.question.isMultiSelect,
                explanation = poolItem.question.explanation ?: "",
                mediaUrl = poolItem.question.mediaUrl ?: "",
                points = poolItem.question.points
            )
        }

    /**
     * Builds a list of [QuestionPoolItem] objects for contributing questions to
     * the community pool.
     *
     * @param questions    The domain [Question] list (already mapped from drafts).
     * @param contributorId The ID of the contributing user, or `null` for anonymous.
     * @param sourceQuizId The quiz that these questions belong to.
     * @param tags         The tags to associate with each pool item.
     * @return A list of [QuestionPoolItem] objects ready for contribution.
     */
    fun buildPoolContributions(
        questions: List<Question>,
        contributorId: String?,
        sourceQuizId: String,
        tags: List<String>
    ): List<QuestionPoolItem> = questions.map { question ->
        QuestionPoolItem(
            id = question.id,
            question = question,
            contributorId = contributorId,
            sourceQuizId = sourceQuizId,
            tags = tags,
            usageCount = 0
        )
    }
}
