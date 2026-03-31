package com.example.androidapp.ui.screens.create

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.ui.components.TagSuggestionDialog
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.forms.SwitchToggle
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.components.navigation.AppTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val owner = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current
    val savedStateHandle = (owner as? androidx.navigation.NavBackStackEntry)?.savedStateHandle
    val importedJsonFlow = androidx.compose.runtime.remember(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("imported_questions_json", null)
            ?: kotlinx.coroutines.flow.MutableStateFlow(null)
    }
    val importedJson by importedJsonFlow.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(importedJson) {
        if (!importedJson.isNullOrBlank()) {
            try {
                val questions = com.google.gson.Gson().fromJson(
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

    if (showTagDialog) {
        TagSuggestionDialog(
            currentTags = uiState.tags,
            availableTags = uiState.availableTags,
            onTagsConfirmed = { newTags ->
                viewModel.onEvent(CreateQuizEvent.TagsChanged(newTags))
            },
            onDismiss = { showTagDialog = false }
        )
    }

    // Navigate away after a successful publish.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaveComplete()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar.
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(CreateQuizEvent.ClearError)
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
                        onClick = { viewModel.onEvent(CreateQuizEvent.SaveDraft) },
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.create_save_draft),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    TextButton(
                        onClick = { viewModel.onEvent(CreateQuizEvent.PublishQuiz) },
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
            androidx.compose.material3.FloatingActionButton(
                onClick = { viewModel.onEvent(CreateQuizEvent.AddQuestion) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_add_question_cd)
                )
            }
        }
    ) { innerPadding ->
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
                    onValueChange = { viewModel.onEvent(CreateQuizEvent.TitleChanged(it)) },
                    label = stringResource(R.string.create_quiz_title_label),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Thumbnail URL
            item {
                TextInputField(
                    value = uiState.thumbnailUrl,
                    onValueChange = { viewModel.onEvent(CreateQuizEvent.ThumbnailUrlChanged(it)) },
                    label = stringResource(R.string.create_quiz_thumbnail_url_label),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Description
            item {
                TextInputField(
                    value = uiState.description,
                    onValueChange = { viewModel.onEvent(CreateQuizEvent.DescriptionChanged(it)) },
                    label = stringResource(R.string.create_quiz_description_label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    singleLine = false
                )
            }

            // Tags
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextInputField(
                        value = uiState.tags,
                        onValueChange = { viewModel.onEvent(CreateQuizEvent.TagsChanged(it)) },
                        label = stringResource(R.string.create_quiz_tags_label),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    FilledTonalIconButton(
                        onClick = { showTagDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = stringResource(R.string.create_quiz_pick_tags)
                        )
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
                            onCheckedChange = { viewModel.onEvent(CreateQuizEvent.IsPublicChanged(it)) }
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
                    onCheckedChange = { viewModel.onEvent(CreateQuizEvent.ShareToPoolChanged(it)) },
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
                        viewModel.onEvent(CreateQuizEvent.UpdateQuestion(index, updated))
                    },
                    onMoveUp = { viewModel.onEvent(CreateQuizEvent.MoveQuestionUp(index)) },
                    onMoveDown = { viewModel.onEvent(CreateQuizEvent.MoveQuestionDown(index)) },
                    onRemove = { viewModel.onEvent(CreateQuizEvent.RemoveQuestion(index)) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
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

            // ---- Choices ----
            question.choices.forEachIndexed { cIdx, choice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Tap to mark as correct answer
                    RadioButton(
                        selected = cIdx in question.correctIndices,
                        onClick = {
                            val newIndices = if (question.isMultiSelect) {
                                if (cIdx in question.correctIndices)
                                    question.correctIndices - cIdx
                                else
                                    question.correctIndices + cIdx
                            } else {
                                setOf(cIdx)
                            }
                            onQuestionChange(question.copy(correctIndices = newIndices))
                        }
                    )

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
