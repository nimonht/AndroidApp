package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firestore DTO for a quiz attempt.
 *
 * Field names align with the Firestore `attempts` schema:
 * - [startedAt] / [finishedAt] for timestamps
 * - [maxScore] for the maximum achievable score
 * - [choiceOrders] for the shuffled choice order per question
 * - [answers] for single-answer backward compat
 * - [multiAnswers] for multi-select answers
 */
data class AttemptDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quizId: String = "",
    val questionOrder: List<String> = emptyList(),
    val choiceOrders: Map<String, List<String>> = emptyMap(),
    val answers: Map<String, String> = emptyMap(),
    val multiAnswers: Map<String, List<String>> = emptyMap(),
    val score: Int = 0,
    val maxScore: Int = 0,
    val startedAt: Timestamp? = null,
    val finishedAt: Timestamp? = null
)
