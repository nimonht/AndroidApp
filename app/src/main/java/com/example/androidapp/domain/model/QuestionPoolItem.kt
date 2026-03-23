package com.example.androidapp.domain.model

/**
 * Domain model representing a question contributed to the shared question pool.
 *
 * @property id Unique identifier for this pool item.
 * @property question The question data reused from the [Question] model.
 * @property authorId The user ID of the contributor (may be blank if anonymized).
 * @property tags Tags for categorization and filtered queries.
 * @property usageCount Number of times this question has been used in auto-generated quizzes.
 * @property isActive Whether this contribution is active. Set to false on revocation.
 */
data class QuestionPoolItem(
    val id: String,
    val question: Question,
    val authorId: String,
    val tags: List<String>,
    val usageCount: Int,
    val isActive: Boolean = true
)