package com.example.androidapp.domain.util

/**
 * Utility object for shuffling quiz questions and their respective choices
 * while preserving the correct answer mapping.
 */
object QuestionShuffler {

    /**
     * Shuffles a list of questions and the choices within each question.
     * By shuffling the objects directly, the correct answer mapping (e.g., an isCorrect flag
     * or a unique choice ID inside the Choice model) is naturally preserved.
     *
     * @param Q The type representing a Question model.
     * @param C The type representing a Choice model.
     * @param questions The original list of questions to shuffle.
     * @param getChoices A selector function to extract the list of choices from a question.
     * @param copyWithNewChoices A function to create a copy of the question with the newly shuffled choices.
     * @return A new list containing shuffled questions and shuffled choices.
     */
    fun <Q, C> shuffle(
        questions: List<Q>,
        getChoices: (Q) -> List<C>,
        copyWithNewChoices: (Q, List<C>) -> Q
    ): List<Q> {
        if (questions.isEmpty()) return emptyList()

        return questions.shuffled().map { question ->
            val originalChoices = getChoices(question)

            val shuffledChoices = originalChoices.shuffled()

            copyWithNewChoices(question, shuffledChoices)
        }
    }
}
