package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Upsert
import androidx.room.Query
import androidx.room.Update
import com.example.androidapp.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Question entities.
 * Provides methods to query, insert, update, and delete questions in the local database.
 */
@Dao
interface QuestionDao {

    /**
     * Get all questions for a specific quiz, ordered by position.
     */
    @Query("SELECT * FROM questions WHERE quiz_id = :quizId ORDER BY position ASC")
    fun getQuestionsByQuizId(quizId: String): Flow<List<QuestionEntity>>

    /**
     * Get all questions for a quiz (suspend version for one-time queries).
     */
    @Query("SELECT * FROM questions WHERE quiz_id = :quizId ORDER BY position ASC")
    suspend fun getQuestionsByQuizIdOnce(quizId: String): List<QuestionEntity>

    /**
     * Get a single question by ID.
     */
    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    /**
     * Get the count of questions for a quiz.
     */
    @Query("SELECT COUNT(*) FROM questions WHERE quiz_id = :quizId")
    suspend fun getQuestionCount(quizId: String): Int

    /**
     * Upsert a question, or update it if it already exists.
     */
    @Upsert
    suspend fun upsertQuestion(question: QuestionEntity)

    /**
     * Update an existing question.
     */
    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    /**
     * Update the position of a question.
     */
    @Query("UPDATE questions SET position = :position WHERE id = :questionId")
    suspend fun updatePosition(questionId: String, position: Int)

    /**
     * Delete a question.
     */
    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)
}
