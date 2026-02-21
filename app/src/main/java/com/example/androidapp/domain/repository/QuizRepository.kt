package com.example.androidapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    // 1. Lấy danh sách quiz
    fun getMyQuizzes(userId: String): Flow<List<Any>>

    // 2. Tìm quiz bằng mã chia sẻ
    suspend fun getQuizByShareCode(code: String): Any?

    // 3. Tạo quiz mới
    suspend fun createQuiz(quiz: Any, questions: List<Any>): Result<String>
}