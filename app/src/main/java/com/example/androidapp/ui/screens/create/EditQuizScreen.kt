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
 * Edit Quiz screen reusing the same form structure as the Create Quiz screen.
 * Pre-populates all fields from the existing quiz loaded via [EditQuizViewModel].
 *
 * @param quizId The ID of the quiz to edit.
 * @param onNavigateBack Callback to navigate back.
 * @param onSaveComplete Callback when the quiz is saved successfully.
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
                EditQuizViewModel(quizId, container.quizRepository, container.authRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away when saved
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaveComplete()
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(EditQuizEvent.ClearError)
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
                        onClick = { viewModel.onEvent(EditQuizEvent.SaveQuiz) },
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
            FloatingActionButton(onClick = { viewModel.onEvent(EditQuizEvent.AddQuestion) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_add_question_cd))
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
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
                        onValueChange = { viewModel.onEvent(EditQuizEvent.TitleChanged(it)) },
                    label = { Text(stringResource(R.string.create_quiz_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.onEvent(EditQuizEvent.DescriptionChanged(it)) },
                        label = { Text(stringResource(R.string.create_quiz_description_label)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
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
                            onCheckedChange = { viewModel.onEvent(EditQuizEvent.IsPublicChanged(it)) }
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
                            viewModel.onEvent(EditQuizEvent.UpdateQuestion(index, updated))
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

