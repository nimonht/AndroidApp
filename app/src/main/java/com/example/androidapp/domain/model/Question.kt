package com.example.androidapp.domain.model

/**
 * Domain model representing a question in a quiz.
 */
data class Question(
    val id: String,
    val quizId: String = "",
    val content: String,
    val choices: List<Choice>, // Supports flexible choices
    val isMultiSelect: Boolean = false,
    val explanation: String? = null,
    val mediaUrl: String? = null,
    val points: Int = 1,
    val position: Int = 0
)
