package com.example.androidapp.domain.model

/**
 * Domain model representing a question contributed to the shared question pool.
 *
 * @property id Unique identifier for this pool item.
 * @property question The question data reused from the [Question] model.
 * @property contributorId The user ID of the contributor (may be blank/null if anonymized).
 * @property sourceQuizId The original quiz ID this question was contributed from.
 * @property tags Tags for categorization and filtered queries.
 * @property usageCount Number of times this question has been used in auto-generated quizzes.
 * @property isActive Whether this contribution is active. Set to false on revocation.
 * @property createdAtMillis Timestamp (in milliseconds) when this item was added to the pool.
 */
data class QuestionPoolItem(
    val id: String,
    val question: Question,
    val contributorId: String?,
    val sourceQuizId: String,
    val tags: List<String>,
    val usageCount: Int,
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)