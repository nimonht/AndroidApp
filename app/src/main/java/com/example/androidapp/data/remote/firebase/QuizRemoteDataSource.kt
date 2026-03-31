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
     * Saves a quiz and its questions using batch writes.
     * Deletes existing choices before saving to avoid stale data, and chunks
     * operations to respect Firestore's 500-operations-per-batch limit.
     * @param quizId The document ID for the quiz.
     * @param quizDto The quiz data to save.
     * @param questionDtos The question list to save in the subcollection.
     */
    suspend fun saveQuiz(quizId: String, quizDto: QuizDto, questionDtos: List<QuestionDto>) {
        val quizRef = firestore.collection(FirestoreCollections.QUIZZES).document(quizId)

        // First, save the quiz document itself
        quizRef.set(quizDto).await()

        // Process each question individually to handle choice deletion
        // This approach avoids batch size limits and ensures stale choices are removed
        questionDtos.forEach { q ->
            val questionRef = quizRef.collection(FirestoreCollections.QUESTIONS).document(q.id)

            // Delete existing choices first to avoid leaving stale documents
            val existingChoices = questionRef
                .collection(FirestoreCollections.CHOICES)
                .get()
                .await()

            val batch = firestore.batch()
            existingChoices.documents.forEach { batch.delete(it.reference) }

            // Save question without embedded choices
            val questionWithoutChoices = q.copy(choices = emptyList())
            batch.set(questionRef, questionWithoutChoices)

            // Save each choice in the choices subcollection
            q.choices.forEach { choice ->
                val choiceRef = questionRef.collection(FirestoreCollections.CHOICES).document(choice.id)
                batch.set(choiceRef, choice)
            }

            batch.commit().await()
        }
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

    /**
     * Deletes all soft-deleted quizzes owned by the user permanently from Firestore.
     * Uses batch writes for efficiency.
     */
    suspend fun emptyTrash(userId: String) {
        val deletedQuizzesQuery = firestore.collection(FirestoreCollections.QUIZZES)
            .whereEqualTo(FirestoreCollections.Fields.OWNER_ID, userId)
            .whereNotEqualTo(FirestoreCollections.Fields.DELETED_AT, null)
            .get()
            .await()

        if (deletedQuizzesQuery.isEmpty) return

        // Firestore batches can hold up to 500 operations
        val batches = deletedQuizzesQuery.documents.chunked(500)
        for (chunk in batches) {
            val batch = firestore.batch()
            for (doc in chunk) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }
}
