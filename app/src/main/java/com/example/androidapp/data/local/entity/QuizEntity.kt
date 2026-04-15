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
        Index(value = ["deleted_at", "title"]),
        Index(value = ["embedding_version", "deleted_at"])
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

    /**
     * Dense vector embedding of the quiz text content (title + description + tags),
     * stored as a little-endian IEEE 754 float array serialized to bytes.
     * Null when the quiz has not yet been indexed by [EmbeddingIndexWorker].
     */
    @ColumnInfo(name = "embedding")
    val embedding: ByteArray? = null,

    /**
     * Version of the embedding model used to compute [embedding].
     * Allows the [EmbeddingIndexWorker] to re-index quizzes when the model
     * is upgraded without requiring a full database wipe.
     */
    @ColumnInfo(name = "embedding_version", defaultValue = "0")
    val embeddingVersion: Int = 0,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.SYNCED.name,

    /**
     * True when the quiz was permanently removed from Firestore (e.g. by an
     * admin) but still exists in the local Room database. The user is warned
     * that only they can see this quiz and should delete it manually.
     */
    @ColumnInfo(name = "is_removed_from_cloud", defaultValue = "0")
    val isRemovedFromCloud: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuizEntity) return false
        return id == other.id &&
                ownerId == other.ownerId &&
                title == other.title &&
                description == other.description &&
                authorName == other.authorName &&
                isPublic == other.isPublic &&
                isDraft == other.isDraft &&
                shareCode == other.shareCode &&
                thumbnailUrl == other.thumbnailUrl &&
                tags == other.tags &&
                checksum == other.checksum &&
                questionCount == other.questionCount &&
                attemptCount == other.attemptCount &&
                createdAt == other.createdAt &&
                updatedAt == other.updatedAt &&
                deletedAt == other.deletedAt &&
                embedding.contentEquals(other.embedding) &&
                embeddingVersion == other.embeddingVersion &&
                syncStatus == other.syncStatus &&
                isRemovedFromCloud == other.isRemovedFromCloud
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + ownerId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + authorName.hashCode()
        result = 31 * result + isPublic.hashCode()
        result = 31 * result + isDraft.hashCode()
        result = 31 * result + (shareCode?.hashCode() ?: 0)
        result = 31 * result + (thumbnailUrl?.hashCode() ?: 0)
        result = 31 * result + tags.hashCode()
        result = 31 * result + (checksum?.hashCode() ?: 0)
        result = 31 * result + questionCount
        result = 31 * result + attemptCount
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + (deletedAt?.hashCode() ?: 0)
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + embeddingVersion
        result = 31 * result + syncStatus.hashCode()
        result = 31 * result + isRemovedFromCloud.hashCode()
        return result
    }
}

/**
 * Represents the synchronization status of local data with the cloud.
 */
enum class SyncStatus {
    PENDING,    // Changes waiting to be synced
    SYNCING,    // Currently syncing
    SYNCED,     // Successfully synced with cloud
    FAILED      // Sync failed, needs retry
}
