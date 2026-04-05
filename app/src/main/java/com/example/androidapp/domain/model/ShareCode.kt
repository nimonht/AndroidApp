package com.example.androidapp.domain.model

/**
 * Domain model representing a share code used to grant access to a quiz.
 *
 * @property code 6-character alphanumeric share code.
 * @property quizId The ID of the quiz this share code grants access to.
 * @property expiresAtMillis Optional expiration timestamp in milliseconds, or null if the code does not expire.
 */
data class ShareCode(
    val code: String,
    val quizId: String,
    val expiresAtMillis: Long?
)
