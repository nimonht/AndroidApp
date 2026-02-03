package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    @Query("""
        SELECT * FROM attempts 
        WHERE user_id = :userId AND quiz_id = :quizId 
        ORDER BY started_at DESC 
        LIMIT 1
    """)
    suspend fun getLatestAttempt(userId: String, quizId: String): AttemptEntity?

    /**
     * Get in-progress attempts (not finished) for a user.
     */
    @Query("SELECT * FROM attempts WHERE user_id = :userId AND finished_at IS NULL")
    suspend fun getInProgressAttempts(userId: String): List<AttemptEntity>

    /**
     * Get the count of completed attempts for a quiz.
     */
    @Query("SELECT COUNT(*) FROM attempts WHERE quiz_id = :quizId AND finished_at IS NOT NULL")
    suspend fun getAttemptCount(quizId: String): Int

    /**
     * Get the average score for a quiz.
     */
    @Query("""
        SELECT AVG(CAST(score AS FLOAT) / max_score * 100) 
        FROM attempts 
        WHERE quiz_id = :quizId AND finished_at IS NOT NULL AND max_score > 0
    """)
    suspend fun getAverageScore(quizId: String): Float?

    /**
     * Insert an attempt, replacing if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
     * Delete all attempts for a quiz.
     */
    @Query("DELETE FROM attempts WHERE quiz_id = :quizId")
    suspend fun deleteAttemptsByQuizId(quizId: String)
}
