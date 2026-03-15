package com.example.androidapp.domain.model

data class Attempt(
    val id: String,
    val userId: String,
    val quizId: String,
    val score: Int,
    val totalQuestions: Int,
    // Map lưu đáp án: Key là ID câu hỏi, Value là danh sách ID đáp án (hỗ trợ multi-select)
    val answers: Map<String, List<String>>,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    // Ordered list of question IDs as they were presented during the quiz attempt
    val questionOrder: List<String> = emptyList()
)