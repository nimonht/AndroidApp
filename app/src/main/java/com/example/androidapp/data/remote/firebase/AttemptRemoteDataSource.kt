package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.AttemptDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Remote data source for quiz attempt Firestore operations.
 */
class AttemptRemoteDataSource(private val firestore: FirebaseFirestore) {

    /**
     * Saves an attempt document to Firestore.
     */
    suspend fun saveAttempt(attemptDto: AttemptDto) {
        firestore.collection(FirestoreCollections.ATTEMPTS)
            .document(attemptDto.id)
            .set(attemptDto)
            .await()
    }

    /**
     * Updates an existing attempt document.
     */
    suspend fun updateAttempt(attemptDto: AttemptDto) {
        firestore.collection(FirestoreCollections.ATTEMPTS)
            .document(attemptDto.id)
            .set(attemptDto)
            .await()
    }

    /**
     * Fetches a single attempt by ID.
     */
    suspend fun getAttemptById(attemptId: String): AttemptDto? {
        return firestore.collection(FirestoreCollections.ATTEMPTS)
            .document(attemptId)
            .get()
            .await()
            .toObject(AttemptDto::class.java)
    }

    /**
     * Fetches all attempts for a user.
     */
    suspend fun getAttemptsByUser(userId: String): List<AttemptDto> {
        return firestore.collection(FirestoreCollections.ATTEMPTS)
            .whereEqualTo(FirestoreCollections.Fields.USER_ID, userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(AttemptDto::class.java) }
    }
}
