package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.PaginatedResult
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
     * Triggers a background refresh of home screen data from Firestore.
     * Suspends until the refresh is complete (or fails).
     * Does nothing when sync is not allowed (offline, wifi-only, etc.).
     */
    suspend fun refreshHomeData(userId: String)

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

    /**
     * Permanently deletes all soft-deleted quizzes for the user.
     */
    suspend fun emptyTrash(userId: String): Result<Unit>

    /**
     * Retrieves all distinct tags from all non-deleted quizzes.
     */
    suspend fun getAllTags(): List<String>

    /**
     * Refreshes a single quiz and its questions/choices from Firestore into Room.
     * Used as a fallback when local data is missing or incomplete (e.g., after
     * a CASCADE delete removes questions/choices from Room).
     *
     * @param quizId The ID of the quiz to refresh.
     * @return [Result.success] with the refreshed [Quiz] if found on remote,
     *         or [Result.failure] if the quiz does not exist remotely or the fetch fails.
     */
    suspend fun refreshQuizFromRemote(quizId: String): Result<Quiz>

    // ==================== Paginated queries ====================

    /**
     * Emits public quizzes with a dynamic limit for incremental loading.
     * The limit increases as the user scrolls, and Room re-emits the full
     * list up to the new limit whenever data changes.
     *
     * @param limit Maximum number of quizzes to return.
     */
    fun getPublicQuizzesLimited(limit: Int): Flow<List<Quiz>>

    /**
     * Emits quizzes owned by the user with a dynamic limit.
     *
     * @param userId The owner's user ID.
     * @param limit Maximum number of quizzes to return.
     */
    fun getMyQuizzesLimited(userId: String, limit: Int): Flow<List<Quiz>>

    /**
     * Emits search results with a dynamic limit.
     *
     * @param query The search query.
     * @param limit Maximum number of quizzes to return.
     */
    fun searchQuizzesLimited(query: String, limit: Int): Flow<List<Quiz>>

    /**
     * Emits soft-deleted quizzes with a dynamic limit.
     *
     * @param userId The owner's user ID.
     * @param limit Maximum number of quizzes to return.
     */
    fun getDeletedQuizzesLimited(userId: String, limit: Int): Flow<List<Quiz>>

    /**
     * Returns the total count of public non-deleted quizzes.
     * Used by pagination to determine if more items are available.
     */
    suspend fun getPublicQuizzesCount(): Int

    /**
     * Returns the total count of search results for a query.
     */
    suspend fun getSearchResultsCount(query: String): Int

    /**
     * Forces a one-shot refresh of the user's quizzes from the remote data source,
     * bypassing sync-allowed checks. Used by the developer console to ensure
     * fresh data.
     *
     * @param userId The owner's user ID whose quizzes should be refreshed.
     */
    suspend fun forceRefreshUserQuizzes(userId: String)

    /**
     * Refreshes the local Room cache of public quizzes from Firestore.
     *
     * Fetches all public quizzes, upserts them into Room (including their
     * questions and choices), and purges stale local entries that no longer
     * exist on the remote.
     *
     * @param currentUserId optional ID of the current user; when provided,
     *        stale-quiz cleanup skips quizzes owned by this user so that the
     *        owner's local data is preserved.
     */
    suspend fun refreshPublicQuizzes(currentUserId: String? = null)

    // ==================== Semantic search ====================

    /**
     * Performs semantic similarity search using pre-computed quiz embeddings.
     * Falls back to [searchQuizzesLimited] if embeddings are not available.
     *
     * @param query Raw search query text.
     * @param limit Maximum number of results.
     */
    fun semanticSearchQuizzes(query: String, limit: Int): Flow<List<Quiz>>

    /**
     * Hybrid search combining FTS keyword matching with semantic similarity.
     * Results are merged via Reciprocal Rank Fusion.
     *
     * @param query Raw search query text.
     * @param limit Maximum number of results.
     */
    fun hybridSearchQuizzes(query: String, limit: Int): Flow<List<Quiz>>

    /**
     * Finds quizzes semantically similar to the given quiz.
     * Used for "Related Quizzes" on the detail screen.
     *
     * @param quizId The source quiz to find similar quizzes for.
     * @param limit Maximum number of similar quizzes to return.
     */
    fun findSimilarQuizzes(quizId: String, limit: Int = 6): Flow<List<Quiz>>
}
