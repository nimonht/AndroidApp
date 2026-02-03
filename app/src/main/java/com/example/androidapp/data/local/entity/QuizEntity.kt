package com.example.androidapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Quiz stored locally for offline cache.
 * Maps to the 'quizzes' table in the local SQLite database.
 */
@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    val title: String,

    val description: String? = null,

    @ColumnInfo(name = "is_public")
    val isPublic: Boolean = false,

    @ColumnInfo(name = "share_code")
    val shareCode: String? = null,

    val tags: String = "", // Stored as comma-separated values

    val checksum: String? = null,

    @ColumnInfo(name = "question_count")
    val questionCount: Int = 0,

    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.SYNCED.name
)

/**
 * Represents the synchronization status of local data with the cloud.
 */
enum class SyncStatus {
    PENDING,    // Changes waiting to be synced
    SYNCING,    // Currently syncing
    SYNCED,     // Successfully synced with cloud
    FAILED      // Sync failed, needs retry
}
