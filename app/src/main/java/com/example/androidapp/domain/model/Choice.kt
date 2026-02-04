package com.example.androidapp.domain.model

// Định nghĩa cấu trúc của một Lựa chọn
data class Choice(
    val id: String,
    val content: String,
    val isCorrect: Boolean = false,
    val position: Int = 0
)

// (Bạn có thể để data class Question ở đây luôn cũng được, nhưng tạm thời ta cần Choice trước)