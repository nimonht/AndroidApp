package com.example.androidapp.ui.screens.pool

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Question Pool screen for browsing community questions and managing contributions.
 * Stateless composable; all state is owned by [QuestionPoolViewModel].
 *
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
 */
@Composable
fun QuestionPoolScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: QuestionPoolViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuestionPoolViewModel(container.poolRepository, container.authRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var revokeTargetId by remember { mutableStateOf<String?>(null) }
    val errorMessage = uiState.error?.toMessage()
    val successMessage = uiState.successMessage?.toMessage()

    LaunchedEffect(uiState.successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            viewModel.onEvent(QuestionPoolEvent.ClearSuccess)
        }
    }
    LaunchedEffect(uiState.error) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onEvent(QuestionPoolEvent.ClearError)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.pool_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Tab Row
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onEvent(QuestionPoolEvent.TabSelected(0)) },
                    text = { Text(stringResource(R.string.pool_tab_my_contributions)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onEvent(QuestionPoolEvent.TabSelected(1)) },
                    text = { Text(stringResource(R.string.pool_tab_browse)) }
                )
            }

            when (uiState.selectedTab) {
                0 -> MyContributionsTab(
                    contributions = uiState.myContributions,
                    isLoading = uiState.isLoading,
                    hasMore = uiState.hasMoreContributions,
                    onLoadMore = { viewModel.onEvent(QuestionPoolEvent.LoadMoreContributions) },
                    onRevoke = { revokeTargetId = it }
                )

                1 -> BrowsePoolTab(
                    results = uiState.browseResults,
                    searchTags = uiState.searchTags,
                    isLoading = uiState.isLoading,
                    hasMore = uiState.hasMoreBrowse,
                    onLoadMore = { viewModel.onEvent(QuestionPoolEvent.LoadMoreBrowse) },
                    onSearchTagsChanged = { viewModel.onEvent(QuestionPoolEvent.SearchTagsChanged(it)) },
                    onSearch = { viewModel.onEvent(QuestionPoolEvent.SearchPool) }
                )
            }
        }

        // Revoke confirmation dialog
        revokeTargetId?.let { itemId ->
            AppAlertDialog(
                title = stringResource(R.string.pool_revoke_title),
                message = stringResource(R.string.pool_revoke_message),
                confirmText = stringResource(R.string.pool_revoke_confirm),
                dismissText = stringResource(R.string.cancel),
                isDestructive = true,
                onConfirm = {
                    viewModel.onEvent(QuestionPoolEvent.RevokeContribution(itemId))
                    revokeTargetId = null
                },
                onDismiss = { revokeTargetId = null }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// My Contributions Tab
// ---------------------------------------------------------------------------

@Composable
private fun MyContributionsTab(
    contributions: List<QuestionPoolItem>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRevoke: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> LoadingSpinner(modifier = modifier.fillMaxSize())
        contributions.isEmpty() -> EmptyState(
            message = stringResource(R.string.pool_my_contributions_empty),
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp)
        )

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(contributions, key = { it.id }) { item ->
                ContributionCard(
                    item = item,
                    onRevoke = { onRevoke(item.id) }
                )
            }

            // Pagination: load more trigger
            if (hasMore) {
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

@Composable
private fun ContributionCard(
    item: QuestionPoolItem,
    onRevoke: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Question content
            Text(
                text = item.question.content,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Choices preview
            item.question.choices.take(4).forEach { choice ->
                Text(
                    text = "• ${choice.content}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (choice.isCorrect) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags
            if (item.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(item.tags) { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Status & actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge
                    if (item.isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.pool_status_active),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.pool_status_revoked),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "  •  " + stringResource(R.string.pool_usage_count, item.usageCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Revoke button (only for active items)
                if (item.isActive) {
                    TextButton(
                        onClick = onRevoke,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.pool_revoke_confirm),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Browse Pool Tab
// ---------------------------------------------------------------------------

@Composable
private fun BrowsePoolTab(
    results: List<QuestionPoolItem>,
    searchTags: String,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onSearchTagsChanged: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchTags,
                onValueChange = onSearchTagsChanged,
                label = { Text(stringResource(R.string.pool_enter_tags_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            FilledTonalButton(
                onClick = onSearch,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.pool_search_button)
                )
            }
        }

        when {
            isLoading -> LoadingSpinner(modifier = Modifier.fillMaxSize())
            results.isEmpty() -> EmptyState(
                message = stringResource(R.string.pool_browse_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results, key = { it.id }) { item ->
                    BrowsePoolCard(item = item)
                }

                // Pagination: load more trigger
                if (hasMore) {
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

@Composable
private fun BrowsePoolCard(
    item: QuestionPoolItem,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.question.content,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Choices
            item.question.choices.take(4).forEach { choice ->
                Text(
                    text = "• ${choice.content}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (choice.isCorrect) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags
            if (item.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(item.tags) { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Usage info
            Text(
                text = stringResource(R.string.pool_usage_count, item.usageCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
