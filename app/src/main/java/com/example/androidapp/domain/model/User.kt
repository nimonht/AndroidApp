package com.example.androidapp.domain.model

/**
 * Domain model representing a user.
 *
 * @property id Unique user identifier
 * @property email User's email address
 * @property displayName Display name shown in UI
 * @property username Unique username
 * @property photoUrl Optional profile picture URL
 * @property role User's role (GUEST, USER, or ADMIN) - defaults to USER
 * @property isBanned Whether the user is banned (soft-deleted via deletedAt)
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val username: String = "",
    val photoUrl: String? = null,
    val role: UserRole = UserRole.USER,
    val isBanned: Boolean = false
) {
    /**
     * Check if this user is an administrator.
     */
    fun isAdmin(): Boolean = role == UserRole.ADMIN
}
