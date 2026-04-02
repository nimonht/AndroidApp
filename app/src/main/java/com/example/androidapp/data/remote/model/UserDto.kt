package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firestore DTO for a user profile.
 *
 * Includes [createdAt] and [updatedAt] timestamps required by the
 * Firestore `users` schema. [deletedAt] supports soft-delete.
 * [role] specifies user role ("guest", "user", or "admin") - defaults to "user".
 */
data class UserDto(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val role: String = "user",  // "guest", "user", or "admin"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)
