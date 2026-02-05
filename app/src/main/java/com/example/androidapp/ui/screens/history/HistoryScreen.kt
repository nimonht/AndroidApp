package com.example.androidapp.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * History screen showing past quiz attempts.
 *
 * @param onNavigateBack Callback to navigate back.
 * @param onAttemptClick Callback when an attempt is clicked.
 * @param modifier Modifier for styling.
 */
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onAttemptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with actual data from ViewModel
    val attempts = listOf(
        AttemptPreview("1", "Math Quiz", "8/10", "2 hours ago"),
        AttemptPreview("2", "Science Trivia", "7/10", "Yesterday"),
        AttemptPreview("3", "History Quiz", "9/10", "3 days ago")
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "Attempt History",
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        if (attempts.isEmpty()) {
            EmptyState(
                message = "No attempts yet",
                actionLabel = "Explore Quizzes",
                onActionClick = onNavigateBack,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(attempts) { attempt ->
                    AttemptCard(
                        attempt = attempt,
                        onClick = { onAttemptClick(attempt.id) }
                    )
                }
            }
        }
    }
}

/**
 * Preview data class for attempts.
 * TODO: Replace with domain model.
 */
private data class AttemptPreview(
    val id: String,
    val quizTitle: String,
    val score: String,
    val timeAgo: String
)

@Composable
private fun AttemptCard(
    attempt: AttemptPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.quizTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = attempt.timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = attempt.score,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
