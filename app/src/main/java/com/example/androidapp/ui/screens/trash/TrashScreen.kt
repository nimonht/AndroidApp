package com.example.androidapp.ui.screens.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Trash/Recycle Bin screen showing deleted quizzes.
 * Allows restore or permanent deletion.
 *
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
 */
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with actual data from ViewModel
    var deletedQuizzes by remember {
        mutableStateOf(
            listOf(
                DeletedQuiz("1", "Old Math Quiz", "Deleted 5 days ago"),
                DeletedQuiz("2", "Draft Quiz", "Deleted 10 days ago")
            )
        )
    }
    var showDeleteDialog by remember { mutableStateOf<DeletedQuiz?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "Recycle Bin",
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        if (deletedQuizzes.isEmpty()) {
            EmptyState(
                message = "Recycle bin is empty",
                icon = Icons.Default.Delete,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // Info banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Items are permanently deleted after 30 days",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(deletedQuizzes) { quiz ->
                        DeletedQuizCard(
                            quiz = quiz,
                            onRestore = {
                                // TODO: Implement restore
                                deletedQuizzes = deletedQuizzes - quiz
                            },
                            onDelete = {
                                showDeleteDialog = quiz
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { quiz ->
        AppAlertDialog(
            title = "Delete Permanently?",
            message = "\"${quiz.title}\" will be permanently deleted. This action cannot be undone.",
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                deletedQuizzes = deletedQuizzes - quiz
                showDeleteDialog = null
            },
            isDestructive = true,
            icon = Icons.Default.Delete
        )
    }
}

/**
 * Preview data class for deleted quizzes.
 * TODO: Replace with domain model.
 */
private data class DeletedQuiz(
    val id: String,
    val title: String,
    val deletedTime: String
)

@Composable
private fun DeletedQuizCard(
    quiz: DeletedQuiz,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = quiz.deletedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete permanently",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
