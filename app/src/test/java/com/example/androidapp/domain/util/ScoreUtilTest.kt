package com.example.androidapp.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ScoreUtil] utility functions.
 */
class ScoreUtilTest {

    // ==================== calculateStarRating ====================

    @Test
    fun `calculateStarRating returns 5 for 90 percent or above`() {
        assertEquals(5, ScoreUtil.calculateStarRating(90))
        assertEquals(5, ScoreUtil.calculateStarRating(95))
        assertEquals(5, ScoreUtil.calculateStarRating(100))
    }

    @Test
    fun `calculateStarRating returns 4 for 80 to 89 percent`() {
        assertEquals(4, ScoreUtil.calculateStarRating(80))
        assertEquals(4, ScoreUtil.calculateStarRating(85))
        assertEquals(4, ScoreUtil.calculateStarRating(89))
    }

    @Test
    fun `calculateStarRating returns 3 for 60 to 79 percent`() {
        assertEquals(3, ScoreUtil.calculateStarRating(60))
        assertEquals(3, ScoreUtil.calculateStarRating(70))
        assertEquals(3, ScoreUtil.calculateStarRating(79))
    }

    @Test
    fun `calculateStarRating returns 2 for 40 to 59 percent`() {
        assertEquals(2, ScoreUtil.calculateStarRating(40))
        assertEquals(2, ScoreUtil.calculateStarRating(50))
        assertEquals(2, ScoreUtil.calculateStarRating(59))
    }

    @Test
    fun `calculateStarRating returns 1 for 20 to 39 percent`() {
        assertEquals(1, ScoreUtil.calculateStarRating(20))
        assertEquals(1, ScoreUtil.calculateStarRating(30))
        assertEquals(1, ScoreUtil.calculateStarRating(39))
    }

    @Test
    fun `calculateStarRating returns 0 for below 20 percent`() {
        assertEquals(0, ScoreUtil.calculateStarRating(0))
        assertEquals(0, ScoreUtil.calculateStarRating(10))
        assertEquals(0, ScoreUtil.calculateStarRating(19))
    }

    // ==================== calculatePercentage ====================

    @Test
    fun `calculatePercentage returns correct percentage`() {
        assertEquals(80, ScoreUtil.calculatePercentage(8, 10))
        assertEquals(100, ScoreUtil.calculatePercentage(10, 10))
        assertEquals(50, ScoreUtil.calculatePercentage(5, 10))
    }

    @Test
    fun `calculatePercentage returns 0 when maxScore is 0`() {
        assertEquals(0, ScoreUtil.calculatePercentage(0, 0))
        assertEquals(0, ScoreUtil.calculatePercentage(5, 0))
    }

    @Test
    fun `calculatePercentage handles perfect score`() {
        assertEquals(100, ScoreUtil.calculatePercentage(1, 1))
        assertEquals(100, ScoreUtil.calculatePercentage(100, 100))
    }

    @Test
    fun `calculatePercentage handles zero score`() {
        assertEquals(0, ScoreUtil.calculatePercentage(0, 10))
        assertEquals(0, ScoreUtil.calculatePercentage(0, 1))
    }

    @Test
    fun `calculatePercentage uses integer division`() {
        // 1/3 = 33.33... should truncate to 33
        assertEquals(33, ScoreUtil.calculatePercentage(1, 3))
        // 2/3 = 66.66... should truncate to 66
        assertEquals(66, ScoreUtil.calculatePercentage(2, 3))
    }
}
