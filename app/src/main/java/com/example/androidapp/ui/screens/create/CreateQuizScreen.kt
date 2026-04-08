package com.example.androidapp.ui.screens.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.example.androidapp.R
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.ui.components.TagSuggestionDialog
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * Create Quiz screen with a multi-step form.
 *
 * Stateless composable; all state is owned by [CreateQuizViewModel].
 * Supports draft saving via "Lưu nháp" and publishing via "Xuất bản".
 *
 * @param onNavigateBack Callback to navigate back.
 * @param onSaveComplete Callback invoked after the quiz is published successfully.
 * @param modifier Modifier for styling.
 */
@Composable
fun CreateQuizScreen(
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    onNavigateToCsvImport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: CreateQuizViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CreateQuizViewModel(
                    container.quizRepository,
                    container.authRepository,
                    container.poolRepository,
                    container.shareCodeRepository
                ) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val owner = LocalViewModelStoreOwner.current
    val savedStateHandle = (owner as? NavBackStackEntry)?.savedStateHandle
    val importedJsonFlow = remember(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("imported_questions_json", null)
            ?: MutableStateFlow(null)
    }
    val importedJson by importedJsonFlow.collectAsStateWithLifecycle()

    LaunchedEffect(importedJson) {
        if (!importedJson.isNullOrBlank()) {
            try {
                val questions = Gson().fromJson(
                    importedJson,
                    Array<QuestionDraft>::class.java
                ).toList()

                viewModel.onEvent(CreateQuizEvent.ImportQuestions(questions))
            } catch (e: Exception) {
                // Ignore parse errors
            } finally {
                savedStateHandle?.remove<String>("imported_questions_json")
            }
        }
    }

    var showTagDialog by remember { mutableStateOf(false) }

    if (uiState.showPoolDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CreateQuizEvent.DismissPoolDialog) },
            title = { Text(stringResource(R.string.pool_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.poolSearchTags,
                        onValueChange = { viewModel.onEvent(CreateQuizEvent.PoolSearchTagsChanged(it)) },
                        label = { Text(stringResource(R.string.pool_dialog_tag_input_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.onEvent(CreateQuizEvent.SearchPool) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isPoolLoading
                    ) {
                        Text(stringResource(R.string.pool_dialog_search))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.isPoolLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(uiState.poolResults) { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onEvent(CreateQuizEvent.TogglePoolItemSelection(item.id))
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = uiState.selectedPoolItemIds.contains(item.id),
                                        onCheckedChange = {
                                            viewModel.onEvent(CreateQuizEvent.TogglePoolItemSelection(item.id))
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.question.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(CreateQuizEvent.ImportFromPool) },
                    enabled = uiState.selectedPoolItemIds.isNotEmpty()
                ) {
                    Text(stringResource(R.string.pool_dialog_add_selected))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(CreateQuizEvent.DismissPoolDialog) }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

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

    // Navigate away after a successful publish.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaveComplete()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve UiError to a display string in composable context, falling back
    // to raw errorDetail for validation messages that have no UiError code.
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.quiz_create),
                canNavigateBack = true,
                navigateUp = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateToCsvImport) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = stringResource(R.string.create_import_csv_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            FloatingActionButton(
                onClick = { viewModel.onEvent(QuizFormEvent.AddQuestion) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_add_question_cd)
                )
            }
        }
    ) { innerPadding ->
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
                .fillMaxSize(),
            questionsHeaderTrailingContent = {
                TextButton(
                    onClick = { viewModel.onEvent(CreateQuizEvent.ShowPoolDialog) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.pool_add_from_pool))
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Shared Question Editor Card
// ---------------------------------------------------------------------------

/**
 * Shared question editor card used in both [CreateQuizScreen] and [EditQuizScreen].
 *
 * Provides fields for question content, media URL, explanation, a dynamic choice list
 * (min 2, max 10 choices), points selector (1–10), move-up/down buttons, and a
 * remove-question button.
 *
 * @param questionNumber 1-based display number of this question.
 * @param question The current [QuestionDraft] data.
 * @param totalQuestions Total number of questions in the list; used to gate remove button.
 * @param onQuestionChange Callback when any field of [question] changes.
 * @param onMoveUp Callback to move this question one position earlier in the list.
 * @param onMoveDown Callback to move this question one position later in the list.
 * @param onRemove Callback to remove this question from the list.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun QuestionEditorCard(
    questionNumber: Int,
    question: QuestionDraft,
    totalQuestions: Int,
    onQuestionChange: (QuestionDraft) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxChoices = 10
    val minChoices = 2

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ---- Header row: question number, completeness indicator, reorder, delete ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.create_question_title, questionNumber),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (question.content.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Move up — disabled for the first question
                IconButton(
                    onClick = onMoveUp,
                    enabled = questionNumber > 1
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.create_question_move_up)
                    )
                }

                // Move down — disabled for the last question
                IconButton(
                    onClick = onMoveDown,
                    enabled = questionNumber < totalQuestions
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.create_question_move_down)
                    )
                }

                // Delete — only shown when there is more than one question
                if (totalQuestions > 1) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.create_question_remove),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Question content ----
            OutlinedTextField(
                value = question.content,
                onValueChange = { onQuestionChange(question.copy(content = it)) },
                label = { Text(stringResource(R.string.create_question_content_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Media URL ----
            OutlinedTextField(
                value = question.mediaUrl,
                onValueChange = { onQuestionChange(question.copy(mediaUrl = it)) },
                label = { Text(stringResource(R.string.create_question_media_url)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Points row: minus / value label / plus ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.create_question_points),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (question.points > 1) {
                            onQuestionChange(question.copy(points = question.points - 1))
                        }
                    },
                    enabled = question.points > 1
                ) {
                    // Minus symbol using a Text since there is no dedicated icon
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (question.points > 1)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = stringResource(R.string.create_question_points_label, question.points),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = {
                        if (question.points < 10) {
                            onQuestionChange(question.copy(points = question.points + 1))
                        }
                    },
                    enabled = question.points < 10
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (question.points < 10)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Multi-select toggle ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.create_question_multi_select),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = question.isMultiSelect,
                    onCheckedChange = { isMulti ->
                        // When switching from multi to single, keep only the first correct index
                        val newIndices = if (!isMulti && question.correctIndices.size > 1) {
                            setOf(question.correctIndices.first())
                        } else {
                            question.correctIndices
                        }
                        onQuestionChange(question.copy(isMultiSelect = isMulti, correctIndices = newIndices))
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Choices ----
            question.choices.forEachIndexed { cIdx, choice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Tap to mark as correct answer
                    if (question.isMultiSelect) {
                        Checkbox(
                            checked = cIdx in question.correctIndices,
                            onCheckedChange = {
                                val newIndices = if (cIdx in question.correctIndices)
                                    question.correctIndices - cIdx
                                else
                                    question.correctIndices + cIdx
                                onQuestionChange(question.copy(correctIndices = newIndices))
                            }
                        )
                    } else {
                        RadioButton(
                            selected = cIdx in question.correctIndices,
                            onClick = {
                                onQuestionChange(question.copy(correctIndices = setOf(cIdx)))
                            }
                        )
                    }

                    OutlinedTextField(
                        value = choice.content,
                        onValueChange = { newContent ->
                            val updatedChoices = question.choices.toMutableList().apply {
                                this[cIdx] = choice.copy(content = newContent)
                            }
                            onQuestionChange(question.copy(choices = updatedChoices))
                        },
                        placeholder = {
                            Text(stringResource(R.string.create_choice_hint, cIdx + 1))
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        singleLine = true
                    )

                    // Remove choice — only shown when above the minimum
                    if (question.choices.size > minChoices) {
                        IconButton(
                            onClick = {
                                val updatedChoices = question.choices.toMutableList().apply {
                                    removeAt(cIdx)
                                }
                                // Remap correct indices: drop the removed index and shift down
                                val newIndices = question.correctIndices
                                    .filter { it != cIdx }
                                    .map { if (it > cIdx) it - 1 else it }
                                    .toSet()
                                    .ifEmpty { setOf(0) }
                                onQuestionChange(
                                    question.copy(
                                        choices = updatedChoices,
                                        correctIndices = newIndices
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(
                                    R.string.create_question_remove_choice
                                ),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ---- Add choice button ----
            if (question.choices.size < maxChoices) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        onQuestionChange(
                            question.copy(choices = question.choices + ChoiceDraft())
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.create_question_add_choice))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Explanation ----
            OutlinedTextField(
                value = question.explanation,
                onValueChange = { onQuestionChange(question.copy(explanation = it)) },
                label = { Text(stringResource(R.string.create_question_explanation)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                minLines = 2
            )
        }
    }
}
