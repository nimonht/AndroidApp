package com.example.androidapp.domain.model

/**
 * Domain model representing a user.
 *
 * @property id Unique user identifier
 * @property email User's email address
 * @property displayName Display name shown in UI
 * @property username Unique username
 * @property photoUrl Optional profile picture URL
 * @property role User's role (GUEST, USER, ADMIN, or SUPERUSER) - defaults to USER
 * @property isBanned Whether the user is banned (soft-deleted via deletedAt)
 * @property permissions Set of granular admin permissions assigned to this user.
 *   Only meaningful for ADMIN role; SUPERUSER implicitly has all permissions.
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val username: String = "",
    val photoUrl: String? = null,
    val role: UserRole = UserRole.USER,
    val isBanned: Boolean = false,
    val permissions: Set<AdminPermission> = emptySet()
) {
    /**
     * Check if this user is an administrator (either ADMIN or SUPERUSER).
     */
    fun isAdmin(): Boolean = role == UserRole.ADMIN || role == UserRole.SUPERUSER

    /**
     * Check if this user is the superuser (app owner with all privileges).
     */
    fun isSuperuser(): Boolean = role == UserRole.SUPERUSER

    /**
     * Check if this user has a specific admin permission.
     * Superusers implicitly have all permissions.
     * Regular admins only have explicitly assigned permissions.
     *
     * @param permission The permission to check
     * @return true if the user has the permission, false otherwise
     */
    fun hasPermission(permission: AdminPermission): Boolean {
        if (isSuperuser()) return true
        return permission in permissions
    }

    /**
     * Returns the effective set of permissions for this user.
     * Superusers receive all permissions; admins receive their assigned set;
     * other roles receive an empty set.
     *
     * @return The effective permission set
     */
    fun effectivePermissions(): Set<AdminPermission> {
        return when (role) {
            UserRole.SUPERUSER -> AdminPermission.all()
            UserRole.ADMIN -> permissions
            else -> emptySet()
        }
    }
}
