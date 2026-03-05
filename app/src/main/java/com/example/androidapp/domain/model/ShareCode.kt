package com.example.androidapp.domain.model

data class ShareCode(
    val code: String, // Mã 6 ký tự
    val quizId: String,
    val expiresAtMillis: Long?
)