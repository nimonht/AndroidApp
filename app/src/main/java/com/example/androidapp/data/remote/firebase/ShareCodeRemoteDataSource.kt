package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.ShareCodeDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ShareCodeRemoteDataSource(private val firestore: FirebaseFirestore) {

    suspend fun lookupShareCode(shareCode: String): ShareCodeDto? {
        return firestore.collection(FirestoreCollections.SHARE_CODES)
            .document(shareCode)
            .get()
            .await()
            .toObject(ShareCodeDto::class.java)
    }

    /**
     * Creates a share code atomically only if it doesn't already exist.
     *
     * @param shareCode The share code to create.
     * @param quizId The quiz ID to associate with the code.
     * @return true if the code was created, false if it already existed.
     */
    suspend fun createShareCodeIfNotExists(shareCode: String, quizId: String): Boolean {
        val docRef = firestore.collection(FirestoreCollections.SHARE_CODES)
            .document(shareCode)

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (snapshot.exists()) {
                false
            } else {
                transaction.set(docRef, mapOf(FirestoreCollections.Fields.QUIZ_ID to quizId))
                true
            }
        }.await()
    }

    suspend fun deleteShareCode(shareCode: String) {
        firestore.collection(FirestoreCollections.SHARE_CODES)
            .document(shareCode)
            .delete()
            .await()
    }
}
