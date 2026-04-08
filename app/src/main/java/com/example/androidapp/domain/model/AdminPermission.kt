package com.example.androidapp.domain.model

/**
 * Granular permissions assignable to admin users by the superuser.
 * Superusers implicitly have ALL permissions.
 * Regular admins only have the permissions explicitly granted to them.
 */
enum class AdminPermission {
    /** Can view and manage user accounts. */
    MANAGE_USERS,

    /** Can change user roles (promote to admin, demote to user). */
    CHANGE_USER_ROLES,

    /** Can permanently delete user accounts. */
    DELETE_USERS,

    /** Can ban and unban users. */
    BAN_USERS,

    /** Can view and manage all quizzes. */
    MANAGE_QUIZZES,

    /** Can permanently delete quizzes. */
    DELETE_QUIZZES,

    /** Can force-publish or unpublish quizzes. */
    PUBLISH_QUIZZES,

    /** Can view admin reports and analytics. */
    VIEW_REPORTS;

    companion object {
        /**
         * Parse a storage string to an [AdminPermission], or null if invalid.
         *
         * @param value Case-insensitive permission name (e.g. "manage_users")
         * @return The matching [AdminPermission], or null if no match found
         */
        fun fromString(value: String?): AdminPermission? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }

        /**
         * Returns all permissions. Used to represent the superuser's implicit permission set.
         *
         * @return A [Set] containing every [AdminPermission] value
         */
        fun all(): Set<AdminPermission> = entries.toSet()
    }

    /**
     * Convert permission to lowercase string for Firestore storage.
     *
     * @return Lowercase representation of this permission name
     */
    fun toStorageValue(): String = name.lowercase()
}
