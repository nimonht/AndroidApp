package com.example.androidapp.ui.screens.quiz

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.androidapp.ui.components.ShareCodeSection
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.example.androidapp.ui.theme.FullShape
import com.example.androidapp.ui.theme.PlayfairDisplayFamily

/**
 * Quiz detail screen showing quiz information before starting.
 * Stateless composable; all state is owned by [QuizDetailViewModel].
 *
 * Supports owner actions: edit and soft-delete (move to trash) via an
 * overflow menu in the top app bar.
 *
 * @param quizId The ID of the quiz to display.
 * @param onNavigateBack Callback to navigate back.
 * @param onStartQuiz Callback when user starts the quiz.
 * @param onEditQuiz Callback when the owner taps "Edit". Receives the quiz ID.
 * @param modifier Modifier for styling.
 */
@Composable
fun QuizDetailScreen(
    quizId: String,
    onNavigateBack: () -> Unit,
    onStartQuiz: () -> Unit,
    onEditQuiz: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: QuizDetailViewModel = viewModel(
        key = "detail_$quizId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuizDetailViewModel(quizId, container.quizRepository, container.authRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Navigate back after successful deletion
    val successState = uiState as? QuizDetailUiState.Success
    val deletedMessage = stringResource(R.string.quiz_deleted_success)
    LaunchedEffect(successState?.isDeleted) {
        if (successState?.isDeleted == true) {
            snackbarHostState.showSnackbar(deletedMessage)
            onNavigateBack()
        }
    }

    // Show delete error as snackbar
    LaunchedEffect(successState?.deleteError) {
        successState?.deleteError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onClearDeleteError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.quiz_detail_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack,
                actions = {
                    if (successState?.isOwner == true) {
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.quiz_detail_more_cd)
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                val isEditable =
                                    successState?.quiz?.shareCode == null && successState?.quiz?.isPublic != true
                                if (isEditable) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.quiz_detail_edit)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onEditQuiz(quizId)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.quiz_detail_edit_disabled),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                        },
                                        onClick = {},
                                        enabled = false,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.quiz_detail_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is QuizDetailUiState.Success && successState?.isDeleted != true) {
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
                onTagClick = onTagClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AppAlertDialog(
            title = stringResource(R.string.quiz_delete_confirm_title),
            message = stringResource(R.string.quiz_delete_confirm_message),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.onDeleteQuiz()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun QuizDetailContent(
    quiz: Quiz,
    questions: List<Question>,
    onTagClick: (String) -> Unit = {},
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
                                onClick = { onTagClick(tag) },
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

        // Share Code Section (shown only when quiz has a share code)
        if (!quiz.shareCode.isNullOrBlank()) {
            item {
                ShareCodeSection(
                    shareCode = quiz.shareCode,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Share Button
        if (!quiz.shareCode.isNullOrBlank()) {
            item {
                val context = LocalContext.current
                val shareText = stringResource(R.string.share_quiz_text, quiz.shareCode)
                val chooserTitle = stringResource(R.string.share_quiz_chooser_title)
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val chooser = Intent.createChooser(sendIntent, chooserTitle)
                        context.startActivity(chooser)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.share_link))
                }
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
