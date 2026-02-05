package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
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
                shape = RoundedCornerShape(12.dp)
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.quiz_thumbnail_label),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quiz title (using quizId for demo)
            Text(
                text = stringResource(R.string.quiz_sample_title, quizId),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = stringResource(R.string.quiz_sample_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuizStatItem(
                    icon = Icons.Default.QuestionAnswer,
                    label = stringResource(R.string.quiz_stat_questions)
                )
                QuizStatItem(
                    icon = Icons.Default.Timer,
                    label = stringResource(R.string.quiz_stat_duration)
                )
                QuizStatItem(
                    icon = Icons.Default.Star,
                    label = stringResource(R.string.quiz_stat_rating)
                )
            }
        }
    }
}

@Composable
private fun QuizStatItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
