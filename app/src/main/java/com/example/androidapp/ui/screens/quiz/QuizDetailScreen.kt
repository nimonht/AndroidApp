package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.example.androidapp.ui.theme.FullShape
import com.example.androidapp.ui.theme.PlayfairDisplayFamily

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
        floatingActionButton = {
            if (uiState is QuizDetailUiState.Success) {
                ExtendedFloatingActionButton(
                    onClick = onStartQuiz,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text(stringResource(R.string.quiz_start_now)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = FullShape
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp) // extra space for FAB
    ) {
        // Hero Image Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (!quiz.thumbnailUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = quiz.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = quiz.title.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontFamily = PlayfairDisplayFamily
                        )
                    }
                }
            }
        }

        // Quiz Information
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.quiz_by_author, quiz.authorName.ifBlank { "..." }),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!quiz.description.isNullOrBlank()) {
                    Text(
                        text = quiz.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tags Row
                if (quiz.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quiz.tags) { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(tag) }
                            )
                        }
                    }
                }

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    InfoChip(
                        label = stringResource(R.string.quiz_questions, quiz.questionCount),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    InfoChip(
                        label = stringResource(R.string.quiz_attempts, quiz.attemptCount),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Preview Questions
        items(questions.take(3)) { question ->
            QuestionPreviewCard(
                question = question,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun QuestionPreviewCard(
    question: Question,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = question.content,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.quiz_choice_count, question.choices.size, question.choices.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
