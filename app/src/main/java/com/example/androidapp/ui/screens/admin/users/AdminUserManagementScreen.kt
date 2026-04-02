package com.example.androidapp.ui.screens.admin.users

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
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.ui.components.admin.AdminUserCard
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Admin user management screen for managing user accounts, roles, and bans.
 *
 * @param viewModel The ViewModel for managing user management state.
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
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
                        onRetry = { viewModel.loadUsers() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    UserManagementContent(
                        users = uiState.users,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onRoleChange = { userId, newRole ->
                            viewModel.updateUserRole(userId, newRole)
                        },
                        onBanToggle = { user ->
                            userToBanUnban = user
                        },
                        onDelete = { user ->
                            userToDelete = user
                        },
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

    // Ban/Unban confirmation dialog
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
            kotlinx.coroutines.delay(3000)
            viewModel.clearActionError()
        }
    }
}

@Composable
private fun UserManagementContent(
    users: List<User>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onRoleChange: (String, UserRole) -> Unit,
    onBanToggle: (User) -> Unit,
    onDelete: (User) -> Unit,
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
            label = stringResource(R.string.admin_search_users),
            modifier = Modifier.fillMaxWidth()
        )

        // User count
        Text(
            text = stringResource(R.string.admin_user_count, users.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // User list
        if (users.isEmpty()) {
            EmptyState(
                message = if (searchQuery.isBlank()) {
                    stringResource(R.string.admin_no_users)
                } else {
                    stringResource(R.string.admin_no_users_search)
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
                items(users, key = { it.id }) { user ->
                    AdminUserCard(
                        user = user,
                        onRoleChange = { newRole -> onRoleChange(user.id, newRole) },
                        onBanToggle = { onBanToggle(user) },
                        onDelete = { onDelete(user) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserManagementContentPreview() {
    QuizzezTheme {
        UserManagementContent(
            users = listOf(
                User(
                    id = "user1",
                    email = "admin@example.com",
                    displayName = "Quản trị viên",
                    role = UserRole.ADMIN
                ),
                User(
                    id = "user2",
                    email = "user@example.com",
                    displayName = "Nguyễn Văn A",
                    role = UserRole.USER
                ),
                User(
                    id = "user3",
                    email = "banned@example.com",
                    displayName = "Người dùng bị cấm",
                    role = UserRole.USER,
                    isBanned = true
                )
            ),
            searchQuery = "",
            onSearchQueryChanged = {},
            onRoleChange = { _, _ -> },
            onBanToggle = {},
            onDelete = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
