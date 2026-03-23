package com.example.androidapp.data.remote.model

import com.google.firebase.firestore.DocumentId

/**
 * Firestore DTO for a question pool item.
 */
data class QuestionPoolItemDto(
    @DocumentId val id: String = "",
    val question: QuestionDto = QuestionDto(),
    val authorId: String = "",
    val tags: List<String> = emptyList(),
    val usageCount: Int = 0,
    val isActive: Boolean = true
)