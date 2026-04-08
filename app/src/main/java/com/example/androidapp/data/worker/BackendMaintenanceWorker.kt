package com.example.androidapp.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.androidapp.QuizzezApplication
import com.example.androidapp.data.remote.firebase.FirestoreCascadeHelper
import com.example.androidapp.data.remote.firebase.FirestoreCollections
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Periodic background worker that performs server-less maintenance tasks
 * using the device as a lightweight backend.
 *
 * Responsibilities:
 * 1. Cascade-delete quizzes soft-deleted more than 30 days ago
 *    (including subcollections: questions, choices, and related attempts).
 *    Writes a deletion tombstone for each permanently removed quiz so that
 *    other clients can detect the removal incrementally.
 * 2. Aggregate quiz statistics (attempt counts) from the attempts collection.
 * 3. Remove inactive question-pool entries.
 * 4. Clean up user documents marked for deletion (with tombstones for their quizzes).
 * 5. Garbage-collect old deletion tombstones (older than 90 days) to prevent
 *    unbounded growth of the `quizDeletions` collection.
 *
 * Each task is executed independently so a failure in one does not prevent
 * the others from running. The worker returns [Result.success] unless a
 * critical unrecoverable error occurs.
 */
class BackendMaintenanceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Obtain dependencies from the DI container so emulator configuration is respected.
        val appContainer = (applicationContext as QuizzezApplication).appContainer
        val firestore = appContainer.firebaseFirestore
        val quizRemoteDataSource = appContainer.quizRemoteDataSource
        Log.d(TAG, "Starting backend maintenance tasks...")

        var hasErrors = false

        try {
            cleanupOldDeletedQuizzes(firestore, quizRemoteDataSource)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old deleted quizzes", e)
            hasErrors = true
        }

        try {
            aggregateQuizStats(firestore)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to aggregate quiz stats", e)
            hasErrors = true
        }

        try {
            cleanupInactivePoolQuestions(firestore)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup inactive pool questions", e)
            hasErrors = true
        }

        try {
            cleanupDeletedUsers(firestore, quizRemoteDataSource)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup deleted users", e)
            hasErrors = true
        }

        try {
            cleanupOldTombstones(firestore)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old deletion tombstones", e)
            hasErrors = true
        }

        Log.d(TAG, "Backend maintenance tasks completed (errors=$hasErrors).")
        return if (hasErrors) Result.retry() else Result.success()
    }

    /**
     * Deletes quizzes that have been in the recycle bin for more than 30 days,
     * along with all their subcollections (questions -> choices) and related attempts.
     *
     * Writes a deletion tombstone for each permanently removed quiz so that
     * other clients can detect the removal during their next incremental
     * invalidation check (via [QuizInvalidationManager.checkForDeletedQuizzes]).
     */
    private suspend fun cleanupOldDeletedQuizzes(
        firestore: FirebaseFirestore,
        quizRemoteDataSource: QuizRemoteDataSource
    ) {
        val cutoff = Timestamp(Date(System.currentTimeMillis() - DELETION_THRESHOLD_MS))

        Log.d(TAG, "Cleaning up quizzes deleted before: $cutoff")

        // Query quizzes where deletedAt is set and older than 30 days.
        val snapshot = firestore.collection(FirestoreCollections.QUIZZES)
            .whereNotEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()

        var deletedCount = 0
        for (doc in snapshot.documents) {
            val deletedAt = doc.getTimestamp(FirestoreCollections.Fields.DELETED_AT) ?: continue
            if (deletedAt > cutoff) continue // Not old enough yet

            val quizId = doc.id
            try {
                // Write a deletion tombstone before removing the quiz document
                // so other clients can detect the permanent removal.
                quizRemoteDataSource.writeDeletionTombstone(quizId)

                // 1. Delete related attempts (top-level collection)
                deleteCollectionByField(
                    firestore,
                    FirestoreCollections.ATTEMPTS,
                    "quizId",
                    quizId
                )

                // 2. Delete related share codes
                deleteCollectionByField(
                    firestore,
                    FirestoreCollections.SHARE_CODES,
                    "quizId",
                    quizId
                )

                // 3. Delete questions subcollection (each question has a choices subcollection)
                val questionsSnapshot = firestore.collection(FirestoreCollections.QUIZZES)
                    .document(quizId)
                    .collection(FirestoreCollections.QUESTIONS)
                    .get()
                    .await()

                for (questionDoc in questionsSnapshot.documents) {
                    // Delete choices subcollection for this question
                    val choicesSnapshot = firestore.collection(FirestoreCollections.QUIZZES)
                        .document(quizId)
                        .collection(FirestoreCollections.QUESTIONS)
                        .document(questionDoc.id)
                        .collection(FirestoreCollections.CHOICES)
                        .get()
                        .await()

                    deleteBatch(firestore, choicesSnapshot)
                }

                // Delete all question documents
                deleteBatch(firestore, questionsSnapshot)

                // 4. Delete the quiz document itself
                firestore.collection(FirestoreCollections.QUIZZES)
                    .document(quizId)
                    .delete()
                    .await()

                deletedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cascade-delete quiz $quizId", e)
                // Continue with other quizzes
            }
        }

        Log.d(TAG, "Cascade-deleted $deletedCount old quizzes with subcollections.")
    }

    /**
     * Counts actual attempts per quiz and updates the quiz's [attemptCount] field
     * if it differs from the stored value.
     */
    private suspend fun aggregateQuizStats(firestore: FirebaseFirestore) {
        Log.d(TAG, "Aggregating quiz stats...")

        // Fetch all non-deleted quizzes
        val quizSnapshot = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()

        var updatedCount = 0
        for (quizDoc in quizSnapshot.documents) {
            try {
                val quizId = quizDoc.id
                val storedCount = quizDoc.getLong(FirestoreCollections.Fields.ATTEMPT_COUNT) ?: 0L

                // Count actual attempts for this quiz
                val attemptsSnapshot = firestore.collection(FirestoreCollections.ATTEMPTS)
                    .whereEqualTo("quizId", quizId)
                    .get()
                    .await()

                val actualCount = attemptsSnapshot.size().toLong()

                if (actualCount != storedCount) {
                    firestore.collection(FirestoreCollections.QUIZZES)
                        .document(quizId)
                        .update(FirestoreCollections.Fields.ATTEMPT_COUNT, actualCount)
                        .await()
                    updatedCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to aggregate stats for quiz ${quizDoc.id}", e)
                // Continue with other quizzes
            }
        }

        Log.d(TAG, "Updated attempt counts for $updatedCount quizzes.")
    }

    /**
     * Removes question pool entries where [isActive] is false.
     */
    private suspend fun cleanupInactivePoolQuestions(firestore: FirebaseFirestore) {
        Log.d(TAG, "Cleaning up inactive pool questions...")

        val snapshot = firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereEqualTo(FirestoreCollections.Fields.IS_ACTIVE, false)
            .get()
            .await()

        deleteBatch(firestore, snapshot)

        Log.d(TAG, "Cleaned up ${snapshot.size()} inactive pool questions.")
    }

    /**
     * Permanently removes user documents that have been soft-deleted for longer
     * than [DELETION_THRESHOLD_MS] (30 days). Recently-banned users (whose
     * [deletedAt] is within the threshold) are intentionally skipped so that
     * admins have time to unban them if needed.
     *
     * Also cleans up quizzes, attempts, and pool contributions owned by those users.
     *
     * Writes deletion tombstones for each of the user's quizzes before removing
     * them, so other clients that cached those quizzes can detect the removal.
     */
    private suspend fun cleanupDeletedUsers(
        firestore: FirebaseFirestore,
        quizRemoteDataSource: QuizRemoteDataSource
    ) {
        Log.d(TAG, "Cleaning up deleted users...")

        val cutoff = Timestamp(Date(System.currentTimeMillis() - DELETION_THRESHOLD_MS))

        val snapshot = firestore.collection(FirestoreCollections.USERS)
            .whereNotEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()

        var deletedCount = 0
        for (doc in snapshot.documents) {
            val userId = doc.id
            val deletedAt = doc.getTimestamp(FirestoreCollections.Fields.DELETED_AT) ?: continue
            if (deletedAt > cutoff) continue
            try {
                // Write deletion tombstones for the user's quizzes before
                // the cascade helper removes them.
                val userQuizzes = firestore.collection(FirestoreCollections.QUIZZES)
                    .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
                    .get()
                    .await()
                quizRemoteDataSource.writeDeletionTombstones(
                    userQuizzes.documents.map { it.id }
                )

                // Cascade-delete quizzes (with questions, choices, share codes),
                // attempts, and the user document itself.
                FirestoreCascadeHelper.cascadeDeleteUserData(firestore, userId)

                // Delete user's pool contributions (not part of the standard cascade)
                deleteCollectionByField(
                    firestore,
                    FirestoreCollections.QUESTION_POOL,
                    FirestoreCollections.Fields.CONTRIBUTOR_ID,
                    userId
                )

                deletedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup user $userId", e)
            }
        }

        Log.d(TAG, "Cleaned up $deletedCount users (deleted before $cutoff) and their data.")
    }

    /**
     * Removes deletion tombstones older than [TOMBSTONE_RETENTION_MS] (90 days).
     *
     * Tombstones serve as lightweight markers so other clients can detect
     * permanent quiz deletions incrementally. After 90 days any client that
     * has not synced will fall back to the full stale-cleanup mechanism in
     * [SyncManager.downloadPublicQuizzes], so old tombstones can be safely
     * garbage-collected to prevent unbounded collection growth.
     */
    private suspend fun cleanupOldTombstones(firestore: FirebaseFirestore) {
        Log.d(TAG, "Cleaning up old deletion tombstones...")

        val cutoff = Timestamp(Date(System.currentTimeMillis() - TOMBSTONE_RETENTION_MS))

        val snapshot = firestore.collection(FirestoreCollections.QUIZ_DELETIONS)
            .whereLessThan(FirestoreCollections.Fields.DELETED_AT, cutoff)
            .get()
            .await()

        deleteBatch(firestore, snapshot)

        Log.d(TAG, "Cleaned up ${snapshot.size()} old deletion tombstones.")
    }

    // ---- Helpers ----

    /**
     * Deletes all documents in a [QuerySnapshot] using batched writes,
     * chunked to respect Firestore's 500-operation-per-batch limit.
     */
    private suspend fun deleteBatch(firestore: FirebaseFirestore, snapshot: QuerySnapshot) {
        if (snapshot.isEmpty) return
        snapshot.documents.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    /**
     * Deletes all documents in a top-level collection where [field] equals [value].
     */
    private suspend fun deleteCollectionByField(
        firestore: FirebaseFirestore,
        collection: String,
        field: String,
        value: String
    ) {
        val snapshot = firestore.collection(collection)
            .whereEqualTo(field, value)
            .get()
            .await()
        deleteBatch(firestore, snapshot)
    }


    companion object {
        private const val TAG = "BackendMaintenance"
        const val WORK_NAME = "BackendMaintenanceWorker"

        /**
         * How long a soft-deleted quiz must remain in the recycle bin before
         * permanent removal. Set to 30 days for production.
         */
        private const val DELETION_THRESHOLD_MS = 30L * 24 * 60 * 60 * 1000 // 30 days


        /**
         * How long deletion tombstones are retained before garbage collection.
         * Set to 90 days -- any client that has not synced within this window
         * will rely on the full stale-cleanup safety net instead.
         */
        private const val TOMBSTONE_RETENTION_MS = 90L * 24 * 60 * 60 * 1000 // 90 days
    }
}
