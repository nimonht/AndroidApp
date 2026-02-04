package com.example.androidapp.domain.model

data class Quiz(
    val id: String,
    val title: String,
    val authorName: String,
    val thumbnailUrl: String?, // Có thể null nếu không có ảnh
    val tags: List<String>,
    val questionCount: Int,
    val attemptCount: Int
)