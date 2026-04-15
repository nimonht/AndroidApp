package com.example.androidapp.data.worker

import android.content.Context
import android.util.Log

import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.androidapp.QuizzezApplication
import com.example.androidapp.domain.service.EmbeddingService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Background worker that computes and persists text embeddings for all
 * quizzes that lack an up-to-date embedding vector.
 *
 * Triggered:
 * - On app start if any quizzes have null or stale embeddings.
 * - After a quiz is created or updated (via [enqueueForQuiz]).
 * - After a Firestore sync deposits new quizzes into Room.
 *
 * Runs in batches of [BATCH_SIZE] on [kotlinx.coroutines.Dispatchers.IO].
 */
class EmbeddingIndexWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as QuizzezApplication).appContainer
        val quizDao = container.quizDao
        val embeddingService = container.embeddingService

        // Wait up to 30 seconds for the model to be ready
        val ready = withTimeoutOrNull(MODEL_READY_TIMEOUT_MS) {
            embeddingService.isReady.first { it }
        } != null

        if (!ready) {
            Log.w(TAG, "Embedding model not ready after ${MODEL_READY_TIMEOUT_MS}ms; retrying later")
            return Result.retry()
        }

        val singleQuizId = inputData.getString(KEY_QUIZ_ID)
        val modelVersion = inputData.getInt(KEY_MODEL_VERSION, CURRENT_MODEL_VERSION)

        return try {
            if (singleQuizId != null) {
                indexSingleQuiz(quizDao, embeddingService, singleQuizId, modelVersion)
            } else {
                indexAllQuizzes(quizDao, embeddingService, modelVersion)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Embedding indexing failed", e)
            Result.retry()
        }
    }

    private suspend fun indexAllQuizzes(
        quizDao: com.example.androidapp.data.local.dao.QuizDao,
        embeddingService: EmbeddingService,
        modelVersion: Int
    ) {
        var totalIndexed = 0
        while (true) {
            val batch = quizDao.getQuizzesNeedingEmbedding(modelVersion, BATCH_SIZE)
            if (batch.isEmpty()) break

            batch.forEach { proj ->
                val text = buildIndexText(proj.title, proj.description, proj.tags)
                val embedding = embeddingService.embed(text) ?: return@forEach
                val bytes = floatArrayToBytes(embedding)
                quizDao.updateEmbedding(proj.id, bytes, modelVersion)
                totalIndexed++
            }

            setProgress(
                workDataOf(
                    PROGRESS_INDEXED to totalIndexed
                )
            )
        }
        Log.d(TAG, "Full index pass complete: $totalIndexed quizzes indexed")
    }

    /**
     * Indexes a single quiz by ID.
     *
     * Uses a dedicated DAO query that fetches the specific quiz rather than
     * a batch query with LIMIT 1 (which could return a different quiz).
     */
    private suspend fun indexSingleQuiz(
        quizDao: com.example.androidapp.data.local.dao.QuizDao,
        embeddingService: EmbeddingService,
        quizId: String,
        modelVersion: Int
    ) {
        val proj = quizDao.getQuizNeedingEmbeddingById(quizId, modelVersion)
        if (proj == null) {
            Log.d(TAG, "Quiz $quizId does not need embedding update")
            return
        }
        val text = buildIndexText(proj.title, proj.description, proj.tags)
        val embedding = embeddingService.embed(text) ?: return
        val bytes = floatArrayToBytes(embedding)
        quizDao.updateEmbedding(quizId, bytes, modelVersion)
        Log.d(TAG, "Indexed quiz $quizId")
    }

    /**
     * Constructs the text to embed for a quiz.
     * Title is repeated for emphasis (simple weighting trick).
     */
    private fun buildIndexText(title: String, description: String?, tags: String): String {
        val tagPart = tags.replace(",", " ").trim()
        return "$title $title ${description.orEmpty()} $tagPart".trim()
    }

    private fun floatArrayToBytes(value: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(value.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(value)
        return buffer.array()
    }

    companion object {
        private const val TAG = "EmbeddingIndexWorker"
        const val WORK_NAME_FULL = "embedding_index_full"
        const val WORK_NAME_INCREMENTAL = "embedding_index_incr"
        const val KEY_QUIZ_ID = "quiz_id"
        const val KEY_MODEL_VERSION = "model_version"
        const val CURRENT_MODEL_VERSION = EmbeddingService.CURRENT_MODEL_VERSION
        const val PROGRESS_INDEXED = "progress_indexed"
        private const val BATCH_SIZE = 20

        /** Maximum time to wait for the embedding model to initialize. */
        private const val MODEL_READY_TIMEOUT_MS = 30_000L

        /** Enqueue a full re-index. */
        fun enqueueFullIndex(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<EmbeddingIndexWorker>()
                .setInputData(workDataOf(KEY_MODEL_VERSION to CURRENT_MODEL_VERSION))
                .build()
            workManager.enqueueUniqueWork(
                WORK_NAME_FULL,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /**
         * Enqueues a full re-index only if no index job is already pending or running.
         * Safe to call frequently (e.g., after each Firestore sync batch) because
         * [ExistingWorkPolicy.KEEP] is a no-op when the work is already enqueued.
         */
        fun enqueueIfNeeded(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<EmbeddingIndexWorker>()
                .setInputData(workDataOf(KEY_MODEL_VERSION to CURRENT_MODEL_VERSION))
                .build()
            workManager.enqueueUniqueWork(
                WORK_NAME_FULL,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        /** Enqueue embedding update for a single quiz after create/edit. */
        fun enqueueForQuiz(workManager: WorkManager, quizId: String) {
            val request = OneTimeWorkRequestBuilder<EmbeddingIndexWorker>()
                .setInputData(
                    workDataOf(
                        KEY_QUIZ_ID to quizId,
                        KEY_MODEL_VERSION to CURRENT_MODEL_VERSION
                    )
                )
                .build()
            workManager.enqueueUniqueWork(
                "$WORK_NAME_INCREMENTAL-$quizId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
