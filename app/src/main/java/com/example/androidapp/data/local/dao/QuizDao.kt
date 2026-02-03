package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.androidapp.data.local.entity.QuizEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Quiz entities.
 * Provides methods to query, insert, update, and delete quizzes in the local database.
 */
@Dao
interface QuizDao {

    /**
     * Get all quizzes for a specific user that are not deleted.
     * Results are ordered by updated date (most recent first).
     */
    @Query("""
        SELECT * FROM quizzes 
        WHERE owner_id = :userId AND deleted_at IS NULL 
        ORDER BY updated_at DESC
    """)
    fun getQuizzesByOwner(userId: String): Flow<List<QuizEntity>>

    /**
     * Get all non-deleted quizzes ordered by updated date.
     */
    @Query("SELECT * FROM quizzes WHERE deleted_at IS NULL ORDER BY updated_at DESC")
    fun getAllQuizzes(): Flow<List<QuizEntity>>

    /**
     * Get a single quiz by ID.
     */
    @Query("SELECT * FROM quizzes WHERE id = :quizId")
    suspend fun getQuizById(quizId: String): QuizEntity?

    /**
     * Get a quiz by its share code.
     */
    @Query("SELECT * FROM quizzes WHERE share_code = :shareCode AND deleted_at IS NULL LIMIT 1")
    suspend fun getQuizByShareCode(shareCode: String): QuizEntity?

    /**
     * Get all public quizzes that are not deleted.
     */
    @Query("SELECT * FROM quizzes WHERE is_public = 1 AND deleted_at IS NULL ORDER BY attempt_count DESC")
    fun getPublicQuizzes(): Flow<List<QuizEntity>>

    /**
     * Get quizzes that need to be synced.
     */
    @Query("SELECT * FROM quizzes WHERE sync_status IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedQuizzes(): List<QuizEntity>

    /**
     * Get deleted quizzes (recycle bin) for a user.
     */
    @Query("""
        SELECT * FROM quizzes 
        WHERE owner_id = :userId AND deleted_at IS NOT NULL 
        ORDER BY deleted_at DESC
    """)
    fun getDeletedQuizzes(userId: String): Flow<List<QuizEntity>>

    /**
     * Insert a quiz, replacing if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    /**
     * Insert multiple quizzes.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<QuizEntity>)

    /**
     * Update an existing quiz.
     */
    @Update
    suspend fun updateQuiz(quiz: QuizEntity)

    /**
     * Update the sync status of a quiz.
     */
    @Query("UPDATE quizzes SET sync_status = :status WHERE id = :quizId")
    suspend fun updateSyncStatus(quizId: String, status: String)

    /**
     * Soft delete a quiz by setting deletedAt timestamp.
     */
    @Query("UPDATE quizzes SET deleted_at = :deletedAt WHERE id = :quizId")
    suspend fun softDeleteQuiz(quizId: String, deletedAt: Long = System.currentTimeMillis())

    /**
     * Restore a soft-deleted quiz.
     */
    @Query("UPDATE quizzes SET deleted_at = NULL WHERE id = :quizId")
    suspend fun restoreQuiz(quizId: String)

    /**
     * Permanently delete a quiz.
     */
    @Delete
    suspend fun deleteQuiz(quiz: QuizEntity)

    /**
     * Permanently delete quizzes older than 30 days in recycle bin.
     */
    @Query("DELETE FROM quizzes WHERE deleted_at IS NOT NULL AND deleted_at < :threshold")
    suspend fun deleteOldQuizzes(threshold: Long)

    /**
     * Search quizzes by title or tags.
     */
    @Query("""
        SELECT * FROM quizzes 
        WHERE deleted_at IS NULL 
        AND (title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY updated_at DESC
    """)
    fun searchQuizzes(query: String): Flow<List<QuizEntity>>
}
