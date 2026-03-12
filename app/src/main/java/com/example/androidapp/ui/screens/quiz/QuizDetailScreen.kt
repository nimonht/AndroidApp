package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.example.androidapp.ui.theme.FullShape

/**
 * Quiz detail screen showing quiz information before starting.
 * Stateless composable; all state is owned by [QuizDetailViewModel].
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
    val container = LocalAppContainer
    val viewModel: QuizDetailViewModel = viewModel(
        key = "detail_$quizId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuizDetailViewModel(quizId, container.quizRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            if (uiState is QuizDetailUiState.Success) {
                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = FullShape
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.quiz_start_now),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is QuizDetailUiState.Loading -> LoadingSpinner(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
            is QuizDetailUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.onRetry() },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
            is QuizDetailUiState.Success -> QuizDetailContent(
                quiz = state.quiz,
                questions = state.questions,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun QuizDetailContent(
    quiz: Quiz,
    questions: List<Question>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = quiz.title, style = MaterialTheme.typography.headlineMedium)
            if (!quiz.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = quiz.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip(label = stringResource(R.string.quiz_questions, quiz.questionCount))
                InfoChip(label = stringResource(R.string.quiz_attempts, quiz.attemptCount))
            }
        }
        items(questions.take(3)) { question ->
            QuestionPreviewCard(question = question)
        }
    }
}

@Composable
private fun InfoChip(label: String, modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier
    )
}

@Composable
private fun QuestionPreviewCard(
    question: Question,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = question.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.quiz_choice_count, question.choices.size, question.choices.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
