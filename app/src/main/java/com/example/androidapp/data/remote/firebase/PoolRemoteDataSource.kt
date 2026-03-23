package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.QuestionPoolItemDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Remote data source for Firestore operations on the question pool collection.
 *
 * @property firestore The Firestore instance for database operations.
 */
class PoolRemoteDataSource(private val firestore: FirebaseFirestore) {

    /**
     * Adds a pool item to Firestore. Uses the item's ID as the document ID if present,
     * otherwise lets Firestore auto-generate one.
     *
     * @param poolItemDto The DTO to persist.
     */
    suspend fun addPoolItem(poolItemDto: QuestionPoolItemDto) {
        val docId = poolItemDto.id.ifBlank { null }
        val ref = if (docId != null) {
            firestore.collection(FirestoreCollections.QUESTION_POOL).document(docId)
        } else {
            firestore.collection(FirestoreCollections.QUESTION_POOL).document()
        }
        ref.set(poolItemDto).await()
    }

    /**
     * Queries pool items that match any of the given tags.
     *
     * @param tags The tags to filter by (at most [MAX_TAG_QUERY_LIMIT]).
     * @return A list of matching [QuestionPoolItemDto] objects.
     * @throws IllegalArgumentException if [tags] exceeds [MAX_TAG_QUERY_LIMIT].
     */
    suspend fun getPoolItemsByTags(tags: List<String>): List<QuestionPoolItemDto> {
        if (tags.isEmpty()) return emptyList()
        require(tags.size <= MAX_TAG_QUERY_LIMIT) {
            "getPoolItemsByTags accepts at most $MAX_TAG_QUERY_LIMIT tags; received ${tags.size}."
        }
        return firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereArrayContainsAny("tags", tags)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuestionPoolItemDto::class.java) }
    }

    /**
     * Queries active pool items that match any of the given tags.
     *
     * @param tags The tags to filter by (at most [MAX_TAG_QUERY_LIMIT]).
     * @return A list of active [QuestionPoolItemDto] objects matching the tags.
     */
    suspend fun getActivePoolItemsByTags(tags: List<String>): List<QuestionPoolItemDto> {
        if (tags.isEmpty()) return emptyList()
        require(tags.size <= MAX_TAG_QUERY_LIMIT) {
            "getActivePoolItemsByTags accepts at most $MAX_TAG_QUERY_LIMIT tags; received ${tags.size}."
        }
        return firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereArrayContainsAny("tags", tags)
            .whereEqualTo(FirestoreCollections.Fields.IS_ACTIVE, true)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuestionPoolItemDto::class.java) }
    }

    /**
     * Queries all pool items contributed by a specific user.
     *
     * @param userId The author's user ID.
     * @return A list of [QuestionPoolItemDto] objects authored by the user.
     */
    suspend fun getContributionsByUser(userId: String): List<QuestionPoolItemDto> {
        return firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereEqualTo(FirestoreCollections.Fields.AUTHOR_ID, userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuestionPoolItemDto::class.java) }
    }

    /**
     * Deletes a pool item document from Firestore.
     *
     * @param poolItemId The ID of the pool item to delete.
     */
    suspend fun deletePoolItem(poolItemId: String) {
        firestore.collection(FirestoreCollections.QUESTION_POOL)
            .document(poolItemId)
            .delete()
            .await()
    }

    /**
     * Atomically increments the usage count of a pool item by 1.
     *
     * @param poolItemId The ID of the pool item to update.
     */
    suspend fun incrementUsageCount(poolItemId: String) {
        firestore.collection(FirestoreCollections.QUESTION_POOL)
            .document(poolItemId)
            .update("usageCount", FieldValue.increment(1))
            .await()
    }

    /**
     * Updates the active status of a pool item.
     *
     * @param poolItemId The ID of the pool item to update.
     * @param isActive The new active status.
     */
    suspend fun setPoolItemActive(poolItemId: String, isActive: Boolean) {
        firestore.collection(FirestoreCollections.QUESTION_POOL)
            .document(poolItemId)
            .update(FirestoreCollections.Fields.IS_ACTIVE, isActive)
            .await()
    }

    companion object {
        /** Maximum number of tags accepted by tag-based queries (Firestore whereArrayContainsAny limit). */
        const val MAX_TAG_QUERY_LIMIT = 10
    }
}

