package com.example.androidapp.domain.util

/**
 * Utility functions for score calculations in quiz results.
 */
object ScoreUtil {

    /**
     * Calculates star rating (0-5) based on percentage score.
     *
     * @param percentage The score percentage (0-100).
     * @return Star rating from 0 to 5.
     */
    fun calculateStarRating(percentage: Int): Int {
        return when {
            percentage >= 90 -> 5
            percentage >= 80 -> 4
            percentage >= 60 -> 3
            percentage >= 40 -> 2
            percentage >= 20 -> 1
            else -> 0
        }
    }

    /**
     * Calculates score percentage from correct answers and total questions.
     *
     * @param score Number of correct answers.
     * @param maxScore Total number of questions.
     * @return Percentage score (0-100), or 0 if maxScore is 0.
     */
    fun calculatePercentage(score: Int, maxScore: Int): Int {
        return if (maxScore > 0) (score * 100) / maxScore else 0
    }
}
