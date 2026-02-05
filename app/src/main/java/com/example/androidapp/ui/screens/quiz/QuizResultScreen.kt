package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.feedback.ScoreCard

/**
 * Quiz result screen showing score and options.
 *
 * @param quizId The ID of the completed quiz.
 * @param attemptId The ID of the attempt (for loading results).
 * @param onNavigateHome Callback to go back to home.
 * @param onRetryQuiz Callback to retry the quiz.
 * @param onReviewAnswers Callback to review answers.
 * @param modifier Modifier for styling.
 */
@Composable
fun QuizResultScreen(
    quizId: String,
    attemptId: String,
    onNavigateHome: () -> Unit,
    onRetryQuiz: () -> Unit,
    onReviewAnswers: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Load actual results from ViewModel based on attemptId
    val score = 8
    val maxScore = 10
    val correctCount = 8
    val wrongCount = 2
    val timeTaken = "8:32"

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Score Card (reusable component)
            ScoreCard(
                score = score,
                maxScore = maxScore,
                correctCount = correctCount,
                wrongCount = wrongCount,
                timeTaken = timeTaken
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.quiz_result_go_home))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRetryQuiz,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.quiz_result_try_again))
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onReviewAnswers) {
                Text(stringResource(R.string.quiz_result_review_answers))
            }
        }
    }
}
