package com.example.androidapp.domain.model

/** Domain model representing a choice option within a quiz question. */
data class Choice(
    val id: String,
    val content: String,
    val isCorrect: Boolean = false,
    val position: Int = 0
)
