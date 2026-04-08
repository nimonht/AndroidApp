package com.example.androidapp.ui.screens.admin.quizzes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.ui.components.admin.AdminQuizCard
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.theme.InterFamily
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
                        message = uiState.error!!.toMessage(),
                        onRetry = { viewModel.loadQuizzes() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    QuizManagementContent(
                        uiState = uiState,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onStatusFilterChanged = { viewModel.onStatusFilterChanged(it) },
                        onTagFilterChanged = { viewModel.onTagFilterChanged(it) },
                        onSortFieldChanged = { viewModel.onSortFieldChanged(it) },
                        onToggleSortOrder = { viewModel.onToggleSortOrder() },
                        onClearFilters = { viewModel.clearFilters() },
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
        val errorMessage = error.toMessage()
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearActionError()
        }
    }
}

/**
 * Returns the number of active non-default filters.
 */
private fun countActiveFilters(uiState: AdminQuizManagementUiState): Int {
    var count = 0
    if (uiState.statusFilter != QuizStatusFilter.ALL) count++
    if (uiState.tagFilter.isNotBlank()) count++
    return count
}

/**
 * Returns the user-facing label for a [QuizStatusFilter].
 */
@Composable
private fun statusFilterLabel(filter: QuizStatusFilter): String = when (filter) {
    QuizStatusFilter.ALL -> stringResource(R.string.admin_filter_status_all)
    QuizStatusFilter.PUBLIC -> stringResource(R.string.admin_filter_status_public)
    QuizStatusFilter.PRIVATE -> stringResource(R.string.admin_filter_status_private)
    QuizStatusFilter.DRAFT -> stringResource(R.string.admin_filter_status_draft)
    QuizStatusFilter.DELETED -> stringResource(R.string.admin_filter_status_deleted)
}

/**
 * Returns the user-facing label for a [QuizSortField].
 */
@Composable
private fun sortFieldLabel(field: QuizSortField): String = when (field) {
    QuizSortField.DATE -> stringResource(R.string.admin_sort_by_date)
    QuizSortField.NAME -> stringResource(R.string.admin_sort_by_name)
    QuizSortField.ATTEMPTS -> stringResource(R.string.admin_sort_by_attempts)
    QuizSortField.QUESTIONS -> stringResource(R.string.admin_sort_by_questions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizManagementContent(
    uiState: AdminQuizManagementUiState,
    onSearchQueryChanged: (String) -> Unit,
    onStatusFilterChanged: (QuizStatusFilter) -> Unit,
    onTagFilterChanged: (String) -> Unit,
    onSortFieldChanged: (QuizSortField) -> Unit,
    onToggleSortOrder: () -> Unit,
    onClearFilters: () -> Unit,
    onQuizClick: (String) -> Unit,
    onPublishToggle: (Quiz) -> Unit,
    onRestore: (Quiz) -> Unit,
    onDelete: (Quiz) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quizzes = uiState.quizzes
    val activeFilterCount = countActiveFilters(uiState)
    var filtersExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { filtersExpanded = !filtersExpanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (filtersExpanded) {
                        stringResource(R.string.admin_hide_filters)
                    } else {
                        stringResource(R.string.admin_show_filters)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = InterFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (filtersExpanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Collapsible filter section
        AnimatedVisibility(
            visible = filtersExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search bar
                TextInputField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = stringResource(R.string.admin_search_quizzes),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status filter chips (horizontally scrollable)
                Text(
                    text = stringResource(R.string.admin_filter_by_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    QuizStatusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.statusFilter == filter,
                            onClick = { onStatusFilterChanged(filter) },
                            label = { Text(statusFilterLabel(filter)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Tag filter input
                OutlinedTextField(
                    value = uiState.tagFilter,
                    onValueChange = onTagFilterChanged,
                    label = { Text(stringResource(R.string.admin_filter_by_tag)) },
                    placeholder = { Text(stringResource(R.string.admin_filter_tag_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.tagFilter.isNotBlank()) {
                            IconButton(onClick = { onTagFilterChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.admin_clear_filters)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Sort controls row
                SortControlsRow(
                    sortField = uiState.sortField,
                    sortAscending = uiState.sortAscending,
                    onSortFieldChanged = onSortFieldChanged,
                    onToggleSortOrder = onToggleSortOrder,
                    modifier = Modifier.fillMaxWidth()
                )

                // Active filter indicator + clear button
                if (activeFilterCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.admin_active_filters, activeFilterCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = onClearFilters) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.admin_clear_filters),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Quiz count
        Text(
            text = stringResource(R.string.admin_quiz_count, quizzes.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Quiz list
        if (quizzes.isEmpty()) {
            EmptyState(
                message = if (uiState.searchQuery.isBlank() && activeFilterCount == 0) {
                    stringResource(R.string.admin_no_quizzes)
                } else if (uiState.statusFilter == QuizStatusFilter.DELETED) {
                    stringResource(R.string.admin_no_deleted_quizzes)
                } else {
                    stringResource(R.string.admin_no_quizzes_search)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            )
        } else {
            val hasPublishPermission = uiState.currentPermissions.contains(
                AdminPermission.PUBLISH_QUIZZES
            ) || uiState.isSuperuser
            val hasDeletePermission = uiState.currentPermissions.contains(
                AdminPermission.DELETE_QUIZZES
            ) || uiState.isSuperuser
            val hasManagePermission = uiState.currentPermissions.contains(
                AdminPermission.MANAGE_QUIZZES
            ) || uiState.isSuperuser

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(quizzes, key = { it.id }) { quiz ->
                    AdminQuizCard(
                        quiz = quiz,
                        onClick = { onQuizClick(quiz.id) },
                        onPublishToggle = { onPublishToggle(quiz) },
                        onRestore = if (quiz.deletedAt != null && hasManagePermission) {
                            { onRestore(quiz) }
                        } else {
                            null
                        },
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

/**
 * Row of sort controls: a dropdown for selecting the sort field and a toggle
 * button for ascending/descending order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortControlsRow(
    sortField: QuizSortField,
    sortAscending: Boolean,
    onSortFieldChanged: (QuizSortField) -> Unit,
    onToggleSortOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = stringResource(R.string.admin_sort_order),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Sort field dropdown
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = sortFieldLabel(sortField),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .height(48.dp)
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                QuizSortField.entries.forEach { field ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sortFieldLabel(field),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onSortFieldChanged(field)
                            dropdownExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        // Sort direction toggle
        IconButton(onClick = onToggleSortOrder) {
            Icon(
                imageVector = if (sortAscending) {
                    Icons.Default.ArrowUpward
                } else {
                    Icons.Default.ArrowDownward
                },
                contentDescription = if (sortAscending) {
                    stringResource(R.string.admin_sort_asc)
                } else {
                    stringResource(R.string.admin_sort_desc)
                },
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuizManagementContentPreview() {
    QuizzezTheme {
        QuizManagementContent(
            uiState = AdminQuizManagementUiState(
                isLoading = false,
                hasMore = false,
                quizzes = listOf(
                    Quiz(
                        id = "quiz1",
                        title = "Kiem tra tieng Viet lop 10",
                        ownerId = "user1",
                        authorName = "Nguyen Van A",
                        tags = listOf("Tieng Viet", "Lop 10"),
                        questionCount = 20,
                        attemptCount = 145,
                        isPublic = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        deletedAt = null
                    ),
                    Quiz(
                        id = "quiz2",
                        title = "Quiz toan hoc",
                        ownerId = "user2",
                        authorName = "Tran Thi B",
                        tags = listOf("Toan hoc"),
                        questionCount = 15,
                        attemptCount = 78,
                        isPublic = false,
                        shareCode = "ABC123",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        deletedAt = null
                    )
                ),
                currentPermissions = setOf(
                    AdminPermission.MANAGE_QUIZZES,
                    AdminPermission.DELETE_QUIZZES,
                    AdminPermission.PUBLISH_QUIZZES
                )
            ),
            onSearchQueryChanged = {},
            onStatusFilterChanged = {},
            onTagFilterChanged = {},
            onSortFieldChanged = {},
            onToggleSortOrder = {},
            onClearFilters = {},
            onQuizClick = {},
            onPublishToggle = {},
            onRestore = {},
            onDelete = {},
            onLoadMore = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizManagementContentDarkPreview() {
    QuizzezTheme {
        QuizManagementContent(
            uiState = AdminQuizManagementUiState(
                isLoading = false,
                hasMore = false,
                quizzes = listOf(
                    Quiz(
                        id = "quiz1",
                        title = "Kiem tra tieng Viet lop 10",
                        ownerId = "user1",
                        authorName = "Nguyen Van A",
                        tags = listOf("Tieng Viet", "Lop 10"),
                        questionCount = 20,
                        attemptCount = 145,
                        isPublic = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        deletedAt = null
                    )
                ),
                statusFilter = QuizStatusFilter.PUBLIC,
                currentPermissions = setOf(AdminPermission.MANAGE_QUIZZES)
            ),
            onSearchQueryChanged = {},
            onStatusFilterChanged = {},
            onTagFilterChanged = {},
            onSortFieldChanged = {},
            onToggleSortOrder = {},
            onClearFilters = {},
            onQuizClick = {},
            onPublishToggle = {},
            onRestore = {},
            onDelete = {},
            onLoadMore = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
