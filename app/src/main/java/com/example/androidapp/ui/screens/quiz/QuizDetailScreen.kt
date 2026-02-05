package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Quiz detail screen showing quiz information before starting.
 *
 * @param quizId The ID of the quiz to display.
 * @param onNavigateBack Callback to navigate back.
 * @param onStartQuiz Callback when user starts the quiz.
 * @param modifier Modifier for styling.
 */
@Composable
fun QuizDetailScreen(
    quizId: String,
    onNavigateBack: () -> Unit,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = false

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.quiz_detail_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        },
        bottomBar = {
            // Start Quiz Button
            Button(
                onClick = onStartQuiz,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isReady
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.quiz_start_now),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyState(
                message = stringResource(R.string.quiz_detail_empty),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
