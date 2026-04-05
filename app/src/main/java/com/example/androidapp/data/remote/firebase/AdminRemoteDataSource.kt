package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.AttemptDto
import com.example.androidapp.data.remote.model.QuizDto
import com.example.androidapp.data.remote.model.UserDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

/**
 * Remote data source for admin-level Firebase operations.
 *
 * Provides elevated access to Firestore collections for administrative tasks.
 * All methods assume the caller has been authorized as an admin.
 */
class AdminRemoteDataSource(private val firestore: FirebaseFirestore) {

    companion object {
        /** Firestore batch operation limit. */
        private const val BATCH_LIMIT = 500
    }

    // ========== USER OPERATIONS ==========

    /**
     * Fetch all users from Firestore (including banned/soft-deleted for admin management).
     * Returns a real-time stream of updates.
     */
    fun getAllUsers(): Flow<List<UserDto>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserDto::class.java)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Update a user's role in Firestore.
     */
    suspend fun updateUserRole(userId: String, role: String) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .update(
                mapOf(
                    "role" to role,
                    FirestoreCollections.Fields.UPDATED_AT to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Soft-delete (ban) a user by setting deletedAt timestamp.
     */
    suspend fun banUser(userId: String) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .update(
                mapOf(
                    FirestoreCollections.Fields.DELETED_AT to Timestamp.now(),
                    FirestoreCollections.Fields.UPDATED_AT to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Unban a user by clearing the deletedAt timestamp.
     */
    suspend fun unbanUser(userId: String) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .update(
                mapOf(
                    FirestoreCollections.Fields.DELETED_AT to null,
                    FirestoreCollections.Fields.UPDATED_AT to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Permanently delete a user document and all associated data.
     * This includes their quizzes (with questions/choices subcollections),
     * attempts, and contributions.
     * Uses chunked batches to respect Firestore's 500-operation limit.
     */
    suspend fun deleteUserPermanently(userId: String) {
        // Collect all document references to delete
        val refsToDelete = mutableListOf(
            firestore.collection(FirestoreCollections.USERS).document(userId)
        )

        // Collect user's quizzes and their subcollections (questions/choices)
        val quizzes = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .get()
            .await()
        quizzes.documents.forEach { quizDoc ->
            val quizRef = quizDoc.reference

            // Collect all questions for this quiz
            val questionsSnapshot = quizRef
                .collection(FirestoreCollections.QUESTIONS)
                .get()
                .await()

            questionsSnapshot.documents.forEach { questionDoc ->
                val questionRef = questionDoc.reference

                // Collect all choices for this question
                val choicesSnapshot = questionRef
                    .collection(FirestoreCollections.CHOICES)
                    .get()
                    .await()

                choicesSnapshot.documents.forEach { choiceDoc ->
                    refsToDelete.add(choiceDoc.reference)
                }

                refsToDelete.add(questionRef)
            }

            refsToDelete.add(quizRef)
        }

        // Collect user's attempts
        val attempts = firestore.collection(FirestoreCollections.ATTEMPTS)
            .whereEqualTo(FirestoreCollections.Fields.USER_ID, userId)
            .get()
            .await()
        attempts.documents.forEach { attemptDoc ->
            refsToDelete.add(attemptDoc.reference)
        }

        // Commit in chunks of 500 (Firestore batch limit)
        refsToDelete.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { ref -> batch.delete(ref) }
            batch.commit().await()
        }
    }

    /**
     * Search users by email or username (case-insensitive substring match).
     * Includes banned/soft-deleted users for admin management.
     */
    fun searchUsers(query: String): Flow<List<UserDto>> = callbackFlow {
        // Note: Firestore doesn't support full-text search natively.
        // For production, consider using Algolia or Elasticsearch.
        // This implementation fetches all users and filters client-side.
        val listener = firestore.collection(FirestoreCollections.USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserDto::class.java)
                }?.filter { user ->
                    user.email.contains(query, ignoreCase = true) ||
                            user.username.contains(query, ignoreCase = true) ||
                            user.displayName.contains(query, ignoreCase = true)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    // ========== QUIZ OPERATIONS ==========

    /**
     * Fetch all quizzes from Firestore.
     * @param includeDeleted If true, includes soft-deleted quizzes.
     */
    fun getAllQuizzes(includeDeleted: Boolean = false): Flow<List<QuizDto>> = callbackFlow {
        val query = if (includeDeleted) {
            firestore.collection(FirestoreCollections.QUIZZES)
        } else {
            firestore.collection(FirestoreCollections.QUIZZES)
                .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val quizzes = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(QuizDto::class.java)
            } ?: emptyList()
            trySend(quizzes)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Permanently delete a quiz and all its questions/choices.
     * Uses chunked batches to respect Firestore's 500-operation limit.
     */
    suspend fun deleteQuizPermanently(quizId: String) {
        // Collect all document references to delete
        val refsToDelete = mutableListOf(
            firestore.collection(FirestoreCollections.QUIZZES).document(quizId)
        )

        // Collect all questions and their choices
        val questions = firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .collection(FirestoreCollections.QUESTIONS)
            .get()
            .await()

        questions.documents.forEach { questionDoc ->
            refsToDelete.add(questionDoc.reference)

            // Collect choices for each question
            val choices = questionDoc.reference
                .collection(FirestoreCollections.CHOICES)
                .get()
                .await()

            choices.documents.forEach { choiceDoc ->
                refsToDelete.add(choiceDoc.reference)
            }
        }

        // Commit in chunks of 500 (Firestore batch limit)
        refsToDelete.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { ref -> batch.delete(ref) }
            batch.commit().await()
        }
    }

    /**
     * Restore a soft-deleted quiz by clearing deletedAt.
     */
    suspend fun restoreQuiz(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(
                mapOf(
                    FirestoreCollections.Fields.DELETED_AT to null,
                    FirestoreCollections.Fields.UPDATED_AT to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Force publish a quiz (set isPublic = true, isDraft = false).
     */
    suspend fun forcePublishQuiz(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(
                mapOf(
                    FirestoreCollections.Fields.IS_PUBLIC to true,
                    "isDraft" to false,
                    FirestoreCollections.Fields.UPDATED_AT to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Unpublish a quiz (set isPublic = false).
     */
    suspend fun unpublishQuiz(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(
                mapOf(
                    FirestoreCollections.Fields.IS_PUBLIC to false,
                    FirestoreCollections.Fields.UPDATED_AT to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Search quizzes by title, description, or author name.
     */
    fun searchQuizzes(query: String, includeDeleted: Boolean = false): Flow<List<QuizDto>> = callbackFlow {
        val baseQuery = if (includeDeleted) {
            firestore.collection(FirestoreCollections.QUIZZES)
        } else {
            firestore.collection(FirestoreCollections.QUIZZES)
                .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
        }

        val listener = baseQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val quizzes = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(QuizDto::class.java)
            }?.filter { quiz ->
                quiz.title.contains(query, ignoreCase = true) ||
                        (quiz.description?.contains(query, ignoreCase = true) == true) ||
                        quiz.authorName.contains(query, ignoreCase = true)
            } ?: emptyList()
            trySend(quizzes)
        }
        awaitClose { listener.remove() }
    }

    // ========== ATTEMPT OPERATIONS ==========

    /**
     * Fetch all quiz attempts from Firestore.
     */
    fun getAllAttempts(): Flow<List<AttemptDto>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.ATTEMPTS)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val attempts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AttemptDto::class.java)
                } ?: emptyList()
                trySend(attempts)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Delete an attempt document.
     */
    suspend fun deleteAttempt(attemptId: String) {
        firestore.collection(FirestoreCollections.ATTEMPTS)
            .document(attemptId)
            .delete()
            .await()
    }

    // ========== STATISTICS ==========

    /**
     * Get count of all users (excluding soft-deleted).
     */
    suspend fun getTotalUsersCount(): Int {
        return firestore.collection(FirestoreCollections.USERS)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .size()
    }

    /**
     * Get count of all quizzes (excluding soft-deleted).
     */
    suspend fun getTotalQuizzesCount(): Int {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .size()
    }

    /**
     * Get count of all attempts.
     */
    suspend fun getTotalAttemptsCount(): Int {
        return firestore.collection(FirestoreCollections.ATTEMPTS)
            .get()
            .await()
            .size()
    }

    /**
     * Get count of questions in the pool.
     */
    suspend fun getTotalQuestionsInPoolCount(): Int {
        return firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereEqualTo(FirestoreCollections.Fields.IS_ACTIVE, true)
            .get()
            .await()
            .size()
    }

    /**
     * Get count of users with admin role.
     */
    suspend fun getAdminUsersCount(): Int {
        return firestore.collection(FirestoreCollections.USERS)
            .whereEqualTo("role", "admin")
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .size()
    }

    /**
     * Get count of public quizzes.
     */
    suspend fun getPublicQuizzesCount(): Int {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.IS_PUBLIC, true)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .size()
    }

    /**
     * Get count of private quizzes (not public, not deleted).
     */
    suspend fun getPrivateQuizzesCount(): Int {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.IS_PUBLIC, false)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .size()
    }

    /**
     * Get count of soft-deleted quizzes in recycle bin.
     * Uses a sentinel timestamp to match documents with a real deletedAt value.
     */
    suspend fun getDeletedQuizzesCount(): Int {
        val epoch = Timestamp(java.util.Date(0))
        return firestore.collection(FirestoreCollections.QUIZZES)
            .whereGreaterThan(FirestoreCollections.Fields.DELETED_AT, epoch)
            .orderBy(FirestoreCollections.Fields.DELETED_AT)
            .get()
            .await()
            .size()
    }

    /**
     * Get count of active users (those with activity in the last 30 days).
     * This checks for users who have created or updated quizzes or taken attempts.
     */
    suspend fun getActiveUsersCount(): Int {
        val thirtyDaysAgo = Timestamp(
            java.util.Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
        )

        // Get users who created/updated quizzes recently
        val activeQuizUsers = firestore.collection(FirestoreCollections.QUIZZES)
            .whereGreaterThan(FirestoreCollections.Fields.UPDATED_AT, thirtyDaysAgo)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString(FirestoreCollections.Fields.OWNER_ID) }
            .toSet()

        // Get users who took attempts recently
        val activeAttemptUsers = firestore.collection(FirestoreCollections.ATTEMPTS)
            .whereGreaterThan("startedAt", thirtyDaysAgo)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString(FirestoreCollections.Fields.USER_ID) }
            .filter { !it.startsWith("guest_") }  // Exclude guests
            .toSet()

        // Combine both sets
        return (activeQuizUsers + activeAttemptUsers).size
    }
}
