package com.example.androidapp.data.repository

import com.example.androidapp.domain.repository.QuizRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class QuizRepositoryImpl(
    // Xin công cụ Firestore từ kho
    private val firestore: FirebaseFirestore
) : QuizRepository {

    // Tạo một cái bảng tên là "quizzes" trên mạng
    private val quizCollection = firestore.collection("quizzes")

    override fun getMyQuizzes(userId: String): Flow<List<Any>> = callbackFlow<List<Any>> {
        // Bật camera theo dõi (SnapshotListener) cái bảng "quizzes"
        val listener = quizCollection
            .whereEqualTo("ownerId", userId) // Chỉ lấy bài của mình
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // (Chỗ này mốt có Model thì nhả comment ra để xài)
                // val quizzes = snapshot?.toObjects(Quiz::class.java) ?: emptyList()
                // trySend(quizzes)
            }

        // Khi người dùng thoát màn hình thì tắt camera theo dõi cho đỡ tốn pin
        awaitClose { listener.remove() }
    }

    override suspend fun createQuiz(quiz: Any): Result<String> {
        return try {
            val docRef = quizCollection.document() // Mở một tờ giấy trắng
            // docRef.set(quiz).await() // Chép nội dung bài test lên giấy
            Result.success(docRef.id) // Trả về mã số của tờ giấy đó
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}