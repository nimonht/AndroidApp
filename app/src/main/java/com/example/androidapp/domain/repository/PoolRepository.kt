package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.PaginatedResult
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.QuestionPoolItem

/**
 * Repository interface for the shared question pool.
 *
 * The question pool allows users to contribute questions for reuse across quizzes.
 * Pool items are stored remotely only (no local Room caching).
 */
interface PoolRepository {

    /**
     * Contributes a single question to the pool.
     *
     * @param poolItem The [QuestionPoolItem] to add.
     * @return [Result] indicating success or failure.
     */
    suspend fun contributeQuestion(poolItem: QuestionPoolItem): Result<Unit>

    /**
     * Contributes multiple questions from a quiz to the pool.
     *
     * Each question is wrapped into a [QuestionPoolItem] with the given tags.
     * If [anonymize] is true, the contributor ID is treated as anonymized (stored as `null`).
     *
     * @param questions The list of [Question] objects to contribute.
     * @param contributorId The user ID of the contributor; mapped to a nullable contributor ID in the model.
     * @param sourceQuizId The ID of the quiz from which these questions originated.
     * @param tags The tags to assign to each pool item.
     * @param anonymize If true, the contributor ID is cleared (set to `null` in the underlying model) for privacy.
     * @return [Result] indicating success or failure.
     */
    suspend fun contributeQuestions(
        questions: List<Question>,
        contributorId: String,
        sourceQuizId: String,
        tags: List<String>,
        anonymize: Boolean = false
    ): Result<Unit>

    /**
     * Queries active pool questions filtered by tags.
     *
     * @param tags The tags to filter by.
     * @param activeOnly If true, returns only items with `isActive == true`.
     * @return [Result] containing the list of matching [QuestionPoolItem] objects.
     */
    suspend fun getPoolQuestionsByTags(
        tags: List<String>,
        activeOnly: Boolean = true
    ): Result<List<QuestionPoolItem>>

    /**
     * Lists all questions contributed by a specific user.
     *
     * @param userId The user's ID.
     * @return [Result] containing the list of the user's [QuestionPoolItem] contributions.
     */
    suspend fun getMyContributions(userId: String): Result<List<QuestionPoolItem>>

    /**
     * Revokes a contribution by setting `isActive = false`.
     *
     * The pool item is not deleted, allowing it to be reactivated if needed.
     *
     * @param poolItemId The ID of the pool item to revoke.
     * @return [Result] indicating success or failure.
     */
    suspend fun revokeContribution(poolItemId: String): Result<Unit>

    /**
     * Atomically increments the usage count of a pool item by 1.
     *
     * Called when a pool question is used in an auto-generated quiz.
     *
     * @param poolItemId The ID of the pool item to update.
     * @return [Result] indicating success or failure.
     */
    suspend fun incrementUsageCount(poolItemId: String): Result<Unit>

    /**
     * Selects random questions from the pool filtered by tags for auto-generating a quiz.
     *
     * Only active pool items are considered. If fewer items are available than [count],
     * all available items are returned.
     *
     * @param tags The tags to filter pool questions by.
     * @param count The desired number of questions.
     * @return [Result] containing the selected [QuestionPoolItem] objects.
     */
    suspend fun autoGenerateQuiz(tags: List<String>, count: Int): Result<List<QuestionPoolItem>>

    // ==================== Paginated queries ====================

    /**
     * Lists contributed questions by the user with cursor-based pagination.
     * Pass [loadMore] = false to reset to the first page.
     *
     * @param userId The user's ID.
     * @param pageSize Number of items per page.
     * @param loadMore If true, fetches the next page; if false, resets to first page.
     * @return [Result] wrapping a [PaginatedResult] with contributions and hasMore flag.
     */
    suspend fun getMyContributionsPaged(
        userId: String,
        pageSize: Int,
        loadMore: Boolean = false
    ): Result<PaginatedResult<QuestionPoolItem>>

    /**
     * Queries pool questions by tags with cursor-based pagination.
     *
     * @param tags The tags to filter by.
     * @param activeOnly If true, returns only active items.
     * @param pageSize Number of items per page.
     * @param loadMore If true, fetches the next page; if false, resets to first page.
     * @return [Result] wrapping a [PaginatedResult] with pool items and hasMore flag.
     */
    suspend fun getPoolQuestionsByTagsPaged(
        tags: List<String>,
        activeOnly: Boolean = true,
        pageSize: Int,
        loadMore: Boolean = false
    ): Result<PaginatedResult<QuestionPoolItem>>
}
