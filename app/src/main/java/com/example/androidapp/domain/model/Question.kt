package com.example.androidapp.domain.model

data class Question(
    val id: String,
    val content: String,
    val choices: List<Choice>, // Hỗ trợ flexible choices
    val isMultiSelect: Boolean = false,
    val explanation: String? = null
)