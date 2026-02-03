package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.androidapp.data.local.entity.ChoiceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Choice entities.
 * Provides methods to query, insert, update, and delete choices in the local database.
 */
@Dao
interface ChoiceDao {

    /**
     * Get all choices for a specific question, ordered by position.
     */
    @Query("SELECT * FROM choices WHERE question_id = :questionId ORDER BY position ASC")
    fun getChoicesByQuestionId(questionId: String): Flow<List<ChoiceEntity>>

    /**
     * Get all choices for a question (suspend version for one-time queries).
     */
    @Query("SELECT * FROM choices WHERE question_id = :questionId ORDER BY position ASC")
    suspend fun getChoicesByQuestionIdOnce(questionId: String): List<ChoiceEntity>

    /**
     * Get a single choice by ID.
     */
    @Query("SELECT * FROM choices WHERE id = :choiceId")
    suspend fun getChoiceById(choiceId: String): ChoiceEntity?

    /**
     * Get correct choices for a question.
     */
    @Query("SELECT * FROM choices WHERE question_id = :questionId AND is_correct = 1")
    suspend fun getCorrectChoices(questionId: String): List<ChoiceEntity>

    /**
     * Insert a choice, replacing if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChoice(choice: ChoiceEntity)

    /**
     * Insert multiple choices.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChoices(choices: List<ChoiceEntity>)

    /**
     * Update an existing choice.
     */
    @Update
    suspend fun updateChoice(choice: ChoiceEntity)

    /**
     * Delete a choice.
     */
    @Delete
    suspend fun deleteChoice(choice: ChoiceEntity)

    /**
     * Delete all choices for a question.
     */
    @Query("DELETE FROM choices WHERE question_id = :questionId")
    suspend fun deleteChoicesByQuestionId(questionId: String)
}
