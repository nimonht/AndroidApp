package com.example.androidapp.domain.model

/**
 * Domain model representing a single quiz attempt by a user.
 *
 * @property id Unique identifier for this attempt.
 * @property userId Identifier of the user who took the quiz.
 * @property quizId Identifier of the quiz that was attempted.
 * @property score Points earned by the user (sum of points from correctly answered questions).
 * @property maxScore Maximum possible score for the quiz (sum of all question point values).
 * @property answers Map of answers: key is the question ID, value is a list of selected choice IDs (supports multi-select).
 * @property startTimeMillis Timestamp in milliseconds when the attempt started.
 * @property endTimeMillis Timestamp in milliseconds when the attempt ended, or null if not yet finished.
 * @property questionOrder Ordered list of question IDs as they were presented during the quiz attempt.
 */
data class Attempt(
    val id: String,
    val userId: String,
    val quizId: String,
    val score: Int,
    val maxScore: Int,
    val answers: Map<String, List<String>>,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val questionOrder: List<String> = emptyList()
)
