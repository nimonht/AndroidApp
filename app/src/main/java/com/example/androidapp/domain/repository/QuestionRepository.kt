package com.example.androidapp.domain.repository

interface QuestionRepository {
    suspend fun addQuestion(quizId: String, question: Any): Result<Boolean>
}