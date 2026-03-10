package com.example.androidapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking pending synchronization operations.
 * Maps to the 'pending_sync_operations' table in the local SQLite database.
 *
 * Used to queue local changes (create, update, delete) that need
 * to be synced with the cloud when connectivity is available.
 */
@Entity(tableName = "pending_sync_operations")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "entity_type")
    val entityType: String, // QUIZ, QUESTION, CHOICE, ATTEMPT

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    val operation: String, // CREATE, UPDATE, DELETE

    val payload: String = "", // JSON payload containing data to sync

    val status: String = PendingSyncStatus.PENDING.name,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "max_retries")
    val maxRetries: Int = 3,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null
)

/**
 * Represents the type of entity that needs to be synced.
 */
enum class SyncEntityType {
    QUIZ,
    QUESTION,
    CHOICE,
    ATTEMPT
}

/**
 * Represents the type of sync operation.
 */
enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Represents the status of a pending sync operation.
 */
enum class PendingSyncStatus {
    PENDING,      // Waiting to be synced
    IN_PROGRESS,  // Currently being synced
    FAILED,       // Sync failed, may retry
    COMPLETED     // Successfully synced
}
