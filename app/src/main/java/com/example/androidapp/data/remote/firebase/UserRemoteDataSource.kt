package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Remote data source for user profile Firestore operations.
 */
class UserRemoteDataSource(private val firestore: FirebaseFirestore) {

    /**
     * Fetches a user profile by UID.
     */
    suspend fun getUserById(userId: String): UserDto? {
        return firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .get()
            .await()
            .toObject(UserDto::class.java)
    }

    /**
     * Saves or updates a user profile document.
     */
    suspend fun saveUser(userDto: UserDto) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userDto.id)
            .set(userDto)
            .await()
    }

    /**
     * Deletes a user profile document by user ID.
     */
    suspend fun deleteUser(userId: String) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .delete()
            .await()
    }
}

