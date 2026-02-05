package com.example.androidapp.ui.components.feedback

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizCodeTheme

/**
 * Displays the quiz result with score, percentage, star rating, and detailed stats.
 *
 * @param score The number of correct answers.
 * @param maxScore The total number of questions.
 * @param correctCount Number of correct answers (usually equals score).
 * @param wrongCount Number of incorrect answers.
 * @param timeTaken Time taken to complete the quiz (formatted string, e.g., "8:32").
 * @param modifier Modifier for styling and layout customization.
 */
@Composable
fun ScoreCard(
    score: Int,
    maxScore: Int,
    correctCount: Int,
    wrongCount: Int,
    timeTaken: String? = null,
    modifier: Modifier = Modifier
) {
    val percentage = if (maxScore > 0) (score * 100) / maxScore else 0
    val starRating = calculateStarRating(percentage)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Celebration emoji
            Text(
                text = when {
                    percentage >= 80 -> stringResource(R.string.score_celebrate_high)
                    percentage >= 60 -> stringResource(R.string.score_celebrate_medium)
                    percentage >= 40 -> stringResource(R.string.score_celebrate_low)
                    else -> stringResource(R.string.score_celebrate_min)
                },
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = stringResource(R.string.score_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Score display
            Text(
                text = stringResource(R.string.score_format, score, maxScore),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Percentage
            Text(
                text = stringResource(R.string.percentage_format, percentage),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Star rating
            StarRating(
                rating = starRating,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            StatsRow(
                correctCount = correctCount,
                wrongCount = wrongCount,
                timeTaken = timeTaken
            )
        }
    }
}

/**
 * Displays star rating based on the score percentage.
 *
 * @param rating Number of filled stars (0-5).
 * @param maxStars Maximum number of stars to display.
 * @param modifier Modifier for styling.
 */
@Composable
private fun StarRating(
    rating: Int,
    maxStars: Int = 5,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(maxStars) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (index < rating) {
                    Color(0xFFFFD700) // Gold color
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
        }
    }
}

/**
 * Displays a row with correct/wrong counts and time taken.
 *
 * @param correctCount Number of correct answers.
 * @param wrongCount Number of incorrect answers.
 * @param timeTaken Formatted time string.
 */
@Composable
private fun StatsRow(
    correctCount: Int,
    wrongCount: Int,
    timeTaken: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Correct count
            StatItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.correct_label),
                        tint = Color(0xFF4CAF50) // Green
                    )
                },
                label = stringResource(R.string.correct_label),
                value = correctCount.toString(),
                valueColor = Color(0xFF4CAF50)
            )

            // Divider
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Wrong count
            StatItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.wrong_label),
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                label = stringResource(R.string.wrong_label),
                value = wrongCount.toString(),
                valueColor = MaterialTheme.colorScheme.error
            )

            // Time taken (if provided)
            if (timeTaken != null) {
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                StatItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = stringResource(R.string.time_label),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = stringResource(R.string.time_label),
                    value = timeTaken,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Individual stat item with icon, label, and value.
 */
@Composable
private fun StatItem(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Calculates star rating (0-5) based on percentage score.
 */
private fun calculateStarRating(percentage: Int): Int {
    return when {
        percentage >= 90 -> 5
        percentage >= 80 -> 4
        percentage >= 60 -> 3
        percentage >= 40 -> 2
        percentage >= 20 -> 1
        else -> 0
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScoreCardPreview() {
    QuizCodeTheme {
        ScoreCard(
            score = 8,
            maxScore = 10,
            correctCount = 8,
            wrongCount = 2,
            timeTaken = "8:32",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScoreCardLowScorePreview() {
    QuizCodeTheme {
        ScoreCard(
            score = 3,
            maxScore = 10,
            correctCount = 3,
            wrongCount = 7,
            timeTaken = "12:45",
            modifier = Modifier.padding(16.dp)
        )
    }
}
