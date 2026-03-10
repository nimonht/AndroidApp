package com.example.androidapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.data.local.converter.Converters
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.data.local.entity.AttemptEntity
import com.example.androidapp.data.local.entity.ChoiceEntity
import com.example.androidapp.data.local.entity.PendingSyncEntity
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
 * - [PendingSyncEntity]: Tracks pending sync operations
 */
@Database(
    entities = [
        QuizEntity::class,
        QuestionEntity::class,
        ChoiceEntity::class,
        AttemptEntity::class,
        UserEntity::class,
        PendingSyncEntity::class
    ],
    version = 2,
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

    /**
     * Get the PendingSync DAO for sync queue operations.
     */
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        const val DATABASE_NAME = "quizcode_database"

        /**
         * Migration from version 1 to 2.
         * Adds the pending_sync_operations table for offline sync queue.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_sync_operations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entity_type` TEXT NOT NULL,
                        `entity_id` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `payload` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `retry_count` INTEGER NOT NULL DEFAULT 0,
                        `max_retries` INTEGER NOT NULL DEFAULT 3,
                        `error_message` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `last_attempt_at` INTEGER
                    )
                """.trimIndent())
            }
        }
    }
}
