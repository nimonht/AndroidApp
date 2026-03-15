package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.theme.FullShape
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.PlayfairDisplayFamily

/**
 * Take Quiz screen — Editorial Minimalist design.
 * Stateless composable; all state is owned by [TakeQuizViewModel].
 *
 * @param quizId The ID of the quiz being taken.
 * @param onNavigateBack Callback to exit the quiz.
 * @param onQuizComplete Callback with the attempt ID when quiz is submitted.
 * @param modifier Modifier for styling.
 */
@Composable
fun TakeQuizScreen(
    quizId: String,
    onNavigateBack: () -> Unit,
    onQuizComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: TakeQuizViewModel = viewModel(
        key = "take_$quizId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TakeQuizViewModel(
                    quizId,
                    container.quizRepository,
                    container.attemptRepository,
                    container.authRepository
                ) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is TakeQuizUiState.Finished -> {
                onQuizComplete(state.attemptId)
            }
            is TakeQuizUiState.Active -> {
                if (state.shouldNavigateBack) {
                    onNavigateBack()
                }
            }
            else -> {} // No action for Loading or Error
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is TakeQuizUiState.Loading -> LoadingSpinner(modifier = Modifier.fillMaxSize())
            is TakeQuizUiState.Error -> ErrorState(
                message = state.message,
                onRetry = onNavigateBack,
                modifier = Modifier.fillMaxSize()
            )
            is TakeQuizUiState.Active -> {
                ActiveQuizContent(
                    state = state,
                    onAnswerSelected = { viewModel.onEvent(TakeQuizEvent.AnswerSelected(it)) },
                    onNext = { viewModel.onEvent(TakeQuizEvent.NextQuestion) },
                    onPrevious = { viewModel.onEvent(TakeQuizEvent.PreviousQuestion) },
                    onSubmit = { viewModel.onEvent(TakeQuizEvent.SubmitQuiz) },
                    onClose = { viewModel.onEvent(TakeQuizEvent.RequestExit) },
                    modifier = Modifier.fillMaxSize()
                )

                // Exit confirmation dialog
                if (state.showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(TakeQuizEvent.DismissExitDialog) },
                        title = {
                            Text(
                                text = stringResource(R.string.quiz_exit_title),
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.quiz_exit_message),
                                fontFamily = InterFamily
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { viewModel.onEvent(TakeQuizEvent.ConfirmExit) }
                            ) {
                                Text(
                                    text = stringResource(R.string.quiz_exit_confirm),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { viewModel.onEvent(TakeQuizEvent.DismissExitDialog) }
                            ) {
                                Text(text = stringResource(R.string.quiz_exit_cancel))
                            }
                        }
                    )
                }
            }
            is TakeQuizUiState.Finished -> LoadingSpinner(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ActiveQuizContent(
    state: TakeQuizUiState.Active,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (state.currentIndex + 1).toFloat() / state.totalQuestions.coerceAtLeast(1)
    val isLast = state.currentIndex == state.totalQuestions - 1

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        // ── Thin top progress bar ──────────────────────────────────────────
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )

        // ── App bar ────────────────────────────────────────────────────────
        QuizAppBar(
            currentIndex = state.currentIndex,
            totalQuestions = state.totalQuestions,
            onClose = onClose
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )

        // ── Scrollable content ─────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero image (if available)
            if (!state.currentQuestion.mediaUrl.isNullOrBlank()) {
                item {
                    AsyncImage(
                        model = state.currentQuestion.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Question text
            item {
                Text(
                    text = state.currentQuestion.content,
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 26.sp,
                    lineHeight = 36.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Options
            itemsIndexed(state.currentQuestion.choices) { index, choice ->
                EditorialChoiceCard(
                    label = ('A' + index).toString(),
                    choice = choice,
                    isSelected = choice.id in state.selectedAnswers,
                    onClick = { onAnswerSelected(choice.id) }
                )
            }
        }

        // ── Footer actions ─────────────────────────────────────────────────
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
        QuizFooter(
            currentIndex = state.currentIndex,
            isLast = isLast,
            isSubmitting = state.isSubmitting,
            onPrevious = onPrevious,
            onNext = onNext,
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun QuizAppBar(
    currentIndex: Int,
    totalQuestions: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Close button (left)
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Step indicator (center)
        Text(
            text = stringResource(R.string.take_quiz_step_indicator, currentIndex + 1, totalQuestions),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center)
        )

        // Help icon (right)
        IconButton(
            onClick = { /* TODO: open quiz help */ },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Outlined.Help,
                contentDescription = stringResource(R.string.take_quiz_help_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditorialChoiceCard(
    label: String,
    choice: Choice,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    else
        MaterialTheme.colorScheme.outlineVariant

    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .border(1.dp, borderColor, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Label badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = choice.content,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun QuizFooter(
    currentIndex: Int,
    isLast: Boolean,
    isSubmitting: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BACK text button
        if (currentIndex > 0) {
            TextButton(onClick = onPrevious, contentPadding = PaddingValues(0.dp)) {
                Text(
                    text = stringResource(R.string.take_quiz_back_button),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        // CONTINUE / SUBMIT accent button
        Button(
            onClick = if (isLast) onSubmit else onNext,
            enabled = !isSubmitting,
            shape = FullShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isLast)
                        stringResource(R.string.take_quiz_submit_button)
                    else
                        stringResource(R.string.take_quiz_continue_button),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

