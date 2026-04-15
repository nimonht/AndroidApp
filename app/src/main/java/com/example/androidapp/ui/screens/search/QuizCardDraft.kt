package com.example.androidapp.ui.screens.search

/**
 * UI-only display model for quiz cards in search results and discover sections.
 * Separated from the domain [Quiz] model to keep the UI layer independent.
 */
data class QuizCardDraft(
    val id: String,
    val title: String,
    val authorName: String,
    val questionCount: Int,
    val attemptCount: Int,
    val coverImageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val updatedAt: Long = 0L
)
