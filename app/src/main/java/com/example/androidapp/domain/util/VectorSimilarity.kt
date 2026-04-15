package com.example.androidapp.domain.util

import kotlin.math.sqrt

/**
 * Pure-Kotlin vector math utilities for semantic search.
 *
 * All methods are stateless and thread-safe.
 */
object VectorSimilarity {

    /**
     * Computes cosine similarity between two vectors.
     *
     * If both vectors are already L2-normalized (as USE outputs are),
     * this reduces to a simple dot-product.
     *
     * @return Value in [-1.0, 1.0]; higher means more similar.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Dimension mismatch: ${a.size} vs ${b.size}" }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom < 1e-8f) 0f else dot / denom
    }

    /**
     * Returns a new L2-normalized copy of [v].
     */
    fun l2Normalize(v: FloatArray): FloatArray {
        val norm = sqrt(v.fold(0f) { acc, x -> acc + x * x })
        return if (norm < 1e-8f) v.copyOf() else FloatArray(v.size) { v[it] / norm }
    }

    /**
     * Ranks quiz IDs by cosine similarity to the [queryEmbedding].
     *
     * @param queryEmbedding Dense vector for the search query.
     * @param corpus         Map of quizId to stored embedding from cache.
     * @param threshold      Minimum cosine similarity to include (default 0.25).
     * @return Pair(quizId, score) sorted descending by similarity, filtered
     *         by [threshold].
     */
    fun rankBySimilarity(
        queryEmbedding: FloatArray,
        corpus: Map<String, FloatArray>,
        threshold: Float = 0.25f
    ): List<Pair<String, Float>> =
        corpus.entries
            .map { (id, emb) -> id to cosineSimilarity(queryEmbedding, emb) }
            .filter { (_, score) -> score >= threshold }
            .sortedByDescending { (_, score) -> score }

    /**
     * Reciprocal Rank Fusion -- merges two independently ranked lists
     * into a single combined ranking.
     *
     * Formula: RRF(d) = sum of weight_i / (k + rank_i(d))
     *
     * @param ftsRanking      Quiz IDs ordered by FTS5 BM25 score (best first).
     * @param semanticRanking Quiz IDs ordered by cosine similarity (best first).
     * @param k               Smoothing constant (default 60, per original RRF paper).
     * @param ftsWeight       Multiplicative weight applied to each FTS rank contribution
     *                        (default 1.0). Increase to favour keyword matches.
     * @param semanticWeight  Multiplicative weight applied to each semantic rank contribution
     *                        (default 1.0). Increase to favour cross-lingual / embedding matches.
     * @return Merged list of quiz IDs ordered by combined RRF score.
     */
    fun reciprocalRankFusion(
        ftsRanking: List<String>,
        semanticRanking: List<String>,
        k: Int = 60,
        ftsWeight: Double = 1.0,
        semanticWeight: Double = 1.0
    ): List<String> {
        val scores = mutableMapOf<String, Double>()

        ftsRanking.forEachIndexed { index, id ->
            scores[id] = (scores[id] ?: 0.0) + ftsWeight / (k + index + 1)
        }
        semanticRanking.forEachIndexed { index, id ->
            scores[id] = (scores[id] ?: 0.0) + semanticWeight / (k + index + 1)
        }

        return scores.entries
            .sortedByDescending { it.value }
            .map { it.key }
    }
}
