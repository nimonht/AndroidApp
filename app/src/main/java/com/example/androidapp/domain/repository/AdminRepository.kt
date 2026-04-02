package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for admin operations.
 *
 * Provides elevated access to manage users, quizzes, and system data.
 * All operations should enforce admin-only access at the implementation level.
 */
interface AdminRepository {

    // ========== USER MANAGEMENT ==========

    /**
     * Retrieve all users in the system (excluding guests).
     * Emits updates whenever user data changes.
     *
     * @return Flow emitting list of all registered users
     */
    fun getAllUsers(): Flow<List<User>>

    /**
     * Update a user's role (e.g., promote to admin, demote to user).
     *
     * @param userId The ID of the user to update
     * @param newRole The new role to assign
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Unit>

    /**
     * Ban a user by marking their account as deleted.
     * The user will no longer be able to access the application.
     *
     * @param userId The ID of the user to ban
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun banUser(userId: String): Result<Unit>

    /**
     * Unban a previously banned user by removing their deleted flag.
     *
     * @param userId The ID of the user to unban
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun unbanUser(userId: String): Result<Unit>

    /**
     * Permanently delete a user account and all associated data.
     * This action is irreversible.
     *
     * @param userId The ID of the user to delete
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun deleteUserPermanently(userId: String): Result<Unit>

    // ========== QUIZ MANAGEMENT ==========

    /**
     * Retrieve all quizzes in the system (including private, public, and deleted).
     * Emits updates whenever quiz data changes.
     *
     * @param includeDeleted Whether to include soft-deleted quizzes (default: false)
     * @return Flow emitting list of all quizzes
     */
    fun getAllQuizzes(includeDeleted: Boolean = false): Flow<List<Quiz>>

    /**
     * Permanently delete a quiz and all its questions/choices.
     * This action is irreversible.
     *
     * @param quizId The ID of the quiz to delete permanently
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun deleteQuizPermanently(quizId: String): Result<Unit>

    /**
     * Restore a soft-deleted quiz from any user's recycle bin.
     *
     * @param quizId The ID of the quiz to restore
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun restoreQuiz(quizId: String): Result<Unit>

    /**
     * Force publish a quiz (set isPublic = true, isDraft = false).
     * Useful for featuring community quizzes.
     *
     * @param quizId The ID of the quiz to publish
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun forcePublishQuiz(quizId: String): Result<Unit>

    /**
     * Unpublish a quiz (set isPublic = false).
     * Useful for moderating inappropriate content.
     *
     * @param quizId The ID of the quiz to unpublish
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun unpublishQuiz(quizId: String): Result<Unit>

    // ========== ATTEMPT MANAGEMENT ==========

    /**
     * Retrieve all quiz attempts in the system.
     * Emits updates whenever attempt data changes.
     *
     * @return Flow emitting list of all attempts
     */
    fun getAllAttempts(): Flow<List<Attempt>>

    /**
     * Delete an attempt record.
     *
     * @param attemptId The ID of the attempt to delete
     * @return [Result.success] on success, or [Result.failure] with error
     */
    suspend fun deleteAttempt(attemptId: String): Result<Unit>

    // ========== STATISTICS ==========

    /**
     * Retrieve system-wide statistics for the admin dashboard.
     * Emits updates whenever underlying data changes.
     *
     * @return Flow emitting current [SystemStats]
     */
    fun getSystemStats(): Flow<SystemStats>

    // ========== SEARCH ==========

    /**
     * Search for users by email or username.
     *
     * @param query Search query string
     * @return Flow emitting matching users
     */
    fun searchUsers(query: String): Flow<List<User>>

    /**
     * Search for quizzes by title, description, or author.
     *
     * @param query Search query string
     * @param includeDeleted Whether to include soft-deleted quizzes
     * @return Flow emitting matching quizzes
     */
    fun searchQuizzes(query: String, includeDeleted: Boolean = false): Flow<List<Quiz>>
}
