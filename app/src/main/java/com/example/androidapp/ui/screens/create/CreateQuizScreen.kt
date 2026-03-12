package com.example.androidapp.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Create Quiz screen with multi-step form.
 * Stateless composable; all state is owned by [CreateQuizViewModel].
 *
 * @param onNavigateBack Callback to navigate back.
 * @param onSaveComplete Callback when quiz is saved successfully.
 * @param modifier Modifier for styling.
 */
@Composable
fun CreateQuizScreen(
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: CreateQuizViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CreateQuizViewModel(container.quizRepository, container.authRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaveComplete()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(CreateQuizEvent.ClearError)
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
                    TextButton(
                        onClick = { viewModel.onEvent(CreateQuizEvent.SaveQuiz) },
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(CreateQuizEvent.AddQuestion) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_add_question_cd))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onEvent(CreateQuizEvent.TitleChanged(it)) },
                    label = { Text(stringResource(R.string.create_quiz_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onEvent(CreateQuizEvent.DescriptionChanged(it)) },
                    label = { Text(stringResource(R.string.create_quiz_description_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = MaterialTheme.shapes.small,
                    maxLines = 5
                )
            }
            item {
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
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.create_questions_header, uiState.questions.size),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            itemsIndexed(uiState.questions) { index, question ->
                QuestionEditorCard(
                    questionNumber = index + 1,
                    question = question,
                    onQuestionChange = { updated ->
                        viewModel.onEvent(CreateQuizEvent.UpdateQuestion(index, updated))
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/**
 * Shared question editor card used in both [CreateQuizScreen] and [EditQuizScreen].
 */
@Composable
internal fun QuestionEditorCard(
    questionNumber: Int,
    question: QuestionDraft,
    onQuestionChange: (QuestionDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.create_question_title, questionNumber),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (question.content.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = question.content,
                onValueChange = { onQuestionChange(question.copy(content = it)) },
                label = { Text(stringResource(R.string.create_question_content_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )
            Spacer(modifier = Modifier.height(8.dp))
            question.choices.forEachIndexed { cIdx, choice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = cIdx in question.correctIndices,
                        onClick = {
                            val newIndices = if (question.isMultiSelect) {
                                if (cIdx in question.correctIndices) question.correctIndices - cIdx
                                else question.correctIndices + cIdx
                            } else setOf(cIdx)
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
                        placeholder = { Text(stringResource(R.string.create_choice_hint, cIdx + 1)) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        singleLine = true
                    )
                }
            }
        }
    }
}
