package com.example.androidapp.domain.model

/**
 * Enum representing user roles in the Quizzez application.
 *
 * Role hierarchy (from lowest to highest privilege):
 * - [GUEST]: Anonymous users who can only take public quizzes
 * - [USER]: Authenticated users with full app features (create quizzes, take quizzes, etc.)
 * - [ADMIN]: Administrators with configurable permissions assigned by the superuser
 * - [SUPERUSER]: App owner with ALL permissions; cannot be modified by admins
 */
enum class UserRole {
    /**
     * Guest user with limited permissions.
     * Can only take public quizzes without authentication.
     */
    GUEST,

    /**
     * Standard authenticated user.
     * Can create quizzes, take quizzes, manage own content, etc.
     */
    USER,

    /**
     * Administrator with configurable elevated permissions.
     * Can access admin panel with permissions granted by the superuser.
     */
    ADMIN,

    /**
     * Superuser (app owner) with ALL permissions.
     * Has unrestricted access to every admin feature.
     * Cannot be modified or demoted by regular admins.
     */
    SUPERUSER;

    companion object {
        /**
         * Parse a string representation to a [UserRole].
         * Defaults to [USER] if the string is invalid.
         *
         * @param value String representation of the role (case-insensitive)
         * @return Corresponding [UserRole], or [USER] as fallback
         */
        fun fromString(value: String?): UserRole {
            return when (value?.uppercase()) {
                "GUEST" -> GUEST
                "USER" -> USER
                "ADMIN" -> ADMIN
                "SUPERUSER" -> SUPERUSER
                else -> USER  // Default to USER for safety
            }
        }
    }

    /**
     * Convert role to lowercase string for storage.
     */
    fun toStorageValue(): String = name.lowercase()
}
