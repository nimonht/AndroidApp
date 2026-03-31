package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firestore DTO for a user profile.
 *
 * Includes [createdAt] and [updatedAt] timestamps required by the
 * Firestore `users` schema. [deletedAt] supports soft-delete.
 */
data class UserDto(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)
