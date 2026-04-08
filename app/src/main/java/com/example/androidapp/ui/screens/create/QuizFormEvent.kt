package com.example.androidapp.ui.screens.create

/**
 * Shared quiz-form events common to both [CreateQuizViewModel] and [EditQuizViewModel].
 *
 * These events represent user interactions with the quiz form that are identical
 * across the create and edit flows. Screen-specific events (e.g., pool import for
 * create) are defined separately in their respective event sealed classes.
 *
 * [EditQuizEvent] is a direct typealias for this interface because the edit flow
 * has no additional events. [CreateQuizEvent] is a separate sealed interface that
 * covers create-only actions (pool import, CSV import, etc.); the
 * [CreateQuizViewModel] exposes two `onEvent` overloads so that both shared and
 * create-specific events funnel through the same method name.
 */
sealed interface QuizFormEvent {

    /** Updates the quiz title. */
    data class TitleChanged(val title: String) : QuizFormEvent

    /** Updates the quiz description. */
    data class DescriptionChanged(val description: String) : QuizFormEvent

    /** Updates the quiz cover image URL. */
    data class ThumbnailUrlChanged(val thumbnailUrl: String) : QuizFormEvent

    /** Toggles the public visibility of the quiz. */
    data class IsPublicChanged(val isPublic: Boolean) : QuizFormEvent

    /** Updates the raw comma-separated tags string. */
    data class TagsChanged(val tags: String) : QuizFormEvent

    /** Appends a blank question to the end of the question list. */
    data object AddQuestion : QuizFormEvent

    /** Replaces the question at [index] with [draft]. */
    data class UpdateQuestion(val index: Int, val draft: QuestionDraft) : QuizFormEvent

    /** Removes the question at [index] if more than one question exists. */
    data class RemoveQuestion(val index: Int) : QuizFormEvent

    /** Moves the question at [index] one position up in the list. */
    data class MoveQuestionUp(val index: Int) : QuizFormEvent

    /** Moves the question at [index] one position down in the list. */
    data class MoveQuestionDown(val index: Int) : QuizFormEvent

    /**
     * Saves the current form as a draft without publishing.
     * Records the last-saved timestamp. The quiz is saved with whatever
     * visibility the user has currently set.
     */
    data object SaveDraft : QuizFormEvent

    /**
     * Validates the quiz and saves it as a published quiz.
     * Triggers a navigation event upon success.
     */
    data object PublishQuiz : QuizFormEvent

    /** Legacy save alias -- behaves identically to [PublishQuiz]. */
    data object SaveQuiz : QuizFormEvent

    /** Toggles whether each question will be contributed to the community pool after publishing. */
    data class ShareToPoolChanged(val shareToPool: Boolean) : QuizFormEvent

    /** Clears the current error message from the UI state. */
    data object ClearError : QuizFormEvent
}
