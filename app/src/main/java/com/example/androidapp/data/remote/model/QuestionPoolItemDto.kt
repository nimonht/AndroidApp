package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for a question pool item.
 *
 * This structure aligns with the Firestore `questionPool` schema which stores
 * question data in a flattened format with top-level fields.
 *
 * Boolean properties prefixed with `is` use explicit `@PropertyName` annotations
 * to prevent Java Bean naming conventions from stripping the prefix during
 * Firestore serialization (e.g. `isActive` being stored as `active`).
 */
data class QuestionPoolItemDto(
    @DocumentId val id: String = "",
    val content: String = "",
    val choices: List<PoolChoiceDto> = emptyList(),
    val correctIndices: List<Int> = emptyList(),
    val tags: List<String> = emptyList(),
    val mediaUrl: String? = null,
    val points: Int = 5,
    @get:PropertyName("allowMultipleCorrect") @set:PropertyName("allowMultipleCorrect")
    var allowMultipleCorrect: Boolean = false,
    val contributorId: String? = null,
    val sourceQuizId: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val usageCount: Int = 0,
    val createdAt: Timestamp? = null
)

/**
 * Simplified choice DTO for question pool items.
 * Matches the Firestore schema for choices in the pool.
 *
 * Uses explicit `@PropertyName` on [isCorrect] to ensure the field name
 * is preserved during Firestore Java Bean serialization.
 */
data class PoolChoiceDto(
    val content: String = "",
    @get:PropertyName("isCorrect") @set:PropertyName("isCorrect")
    var isCorrect: Boolean = false
)
