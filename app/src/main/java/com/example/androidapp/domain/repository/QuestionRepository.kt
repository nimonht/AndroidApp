package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.Question
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing quiz questions and their choices.
 *
 * Operations follow a local-first pattern: writes persist to Room immediately,
 * then sync to Firestore in the background. Reads emit cached Room data.
 */
interface QuestionRepository {

    /**
     * Observes all questions (with choices) for the given quiz as a reactive stream.
     *
     * @param quizId The ID of the quiz whose questions to observe.
     * @return A [Flow] emitting the latest list of [Question] objects sorted by position.
     */
    fun getQuestionsForQuiz(quizId: String): Flow<List<Question>>

    /**
     * Fetches all questions (with choices) for the given quiz as a one-shot call.
     *
     * @param quizId The ID of the quiz whose questions to fetch.
     * @return A list of [Question] objects sorted by position.
     */
    suspend fun getQuestionsForQuizOnce(quizId: String): List<Question>

    /**
     * Adds a new question to a quiz, including its choices subcollection.
     *
     * Assigns a UUID if the question ID is blank and normalizes choice IDs and positions.
     *
     * @param quizId The ID of the quiz to add the question to.
     * @param question The [Question] to add.
     * @return [Result] containing the generated question ID on success.
     */
    suspend fun addQuestion(quizId: String, question: Question): Result<String>

    /**
     * Updates an existing question's content and replaces its choices.
     *
     * All existing choices for the question are deleted and replaced with the
     * normalized choices from the provided [question] object.
     *
     * @param question The [Question] with updated content and choices.
     * @return [Result] indicating success or failure.
     */
    suspend fun updateQuestion(question: Question): Result<Unit>

    /**
     * Deletes a question and all its associated choices from a quiz.
     *
     * Verifies that the question belongs to the specified quiz before deletion.
     *
     * @param quizId The ID of the quiz the question belongs to.
     * @param questionId The ID of the question to delete.
     * @return [Result] indicating success or failure.
     */
    suspend fun deleteQuestion(quizId: String, questionId: String): Result<Unit>

    /**
     * Reorders questions within a quiz by updating the position field for each question.
     *
     * @param quizId The ID of the quiz whose questions to reorder.
     * @param questionIds The question IDs in the desired order (index = new position).
     * @return [Result] indicating success or failure.
     */
    suspend fun reorderQuestions(quizId: String, questionIds: List<String>): Result<Unit>
}
