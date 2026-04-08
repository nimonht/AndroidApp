package com.example.androidapp.ui.screens.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.ui.components.TagSuggestionDialog
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Edit Quiz screen reusing the same form structure as the Create Quiz screen.
 *
 * Pre-populates all fields from the existing quiz loaded via [EditQuizViewModel].
 * Supports draft saving via "Luu nhap" and publishing via "Xuat ban".
 *
 * @param quizId The ID of the quiz to edit.
 * @param onNavigateBack Callback to navigate back.
 * @param onSaveComplete Callback invoked after the quiz is published successfully.
 * @param modifier Modifier for styling.
 */
@Composable
fun EditQuizScreen(
    quizId: String,
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: EditQuizViewModel = viewModel(
        key = "edit_$quizId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditQuizViewModel(
                    quizId,
                    container.quizRepository,
                    container.authRepository,
                    container.poolRepository,
                    container.shareCodeRepository
                ) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away after a successful publish.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaveComplete()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve UiError to a display string in composable context, falling back to
    // raw errorDetail for validation messages that are not yet enum-based.
    val errorMessage = uiState.error?.toMessage(uiState.errorDetail) ?: uiState.errorDetail

    // Show error snackbar.
    LaunchedEffect(uiState.error, uiState.errorDetail) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(QuizFormEvent.ClearError)
        }
    }

    // Show "draft saved" snackbar when lastSavedAt changes from null to a timestamp.
    val draftSavedMessage = stringResource(R.string.create_quiz_draft_saved)
    LaunchedEffect(uiState.lastSavedAt) {
        if (uiState.lastSavedAt != null && !uiState.isPublished) {
            snackbarHostState.showSnackbar(draftSavedMessage)
        }
    }

    // Show "published" snackbar.
    val publishedMessage = stringResource(R.string.create_quiz_published)
    LaunchedEffect(uiState.isPublished) {
        if (uiState.isPublished) {
            snackbarHostState.showSnackbar(publishedMessage)
        }
    }

    // Tag suggestion dialog state — hoisted to screen level.
    var showTagDialog by remember { mutableStateOf(false) }

    if (showTagDialog) {
        TagSuggestionDialog(
            currentTags = uiState.tags,
            availableTags = uiState.availableTags,
            onTagsConfirmed = { newTags ->
                viewModel.onEvent(QuizFormEvent.TagsChanged(newTags))
            },
            onDismiss = { showTagDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.edit_quiz_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(QuizFormEvent.SaveDraft) },
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.create_save_draft),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    TextButton(
                        onClick = { viewModel.onEvent(QuizFormEvent.PublishQuiz) },
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.create_publish),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(QuizFormEvent.AddQuestion) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_add_question_cd)
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            QuizFormContent(
                title = uiState.title,
                onTitleChange = { viewModel.onEvent(QuizFormEvent.TitleChanged(it)) },
                thumbnailUrl = uiState.thumbnailUrl,
                onThumbnailUrlChange = { viewModel.onEvent(QuizFormEvent.ThumbnailUrlChanged(it)) },
                description = uiState.description,
                onDescriptionChange = { viewModel.onEvent(QuizFormEvent.DescriptionChanged(it)) },
                tags = uiState.tags,
                onTagsChange = { viewModel.onEvent(QuizFormEvent.TagsChanged(it)) },
                onShowTagSuggestions = { showTagDialog = true },
                isPublic = uiState.isPublic,
                onPublicToggle = { viewModel.onEvent(QuizFormEvent.IsPublicChanged(it)) },
                shareToPool = uiState.shareToPool,
                onShareToPoolToggle = { viewModel.onEvent(QuizFormEvent.ShareToPoolChanged(it)) },
                questions = uiState.questions,
                onUpdateQuestion = { index, updated ->
                    viewModel.onEvent(QuizFormEvent.UpdateQuestion(index, updated))
                },
                onMoveQuestionUp = { viewModel.onEvent(QuizFormEvent.MoveQuestionUp(it)) },
                onMoveQuestionDown = { viewModel.onEvent(QuizFormEvent.MoveQuestionDown(it)) },
                onRemoveQuestion = { viewModel.onEvent(QuizFormEvent.RemoveQuestion(it)) },
                lastSavedAt = uiState.lastSavedAt,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        }
    }
}
