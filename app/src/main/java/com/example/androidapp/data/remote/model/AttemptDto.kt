package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class AttemptDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quizId: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val answers: Map<String, List<String>> = emptyMap(), // Firebase hỗ trợ Map<String, List> tự động
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null
)
