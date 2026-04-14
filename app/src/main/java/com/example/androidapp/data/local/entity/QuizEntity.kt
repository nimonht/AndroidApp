package com.example.androidapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a Quiz stored locally for offline cache.
 * Maps to the 'quizzes' table in the local SQLite database.
 */
@Entity(
    tableName = "quizzes",
    indices = [
        Index(value = ["is_public", "deleted_at", "attempt_count"]),
        Index(value = ["owner_id", "deleted_at", "updated_at"]),
        Index(value = ["deleted_at", "updated_at"]),
        Index(value = ["deleted_at", "title"])
    ]
)
data class QuizEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    val title: String,

    val description: String? = null,

    @ColumnInfo(name = "author_name")
    val authorName: String = "",

    @ColumnInfo(name = "is_public")
    val isPublic: Boolean = false,

    @ColumnInfo(name = "is_draft", defaultValue = "0")
    val isDraft: Boolean = false,

    @ColumnInfo(name = "share_code")
    val shareCode: String? = null,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

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
    val syncStatus: String = SyncStatus.SYNCED.name,

    /**
     * True when the quiz was permanently removed from Firestore (e.g. by an
     * admin) but still exists in the local Room database. The user is warned
     * that only they can see this quiz and should delete it manually.
     */
    @ColumnInfo(name = "is_removed_from_cloud", defaultValue = "0")
    val isRemovedFromCloud: Boolean = false
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
