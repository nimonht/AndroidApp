package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.ChoiceDto
import com.example.androidapp.data.remote.model.QuestionDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class QuestionRemoteDataSource(private val firestore: FirebaseFirestore) {

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

    suspend fun getChoicesForQuestion(quizId: String, questionId: String): List<ChoiceDto> {
        return firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .collection(FirestoreCollections.QUESTIONS)
            .document(questionId)
            .collection(FirestoreCollections.CHOICES)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(ChoiceDto::class.java) }
            .sortedBy { it.position }
    }

    suspend fun saveQuestion(
        quizId: String,
        questionDto: QuestionDto,
        choiceDtos: List<ChoiceDto>
    ) {
        val questionRef = firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .collection(FirestoreCollections.QUESTIONS)
            .document(questionDto.id)

        // Delete existing choices first to avoid leaving stale documents when choice IDs change.
        val existingChoices = questionRef
            .collection(FirestoreCollections.CHOICES)
            .get()
            .await()

        val batch = firestore.batch()
        existingChoices.documents.forEach { batch.delete(it.reference) }
        batch.set(questionRef, questionDto)

        choiceDtos.forEach { choice ->
            val choiceRef = questionRef
                .collection(FirestoreCollections.CHOICES)
                .document(choice.id)
            batch.set(choiceRef, choice)
        }

        batch.commit().await()
    }

    suspend fun deleteQuestion(quizId: String, questionId: String) {
        val questionRef = firestore.collection(FirestoreCollections.QUIZZES)
            .document(quizId)
            .collection(FirestoreCollections.QUESTIONS)
            .document(questionId)

        val choices = questionRef
            .collection(FirestoreCollections.CHOICES)
            .get()
            .await()

        val batch = firestore.batch()
        choices.documents.forEach { batch.delete(it.reference) }
        batch.delete(questionRef)
        batch.commit().await()
    }

    suspend fun updateQuestionPositions(quizId: String, questionPositions: Map<String, Int>) {
        val batch = firestore.batch()
        questionPositions.forEach { (questionId, position) ->
            val ref = firestore.collection(FirestoreCollections.QUIZZES)
                .document(quizId)
                .collection(FirestoreCollections.QUESTIONS)
                .document(questionId)
            batch.update(ref, "position", position)
        }
        batch.commit().await()
    }
}
