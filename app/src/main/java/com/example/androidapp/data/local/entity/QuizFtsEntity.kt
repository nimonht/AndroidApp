package com.example.androidapp.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search virtual table backed by [QuizEntity] content.
 *
 * Room creates FTS triggers automatically to keep this table in sync
 * with the [QuizEntity] (quizzes) table whenever rows are inserted,
 * updated, or deleted.
 *
 * Indexes [title], [description], and [tags] for fast MATCH queries.
 * Use [com.example.androidapp.data.local.dao.QuizDao.searchQuizzesFts]
 * to query via MATCH instead of LIKE for dramatically faster text search.
 */
@Entity(tableName = "quizzes_fts")
@Fts4(contentEntity = QuizEntity::class)
data class QuizFtsEntity(
    val title: String,
    val description: String?,
    val tags: String
)
