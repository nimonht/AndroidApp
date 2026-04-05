package com.example.androidapp.domain.model

/**
 * Domain model representing system-wide statistics for the admin dashboard.
 *
 * Provides an overview of key metrics across the application.
 *
 * @property totalUsers Total number of registered users (excluding guests)
 * @property totalQuizzes Total number of quizzes created (excluding soft-deleted)
 * @property totalAttempts Total number of quiz attempts across all users
 * @property totalQuestionsInPool Total number of questions in the shared pool
 * @property activeUsers Number of users who have activity in the last 30 days
 * @property publicQuizzes Number of quizzes marked as public
 * @property privateQuizzes Number of quizzes that are private (not public, not deleted)
 * @property deletedQuizzes Number of soft-deleted quizzes in recycle bin
 * @property adminUsers Number of users with admin role
 */
data class SystemStats(
    val totalUsers: Int = 0,
    val totalQuizzes: Int = 0,
    val totalAttempts: Int = 0,
    val totalQuestionsInPool: Int = 0,
    val activeUsers: Int = 0,
    val publicQuizzes: Int = 0,
    val privateQuizzes: Int = 0,
    val deletedQuizzes: Int = 0,
    val adminUsers: Int = 0
) {
    /**
     * Average number of attempts per quiz.
     * Returns 0.0 if no quizzes exist.
     */
    val averageAttemptsPerQuiz: Double
        get() = if (totalQuizzes > 0) totalAttempts.toDouble() / totalQuizzes else 0.0

    /**
     * Percentage of active users.
     * Returns 0.0 if no users exist.
     */
    val activeUserPercentage: Double
        get() = if (totalUsers > 0) (activeUsers.toDouble() / totalUsers) * 100 else 0.0

    /**
     * Percentage of public quizzes.
     * Returns 0.0 if no quizzes exist.
     */
    val publicQuizPercentage: Double
        get() = if (totalQuizzes > 0) (publicQuizzes.toDouble() / totalQuizzes) * 100 else 0.0
}
