package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ChoiceDto(
    val id: String = "",
    val content: String = "",
    val isCorrect: Boolean = false,
    val position: Int = 0
)

data class QuestionDto(
    val id: String = "", // Không dùng @DocumentId ở đây vì nó nằm trong mảng của Quiz
    val content: String = "",
    val choices: List<ChoiceDto> = emptyList(),
    val isMultiSelect: Boolean = false,
    val explanation: String? = null
)

data class QuizDto(
    @DocumentId val id: String = "",
    val title: String = "",
    val authorName: String = "",
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val questionCount: Int = 0,
    val attemptCount: Int = 0
)