package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import kotlinx.coroutines.flow.Flow

/**
 * Container for home screen quiz sections.
 */
data class HomeQuizzes(
    val recentAttemptQuizzes: List<Quiz> = emptyList(),
    val myQuizzes: List<Quiz> = emptyList(),
    val trendingQuizzes: List<Quiz> = emptyList()
)

/**
 * Repository interface for quiz and question operations.
 * Follows local-first pattern: Room is the single source of truth.
 * Firestore updates are synced in the background.
 */
interface QuizRepository {

    /**
     * Emits combined home screen quiz data for a user.
     * Refreshes from Firestore when online.
     */
    fun getHomeQuizzes(userId: String): Flow<HomeQuizzes>

    /**
     * Emits quizzes owned by the user that are not deleted.
     */
    fun getMyQuizzes(userId: String): Flow<List<Quiz>>

    /**
     * Emits public quizzes ordered by popularity.
     */
    fun getPublicQuizzes(): Flow<List<Quiz>>

    /**
     * Emits quizzes that match the search query.
     */
    fun searchQuizzes(query: String): Flow<List<Quiz>>

    /**
     * Emits soft-deleted quizzes for the user (recycle bin).
     */
    fun getDeletedQuizzes(userId: String): Flow<List<Quiz>>

    /**
     * Returns a single quiz by ID, or null if not found.
     */
    suspend fun getQuizById(quizId: String): Quiz?

    /**
     * Returns a quiz by its share code, or null if not found.
     */
    suspend fun getQuizByShareCode(shareCode: String): Quiz?

    /**
     * Emits questions for a quiz ordered by position.
     */
    fun getQuestionsForQuiz(quizId: String): Flow<List<Question>>

    /**
     * Returns questions for a quiz as a one-time fetch.
     */
    suspend fun getQuestionsForQuizOnce(quizId: String): List<Question>

    /**
     * Saves a new quiz with its questions.
     * Writes to Room first (PENDING), then syncs to Firestore.
     */
    suspend fun saveQuiz(quiz: Quiz, questions: List<Question>): Result<Unit>

    /**
     * Updates an existing quiz.
     * Writes to Room first (PENDING), then syncs to Firestore.
     */
    suspend fun updateQuiz(quiz: Quiz, questions: List<Question>): Result<Unit>

    /**
     * Soft-deletes a quiz (moves to recycle bin).
     */
    suspend fun deleteQuiz(quizId: String): Result<Unit>

    /**
     * Restores a soft-deleted quiz from the recycle bin.
     */
    suspend fun restoreQuiz(quizId: String): Result<Unit>

    /**
     * Permanently deletes a quiz from both Room and Firestore.
     */
    suspend fun permanentlyDeleteQuiz(quizId: String): Result<Unit>

    /**
     * Atomically increments the attempt count for a quiz.
     */
    suspend fun incrementAttemptCount(quizId: String): Result<Unit>

    /**
     * Emits public quizzes sorted by attempt count descending (trending).
     */
    fun getTrendingQuizzes(): Flow<List<Quiz>>
}

