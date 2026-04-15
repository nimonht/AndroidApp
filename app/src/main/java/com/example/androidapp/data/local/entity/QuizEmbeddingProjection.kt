package com.example.androidapp.data.local.entity

import androidx.room.ColumnInfo

/**
 * Room projection class for loading only quiz ID and embedding vector
 * from the [QuizEntity] table. Used by [EmbeddingCache] to populate
 * the in-memory vector index without loading full quiz rows.
 */
data class QuizEmbeddingProjection(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "embedding") val embedding: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuizEmbeddingProjection) return false
        return id == other.id && embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Room projection class for loading quiz metadata needed to generate
 * an embedding. Used by [EmbeddingIndexWorker] to fetch un-indexed quizzes.
 */
data class QuizIndexProjection(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "tags") val tags: String
)
