package com.example.androidapp.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search virtual table backed by [QuizEntity] content.
 * Room creates FTS triggers automatically to keep this table in sync
 * with the [QuizEntity] (quizzes) table whenever rows are inserted,
 * updated, or deleted.
 *
 * Only [title] and [tags] are indexed to limit FTS table size.
 * Use [QuizDao.searchQuizzesLimitedFts] and [QuizDao.searchQuizzesCountFts]
 * to query via MATCH instead of LIKE for dramatically faster text search.
 */
@Entity(tableName = "quizzes_fts")
@Fts4(contentEntity = QuizEntity::class)
data class QuizFtsEntity(
    val title: String,
    val tags: String
)
