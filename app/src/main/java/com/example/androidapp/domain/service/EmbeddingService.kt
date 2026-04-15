package com.example.androidapp.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Provides dense vector embeddings for text inputs.
 *
 * The embedding space is shared between queries and documents:
 * cosine similarity between two embeddings reflects semantic similarity.
 * Implementations must be thread-safe; [embed] may be called from
 * a coroutine pool.
 */
interface EmbeddingService {

    /** True when the underlying model is loaded and ready for inference. */
    val isReady: StateFlow<Boolean>

    /**
     * Converts [text] into a unit-normalized dense vector.
     *
     * @return A [FloatArray] of length [EMBEDDING_DIM], or null if the
     *         model is not yet ready.
     */
    suspend fun embed(text: String): FloatArray?

    /**
     * Embeds a batch of texts. Falls back to sequential [embed] calls
     * when the runtime does not support native batching.
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray?>

    companion object {
        /** Dimensionality of the MediaPipe Universal Sentence Encoder output. */
        const val EMBEDDING_DIM = 100

        /**
         * Current embedding model version. Bump when upgrading the TFLite model
         * to trigger automatic re-indexing of stale embeddings via the
         * background worker.
         */
        const val CURRENT_MODEL_VERSION = 1
    }
}
