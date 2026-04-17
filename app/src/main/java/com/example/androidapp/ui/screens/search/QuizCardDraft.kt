package com.example.androidapp.ui.screens.search

import com.example.androidapp.domain.model.Quiz

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

/**
 * Converts a [QuizCardDraft] to a domain [Quiz] for navigation purposes.
 *
 * @param isPublic Whether to set the quiz as public. Defaults to false.
 */
fun QuizCardDraft.toQuiz(isPublic: Boolean = false): Quiz = Quiz(
    id = id,
    ownerId = "",
    title = title,
    authorName = authorName,
    thumbnailUrl = coverImageUrl,
    tags = tags,
    questionCount = questionCount,
    attemptCount = attemptCount,
    isPublic = isPublic
)
