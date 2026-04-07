package com.example.androidapp.ui.screens.admin.quizzes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.admin.AdminQuizCard
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Admin quiz management screen for managing quizzes, publishing, and deletion.
 *
 * @param viewModel The ViewModel for managing quiz management state.
 * @param onNavigateBack Callback to navigate back.
 * @param onQuizClick Callback when a quiz card is clicked.
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizManagementScreen(
    viewModel: AdminQuizManagementViewModel,
    onNavigateBack: () -> Unit,
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var quizToDelete by remember { mutableStateOf<Quiz?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_manage_quizzes),
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingSpinner(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadQuizzes() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    QuizManagementContent(
                        uiState = uiState,
                        quizzes = uiState.quizzes,
                        searchQuery = uiState.searchQuery,
                        showDeleted = uiState.showDeleted,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onToggleShowDeleted = { viewModel.toggleShowDeleted() },
                        onQuizClick = onQuizClick,
                        onPublishToggle = { quiz ->
                            viewModel.togglePublishQuiz(quiz.id, quiz.isPublic)
                        },
                        onRestore = { quiz ->
                            viewModel.restoreQuiz(quiz.id)
                        },
                        onDelete = { quiz ->
                            quizToDelete = quiz
                        },
                        onLoadMore = { viewModel.loadMoreQuizzes() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Loading overlay for actions
            if (uiState.isPerformingAction) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingSpinner()
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    quizToDelete?.let { quiz ->
        val isPermanentDelete = quiz.deletedAt != null
        AppAlertDialog(
            title = if (isPermanentDelete) {
                stringResource(R.string.admin_delete_quiz_permanent_title)
            } else {
                stringResource(R.string.admin_delete_quiz_title)
            },
            message = if (isPermanentDelete) {
                stringResource(R.string.admin_delete_quiz_permanent_message, quiz.title)
            } else {
                stringResource(R.string.admin_delete_quiz_message, quiz.title)
            },
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.deleteQuiz(quiz.id)
                quizToDelete = null
            },
            onDismiss = { quizToDelete = null },
            isDestructive = true
        )
    }

    // Action error snackbar
    uiState.actionError?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearActionError()
        }
    }
}

@Composable
private fun QuizManagementContent(
    uiState: AdminQuizManagementUiState,
    quizzes: List<Quiz>,
    searchQuery: String,
    showDeleted: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onToggleShowDeleted: () -> Unit,
    onQuizClick: (String) -> Unit,
    onPublishToggle: (Quiz) -> Unit,
    onRestore: (Quiz) -> Unit,
    onDelete: (Quiz) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar
        TextInputField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = stringResource(R.string.admin_search_quizzes),
            modifier = Modifier.fillMaxWidth()
        )

        // Filters row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.admin_quiz_count, quizzes.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.admin_show_deleted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Switch(
                    checked = showDeleted,
                    onCheckedChange = { onToggleShowDeleted() }
                )
            }
        }

        // Quiz list
        if (quizzes.isEmpty()) {
            EmptyState(
                message = if (searchQuery.isBlank()) {
                    if (showDeleted) {
                        stringResource(R.string.admin_no_deleted_quizzes)
                    } else {
                        stringResource(R.string.admin_no_quizzes)
                    }
                } else {
                    stringResource(R.string.admin_no_quizzes_search)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(quizzes, key = { it.id }) { quiz ->
                    AdminQuizCard(
                        quiz = quiz,
                        onClick = { onQuizClick(quiz.id) },
                        onPublishToggle = { onPublishToggle(quiz) },
                        onRestore = if (quiz.deletedAt != null) {
                            { onRestore(quiz) }
                        } else null,
                        onDelete = { onDelete(quiz) }
                    )
                }

                // Pagination: load more trigger
                if (uiState.hasMore && !uiState.isLoading) {
                    item {
                        LaunchedEffect(Unit) {
                            onLoadMore()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuizManagementContentPreview() {
    QuizzezTheme {
        QuizManagementContent(
            uiState = AdminQuizManagementUiState(isLoading = false, hasMore = false),
            quizzes = listOf(
                Quiz(
                    id = "quiz1",
                    title = "Kiểm tra tiếng Việt lớp 10",
                    ownerId = "user1",
                    authorName = "Nguyễn Văn A",
                    tags = listOf("Tiếng Việt", "Lớp 10"),
                    questionCount = 20,
                    attemptCount = 145,
                    isPublic = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    deletedAt = null
                ),
                Quiz(
                    id = "quiz2",
                    title = "Quiz toán học",
                    ownerId = "user2",
                    authorName = "Trần Thị B",
                    tags = listOf("Toán học"),
                    questionCount = 15,
                    attemptCount = 78,
                    isPublic = false,
                    shareCode = "ABC123",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    deletedAt = null
                )
            ),
            searchQuery = "",
            showDeleted = false,
            onSearchQueryChanged = {},
            onToggleShowDeleted = {},
            onQuizClick = {},
            onPublishToggle = {},
            onRestore = {},
            onDelete = {},
            onLoadMore = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
