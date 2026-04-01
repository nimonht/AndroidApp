package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.Attempt
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for quiz attempt operations.
 */
interface AttemptRepository {

    /**
     * Emits all attempts for a user, ordered by most recent first.
     */
    fun getAttemptsByUser(userId: String): Flow<List<Attempt>>

    /**
     * Emits all attempts for a specific quiz.
     */
    fun getAttemptsByQuiz(quizId: String): Flow<List<Attempt>>

    /**
     * Returns a single attempt by ID, or null if not found.
     */
    suspend fun getAttemptById(attemptId: String): Attempt?

    /**
     * Returns the most recent attempt for a user on a quiz, or null.
     */
    suspend fun getLatestAttempt(userId: String, quizId: String): Attempt?

    /**
     * Saves a new attempt. Writes to Room first, then syncs to Firestore.
     * @return [Result.success] with the generated attempt ID.
     */
    suspend fun saveAttempt(attempt: Attempt): Result<String>

    /**
     * Updates an existing attempt (e.g., when finishing).
     */
    suspend fun updateAttempt(attempt: Attempt): Result<Unit>

    /**
     * Links all attempts made by a guest to a registered user account.
     * Called after a guest signs up to preserve their quiz history.
     *
     * @param guestId The guest's temporary ID (e.g., "guest_UUID").
     * @param userId The newly registered user's ID.
     * @return [Result.success] with the count of migrated attempts.
     */
    suspend fun linkGuestAttempts(guestId: String, userId: String): Result<Int>
}

