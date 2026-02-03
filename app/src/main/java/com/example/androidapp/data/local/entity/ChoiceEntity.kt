package com.example.androidapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a Choice for a Question.
 * Maps to the 'choices' table in the local SQLite database.
 */
@Entity(
    tableName = "choices",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["question_id"])]
)
data class ChoiceEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "question_id")
    val questionId: String,

    val content: String,

    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean = false,

    val position: Int = 0
)
