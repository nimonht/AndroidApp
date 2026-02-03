package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
     * Insert a question, replacing if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    /**
     * Insert multiple questions.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    /**
     * Update an existing question.
     */
    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    /**
     * Delete a question.
     */
    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    /**
     * Delete all questions for a quiz.
     */
    @Query("DELETE FROM questions WHERE quiz_id = :quizId")
    suspend fun deleteQuestionsByQuizId(quizId: String)
}
