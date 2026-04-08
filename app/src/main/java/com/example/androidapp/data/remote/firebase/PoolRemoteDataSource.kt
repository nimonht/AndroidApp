package com.example.androidapp.data.remote.firebase

import android.util.Log
import com.example.androidapp.data.remote.model.QuestionPoolItemDto
import com.google.firebase.firestore.DocumentSnapshot
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
     * @param userId The contributor's user ID.
     * @return A list of [QuestionPoolItemDto] objects contributed by the user.
     */
    suspend fun getContributionsByUser(userId: String): List<QuestionPoolItemDto> {
        return firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereEqualTo(FirestoreCollections.Fields.CONTRIBUTOR_ID, userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuestionPoolItemDto::class.java) }
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
     * Updates the active status of a pool item and verifies the write persisted.
     *
     * After the Firestore `update()` call completes, a read-back is performed to
     * confirm the field value matches the requested state. If the document does not
     * exist or the value does not match, an [IllegalStateException] is thrown so the
     * caller can surface the failure to the user.
     *
     * @param poolItemId The ID of the pool item to update.
     * @param isActive The new active status.
     * @throws IllegalStateException if the document is missing or the persisted value
     *   does not match [isActive] after the update.
     */
    suspend fun setPoolItemActive(poolItemId: String, isActive: Boolean) {
        val docRef = firestore.collection(FirestoreCollections.QUESTION_POOL)
            .document(poolItemId)

        Log.d(TAG, "setPoolItemActive: updating $poolItemId -> isActive=$isActive")

        docRef.update(
            mapOf(
                FirestoreCollections.Fields.IS_ACTIVE to isActive,
                "active" to isActive
            )
        ).await()

        // Verify the update actually persisted
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            throw IllegalStateException(
                "Pool item $poolItemId does not exist after update"
            )
        }
        val actualValue = snapshot.getBoolean(FirestoreCollections.Fields.IS_ACTIVE)
        Log.d(TAG, "setPoolItemActive: verification read for $poolItemId -> isActive=$actualValue")
        if (actualValue != isActive) {
            throw IllegalStateException(
                "Pool item $poolItemId isActive verification failed: expected $isActive, got $actualValue"
            )
        }
    }

    // ==================== Paginated queries ====================

    /**
     * Queries pool items contributed by a user with cursor-based pagination.
     *
     * @param userId The contributor's user ID.
     * @param pageSize Number of documents to fetch.
     * @param startAfterDoc The last [DocumentSnapshot] from the previous page, or null for the first page.
     * @return A pair of the pool item DTOs and the last [DocumentSnapshot] for cursor continuation.
     */
    suspend fun getContributionsByUserPaged(
        userId: String,
        pageSize: Int,
        startAfterDoc: DocumentSnapshot? = null
    ): Pair<List<QuestionPoolItemDto>, DocumentSnapshot?> {
        var query = firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereEqualTo(FirestoreCollections.Fields.CONTRIBUTOR_ID, userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (startAfterDoc != null) {
            query = query.startAfter(startAfterDoc)
        }

        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { it.toObject(QuestionPoolItemDto::class.java) }
        val lastDoc = snapshot.documents.lastOrNull()
        return Pair(items, lastDoc)
    }

    /**
     * Queries active pool items by tags with cursor-based pagination.
     *
     * @param tags The tags to filter by (at most [MAX_TAG_QUERY_LIMIT]).
     * @param pageSize Number of documents to fetch.
     * @param startAfterDoc The last [DocumentSnapshot] from the previous page, or null for the first page.
     * @return A pair of the pool item DTOs and the last [DocumentSnapshot] for cursor continuation.
     * @throws IllegalArgumentException if [tags] exceeds [MAX_TAG_QUERY_LIMIT].
     */
    suspend fun getActivePoolItemsByTagsPaged(
        tags: List<String>,
        pageSize: Int,
        startAfterDoc: DocumentSnapshot? = null
    ): Pair<List<QuestionPoolItemDto>, DocumentSnapshot?> {
        if (tags.isEmpty()) return Pair(emptyList(), null)
        require(tags.size <= MAX_TAG_QUERY_LIMIT) {
            "getActivePoolItemsByTagsPaged accepts at most $MAX_TAG_QUERY_LIMIT tags; received ${tags.size}."
        }

        var query = firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereArrayContainsAny("tags", tags)
            .whereEqualTo(FirestoreCollections.Fields.IS_ACTIVE, true)
            .limit(pageSize.toLong())

        if (startAfterDoc != null) {
            query = query.startAfter(startAfterDoc)
        }

        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { it.toObject(QuestionPoolItemDto::class.java) }
        val lastDoc = snapshot.documents.lastOrNull()
        return Pair(items, lastDoc)
    }

    companion object {
        private const val TAG = "PoolRemoteDataSource"

        /** Maximum number of tags accepted by tag-based queries (Firestore whereArrayContainsAny limit). */
        const val MAX_TAG_QUERY_LIMIT = 10
    }
}
