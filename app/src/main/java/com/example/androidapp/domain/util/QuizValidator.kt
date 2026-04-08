package com.example.androidapp.domain.util

/**
 * Represents the result of a quiz validation process.
 *
 * @property isValid True if the quiz meets all business requirements, false otherwise.
 * @property errorMessage A clear description of the validation failure, or null if valid.
 *   Messages are English-language codes intended for developer diagnostics.
 *   For user-facing display, callers should map [errorCode] to localised string resources.
 * @property errorCode A machine-readable key identifying the specific validation failure.
 *   UI code can `when` on this value to select a `stringResource`. Null when valid.
 */
data class QuizValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val errorCode: String? = null
)

/**
 * Utility object for validating a quiz before saving it to the database or remote server.
 */
object QuizValidator {

    private const val MIN_QUESTIONS = 1
    private const val MIN_CHOICES = 2
    private const val MAX_CHOICES = 10

    /**
     * Validates the structure of a quiz based on strict business rules.
     * Rules: Minimum 1 question, each question must have 2-10 choices,
     * each question must have at least 1 correct choice, and optionally
     * validates that question and choice content is not blank.
     *
     * @param Q The type representing a Question model.
     * @param C The type representing a Choice model.
     * @param questions The list of questions to validate.
     * @param getChoices A selector function to extract the list of choices from a question.
     * @param isCorrect A selector function to determine if a choice is marked as correct.
     * @param getQuestionContent Optional selector to extract question content for blank validation.
     * @param getChoiceContent Optional selector to extract choice content for blank validation.
     * @return A QuizValidationResult indicating success or detailing the specific error.
     */
    fun <Q, C> validate(
        questions: List<Q>,
        getChoices: (Q) -> List<C>,
        isCorrect: (C) -> Boolean,
        getQuestionContent: ((Q) -> String)? = null,
        getChoiceContent: ((C) -> String)? = null
    ): QuizValidationResult {
        if (questions.size < MIN_QUESTIONS) {
            return QuizValidationResult(
                isValid = false,
                errorMessage = "A quiz must have at least $MIN_QUESTIONS question.",
                errorCode = "QUIZ_TOO_FEW_QUESTIONS"
            )
        }

        questions.forEachIndexed { index, question ->
            val choices = getChoices(question)
            val questionNumber = index + 1

            // Validate question content if accessor is provided
            if (getQuestionContent != null) {
                val content = getQuestionContent(question)
                if (content.isBlank()) {
                    return QuizValidationResult(
                        isValid = false,
                        errorMessage = "Question $questionNumber must have content.",
                        errorCode = "QUESTION_BLANK"
                    )
                }
            }

            if (choices.size !in MIN_CHOICES..MAX_CHOICES) {
                return QuizValidationResult(
                    isValid = false,
                    errorMessage = "Question $questionNumber must have between $MIN_CHOICES and $MAX_CHOICES choices.",
                    errorCode = "QUESTION_INVALID_CHOICE_COUNT"
                )
            }

            // Validate choice content if accessor is provided
            if (getChoiceContent != null) {
                choices.forEachIndexed { choiceIndex, choice ->
                    if (getChoiceContent(choice).isBlank()) {
                        return QuizValidationResult(
                            isValid = false,
                            errorMessage = "Choice ${choiceIndex + 1} in question $questionNumber must have content.",
                            errorCode = "CHOICE_BLANK"
                        )
                    }
                }
            }

            val correctCount = choices.count { isCorrect(it) }
            if (correctCount < 1) {
                return QuizValidationResult(
                    isValid = false,
                    errorMessage = "Question $questionNumber must have at least 1 correct choice.",
                    errorCode = "QUESTION_NO_CORRECT_CHOICE"
                )
            }
        }

        return QuizValidationResult(isValid = true)
    }
}
