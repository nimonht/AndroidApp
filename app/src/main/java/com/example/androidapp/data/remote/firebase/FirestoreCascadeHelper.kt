package com.example.androidapp.data.remote.firebase

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
 * ```
 *
 * [cascadeDeleteUserData] traverses this entire graph, collects every
 * [DocumentReference], and batch-deletes them in chunks of
 * [FirestoreCollections.BATCH_LIMIT] to stay within Firestore limits.
 */
object FirestoreCascadeHelper {

    /**
     * Cascade-deletes all Firestore data owned by [userId].
     *
     * Traverses and removes the following in a single pass:
     * 1. **Quizzes** owned by the user, including each quiz's
     *    **questions** and nested **choices** subcollections.
     * 2. **Share-code** documents referenced by those quizzes.
     * 3. **Attempts** submitted by the user.
     * 4. The **user document** itself.
     *
     * All document references are collected first, then deleted via
     * batched writes chunked at [FirestoreCollections.BATCH_LIMIT].
     *
     * Callers that need to perform additional work (e.g. writing deletion
     * tombstones, cleaning up pool contributions, or deleting the
     * Firebase Auth account) should do so before or after invoking this
     * function.
     *
     * @param firestore The [FirebaseFirestore] instance to use.
     * @param userId    The ID of the user whose data should be deleted.
     */
    suspend fun cascadeDeleteUserData(
        firestore: FirebaseFirestore,
        userId: String
    ) {
        val refsToDelete = mutableListOf<DocumentReference>()

        // 1. Collect user's quizzes and their subcollections + share codes
        val quizzes = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .get()
            .await()

        quizzes.documents.forEach { quizDoc ->
            val quizRef = quizDoc.reference

            // Collect share-code document if the quiz has one
            val shareCode = quizDoc.getString(FirestoreCollections.Fields.SHARE_CODE)
            if (!shareCode.isNullOrBlank()) {
                refsToDelete.add(
                    firestore.collection(FirestoreCollections.SHARE_CODES)
                        .document(shareCode)
                )
            }

            // Collect all questions and their nested choices
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
                    refsToDelete.add(choiceDoc.reference)
                }
                refsToDelete.add(questionDoc.reference)
            }

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

        // 4. Batch-delete in chunks of BATCH_LIMIT (Firestore limit)
        refsToDelete.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { ref -> batch.delete(ref) }
            batch.commit().await()
        }
    }
}
