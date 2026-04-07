package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Upsert
import androidx.room.Query
import androidx.room.Update
import com.example.androidapp.data.local.entity.AttemptEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Attempt entities.
 * Provides methods to query, insert, update, and delete quiz attempts in the local database.
 */
@Dao
interface AttemptDao {

    /**
     * Get all attempts for a specific user, ordered by start time (most recent first).
     */
    @Query("SELECT * FROM attempts WHERE user_id = :userId ORDER BY started_at DESC")
    fun getAttemptsByUser(userId: String): Flow<List<AttemptEntity>>

    /**
     * Get all attempts for a specific quiz.
     */
    @Query("SELECT * FROM attempts WHERE quiz_id = :quizId ORDER BY started_at DESC")
    fun getAttemptsByQuiz(quizId: String): Flow<List<AttemptEntity>>

    /**
     * Get a specific attempt by ID.
     */
    @Query("SELECT * FROM attempts WHERE id = :attemptId")
    suspend fun getAttemptById(attemptId: String): AttemptEntity?

    /**
     * Get the most recent attempt for a user on a specific quiz.
     */
    @Query(
        """
        SELECT * FROM attempts
        WHERE user_id = :userId AND quiz_id = :quizId
        ORDER BY started_at DESC
        LIMIT 1
    """
    )
    suspend fun getLatestAttempt(userId: String, quizId: String): AttemptEntity?

    /**
     * Insert an attempt, or update it if it already exists.
     */
    @Upsert
    suspend fun insertAttempt(attempt: AttemptEntity)

    /**
     * Update an existing attempt.
     */
    @Update
    suspend fun updateAttempt(attempt: AttemptEntity)

    /**
     * Delete an attempt.
     */
    @Delete
    suspend fun deleteAttempt(attempt: AttemptEntity)

    /**
     * Get the total number of attempts for a specific user.
     * Returns a Flow that emits the count whenever it changes.
     */
    @Query("SELECT COUNT(*) FROM attempts WHERE user_id = :userId")
    fun getAttemptCountByUser(userId: String): Flow<Int>

    /**
     * Update the userId for all attempts belonging to a guest.
     * Returns the number of rows affected.
     */
    @Query("UPDATE attempts SET user_id = :newUserId WHERE user_id = :guestId")
    suspend fun updateUserId(guestId: String, newUserId: String): Int

    // ==================== Paginated queries ====================

    /**
     * Get attempts for a user with a dynamic limit for pagination.
     * Used by the History screen to incrementally load attempt history.
     */
    @Query("SELECT * FROM attempts WHERE user_id = :userId ORDER BY started_at DESC LIMIT :limit")
    fun getAttemptsByUserLimited(userId: String, limit: Int): Flow<List<AttemptEntity>>

    /**
     * Get the total count of attempts for a user.
     * Used by pagination to determine if more items are available.
     * This is a one-shot suspend query (not a Flow).
     */
    @Query("SELECT COUNT(*) FROM attempts WHERE user_id = :userId")
    suspend fun getAttemptCountByUserOnce(userId: String): Int
}
