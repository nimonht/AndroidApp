package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.QuestionDto
import com.example.androidapp.data.remote.model.QuizDto
import com.google.firebase.Timestamp
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
     * Permanently deletes a quiz document and writes a deletion tombstone
     * in an atomic batch so other clients can detect the removal.
     *
     * The tombstone is a lightweight document in the
     * [FirestoreCollections.QUIZ_DELETIONS] collection containing only
     * the quiz ID and the deletion timestamp.
     */
    suspend fun permanentlyDeleteQuiz(quizId: String) {
        val batch = firestore.batch()

        // Write tombstone so other clients detect the removal during
        // their next incremental invalidation check.
        val tombstoneRef = firestore
            .collection(FirestoreCollections.QUIZ_DELETIONS)
            .document()
        batch.set(
            tombstoneRef,
            hashMapOf(
                FirestoreCollections.Fields.QUIZ_ID to quizId,
                FirestoreCollections.Fields.DELETED_AT to Timestamp.now()
            )
        )

        // Delete the quiz document itself.
        batch.delete(
            firestore.collection(FirestoreCollections.QUIZZES).document(quizId)
        )

        batch.commit().await()
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
     * Deletes all soft-deleted quizzes owned by the user permanently from Firestore.
     * Uses batch writes for efficiency. Each batch atomically pairs a deletion
     * tombstone write with the quiz document delete (2 operations per quiz),
     * so batches are chunked at 250 to stay within the 500-operation limit.
     */
    suspend fun emptyTrash(userId: String) {
        val deletedQuizzesQuery = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .whereNotEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()

        if (deletedQuizzesQuery.isEmpty) return

        // Each quiz needs 2 operations (tombstone + delete), so chunk at 250
        // to stay within Firestore's 500-operation batch limit.
        val chunks = deletedQuizzesQuery.documents.chunked(TOMBSTONE_BATCH_LIMIT)
        for (chunk in chunks) {
            val batch = firestore.batch()
            for (doc in chunk) {
                // Write tombstone
                val tombstoneRef = firestore
                    .collection(FirestoreCollections.QUIZ_DELETIONS)
                    .document()
                batch.set(
                    tombstoneRef,
                    hashMapOf(
                        FirestoreCollections.Fields.QUIZ_ID to doc.id,
                        FirestoreCollections.Fields.DELETED_AT to Timestamp.now()
                    )
                )
                // Delete quiz document
                batch.delete(doc.reference)
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
        val tombstone = hashMapOf(
            FirestoreCollections.Fields.QUIZ_ID to quizId,
            FirestoreCollections.Fields.DELETED_AT to Timestamp.now()
        )
        firestore.collection(FirestoreCollections.QUIZ_DELETIONS)
            .add(tombstone)
            .await()
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

        snapshot.documents.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    companion object {
        /** Maximum operations per Firestore batch write. */
        private const val BATCH_LIMIT = 500

        /**
         * When a batch pairs a tombstone write with a quiz delete (2 ops per quiz),
         * chunk at half the batch limit to stay within Firestore constraints.
         */
        private const val TOMBSTONE_BATCH_LIMIT = 250
    }
}
