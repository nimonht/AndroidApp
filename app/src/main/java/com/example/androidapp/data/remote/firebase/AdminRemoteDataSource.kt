package com.example.androidapp.data.remote.firebase

import android.util.Log
import com.example.androidapp.data.remote.model.AttemptDto
import com.example.androidapp.data.remote.model.QuizDto
import com.example.androidapp.data.remote.model.UserDto
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
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
class AdminRemoteDataSource(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val firebaseAuth: FirebaseAuth
) {


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
                    FirestoreCollections.Fields.ROLE to role,
                    FirestoreCollections.Fields.UPDATED_AT to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Updates the permissions list for a user in Firestore.
     * Only the superuser should call this to grant or revoke admin permissions.
     *
     * @param userId The ID of the user whose permissions are being updated
     * @param permissions List of permission name strings (lowercase) to store
     */
    suspend fun updateUserPermissions(userId: String, permissions: List<String>) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .update(
                mapOf(
                    FirestoreCollections.Fields.PERMISSIONS to permissions,
                    FirestoreCollections.Fields.UPDATED_AT to Timestamp.now()
                )
            )
            .await()
    }

    /**
     * Fetches the currently authenticated user's [UserDto] from Firestore.
     *
     * @return The [UserDto] for the current user, or null if not authenticated
     *   or the document does not exist
     */
    suspend fun getCurrentUser(): UserDto? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        val snapshot = firestore.collection(FirestoreCollections.USERS)
            .document(uid)
            .get()
            .await()
        return snapshot.toObject(UserDto::class.java)
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
        // Cascade-delete all Firestore data owned by the user
        FirestoreCascadeHelper.cascadeDeleteUserData(firestore, userId)

        // Delete user from Firebase Authentication via Cloud Function
        try {
            functions.getHttpsCallable("deleteUserAuth")
                .call(hashMapOf("userId" to userId))
                .await()
        } catch (e: Exception) {
            // Log but do not throw -- Firestore data is already deleted.
            // The Auth record may need manual cleanup if this fails.
            Log.e(TAG, "Failed to delete user from Auth: ${e.message}")
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
     * Writes a deletion tombstone so other clients can detect the removal
     * via [com.example.androidapp.data.sync.QuizInvalidationManager].
     * Attempts are intentionally preserved -- they are the user's history
     * and should only be removed during account deletion.
     * Uses chunked batches to respect Firestore's 500-operation limit.
     */
    suspend fun deleteQuizPermanently(quizId: String) {
        val quizRef = firestore.collection(FirestoreCollections.QUIZZES).document(quizId)
        val refsToDelete = mutableListOf<DocumentReference>()

        // Collect subcollection refs (questions + choices) via shared helper
        refsToDelete.addAll(FirestoreCascadeHelper.collectQuizSubcollectionRefs(quizRef))

        // Collect share-code document if the quiz has one
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

        // First batch includes the tombstone write, so reserve 1 operation
        val firstChunkLimit = FirestoreCollections.BATCH_LIMIT - 1
        val firstChunk = refsToDelete.take(firstChunkLimit)
        val remaining = refsToDelete.drop(firstChunkLimit)

        // First batch: tombstone + initial deletes
        val firstBatch = firestore.batch()
        val tombstoneRef = firestore
            .collection(FirestoreCollections.QUIZ_DELETIONS)
            .document()
        firstBatch.set(tombstoneRef, FirestoreCascadeHelper.buildTombstoneData(quizId))
        firstChunk.forEach { ref -> firstBatch.delete(ref) }
        firstBatch.commit().await()

        // Remaining batches: pure deletes
        remaining.chunked(FirestoreCollections.BATCH_LIMIT).forEach { chunk ->
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
                    FirestoreCollections.Fields.IS_DRAFT to false,
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
            .orderBy(FirestoreCollections.Fields.STARTED_AT, Query.Direction.DESCENDING)
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
    // TODO: Replace full-collection `.get().size()` with Firestore count aggregation
    //       queries (e.g. `query.count().get()`) once the app's minimum Firebase SDK
    //       version supports them, to reduce read costs and latency at scale.

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
            .whereEqualTo(FirestoreCollections.Fields.ROLE, "admin")
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
            .whereGreaterThan(FirestoreCollections.Fields.STARTED_AT, thirtyDaysAgo)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString(FirestoreCollections.Fields.USER_ID) }
            .filter { !it.startsWith("guest_") }  // Exclude guests
            .toSet()

        // Combine both sets
        return (activeQuizUsers + activeAttemptUsers).size
    }

    // ==================== Paginated queries ====================

    /**
     * Fetches a page of users from Firestore, ordered by email.
     * Uses cursor-based pagination via [startAfterDoc].
     *
     * @param pageSize Number of documents to fetch.
     * @param startAfterDoc The last [DocumentSnapshot] from the previous page, or null for the first page.
     * @return A pair of the user DTOs and the last [DocumentSnapshot] for cursor continuation (null if no results).
     */
    suspend fun getUsersPage(
        pageSize: Int,
        startAfterDoc: DocumentSnapshot? = null
    ): Pair<List<UserDto>, DocumentSnapshot?> {
        var query = firestore.collection(FirestoreCollections.USERS)
            .orderBy(FirestoreCollections.Fields.EMAIL)
            .limit(pageSize.toLong())

        if (startAfterDoc != null) {
            query = query.startAfter(startAfterDoc)
        }

        val snapshot = query.get().await()
        val users = snapshot.documents.mapNotNull { it.toObject(UserDto::class.java) }
        val lastDoc = snapshot.documents.lastOrNull()
        return Pair(users, lastDoc)
    }

    /**
     * Fetches a page of quizzes from Firestore, ordered by updatedAt descending.
     * Uses cursor-based pagination via [startAfterDoc].
     *
     * @param pageSize Number of documents to fetch.
     * @param includeDeleted If true, includes soft-deleted quizzes.
     * @param startAfterDoc The last [DocumentSnapshot] from the previous page, or null for the first page.
     * @return A pair of the quiz DTOs and the last [DocumentSnapshot] for cursor continuation (null if no results).
     */
    suspend fun getQuizzesPage(
        pageSize: Int,
        includeDeleted: Boolean = false,
        startAfterDoc: DocumentSnapshot? = null
    ): Pair<List<QuizDto>, DocumentSnapshot?> {
        var query = if (includeDeleted) {
            firestore.collection(FirestoreCollections.QUIZZES)
                .orderBy(FirestoreCollections.Fields.UPDATED_AT, Query.Direction.DESCENDING)
        } else {
            firestore.collection(FirestoreCollections.QUIZZES)
                .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
                .orderBy(FirestoreCollections.Fields.UPDATED_AT, Query.Direction.DESCENDING)
        }

        query = query.limit(pageSize.toLong())

        if (startAfterDoc != null) {
            query = query.startAfter(startAfterDoc)
        }

        val snapshot = query.get().await()
        val quizzes = snapshot.documents.mapNotNull { it.toObject(QuizDto::class.java) }
        val lastDoc = snapshot.documents.lastOrNull()
        return Pair(quizzes, lastDoc)
    }

    companion object {
        private const val TAG = "AdminRemoteDataSource"
    }
}
