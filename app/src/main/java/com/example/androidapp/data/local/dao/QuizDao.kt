package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Update
import com.example.androidapp.data.local.entity.QuizEmbeddingProjection
import com.example.androidapp.data.local.entity.QuizEntity
import com.example.androidapp.data.local.entity.QuizIndexProjection
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
    @Query(
        """
        SELECT * FROM quizzes
        WHERE owner_id = :userId AND deleted_at IS NULL
        ORDER BY updated_at DESC
    """
    )
    fun getQuizzesByOwner(userId: String): Flow<List<QuizEntity>>

    /**
     * Get quizzes recently attempted by the user.
     */
    @Query(
        """
        SELECT q.* FROM quizzes q
        INNER JOIN attempts a ON q.id = a.quiz_id
        WHERE a.user_id = :userId AND q.deleted_at IS NULL
        GROUP BY q.id
        ORDER BY MAX(a.started_at) DESC
        LIMIT 10
    """
    )
    fun getRecentAttemptQuizzes(userId: String): Flow<List<QuizEntity>>

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
     * Get all public quizzes that are not deleted (one-time, non-reactive).
     * Used for comparing local cache against remote set during stale quiz cleanup.
     */
    @Query("SELECT * FROM quizzes WHERE is_public = 1 AND deleted_at IS NULL")
    suspend fun getPublicQuizzesOnce(): List<QuizEntity>

    /**
     * Get all quizzes owned by a specific user that are not deleted (one-time, non-reactive).
     * Used for comparing local cache against remote set during stale quiz cleanup.
     */
    @Query("SELECT * FROM quizzes WHERE owner_id = :userId AND deleted_at IS NULL")
    suspend fun getQuizzesByOwnerOnce(userId: String): List<QuizEntity>

    /**
     * Marks a quiz as removed from the cloud (or clears the flag).
     * Used when a sync detects an owner's quiz no longer exists on Firestore
     * (e.g. deleted by an admin) so the user can be warned.
     */
    @Query("UPDATE quizzes SET is_removed_from_cloud = :isRemoved WHERE id = :quizId")
    suspend fun markRemovedFromCloud(quizId: String, isRemoved: Boolean)

    /**
     * Permanently delete a quiz by its ID.
     */
    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuizById(quizId: String)

    /**
     * Get deleted quizzes (recycle bin) for a user.
     */
    @Query(
        """
        SELECT * FROM quizzes
        WHERE owner_id = :userId AND deleted_at IS NOT NULL
        ORDER BY deleted_at DESC
    """
    )
    fun getDeletedQuizzes(userId: String): Flow<List<QuizEntity>>

    /**
     * Upsert a quiz, replacing if it already exists.
     */
    @Upsert
    suspend fun upsertQuiz(quiz: QuizEntity)

    /**
     * Update an existing quiz.
     */
    @Update
    suspend fun updateQuiz(quiz: QuizEntity)

    /**
     * Updates all quiz metadata fields WITHOUT touching the embedding columns.
     * Used by sync operations to preserve locally-computed embeddings when
     * refreshing quiz data from Firestore.
     *
     * @return Number of rows affected (0 if quiz does not exist locally).
     */
    @Query(
        """
        UPDATE quizzes SET
            owner_id = :ownerId,
            title = :title,
            description = :description,
            author_name = :authorName,
            thumbnail_url = :thumbnailUrl,
            is_public = :isPublic,
            is_draft = :isDraft,
            share_code = :shareCode,
            tags = :tags,
            checksum = :checksum,
            question_count = :questionCount,
            attempt_count = :attemptCount,
            created_at = :createdAt,
            updated_at = :updatedAt,
            deleted_at = :deletedAt,
            sync_status = :syncStatus,
            is_removed_from_cloud = :isRemovedFromCloud
        WHERE id = :id
    """
    )
    suspend fun updateQuizMetadata(
        id: String,
        ownerId: String,
        title: String,
        description: String?,
        authorName: String,
        thumbnailUrl: String?,
        isPublic: Boolean,
        isDraft: Boolean,
        shareCode: String?,
        tags: String,
        checksum: String?,
        questionCount: Int,
        attemptCount: Int,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        syncStatus: String,
        isRemovedFromCloud: Boolean
    ): Int

    /**
     * Update the sync status of a quiz.
     */
    @Query("UPDATE quizzes SET sync_status = :status WHERE id = :quizId")
    suspend fun updateSyncStatus(quizId: String, status: String)

    /**
     * Update the content checksum of a quiz.
     */
    @Query("UPDATE quizzes SET checksum = :checksum WHERE id = :quizId")
    suspend fun updateChecksum(quizId: String, checksum: String)

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
     * Search quizzes by title or tags.
     */
    @Query(
        """
        SELECT * FROM quizzes
        WHERE deleted_at IS NULL
        AND (title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY updated_at DESC
    """
    )
    fun searchQuizzes(query: String): Flow<List<QuizEntity>>

    /**
     * Atomically increment the attempt count for a quiz.
     */
    @Query("UPDATE quizzes SET attempt_count = attempt_count + 1 WHERE id = :quizId")
    suspend fun incrementAttemptCount(quizId: String)

    // ==================== Paginated queries ====================

    /**
     * Get public quizzes with a dynamic limit for pagination.
     * Used by search/discover screens to incrementally load public quizzes.
     */
    @Query("SELECT * FROM quizzes WHERE is_public = 1 AND deleted_at IS NULL ORDER BY attempt_count DESC LIMIT :limit")
    fun getPublicQuizzesLimited(limit: Int): Flow<List<QuizEntity>>

    /**
     * Get quizzes owned by a user with a dynamic limit for pagination.
     */
    @Query(
        """
        SELECT * FROM quizzes
        WHERE owner_id = :userId AND deleted_at IS NULL
        ORDER BY updated_at DESC
        LIMIT :limit
    """
    )
    fun getQuizzesByOwnerLimited(userId: String, limit: Int): Flow<List<QuizEntity>>

    /**
     * Search quizzes with a dynamic limit for pagination.
     */
    @Query(
        """
        SELECT * FROM quizzes
        WHERE deleted_at IS NULL
        AND (title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY updated_at DESC
        LIMIT :limit
    """
    )
    fun searchQuizzesLimited(query: String, limit: Int): Flow<List<QuizEntity>>

    /**
     * Get deleted quizzes (recycle bin) with a dynamic limit for pagination.
     */
    @Query(
        """
        SELECT * FROM quizzes
        WHERE owner_id = :userId AND deleted_at IS NOT NULL
        ORDER BY deleted_at DESC
        LIMIT :limit
    """
    )
    fun getDeletedQuizzesLimited(userId: String, limit: Int): Flow<List<QuizEntity>>

    /**
     * Get the total count of public non-deleted quizzes.
     * Used by pagination to determine if more items are available.
     */
    @Query("SELECT COUNT(*) FROM quizzes WHERE is_public = 1 AND deleted_at IS NULL")
    suspend fun getPublicQuizzesCount(): Int

    /**
     * Get the total count of quizzes owned by a user.
     */
    @Query("SELECT COUNT(*) FROM quizzes WHERE owner_id = :userId AND deleted_at IS NULL")
    suspend fun getQuizzesByOwnerCount(userId: String): Int

    /**
     * Get the total count of search results.
     */
    @Query(
        """
        SELECT COUNT(*) FROM quizzes
        WHERE deleted_at IS NULL
        AND (title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
    """
    )
    suspend fun searchQuizzesCount(query: String): Int

    /**
     * Get the total count of deleted quizzes for a user.
     */
    @Query("SELECT COUNT(*) FROM quizzes WHERE owner_id = :userId AND deleted_at IS NOT NULL")
    suspend fun getDeletedQuizzesCount(userId: String): Int

    /**
     * Returns only the [tags] column for all non-deleted quizzes.
     * Used by [com.example.androidapp.data.repository.QuizRepositoryImpl.getAllTags]
     * to extract distinct tags without loading full quiz rows into memory.
     */
    @Query("SELECT tags FROM quizzes WHERE deleted_at IS NULL")
    suspend fun getAllTagStrings(): List<String>

    // ==================== FTS search  ====================

    /**
     * Full-text search using FTS4 virtual table with MATCH syntax.
     * Much faster than LIKE for large datasets due to inverted index.
     *
     * @param query FTS MATCH query. Supports prefix: "koth*".
     * @param limit Maximum results.
     */
    @Query(
        """
        SELECT q.* FROM quizzes q
        JOIN quizzes_fts ON quizzes_fts.rowid = q.rowid
        WHERE quizzes_fts MATCH :query
        AND q.deleted_at IS NULL
        ORDER BY q.updated_at DESC
        LIMIT :limit
    """
    )
    fun searchQuizzesFts(query: String, limit: Int): Flow<List<QuizEntity>>

    /**
     * Count of full-text search results using FTS4 MATCH.
     */
    @Query(
        """
        SELECT COUNT(*) FROM quizzes q
        JOIN quizzes_fts ON quizzes_fts.rowid = q.rowid
        WHERE quizzes_fts MATCH :query
        AND q.deleted_at IS NULL
    """
    )
    suspend fun searchQuizzesFtsCount(query: String): Int

    // ==================== Embedding queries ====================

    /**
     * Returns quiz IDs and their embeddings for the in-memory cache.
     * Only returns embeddings computed with the specified model version.
     */
    @Query(
        """
        SELECT id, embedding FROM quizzes
        WHERE deleted_at IS NULL
          AND embedding IS NOT NULL
          AND embedding_version = :modelVersion
    """
    )
    suspend fun getAllEmbeddings(modelVersion: Int): List<QuizEmbeddingProjection>

    /**
     * Returns quizzes that need embedding (re-)generation.
     * Selects quizzes with null embedding or outdated model version.
     */
    @Query(
        """
        SELECT id, title, description, tags FROM quizzes
        WHERE deleted_at IS NULL
          AND (embedding IS NULL OR embedding_version < :currentModelVersion)
        LIMIT :batchSize
    """
    )
    suspend fun getQuizzesNeedingEmbedding(
        currentModelVersion: Int,
        batchSize: Int
    ): List<QuizIndexProjection>

    /**
     * Returns embedding metadata for a specific quiz if it needs (re-)generation.
     * Returns null if the quiz already has an up-to-date embedding or does not exist.
     */
    @Query(
        """
        SELECT id, title, description, tags FROM quizzes
        WHERE id = :quizId
          AND deleted_at IS NULL
          AND (embedding IS NULL OR embedding_version < :currentModelVersion)
    """
    )
    suspend fun getQuizNeedingEmbeddingById(
        quizId: String,
        currentModelVersion: Int
    ): QuizIndexProjection?

    /**
     * Saves a computed embedding for a quiz.
     */
    @Query("UPDATE quizzes SET embedding = :embedding, embedding_version = :version WHERE id = :id")
    suspend fun updateEmbedding(id: String, embedding: ByteArray, version: Int)

    /**
     * Fetches full quiz rows by a list of IDs for reassembly after
     * Reciprocal Rank Fusion merges results from multiple search backends.
     */
    @Query("SELECT * FROM quizzes WHERE id IN (:ids) AND deleted_at IS NULL")
    suspend fun getQuizzesByIds(ids: List<String>): List<QuizEntity>
}
