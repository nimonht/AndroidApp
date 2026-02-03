package com.example.androidapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a quiz Attempt stored locally.
 * Maps to the 'attempts' table in the local SQLite database.
 */
@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quiz_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["quiz_id"]), Index(value = ["user_id"])]
)
data class AttemptEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "quiz_id")
    val quizId: String,

    @ColumnInfo(name = "user_id")
    val userId: String, // Can be "guest_xxx" for guest users

    @ColumnInfo(name = "question_order")
    val questionOrder: String = "", // Stored as comma-separated IDs

    @ColumnInfo(name = "choice_orders")
    val choiceOrders: String = "", // Stored as JSON

    val answers: String = "", // Stored as JSON: Map<questionId, choiceId>

    @ColumnInfo(name = "multi_answers")
    val multiAnswers: String = "", // Stored as JSON: Map<questionId, List<choiceId>>

    val score: Int = 0,

    @ColumnInfo(name = "max_score")
    val maxScore: Int = 0,

    @ColumnInfo(name = "started_at")
    val startedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "finished_at")
    val finishedAt: Long? = null
)
