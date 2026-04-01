package com.example.androidapp.domain.util

import com.example.androidapp.domain.model.Question

/**
 * Utility object for calculating quiz scores.
 * Supports both single-choice and multiple-choice questions.
 */
object ScoreCalculator {

    /**
     * Calculates the total number of correctly answered questions.
     * A question is considered correct only if the user's selected choices
     * exactly match the correct choices (strict grading).
     *
     * @param correctAnswers A map where the key is the question ID and the value is a set of correct choice IDs.
     * @param userAnswers A map where the key is the question ID and the value is a set of the user's selected choice IDs.
     * @return The total number of correctly answered questions.
     */
    fun calculateCorrectCount(
        correctAnswers: Map<String, Set<String>>,
        userAnswers: Map<String, Set<String>>
    ): Int {
        var score = 0
        for ((questionId, correctChoiceIds) in correctAnswers) {
            val userChoiceIds = userAnswers[questionId] ?: emptySet()

            // Set equality checking in Kotlin compares the contents regardless of order.
            // This perfectly handles both single choice (Set size = 1) and multiple choice (Set size > 1).
            if (correctChoiceIds == userChoiceIds) {
                score++
            }
        }
        return score
    }

    /**
     * Calculates the point-based score by summing the [Question.points] value
     * for every correctly answered question (exact set-equality grading).
     *
     * @param questions     The full list of questions in the quiz (each carries its own [Question.points]).
     * @param userAnswers   A map where the key is the question ID and the value is a set of the user's selected choice IDs.
     * @return A [PointScoreResult] containing the earned score, the maximum possible score,
     *         and the number of correctly / incorrectly answered questions.
     */
    fun calculatePointScore(
        questions: List<Question>,
        userAnswers: Map<String, Set<String>>
    ): PointScoreResult {
        var earned = 0
        var maxScore = 0
        var correctCount = 0
        for (question in questions) {
            maxScore += question.points
            val correctChoiceIds = question.choices.filter { it.isCorrect }.map { it.id }.toSet()
            val userChoiceIds = userAnswers[question.id] ?: emptySet()
            if (correctChoiceIds == userChoiceIds) {
                earned += question.points
                correctCount++
            }
        }
        return PointScoreResult(
            earnedScore = earned,
            maxScore = maxScore,
            correctCount = correctCount,
            wrongCount = questions.size - correctCount
        )
    }

    /**
     * Calculates the percentage score based on correct answers and total questions.
     *
     * @param correctCount The number of correctly answered questions.
     * @param totalQuestions The total number of questions in the quiz.
     * @return The percentage score from 0.0 to 100.0. Returns 0.0 if totalQuestions is 0 or negative.
     */
    fun calculatePercentage(correctCount: Int, totalQuestions: Int): Double {
        if (totalQuestions <= 0) return 0.0
        return (correctCount.toDouble() / totalQuestions) * 100.0
    }
}

/**
 * Result of a point-based score calculation.
 *
 * @property earnedScore  Sum of [Question.points] for correctly answered questions.
 * @property maxScore     Sum of [Question.points] for all questions in the quiz.
 * @property correctCount Number of questions answered correctly.
 * @property wrongCount   Number of questions answered incorrectly.
 */
data class PointScoreResult(
    val earnedScore: Int,
    val maxScore: Int,
    val correctCount: Int,
    val wrongCount: Int
)
