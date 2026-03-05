package com.example.androidapp.data.repository

import com.example.androidapp.domain.repository.QuestionRepository
import com.google.firebase.firestore.FirebaseFirestore

class QuestionRepositoryImpl(
    private val firestore: FirebaseFirestore
) : QuestionRepository {

    override suspend fun addQuestion(quizId: String, question: Any): Result<Boolean> {
        return try {
            // Tìm đúng bài thi (quizzes) -> Tìm túi chứa câu hỏi (questions) -> Bỏ câu hỏi mới vô
            firestore.collection("quizzes")
                .document(quizId)
                .collection("questions")
                .document()
            //  .set(question).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}