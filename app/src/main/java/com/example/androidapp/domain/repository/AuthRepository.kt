package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 * The implementation wraps FirebaseAuth and caches user data in Room.
 */
interface AuthRepository {

    /** Emits the currently authenticated [User], or null when logged out. */
    val currentUser: Flow<User?>

    /** Returns true if a user is currently authenticated. */
    val isLoggedIn: Boolean

    /**
     * Signs in with email and password.
     * @return [Result.success] with the [User] on success, or [Result.failure] with the error.
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Creates a new account with email, password, and username.
     * @return [Result.success] with the new [User] on success, or [Result.failure] with the error.
     */
    suspend fun register(email: String, password: String, username: String): Result<User>

    /**
     * Signs out the current user.
     */
    suspend fun logout()

    /**
     * Returns the currently authenticated user synchronously, or null.
     */
    suspend fun getCurrentUser(): User?

    /**
     * Sends a password reset email to the given address.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Sends a verification email to the currently authenticated user.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    suspend fun sendEmailVerification(): Result<Unit>

    /** Returns true if the current user's email is verified. */
    val isEmailVerified: Boolean

    /**
     * Deletes the current user's Firebase account and performs local cleanup.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Signs in using a Google ID token credential.
     * @return [Result.success] with the [User] on success, or [Result.failure] with the error.
     */
    suspend fun signInWithGoogleToken(idToken: String): Result<User>

    /**
     * Generates and returns a unique guest identifier (UUID).
     * @return A new guest UUID string.
     */
    fun generateGuestId(): String

    /**
     * Refreshes the current authentication session token.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    suspend fun refreshSession(): Result<Unit>
}

