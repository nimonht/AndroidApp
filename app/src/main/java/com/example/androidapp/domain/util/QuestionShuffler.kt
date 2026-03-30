package com.example.androidapp.domain.util

/**
 * Utility object for shuffling questions and their corresponding choices.
 * Uses generics to remain fully decoupled from specific domain models,
 * ensuring that correctness is preserved during the shuffle.
 */
object QuestionShuffler {

    /**
     * Shuffles a list of questions and the choices within each question.
     *
     * @param Q The type representing a Question model.
     * @param C The type representing a Choice model.
     * @param questions The original list of questions.
     * @param getChoices A selector function to extract the list of choices from a question.
     * @param copyChoice A function to create a copy of a choice with a new position index.
     * @param copyQuestion A function to create a copy of a question with a new list of choices and a new position index.
     * @return A new list of shuffled questions with shuffled choices.
     */
    fun <Q, C> shuffle(
        questions: List<Q>,
        getChoices: (Q) -> List<C>,
        copyChoice: (choice: C, newPosition: Int) -> C,
        copyQuestion: (question: Q, newChoices: List<C>, newPosition: Int) -> Q
    ): List<Q> {
        if (questions.isEmpty()) return emptyList()

        // 1. Trộn câu hỏi và cập nhật vị trí mới (qIndex)
        return questions.shuffled().mapIndexed { qIndex, question ->

            // 2. Lấy danh sách lựa chọn của câu hỏi đó
            val choices = getChoices(question)

            // 3. Trộn lựa chọn và cập nhật vị trí mới (cIndex)
            val shuffledChoices = choices.shuffled().mapIndexed { cIndex, choice ->
                copyChoice(choice, cIndex)
            }

            // 4. Trả về câu hỏi mới đã được cập nhật lựa chọn và vị trí
            copyQuestion(question, shuffledChoices, qIndex)
        }
    }
}