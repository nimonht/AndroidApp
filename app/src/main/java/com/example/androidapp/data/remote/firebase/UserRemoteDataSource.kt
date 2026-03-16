package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth


/**
 * Remote data source for user profile Firestore operations.
 */
class UserRemoteDataSource(private val firestore: FirebaseFirestore,
                           private val firebaseAuth: FirebaseAuth
) {

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
    ////

    suspend fun updateUserProfile(
        displayName: String,
        avatarUrl: String?
    ) {

        val uid = firebaseAuth.currentUser?.uid ?: return

        val updates = mutableMapOf<String, Any>(
            "displayName" to displayName
        )

        avatarUrl?.let {
            updates["avatarUrl"] = it
        }

        firestore
            .collection("users")
            .document(uid)
            .update(updates)
            .await()
    }
}

