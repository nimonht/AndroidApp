package com.example.androidapp.domain.util

import java.util.Locale

/**
 * Utility object for formatting time durations and timestamps for display.
 */
object TimeFormatter {

    private const val SECONDS_IN_MINUTE = 60
    private const val SECONDS_IN_HOUR = 3600

    /**
     * Formats a duration in seconds into a standard HH:MM:SS or MM:SS string.
     * Useful for displaying the countdown timer or total time taken for a quiz.
     *
     * @param totalSeconds The total duration in seconds.
     * @return A formatted time string (e.g., "12:34" or "01:15:00").
     */
    fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds < 0) return "00:00"

        val hours = totalSeconds / SECONDS_IN_HOUR
        val minutes = (totalSeconds % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE
        val seconds = totalSeconds % SECONDS_IN_MINUTE

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
