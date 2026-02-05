package com.example.androidapp.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.androidapp.R
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Create Quiz screen with multi-step form.
 * Allows creating quiz metadata and adding questions.
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
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf(listOf(QuestionDraft())) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.quiz_create),
                canNavigateBack = true,
                navigateUp = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = {
                            // TODO: Save quiz to repository
                            onSaveComplete()
                        }
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
            FloatingActionButton(
                onClick = {
                    questions = questions + QuestionDraft()
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_add_question_cd)
                )
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
            // Quiz Title Input
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.create_quiz_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Quiz Description Input
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.create_quiz_description_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
            }

            // Divider and Questions Header
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.create_questions_header, questions.size),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Question Cards
            itemsIndexed(questions) { index, question ->
                QuestionCard(
                    questionNumber = index + 1,
                    question = question,
                    onQuestionChange = { updated ->
                        questions = questions.toMutableList().apply {
                            this[index] = updated
                        }
                    }
                )
            }

            // Bottom spacer for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Draft data class for question creation.
 * TODO: Move to domain model when implementing ViewModel.
 */
data class QuestionDraft(
    val content: String = "",
    val choices: List<String> = listOf("", "", "", ""),
    val correctIndex: Int = 0
)

@Composable
private fun QuestionCard(
    questionNumber: Int,
    question: QuestionDraft,
    onQuestionChange: (QuestionDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = question.content.ifBlank {
                    stringResource(R.string.create_question_tap_to_edit)
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = if (question.content.isBlank()) 
                    MaterialTheme.colorScheme.onSurfaceVariant 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
