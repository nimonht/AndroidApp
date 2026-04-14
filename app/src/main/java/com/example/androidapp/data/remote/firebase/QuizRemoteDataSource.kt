package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.QuestionDto
import com.example.androidapp.data.remote.model.QuizDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Remote data source for quiz and question Firestore operations.
 * Uses [callbackFlow] with [addSnapshotListener] for real-time streams
 * and batch writes for multi-document mutations.
 *
 * Permanent quiz deletions write a lightweight tombstone document to the
 * [FirestoreCollections.QUIZ_DELETIONS] collection so that other clients
 * can detect the removal without re-fetching all quizzes.
 */
class QuizRemoteDataSource(private val firestore: FirebaseFirestore) {

    /**
     * Emits real-time public quizzes ordered by attempt count descending.
     */
    fun getPublicQuizzes(): Flow<List<QuizDto>> = observeQuizzes(
        firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.IS_PUBLIC, true)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
    )

    /**
     * Emits real-time quizzes owned by [userId].
     */
    fun getQuizzesByOwner(userId: String): Flow<List<QuizDto>> = observeQuizzes(
        firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
    )

    /**
     * One-shot fetch of all public non-deleted quizzes using a single [get] call
     * instead of opening a real-time snapshot listener. Preferred over
     * [getPublicQuizzes] for background sync operations that do not need
     * real-time updates.
     */
    suspend fun getPublicQuizzesOnce(): List<QuizDto> {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.IS_PUBLIC, true)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuizDto::class.java) }
    }

    /**
     * One-shot fetch of quizzes owned by [userId] using a single [get] call.
     * Preferred over [getQuizzesByOwner] for background sync operations.
     */
    suspend fun getQuizzesByOwnerOnce(userId: String): List<QuizDto> {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuizDto::class.java) }
    }

    /**
     * Observes a Firestore [query] in real time, mapping each snapshot to a list of [QuizDto].
     * Centralizes the [callbackFlow] + [addSnapshotListener] boilerplate.
     */
    private fun observeQuizzes(query: Query): Flow<List<QuizDto>> = callbackFlow {
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val quizzes = snapshot?.documents?.mapNotNull {
                it.toObject(QuizDto::class.java)
            } ?: emptyList()
            trySend(quizzes)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Fetches a single quiz by ID.
     */
    suspend fun getQuizById(quizId: String): QuizDto? {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .get()
            .await()
            .toObject(QuizDto::class.java)
    }

    /**
     * Fetches a quiz by its share code.
     */
    suspend fun getQuizByShareCode(shareCode: String): QuizDto? {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.SHARE_CODE, shareCode)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject(QuizDto::class.java)
    }

    /**
     * Saves a quiz and its questions using per-question batch writes.
     * Deletes existing choices before saving to avoid stale data.
     * Processes each question individually to ensure choice cleanup and avoid batch size limits.
     * @param quizId The document ID for the quiz.
     * @param quizDto The quiz data to save.
     * @param questionDtos The question list to save in the subcollection.
     */
    suspend fun saveQuiz(quizId: String, quizDto: QuizDto, questionDtos: List<QuestionDto>) {
        val quizRef = firestore.collection(FirestoreCollections.QUIZZES).document(quizId)

        // First, save the quiz document itself
        quizRef.set(quizDto).await()

        // Process each question individually to handle choice deletion
        // This approach avoids batch size limits and ensures stale choices are removed
        questionDtos.forEach { q ->
            val questionRef = quizRef.collection(FirestoreCollections.QUESTIONS).document(q.id)

            // Delete existing choices first to avoid leaving stale documents
            val existingChoices = questionRef
                .collection(FirestoreCollections.CHOICES)
                .get()
                .await()

            val batch = firestore.batch()
            existingChoices.documents.forEach { batch.delete(it.reference) }

            // Save question without embedded choices
            val questionWithoutChoices = q.copy(choices = emptyList())
            batch.set(questionRef, questionWithoutChoices)

            // Save each choice in the choices subcollection
            q.choices.forEach { choice ->
                val choiceRef = questionRef.collection(FirestoreCollections.CHOICES).document(choice.id)
                batch.set(choiceRef, choice)
            }

            batch.commit().await()
        }
    }

    /**
     * Soft-deletes a quiz by setting the deletedAt timestamp.
     */
    suspend fun softDeleteQuiz(quizId: String, deletedAt: com.google.firebase.Timestamp) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(FirestoreCollections.Fields.DELETED_AT, deletedAt)
            .await()
    }

    /**
     * Restores a soft-deleted quiz by clearing deletedAt.
     */
    suspend fun restoreQuiz(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(FirestoreCollections.Fields.DELETED_AT, null)
            .await()
    }

    /**
     * Permanently deletes a quiz document, its questions/choices subcollections,
     * and its associated share-code document. Writes a deletion tombstone so
     * other clients can detect the removal.
     *
     * All document references are collected first, then deleted via batched
     * writes chunked at [FirestoreCollections.BATCH_LIMIT] to stay within
     * Firestore limits. The tombstone is included in the first batch.
     *
     * @param quizId the ID of the quiz to permanently delete.
     */
    suspend fun permanentlyDeleteQuiz(quizId: String) {
        val quizRef = firestore.collection(FirestoreCollections.QUIZZES).document(quizId)

        // Collect subcollection refs (questions + choices)
        val refsToDelete = collectQuizSubcollectionRefs(quizRef).toMutableList()

        // Check for an associated share-code document
        val quizDoc = quizRef.get().await()
        val shareCode = quizDoc.getString(FirestoreCollections.Fields.SHARE_CODE)
        if (!shareCode.isNullOrBlank()) {
            refsToDelete.add(
                firestore.collection(FirestoreCollections.SHARE_CODES)
                    .document(shareCode)
            )
        }

        // The quiz document itself
        refsToDelete.add(quizRef)

        // First batch includes the tombstone write, so reserve 1 operation for it
        val firstChunkLimit = FirestoreCollections.BATCH_LIMIT - 1
        val firstChunk = refsToDelete.take(firstChunkLimit)
        val remaining = refsToDelete.drop(firstChunkLimit)

        // First batch: tombstone + initial deletes
        val firstBatch = firestore.batch()
        val tombstoneRef = firestore
            .collection(FirestoreCollections.QUIZ_DELETIONS)
            .document()
        firstBatch.set(tombstoneRef, buildTombstoneData(quizId))
        firstChunk.forEach { ref -> firstBatch.delete(ref) }
        firstBatch.commit().await()

        // Remaining batches (pure deletes)
        remaining.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { ref -> batch.delete(ref) }
            batch.commit().await()
        }
    }

    /**
     * Atomically increments the attempt count for a quiz.
     */
    suspend fun incrementAttemptCount(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(
                FirestoreCollections.Fields.ATTEMPT_COUNT,
                com.google.firebase.firestore.FieldValue.increment(1)
            )
            .await()
    }

    /**
     * Deletes all soft-deleted quizzes owned by the user permanently from Firestore,
     * including each quiz's questions/choices subcollections and associated share-code
     * documents. Writes a deletion tombstone per quiz so other clients can detect
     * the removals.
     *
     * All document references and tombstone data are collected first, then committed
     * via batched writes chunked at [FirestoreCollections.BATCH_LIMIT].
     */
    suspend fun emptyTrash(userId: String) {
        val deletedQuizzesQuery = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .whereNotEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()

        if (deletedQuizzesQuery.isEmpty) return

        // Collect every ref that needs deleting and every tombstone that needs writing.
        val refsToDelete = mutableListOf<DocumentReference>()
        val tombstones = mutableListOf<HashMap<String, Any>>()

        for (quizDoc in deletedQuizzesQuery.documents) {
            val quizRef = quizDoc.reference

            // Subcollection refs (questions + choices)
            refsToDelete.addAll(collectQuizSubcollectionRefs(quizRef))

            // Associated share-code document
            val shareCode = quizDoc.getString(FirestoreCollections.Fields.SHARE_CODE)
            if (!shareCode.isNullOrBlank()) {
                refsToDelete.add(
                    firestore.collection(FirestoreCollections.SHARE_CODES)
                        .document(shareCode)
                )
            }

            // The quiz document itself
            refsToDelete.add(quizRef)

            // Tombstone data for this quiz
            tombstones.add(buildTombstoneData(quizDoc.id))
        }

        // Interleave tombstone writes and deletes into a flat operation list,
        // then chunk at BATCH_LIMIT.
        data class BatchOp(
            val ref: DocumentReference,
            val tombstoneData: HashMap<String, Any>? = null
        )

        val ops = mutableListOf<BatchOp>()

        // Tombstone writes first (each targets a new document)
        tombstones.forEach { data ->
            val tombstoneRef = firestore
                .collection(FirestoreCollections.QUIZ_DELETIONS)
                .document()
            ops.add(BatchOp(ref = tombstoneRef, tombstoneData = data))
        }

        // Then all deletes
        refsToDelete.forEach { ref ->
            ops.add(BatchOp(ref = ref))
        }

        // Commit in chunks
        ops.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { op ->
                if (op.tombstoneData != null) {
                    batch.set(op.ref, op.tombstoneData)
                } else {
                    batch.delete(op.ref)
                }
            }
            batch.commit().await()
        }
    }

    // ==================== Tombstone query methods ====================

    /**
     * Returns quiz IDs that have been permanently deleted since [sinceTimestamp].
     * Used by [com.example.androidapp.data.sync.QuizInvalidationManager] for
     * incremental invalidation of locally-cached quizzes.
     *
     * Cost: a single Firestore query that typically returns 0-5 documents
     * per sync cycle, making it far cheaper than re-fetching all quizzes.
     *
     * @param sinceTimestamp epoch millis; only tombstones created after this
     *                       time are returned.
     * @return list of deleted quiz IDs (may contain duplicates if multiple
     *         tombstones exist for the same quiz).
     */
    suspend fun getDeletionsSince(sinceTimestamp: Long): List<String> {
        val cutoff = Timestamp(Date(sinceTimestamp))
        val snapshot = firestore.collection(FirestoreCollections.QUIZ_DELETIONS)
            .whereGreaterThan(FirestoreCollections.Fields.DELETED_AT, cutoff)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.getString(FirestoreCollections.Fields.QUIZ_ID)
        }
    }

    /**
     * Writes a single deletion tombstone for the given [quizId].
     * Use this when permanently deleting a quiz outside of
     * [permanentlyDeleteQuiz] or [emptyTrash] (e.g., in maintenance workers
     * that operate directly on Firestore).
     *
     * @param quizId the ID of the quiz being permanently deleted.
     */
    suspend fun writeDeletionTombstone(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZ_DELETIONS)
            .add(buildTombstoneData(quizId))
            .await()
    }

    /**
     * Writes deletion tombstones for multiple quiz IDs in batched writes,
     * chunked to respect Firestore's 500-operation limit.
     *
     * Call this before batch-deleting the quiz documents themselves so that
     * other clients can detect the removal during their next invalidation sweep.
     *
     * @param quizIds the IDs of the quizzes being permanently deleted.
     */
    suspend fun writeDeletionTombstones(quizIds: List<String>) {
        if (quizIds.isEmpty()) return

        quizIds.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { quizId ->
                val tombstoneRef = firestore
                    .collection(FirestoreCollections.QUIZ_DELETIONS)
                    .document()
                batch.set(tombstoneRef, buildTombstoneData(quizId))
            }
            batch.commit().await()
        }
    }

    /**
     * Removes tombstone entries older than [cutoffTimestamp] to prevent
     * unbounded growth of the [FirestoreCollections.QUIZ_DELETIONS] collection.
     * Called by [com.example.androidapp.data.worker.BackendMaintenanceWorker].
     *
     * Tombstones older than the cutoff have served their purpose -- any client
     * that has not synced within that window will fall back to the full stale
     * cleanup in [com.example.androidapp.data.sync.SyncManager.downloadPublicQuizzes].
     *
     * @param cutoffTimestamp epoch millis; tombstones older than this are removed.
     */
    suspend fun cleanupOldTombstones(cutoffTimestamp: Long) {
        val cutoff = Timestamp(Date(cutoffTimestamp))
        val snapshot = firestore.collection(FirestoreCollections.QUIZ_DELETIONS)
            .whereLessThan(FirestoreCollections.Fields.DELETED_AT, cutoff)
            .get()
            .await()

        if (snapshot.isEmpty) return

        snapshot.documents.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    /**
     * Collects all subcollection document references (questions + choices)
     * under the given quiz document. Delegates to [FirestoreCascadeHelper].
     */
    private suspend fun collectQuizSubcollectionRefs(
        quizRef: DocumentReference
    ): List<DocumentReference> =
        FirestoreCascadeHelper.collectQuizSubcollectionRefs(quizRef)

    /**
     * Builds the tombstone data map for a permanently deleted quiz.
     * Delegates to [FirestoreCascadeHelper] (single source of truth).
     */
    private fun buildTombstoneData(quizId: String): HashMap<String, Any> =
        FirestoreCascadeHelper.buildTombstoneData(quizId)

    companion object {
        /**
         * When a batch pairs a tombstone write with a quiz delete (2 ops per quiz),
         * chunk at half the batch limit to stay within Firestore constraints.
         */
        private const val TOMBSTONE_BATCH_LIMIT = FirestoreCollections.BATCH_LIMIT / 2
    }
}
