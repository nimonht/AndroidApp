package com.example.androidapp.ui.screens.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.androidapp.ui.components.forms.SwitchToggle
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.components.navigation.AppTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Edit Quiz screen reusing the same form structure as the Create Quiz screen.
 *
 * Pre-populates all fields from the existing quiz loaded via [EditQuizViewModel].
 * Supports draft saving via "Lưu nháp" and publishing via "Xuất bản".
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

    // Show error snackbar.
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(EditQuizEvent.ClearError)
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
                        onClick = { viewModel.onEvent(EditQuizEvent.SaveDraft) },
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.create_save_draft),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    TextButton(
                        onClick = { viewModel.onEvent(EditQuizEvent.PublishQuiz) },
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
            FloatingActionButton(onClick = { viewModel.onEvent(EditQuizEvent.AddQuestion) }) {
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
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Last-saved indicator
                item {
                    uiState.lastSavedAt?.let { savedAt ->
                        val formatted = remember(savedAt) {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(savedAt))
                        }
                        Text(
                            text = stringResource(R.string.create_last_saved, formatted),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Title
                item {
                    TextInputField(
                        value = uiState.title,
                        onValueChange = { viewModel.onEvent(EditQuizEvent.TitleChanged(it)) },
                        label = stringResource(R.string.create_quiz_title_label),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Thumbnail URL
                item {
                    TextInputField(
                        value = uiState.thumbnailUrl,
                        onValueChange = { viewModel.onEvent(EditQuizEvent.ThumbnailUrlChanged(it)) },
                        label = stringResource(R.string.create_quiz_thumbnail_url_label),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Description
                item {
                    TextInputField(
                        value = uiState.description,
                        onValueChange = { viewModel.onEvent(EditQuizEvent.DescriptionChanged(it)) },
                        label = stringResource(R.string.create_quiz_description_label),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        singleLine = false
                    )
                }

                // Tags
                item {
                    val showTagDialog = androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(false)
                    }

                    if (showTagDialog.value) {
                        com.example.androidapp.ui.components.TagSuggestionDialog(
                            currentTags = uiState.tags,
                            availableTags = uiState.availableTags,
                            onTagsConfirmed = { newTags ->
                                viewModel.onEvent(EditQuizEvent.TagsChanged(newTags))
                            },
                            onDismiss = { showTagDialog.value = false }
                        )
                    }

                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        TextInputField(
                            value = uiState.tags,
                            onValueChange = { viewModel.onEvent(EditQuizEvent.TagsChanged(it)) },
                            label = stringResource(R.string.create_quiz_tags_label),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        androidx.compose.material3.FilledTonalButton(
                            onClick = { showTagDialog.value = true }
                        ) {
                            androidx.compose.material3.Text("Gợi ý")
                        }
                    }
                }

                // Public toggle
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.create_quiz_public),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = uiState.isPublic,
                                onCheckedChange = { viewModel.onEvent(EditQuizEvent.IsPublicChanged(it)) }
                            )
                        }
                        if (uiState.isPublic) {
                            Text(
                                text = stringResource(R.string.create_quiz_public_warning),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                            )
                        }
                    }
                }

                // Share to pool toggle
                item {
                    SwitchToggle(
                        checked = uiState.shareToPool,
                        onCheckedChange = { viewModel.onEvent(EditQuizEvent.ShareToPoolChanged(it)) },
                        label = stringResource(R.string.create_quiz_share_to_pool),
                        description = stringResource(R.string.create_quiz_share_to_pool_desc),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section header
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = stringResource(R.string.create_questions_header, uiState.questions.size),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Question cards
                itemsIndexed(uiState.questions) { index, question ->
                    QuestionEditorCard(
                        questionNumber = index + 1,
                        question = question,
                        totalQuestions = uiState.questions.size,
                        onQuestionChange = { updated ->
                            viewModel.onEvent(EditQuizEvent.UpdateQuestion(index, updated))
                        },
                        onMoveUp = { viewModel.onEvent(EditQuizEvent.MoveQuestionUp(index)) },
                        onMoveDown = { viewModel.onEvent(EditQuizEvent.MoveQuestionDown(index)) },
                        onRemove = { viewModel.onEvent(EditQuizEvent.RemoveQuestion(index)) }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
