package com.example.androidapp.data.local

import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao

/**
 * Shared utility for purging a quiz and all its associated questions and
 * choices from the local Room database.
 *
 * Attempts are intentionally **not** deleted because they reference the quiz
 * by ID without a foreign-key constraint -- the user's history remains intact.
 *
 * This object centralises the "get questions -> delete choices -> delete
 * questions -> delete quiz" sequence that was previously duplicated across
 * [com.example.androidapp.data.repository.QuizRepositoryImpl],
 * [com.example.androidapp.data.sync.QuizInvalidationManager], and
 * [com.example.androidapp.data.sync.SyncManager].
 */
object LocalQuizPurger {

    /**
     * Removes a quiz and all its associated questions and choices from the
     * local Room database.
     *
     * The deletion order respects the absence of cascading foreign keys in the
     * Room schema: choices are deleted first, then questions, and finally the
     * quiz entity itself.
     *
     * @param quizId      the Firestore / Room primary-key ID of the quiz to purge.
     * @param quizDao     Room DAO for quiz entity operations.
     * @param questionDao Room DAO for question entity operations.
     * @param choiceDao   Room DAO for choice entity operations.
     */
    suspend fun purgeLocalQuiz(
        quizId: String,
        quizDao: QuizDao,
        questionDao: QuestionDao,
        choiceDao: ChoiceDao
    ) {
        val questions = questionDao.getQuestionsByQuizIdOnce(quizId)
        for (question in questions) {
            choiceDao.deleteChoicesByQuestionId(question.id)
            questionDao.deleteQuestion(question)
        }
        quizDao.deleteQuizById(quizId)
    }
}
