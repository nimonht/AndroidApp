package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firestore DTO for a user profile.
 *
 * Includes [createdAt] and [updatedAt] timestamps required by the
 * Firestore `users` schema. [deletedAt] supports soft-delete.
 * [role] specifies user role ("guest", "user", "admin", or "superuser") - defaults to "user".
 * [permissions] holds the list of admin permission strings granted to this user.
 */
data class UserDto(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val role: String = "user",  // "guest", "user", "admin", or "superuser"
    val permissions: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)
