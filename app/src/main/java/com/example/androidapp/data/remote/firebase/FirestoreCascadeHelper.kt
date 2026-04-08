package com.example.androidapp.data.remote.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Shared helper for Firestore cascade-delete operations.
 *
 * The user data graph in Firestore follows this structure:
 * ```
 * users/{userId}
 * quizzes/{quizId}           (ownerId == userId)
 *   questions/{questionId}
 *     choices/{choiceId}
 * shareCodes/{code}           (referenced by quiz doc's shareCode field)
 * attempts/{attemptId}        (userId == userId)
 * quizDeletions/{tombstoneId} (tombstone for permanently deleted quizzes)
 * ```
 *
 * [cascadeDeleteUserData] traverses this entire graph, collects every
 * [DocumentReference], writes deletion tombstones for each quiz, and
 * batch-deletes them in chunks of [FirestoreCollections.BATCH_LIMIT]
 * to stay within Firestore limits.
 */
object FirestoreCascadeHelper {

    /**
     * Collects all subcollection document references (questions and their
     * nested choices) under a quiz document.
     *
     * ```
     * quizzes/{quizId}
     *   questions/{questionId}       <-- collected
     *     choices/{choiceId}         <-- collected
     * ```
     *
     * @param quizRef reference to the quiz document whose subcollections
     *                should be collected.
     * @return list of [DocumentReference]s for every question and choice
     *         under [quizRef], choices listed before their parent question.
     */
    suspend fun collectQuizSubcollectionRefs(
        quizRef: DocumentReference
    ): List<DocumentReference> {
        val refs = mutableListOf<DocumentReference>()

        val questionsSnapshot = quizRef
            .collection(FirestoreCollections.QUESTIONS)
            .get()
            .await()

        questionsSnapshot.documents.forEach { questionDoc ->
            val choicesSnapshot = questionDoc.reference
                .collection(FirestoreCollections.CHOICES)
                .get()
                .await()

            choicesSnapshot.documents.forEach { choiceDoc ->
                refs.add(choiceDoc.reference)
            }
            refs.add(questionDoc.reference)
        }

        return refs
    }

    /**
     * Builds the tombstone data map for a permanently deleted quiz.
     * Single source of truth for the tombstone document structure.
     *
     * @param quizId the ID of the deleted quiz.
     * @return a [HashMap] ready to be written to [FirestoreCollections.QUIZ_DELETIONS].
     */
    fun buildTombstoneData(quizId: String): HashMap<String, Any> = hashMapOf(
        FirestoreCollections.Fields.QUIZ_ID to quizId,
        FirestoreCollections.Fields.DELETED_AT to Timestamp.now()
    )

    /**
     * Cascade-deletes all Firestore data owned by [userId].
     *
     * Traverses and removes the following in a single pass:
     * 1. **Quizzes** owned by the user, including each quiz's
     *    **questions** and nested **choices** subcollections.
     * 2. **Share-code** documents referenced by those quizzes.
     * 3. **Deletion tombstones** for each quiz (so other clients can detect
     *    the removal via [com.example.androidapp.data.sync.QuizInvalidationManager]).
     * 4. **Attempts** submitted by the user.
     * 5. The **user document** itself.
     *
     * All document references are collected first, then deleted via
     * batched writes chunked at [FirestoreCollections.BATCH_LIMIT].
     * Tombstone writes are included in the first batch.
     *
     * Callers that need to perform additional work (e.g. cleaning up pool
     * contributions or deleting the Firebase Auth account) should do so
     * before or after invoking this function.
     *
     * @param firestore The [FirebaseFirestore] instance to use.
     * @param userId    The ID of the user whose data should be deleted.
     */
    suspend fun cascadeDeleteUserData(
        firestore: FirebaseFirestore,
        userId: String
    ) {
        val refsToDelete = mutableListOf<DocumentReference>()
        val quizIdsToTombstone = mutableListOf<String>()

        // 1. Collect user's quizzes and their subcollections + share codes
        val quizzes = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .get()
            .await()

        quizzes.documents.forEach { quizDoc ->
            val quizRef = quizDoc.reference

            // Track quiz ID for tombstone
            quizIdsToTombstone.add(quizDoc.id)

            // Collect share-code document if the quiz has one
            val shareCode = quizDoc.getString(FirestoreCollections.Fields.SHARE_CODE)
            if (!shareCode.isNullOrBlank()) {
                refsToDelete.add(
                    firestore.collection(FirestoreCollections.SHARE_CODES)
                        .document(shareCode)
                )
            }

            // Collect all questions and their nested choices
            refsToDelete.addAll(collectQuizSubcollectionRefs(quizRef))

            refsToDelete.add(quizRef)
        }

        // 2. Collect user's attempts
        val attempts = firestore.collection(FirestoreCollections.ATTEMPTS)
            .whereEqualTo(FirestoreCollections.Fields.USER_ID, userId)
            .get()
            .await()

        attempts.documents.forEach { attemptDoc ->
            refsToDelete.add(attemptDoc.reference)
        }

        // 3. Collect user document
        refsToDelete.add(
            firestore.collection(FirestoreCollections.USERS).document(userId)
        )

        // 4. Write tombstones + batch-delete in chunks of BATCH_LIMIT
        if (quizIdsToTombstone.isNotEmpty()) {
            // First batch: tombstone writes + initial deletes
            val tombstoneOpsCount = quizIdsToTombstone.size
            val firstChunkLimit = (FirestoreCollections.BATCH_LIMIT - tombstoneOpsCount)
                .coerceAtLeast(0)
            val firstChunk = refsToDelete.take(firstChunkLimit)
            val remaining = refsToDelete.drop(firstChunkLimit)

            val firstBatch = firestore.batch()
            quizIdsToTombstone.forEach { quizId ->
                val tombstoneRef = firestore
                    .collection(FirestoreCollections.QUIZ_DELETIONS)
                    .document()
                firstBatch.set(tombstoneRef, buildTombstoneData(quizId))
            }
            firstChunk.forEach { ref -> firstBatch.delete(ref) }
            firstBatch.commit().await()

            // Remaining batches: pure deletes
            remaining.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { ref -> batch.delete(ref) }
                batch.commit().await()
            }
        } else {
            // No quizzes to tombstone -- just batch-delete everything
            refsToDelete.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { ref -> batch.delete(ref) }
                batch.commit().await()
            }
        }
    }
}
