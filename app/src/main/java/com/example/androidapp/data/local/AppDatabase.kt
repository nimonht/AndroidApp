package com.example.androidapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.androidapp.data.local.converter.Converters
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.data.local.entity.AttemptEntity
import com.example.androidapp.data.local.entity.ChoiceEntity
import com.example.androidapp.data.local.entity.QuestionEntity
import com.example.androidapp.data.local.entity.QuizEntity
import com.example.androidapp.data.local.entity.UserEntity

/**
 * Room Database for QuizCode application.
 * Provides local SQLite storage for offline-first functionality.
 *
 * Entities:
 * - [QuizEntity]: Stores quiz metadata
 * - [QuestionEntity]: Stores questions with 2-10 flexible choices
 * - [ChoiceEntity]: Stores answer choices
 * - [AttemptEntity]: Stores quiz attempt history
 * - [UserEntity]: Stores user profile data
 */
@Database(
    entities = [
        QuizEntity::class,
        QuestionEntity::class,
        ChoiceEntity::class,
        AttemptEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Get the Quiz DAO for quiz-related database operations.
     */
    abstract fun quizDao(): QuizDao

    /**
     * Get the Question DAO for question-related database operations.
     */
    abstract fun questionDao(): QuestionDao

    /**
     * Get the Choice DAO for choice-related database operations.
     */
    abstract fun choiceDao(): ChoiceDao

    /**
     * Get the Attempt DAO for attempt-related database operations.
     */
    abstract fun attemptDao(): AttemptDao

    /**
     * Get the User DAO for user-related database operations.
     */
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "quizcode_database"
    }
}
