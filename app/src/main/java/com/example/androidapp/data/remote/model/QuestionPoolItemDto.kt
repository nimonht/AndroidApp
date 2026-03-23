package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firestore DTO for a question pool item.
 *
 * This structure aligns with the Firestore `questionPool` schema which stores
 * question data in a flattened format with top-level fields.
 */
data class QuestionPoolItemDto(
    @DocumentId val id: String = "",
    val content: String = "",
    val choices: List<PoolChoiceDto> = emptyList(),
    val correctIndices: List<Int> = emptyList(),
    val tags: List<String> = emptyList(),
    val mediaUrl: String? = null,
    val points: Int = 5,
    val allowMultipleCorrect: Boolean = false,
    val contributorId: String? = null,
    val sourceQuizId: String = "",
    val isActive: Boolean = true,
    val usageCount: Int = 0,
    val createdAt: Timestamp? = null
)

/**
 * Simplified choice DTO for question pool items.
 * Matches the Firestore schema for choices in the pool.
 */
data class PoolChoiceDto(
    val content: String = "",
    val isCorrect: Boolean = false
)