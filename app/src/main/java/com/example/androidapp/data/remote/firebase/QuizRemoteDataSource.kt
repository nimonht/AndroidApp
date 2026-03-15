package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.QuestionDto
import com.example.androidapp.data.remote.model.QuizDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Remote data source for quiz and question Firestore operations.
 * Uses [callbackFlow] with [addSnapshotListener] for real-time streams
 * and batch writes for multi-document mutations.
 */
class QuizRemoteDataSource(private val firestore: FirebaseFirestore) {

    /**
     * Emits real-time public quizzes ordered by attempt count descending.
     */
    fun getPublicQuizzes(): Flow<List<QuizDto>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.IS_PUBLIC, true)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val quizzes = snapshot?.documents?.mapNotNull {
                    it.toObject(QuizDto::class.java)
                } ?: emptyList()
                trySend(quizzes)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Emits real-time quizzes owned by [userId].
     */
    fun getQuizzesByOwner(userId: String): Flow<List<QuizDto>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val quizzes = snapshot?.documents?.mapNotNull {
                    it.toObject(QuizDto::class.java)
                } ?: emptyList()
                trySend(quizzes)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Fetches a single quiz by ID.
     */
    suspend fun getQuizById(quizId: String): QuizDto? {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .get()
            .await()
            .toObject(QuizDto::class.java)
    }

    /**
     * Fetches a quiz by its share code.
     */
    suspend fun getQuizByShareCode(shareCode: String): QuizDto? {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.SHARE_CODE, shareCode)
            .whereEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject(QuizDto::class.java)
    }

    /**
     * Fetches questions for a quiz by quiz ID.
     */
    suspend fun getQuestionsForQuiz(quizId: String): List<QuestionDto> {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .collection(FirestoreCollections.QUESTIONS)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuestionDto::class.java) }
            .sortedBy { it.position }
    }

    /**
     * Saves a quiz and its questions using a batch write.
     * @param quizId The document ID for the quiz.
     * @param quizDto The quiz data to save.
     * @param questionDtos The question list to save in the subcollection.
     */
    suspend fun saveQuiz(quizId: String, quizDto: QuizDto, questionDtos: List<QuestionDto>) {
        val batch = firestore.batch()
        val quizRef = firestore.collection(FirestoreCollections.QUIZZES).document(quizId)
        batch.set(quizRef, quizDto)

        questionDtos.forEach { q ->
            val questionRef = quizRef.collection(FirestoreCollections.QUESTIONS).document(q.id)
            batch.set(questionRef, q)
        }

        batch.commit().await()
    }

    /**
     * Soft-deletes a quiz by setting the deletedAt timestamp.
     */
    suspend fun softDeleteQuiz(quizId: String, deletedAt: com.google.firebase.Timestamp) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(FirestoreCollections.Fields.DELETED_AT, deletedAt)
            .await()
    }

    /**
     * Restores a soft-deleted quiz by clearing deletedAt.
     */
    suspend fun restoreQuiz(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(FirestoreCollections.Fields.DELETED_AT, null)
            .await()
    }

    /**
     * Permanently deletes a quiz document.
     */
    suspend fun permanentlyDeleteQuiz(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .delete()
            .await()
    }

    /**
     * Atomically increments the attempt count for a quiz.
     */
    suspend fun incrementAttemptCount(quizId: String) {
        firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .update(
                FirestoreCollections.Fields.ATTEMPT_COUNT,
                com.google.firebase.firestore.FieldValue.increment(1)
            )
            .await()
    }
}

