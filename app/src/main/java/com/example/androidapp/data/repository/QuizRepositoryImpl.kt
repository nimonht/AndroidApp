package com.example.androidapp.data.repository

import com.example.androidapp.domain.repository.QuizRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Code bám sát logic mục 5.2 trong tài liệu 03_frontend_design.md
class QuizRepositoryImpl(
    private val firestore: FirebaseFirestore,
    // private val quizDao: QuizDao // Tạm đóng comment lại chờ tạo Model sau
) : QuizRepository {

    private val quizzesRef = firestore.collection("quizzes")

    // Real-time updates with offline cache
    override fun getMyQuizzes(userId: String): Flow<List<Any>> = callbackFlow<List<Any>> {
        val listener = quizzesRef
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("deletedAt", null)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }

                // val quizzes = snapshot?.toObjects(Quiz::class.java) ?: emptyList()
                // trySend(quizzes)

                // Cache to Room for offline
                // launch { quizDao.insertAll(quizzes.map { it.toEntity() }) }
            }
        awaitClose { listener.remove() }
    }.catch {
        // Fallback to Room cache when offline
        // emitAll(quizDao.getAllQuizzes().map { it.map { e -> e.toDomain() } })
    }

    // Get quiz by share code
    override suspend fun getQuizByShareCode(code: String): Any? {
        val codeDoc = firestore.collection("shareCodes")
            .document(code).get().await()
        if (!codeDoc.exists()) return null

        val quizId = codeDoc.getString("quizId") ?: return null

        // return quizzesRef.document(quizId).get().await().toObject(Quiz::class.java)
        return null // Tạm return null chờ có Model Quiz
    }

    // Create quiz with all questions
    override suspend fun createQuiz(quiz: Any, questions: List<Any>): Result<String> {
        return try {
            val batch = firestore.batch()
            val quizRef = quizzesRef.document()

            // val checksum = ChecksumUtil.computeChecksum(quiz.title, questions)

            // Logic lưu quiz và questions lên Firestore bằng batch...

            batch.commit().await()
            Result.success(quizRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateShareCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}