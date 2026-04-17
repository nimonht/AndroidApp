package com.example.androidapp.ui.screens.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.navigation.AppTopBar
import kotlin.math.max

/**
 * Trash/Recycle Bin screen showing soft-deleted quizzes.
 * Stateless composable; all state is owned by [RecycleBinViewModel].
 *
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
 */
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: RecycleBinViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecycleBinViewModel(container.quizRepository, container.authRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var quizToDeletePermanently by remember { mutableStateOf<String?>(null) }

    val successMessage = uiState.successMessage?.toMessage()
    LaunchedEffect(uiState.successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            viewModel.onEvent(RecycleBinEvent.ClearMessage)
        }
    }
    val errorMessage = uiState.error?.toMessage()
    LaunchedEffect(uiState.error) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onEvent(RecycleBinEvent.ClearError)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.trash_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack,
                actions = {
                    if (uiState.deletedQuizzes.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.trash_empty_action_cd)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingSpinner(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )

            uiState.deletedQuizzes.isEmpty() -> EmptyState(
                message = stringResource(R.string.trash_empty),
                icon = Icons.Default.Delete,
                modifier = Modifier.padding(innerPadding).fillMaxWidth()
            )

            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.deletedQuizzes) { quiz ->
                    TrashQuizCard(
                        quiz = quiz,
                        onRestore = { viewModel.onEvent(RecycleBinEvent.RestoreQuiz(quiz.id)) },
                        onDeletePermanently = { quizToDeletePermanently = quiz.id }
                    )
                }

                // Pagination: load more trigger
                if (uiState.hasMore) {
                    item {
                        LaunchedEffect(Unit) {
                            viewModel.onEvent(RecycleBinEvent.LoadMore)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        if (showEmptyTrashDialog) {
            AppAlertDialog(
                title = stringResource(R.string.trash_empty_confirm_title),
                message = stringResource(R.string.trash_empty_confirm_message),
                confirmText = stringResource(R.string.delete),
                dismissText = stringResource(R.string.cancel),
                onConfirm = {
                    showEmptyTrashDialog = false
                    viewModel.onEvent(RecycleBinEvent.EmptyTrash)
                },
                onDismiss = { showEmptyTrashDialog = false }
            )
        }

        quizToDeletePermanently?.let { quizId ->
            AppAlertDialog(
                title = stringResource(R.string.delete_confirm_title),
                message = stringResource(R.string.trash_delete_permanently_confirm_message),
                confirmText = stringResource(R.string.delete),
                dismissText = stringResource(R.string.cancel),
                onConfirm = {
                    viewModel.onEvent(RecycleBinEvent.DeletePermanently(quizId))
                    quizToDeletePermanently = null
                },
                onDismiss = { quizToDeletePermanently = null }
            )
        }
    }
}

@Composable
private fun TrashQuizCard(
    quiz: Quiz,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Retention period: 30 days
    val millisInDay = 1000L * 60 * 60 * 24
    val deletedAt = quiz.deletedAt ?: System.currentTimeMillis()
    val elapsedDays = (System.currentTimeMillis() - deletedAt) / millisInDay
    val daysLeft = max(0, 30 - elapsedDays).toInt()

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = quiz.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.quiz_questions, quiz.questionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.trash_days_remaining, daysLeft),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = stringResource(R.string.trash_action_restore_cd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeletePermanently) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.trash_action_delete_permanently_cd),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
