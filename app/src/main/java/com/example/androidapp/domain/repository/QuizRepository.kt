package com.example.androidapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    fun getMyQuizzes(userId: String): Flow<List<Any>>
    suspend fun createQuiz(quiz: Any): Result<String>
}