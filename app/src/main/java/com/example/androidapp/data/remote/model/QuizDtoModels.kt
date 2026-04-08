package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class ChoiceDto(
    @DocumentId val id: String = "",
    val content: String = "",
    @get:PropertyName("isCorrect") @set:PropertyName("isCorrect") var isCorrect: Boolean = false,
    val position: Int = 0
)

/**
 * Firestore DTO for a question document.
 *
 * Field names align with the Firestore `questions` schema:
 * - [allowMultipleCorrect] instead of `isMultiSelect`
 * - [choiceCount] denormalized count of choices
 * - [choices] embedded list (set to empty when choices are stored in subcollection)
 */
data class QuestionDto(
    @DocumentId val id: String = "",
    val content: String = "",
    val choices: List<ChoiceDto> = emptyList(),
    @get:PropertyName("allowMultipleCorrect") @set:PropertyName("allowMultipleCorrect") var allowMultipleCorrect: Boolean = false,
    val choiceCount: Int = 0,
    val explanation: String? = null,
    val mediaUrl: String? = null,
    val points: Int = 1,
    val position: Int = 0
)

/**
 * Firestore DTO for a quiz document.
 *
 * Includes [checksum] for SHA-256 integrity verification during cloud sync.
 */
data class QuizDto(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String? = null,
    val authorName: String = "",
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val questionCount: Int = 0,
    val attemptCount: Int = 0,
    @get:PropertyName("isPublic") @set:PropertyName("isPublic") var isPublic: Boolean = false,
    @get:PropertyName("isDraft") @set:PropertyName("isDraft") var isDraft: Boolean = false,
    val shareCode: String? = null,
    val checksum: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)
