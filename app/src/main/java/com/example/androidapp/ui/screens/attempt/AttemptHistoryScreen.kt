package com.example.androidapp.ui.screens.attempt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.domain.model.Attempt

@Composable
fun AttemptHistoryScreen(
    viewModel: AttemptHistoryViewModel,
    userId: String,
    modifier: Modifier = Modifier
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadAttempts(userId)
    }

    if (uiState.isLoading) {
        Box(modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
    } else {

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(uiState.attempts) { attempt ->
                AttemptItem(attempt)
            }

        }

    }
}
@Composable
fun AttemptItem(
    attempt: Attempt,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "QuizId: ${attempt.quizId}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Score: ${attempt.score}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Completed: ${attempt.endTimeMillis ?: "Chưa hoàn thành"}",
                style = MaterialTheme.typography.bodySmall
            )

        }

    }
}
