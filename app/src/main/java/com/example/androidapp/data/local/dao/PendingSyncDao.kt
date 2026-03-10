package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.androidapp.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PendingSyncEntity.
 * Provides methods to manage the pending sync operations queue.
 */
@Dao
interface PendingSyncDao {

    /**
     * Get all pending operations that are ready to sync.
     * Returns operations with PENDING or FAILED status that haven't exceeded retry limit.
     * Ordered by creation time (oldest first = FIFO queue).
     */
    @Query("""
        SELECT * FROM pending_sync_operations 
        WHERE status IN ('PENDING', 'FAILED') AND retry_count < max_retries 
        ORDER BY created_at ASC
    """)
    suspend fun getPendingOperations(): List<PendingSyncEntity>

    /**
     * Get all operations for a specific entity.
     */
    @Query("""
        SELECT * FROM pending_sync_operations 
        WHERE entity_type = :entityType AND entity_id = :entityId 
        ORDER BY created_at ASC
    """)
    suspend fun getOperationsByEntity(entityType: String, entityId: String): List<PendingSyncEntity>

    /**
     * Get all operations filtered by status.
     */
    @Query("SELECT * FROM pending_sync_operations WHERE status = :status ORDER BY created_at ASC")
    suspend fun getOperationsByStatus(status: String): List<PendingSyncEntity>

    /**
     * Get a single operation by ID.
     */
    @Query("SELECT * FROM pending_sync_operations WHERE id = :id")
    suspend fun getOperationById(id: Long): PendingSyncEntity?

    /**
     * Get the count of pending operations (PENDING or FAILED within retry limit).
     */
    @Query("""
        SELECT COUNT(*) FROM pending_sync_operations 
        WHERE status IN ('PENDING', 'FAILED') AND retry_count < max_retries
    """)
    suspend fun getPendingCount(): Int

    /**
     * Observe the count of pending operations as a Flow.
     * Useful for showing sync status indicators in the UI.
     */
    @Query("""
        SELECT COUNT(*) FROM pending_sync_operations 
        WHERE status IN ('PENDING', 'FAILED') AND retry_count < max_retries
    """)
    fun observePendingCount(): Flow<Int>

    /**
     * Insert a new pending sync operation.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingSyncEntity): Long

    /**
     * Insert multiple pending sync operations.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperations(operations: List<PendingSyncEntity>)

    /**
     * Update an existing operation.
     */
    @Update
    suspend fun updateOperation(operation: PendingSyncEntity)

    /**
     * Update the status and last attempt timestamp of an operation.
     */
    @Query("""
        UPDATE pending_sync_operations 
        SET status = :status, last_attempt_at = :lastAttemptAt 
        WHERE id = :id
    """)
    suspend fun updateStatus(id: Long, status: String, lastAttemptAt: Long = System.currentTimeMillis())

    /**
     * Increment the retry count and update the error message for a failed operation.
     */
    @Query("""
        UPDATE pending_sync_operations 
        SET retry_count = retry_count + 1, 
            error_message = :errorMessage, 
            status = 'FAILED',
            last_attempt_at = :lastAttemptAt 
        WHERE id = :id
    """)
    suspend fun incrementRetryCount(id: Long, errorMessage: String?, lastAttemptAt: Long = System.currentTimeMillis())

    /**
     * Delete an operation.
     */
    @Delete
    suspend fun deleteOperation(operation: PendingSyncEntity)

    /**
     * Delete all completed operations.
     */
    @Query("DELETE FROM pending_sync_operations WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedOperations()

    /**
     * Delete all operations for a specific entity.
     * Useful when the entity is permanently deleted.
     */
    @Query("DELETE FROM pending_sync_operations WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun deleteByEntity(entityType: String, entityId: String)

    /**
     * Delete all operations (reset sync queue).
     */
    @Query("DELETE FROM pending_sync_operations")
    suspend fun deleteAllOperations()
}
