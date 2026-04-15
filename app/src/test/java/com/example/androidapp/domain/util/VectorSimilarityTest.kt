package com.example.androidapp.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class VectorSimilarityTest {

    private fun assertFloatEquals(expected: Float, actual: Float, delta: Float = 1e-4f) {
        assertTrue(
            "Expected $expected but was $actual (delta=$delta)",
            abs(expected - actual) < delta
        )
    }

    // ---- cosineSimilarity ----

    @Test
    fun `cosineSimilarity of identical vectors returns 1`() {
        val v = floatArrayOf(1f, 2f, 3f)
        assertFloatEquals(1f, VectorSimilarity.cosineSimilarity(v, v))
    }

    @Test
    fun `cosineSimilarity of opposite vectors returns -1`() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(-1f, 0f, 0f)
        assertFloatEquals(-1f, VectorSimilarity.cosineSimilarity(a, b))
    }

    @Test
    fun `cosineSimilarity of orthogonal vectors returns 0`() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertFloatEquals(0f, VectorSimilarity.cosineSimilarity(a, b))
    }

    @Test
    fun `cosineSimilarity ignores magnitude`() {
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(2f, 4f, 6f)
        assertFloatEquals(1f, VectorSimilarity.cosineSimilarity(a, b))
    }

    @Test
    fun `cosineSimilarity of zero vectors returns 0`() {
        val zero = floatArrayOf(0f, 0f, 0f)
        val v = floatArrayOf(1f, 2f, 3f)
        assertFloatEquals(0f, VectorSimilarity.cosineSimilarity(zero, v))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cosineSimilarity throws on dimension mismatch`() {
        VectorSimilarity.cosineSimilarity(floatArrayOf(1f), floatArrayOf(1f, 2f))
    }

    // ---- l2Normalize ----

    @Test
    fun `l2Normalize produces unit vector`() {
        val v = floatArrayOf(3f, 4f)
        val normalized = VectorSimilarity.l2Normalize(v)
        val norm = sqrt(normalized.fold(0f) { acc, x -> acc + x * x })
        assertFloatEquals(1f, norm)
        assertFloatEquals(0.6f, normalized[0])
        assertFloatEquals(0.8f, normalized[1])
    }

    @Test
    fun `l2Normalize of zero vector returns copy`() {
        val zero = floatArrayOf(0f, 0f, 0f)
        val result = VectorSimilarity.l2Normalize(zero)
        assertEquals(3, result.size)
        assertFloatEquals(0f, result[0])
    }

    @Test
    fun `l2Normalize does not modify original`() {
        val v = floatArrayOf(3f, 4f)
        VectorSimilarity.l2Normalize(v)
        assertFloatEquals(3f, v[0])
        assertFloatEquals(4f, v[1])
    }

    // ---- rankBySimilarity ----

    @Test
    fun `rankBySimilarity returns sorted descending by score`() {
        val query = floatArrayOf(1f, 0f)
        val corpus = mapOf(
            "A" to floatArrayOf(1f, 0f),   // perfect match = 1.0
            "B" to floatArrayOf(0.7f, 0.7f), // partial match
            "C" to floatArrayOf(0f, 1f)    // orthogonal = 0.0
        )
        val ranked = VectorSimilarity.rankBySimilarity(query, corpus, threshold = 0.1f)
        assertEquals("A", ranked[0].first)
        assertEquals("B", ranked[1].first)
        assertEquals(2, ranked.size) // C should be filtered out (score=0 < threshold=0.1)
    }

    @Test
    fun `rankBySimilarity filters by threshold`() {
        val query = floatArrayOf(1f, 0f)
        val corpus = mapOf(
            "A" to floatArrayOf(1f, 0f),
            "B" to floatArrayOf(0f, 1f)
        )
        val ranked = VectorSimilarity.rankBySimilarity(query, corpus, threshold = 0.5f)
        assertEquals(1, ranked.size)
        assertEquals("A", ranked[0].first)
    }

    @Test
    fun `rankBySimilarity with empty corpus returns empty`() {
        val query = floatArrayOf(1f, 0f)
        val ranked = VectorSimilarity.rankBySimilarity(query, emptyMap())
        assertTrue(ranked.isEmpty())
    }

    // ---- reciprocalRankFusion ----

    @Test
    fun `RRF ranks items appearing in both lists higher`() {
        val fts = listOf("A", "B", "C")
        val semantic = listOf("B", "A", "D")
        val merged = VectorSimilarity.reciprocalRankFusion(fts, semantic)

        // A appears at FTS rank 1, semantic rank 2 -> RRF = 1/61 + 1/62
        // B appears at FTS rank 2, semantic rank 1 -> RRF = 1/62 + 1/61
        // Both A and B have the same score; they should both be at the top
        assertTrue(merged.indexOf("A") < merged.indexOf("C"))
        assertTrue(merged.indexOf("B") < merged.indexOf("C"))
        assertTrue(merged.indexOf("A") < merged.indexOf("D"))
        assertTrue(merged.indexOf("B") < merged.indexOf("D"))
    }

    @Test
    fun `RRF with disjoint lists interleaves by rank`() {
        val fts = listOf("A", "B")
        val semantic = listOf("C", "D")
        val merged = VectorSimilarity.reciprocalRankFusion(fts, semantic)

        // A: 1/61, B: 1/62, C: 1/61, D: 1/62
        // A and C tie at rank 1, B and D tie at rank 2
        assertEquals(4, merged.size)
        assertTrue(merged.containsAll(listOf("A", "B", "C", "D")))
    }

    @Test
    fun `RRF with empty lists returns empty`() {
        val merged = VectorSimilarity.reciprocalRankFusion(emptyList(), emptyList())
        assertTrue(merged.isEmpty())
    }

    @Test
    fun `RRF with one empty list returns other list order`() {
        val fts = listOf("A", "B", "C")
        val merged = VectorSimilarity.reciprocalRankFusion(fts, emptyList())
        assertEquals(listOf("A", "B", "C"), merged)
    }
}
