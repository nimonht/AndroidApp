package com.example.androidapp.domain.model

data class QuestionPoolItem(
    val id: String,
    val question: Question, // Tái sử dụng model Question
    val authorId: String,
    val tags: List<String>,
    val usageCount: Int
)