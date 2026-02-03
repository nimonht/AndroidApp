package com.example.androidapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a Question stored locally for offline cache.
 * Maps to the 'questions' table in the local SQLite database.
 * Supports 2-10 flexible choices per question.
 */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quiz_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["quiz_id"])]
)
data class QuestionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "quiz_id")
    val quizId: String,

    val content: String,

    @ColumnInfo(name = "media_url")
    val mediaUrl: String? = null,

    val explanation: String? = null,

    val points: Int = 1,

    val position: Int = 0,

    @ColumnInfo(name = "choice_count")
    val choiceCount: Int = 4, // 2-10 choices, flexible

    @ColumnInfo(name = "allow_multiple_correct")
    val allowMultipleCorrect: Boolean = false
)
