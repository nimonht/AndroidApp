package com.example.androidapp.data.remote.model

import com.google.firebase.firestore.DocumentId

data class QuestionPoolItemDto(
    @DocumentId val id: String = "",
    val question: QuestionDto = QuestionDto(),
    val authorId: String = "",
    val tags: List<String> = emptyList(),
    val usageCount: Int = 0
)