package com.example.androidapp.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.util.ScoreUtil
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * History screen showing past quiz attempts.
 * Stateless composable; all state is owned by [HistoryViewModel].
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
    val container = LocalAppContainer
    val viewModel: HistoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(container.attemptRepository, container.quizRepository, container.authRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.history_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingSpinner(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )

            uiState.attempts.isEmpty() -> EmptyState(
                message = stringResource(R.string.history_empty),
                actionLabel = stringResource(R.string.history_action_explore),
                onActionClick = onNavigateBack,
                modifier = Modifier.padding(innerPadding).fillMaxWidth()
            )

            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.attempts) { attemptWithQuiz ->
                    AttemptCard(
                        attemptWithQuiz = attemptWithQuiz,
                        onClick = { onAttemptClick(attemptWithQuiz.attempt.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttemptCard(
    attemptWithQuiz: AttemptWithQuiz,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val attempt = attemptWithQuiz.attempt
    val percentage = ScoreUtil.calculatePercentage(attempt.score, attempt.totalQuestions)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (attemptWithQuiz.isQuizDeleted && attemptWithQuiz.quizTitle == HistoryViewModel.DELETED_QUIZ_FALLBACK_TITLE) {
                        stringResource(R.string.history_deleted_quiz)
                    } else {
                        attemptWithQuiz.quizTitle
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (attemptWithQuiz.isQuizDeleted) {
                    Text(
                        text = stringResource(R.string.history_deleted_quiz_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = stringResource(R.string.score_format, attempt.score, attempt.totalQuestions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.percentage_format, percentage),
                style = MaterialTheme.typography.titleMedium,
                color = if (percentage >= 60)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}
