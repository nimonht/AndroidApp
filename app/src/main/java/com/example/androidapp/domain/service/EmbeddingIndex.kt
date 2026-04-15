package com.example.androidapp.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only interface for the in-memory embedding vector index.
 *
 * Implementations cache pre-computed quiz embeddings and expose them
 * for fast cosine-similarity lookups during semantic search.
 * The UI and repository layers depend on this interface; the concrete
 * implementation lives in the data layer.
 */
interface EmbeddingIndex {

    /** True when at least one embedding has been loaded into the index. */
    val isReady: StateFlow<Boolean>

    /** Number of quiz embeddings currently in the index. */
    val size: Int

    /**
     * Returns an unmodifiable snapshot of all cached embeddings.
     *
     * Keys are quiz IDs, values are dense float vectors (typically 100-dim).
     * The returned map is safe to iterate concurrently with index updates.
     */
    fun snapshot(): Map<String, FloatArray>

    /**
     * Returns the embedding for a single quiz, or null if not indexed.
     *
     * @param quizId The quiz ID to look up.
     */
    operator fun get(quizId: String): FloatArray?

    /**
     * Requests a full re-index of all quiz embeddings.
     *
     * The actual indexing runs asynchronously in the background.
     * Implementations typically delegate to WorkManager or a similar
     * background task scheduler.
     *
     * @return true if the request was accepted, false if re-indexing
     *         could not be triggered (e.g., no trigger configured).
     */
    fun requestFullReindex(): Boolean
}
