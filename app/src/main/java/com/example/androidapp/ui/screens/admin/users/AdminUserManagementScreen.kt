package com.example.androidapp.ui.screens.admin.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.ui.components.admin.AdminUserCard
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Admin user management screen for managing user accounts, roles, and bans.
 *
 * Features a modern pill-shaped search bar, user count indicator, and a
 * scrollable list of [AdminUserCard] items with confirmation dialogs for
 * destructive actions.
 *
 * @param viewModel The ViewModel for managing user management state.
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for external styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    viewModel: AdminUserManagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var userToDelete by remember { mutableStateOf<User?>(null) }
    var userToBanUnban by remember { mutableStateOf<User?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_manage_users),
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
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
                        message = uiState.error,
                        onRetry = { viewModel.loadUsers() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    UserManagementContent(
                        uiState = uiState,
                        users = uiState.users,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onRoleChange = { userId, newRole ->
                            viewModel.updateUserRole(userId, newRole)
                        },
                        onBanToggle = { user -> userToBanUnban = user },
                        onDelete = { user -> userToDelete = user },
                        onLoadMore = { viewModel.loadMoreUsers() },
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
    userToDelete?.let { user ->
        AppAlertDialog(
            title = stringResource(R.string.admin_delete_user_title),
            message = stringResource(R.string.admin_delete_user_message, user.displayName),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.deleteUser(user.id)
                userToDelete = null
            },
            onDismiss = { userToDelete = null },
            isDestructive = true
        )
    }

    // Ban / Unban confirmation dialog
    userToBanUnban?.let { user ->
        val isBanning = !user.isBanned
        AppAlertDialog(
            title = if (isBanning) {
                stringResource(R.string.admin_ban_user_title)
            } else {
                stringResource(R.string.admin_unban_user_title)
            },
            message = if (isBanning) {
                stringResource(R.string.admin_ban_user_message, user.displayName)
            } else {
                stringResource(R.string.admin_unban_user_message, user.displayName)
            },
            confirmText = if (isBanning) {
                stringResource(R.string.admin_ban)
            } else {
                stringResource(R.string.admin_unban)
            },
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                if (isBanning) {
                    viewModel.banUser(user.id)
                } else {
                    viewModel.unbanUser(user.id)
                }
                userToBanUnban = null
            },
            onDismiss = { userToBanUnban = null }
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

// ---------------------------------------------------------------------------
// Private composables
// ---------------------------------------------------------------------------

/**
 * Main content area: search bar, user count, and scrollable user list.
 */
@Composable
private fun UserManagementContent(
    uiState: AdminUserManagementUiState,
    users: List<User>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onRoleChange: (String, UserRole) -> Unit,
    onBanToggle: (User) -> Unit,
    onDelete: (User) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search bar + user count header (non-scrollable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillSearchBar(
                query = searchQuery,
                onQueryChanged = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.admin_user_count, users.size),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = InterFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // User list (scrollable)
        if (users.isEmpty()) {
            EmptyState(
                message = if (searchQuery.isBlank()) {
                    stringResource(R.string.admin_no_users)
                } else {
                    stringResource(R.string.admin_no_users_search)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(users, key = { it.id }) { user ->
                    AdminUserCard(
                        user = user,
                        onRoleChange = { newRole -> onRoleChange(user.id, newRole) },
                        onBanToggle = { onBanToggle(user) },
                        onDelete = { onDelete(user) }
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
 * Modern pill-shaped search bar with no outline, filled background,
 * leading search icon, and optional trailing clear button.
 */
@Composable
private fun PillSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    TextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = {
            Text(
                text = stringResource(R.string.admin_search_users_placeholder),
                fontFamily = InterFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.admin_search_users),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = InterFamily,
            color = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        ),
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val previewUsers = listOf(
    User(
        id = "1",
        email = "admin@example.com",
        displayName = "Quan Tri Vien",
        role = UserRole.ADMIN
    ),
    User(
        id = "2",
        email = "user@example.com",
        displayName = "Nguyen Van A",
        photoUrl = "https://example.com/avatar.jpg",
        role = UserRole.USER
    ),
    User(
        id = "3",
        email = "banned@example.com",
        displayName = "Nguoi Dung Bi Cam",
        role = UserRole.USER,
        isBanned = true
    ),
    User(
        id = "4",
        email = "guest@example.com",
        displayName = "Khach",
        role = UserRole.GUEST
    )
)

@Preview(showBackground = true, name = "User Management Content - Light")
@Composable
private fun UserManagementContentLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UserManagementContent(
                uiState = AdminUserManagementUiState(isLoading = false),
                users = previewUsers,
                searchQuery = "",
                onSearchQueryChanged = {},
                onRoleChange = { _, _ -> },
                onBanToggle = {},
                onDelete = {},
                onLoadMore = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, name = "User Management Content - Dark")
@Composable
private fun UserManagementContentDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UserManagementContent(
                uiState = AdminUserManagementUiState(isLoading = false),
                users = previewUsers,
                searchQuery = "Nguyen",
                onSearchQueryChanged = {},
                onRoleChange = { _, _ -> },
                onBanToggle = {},
                onDelete = {},
                onLoadMore = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, name = "User Management - Empty State")
@Composable
private fun UserManagementEmptyPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UserManagementContent(
                uiState = AdminUserManagementUiState(isLoading = false),
                users = emptyList(),
                searchQuery = "xyz",
                onSearchQueryChanged = {},
                onRoleChange = { _, _ -> },
                onBanToggle = {},
                onDelete = {},
                onLoadMore = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, name = "Pill Search Bar - Light")
@Composable
private fun PillSearchBarLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(16.dp)
        ) {
            PillSearchBar(
                query = "",
                onQueryChanged = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, name = "Pill Search Bar - Dark")
@Composable
private fun PillSearchBarDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(16.dp)
        ) {
            PillSearchBar(
                query = "admin",
                onQueryChanged = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
