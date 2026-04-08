package com.example.androidapp.ui.screens.admin.users

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.toMutableStateMap
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
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.ui.components.admin.AdminUserCard
import com.example.androidapp.ui.components.common.AppAlertDialog
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Admin user management screen for managing user accounts, roles, and bans.
 *
 * Features a modern pill-shaped search bar, role filter chips, sort controls,
 * user count indicator, and a scrollable list of [AdminUserCard] items with
 * confirmation dialogs for destructive actions and a permission editor dialog
 * for superuser-to-admin permission management.
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
    var userToEditPermissions by remember { mutableStateOf<User?>(null) }
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
                        message = uiState.error?.toMessage(),
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
                        onRoleFilterChanged = { viewModel.onRoleFilterChanged(it) },
                        onSortFieldChanged = { viewModel.onSortFieldChanged(it) },
                        onToggleSortOrder = { viewModel.onToggleSortOrder() },
                        onClearFilters = { viewModel.clearFilters() },
                        onManagePermissions = { user -> userToEditPermissions = user },
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

    // Permission edit dialog (superuser only, admin target only)
    userToEditPermissions?.let { user ->
        PermissionEditDialog(
            user = user,
            onSave = { permissions ->
                viewModel.updatePermissions(user.id, permissions)
                userToEditPermissions = null
            },
            onDismiss = { userToEditPermissions = null }
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

// ---------------------------------------------------------------------------
// Private composables
// ---------------------------------------------------------------------------

/**
 * Main content area: search bar, filter controls, user count, and scrollable user list.
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
    onRoleFilterChanged: (UserRoleFilter) -> Unit,
    onSortFieldChanged: (UserSortField) -> Unit,
    onToggleSortOrder: () -> Unit,
    onClearFilters: () -> Unit,
    onManagePermissions: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    var filtersExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Filter toggle + filter controls (non-scrollable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Toggle row for expanding/collapsing filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filtersExpanded = !filtersExpanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                        style = MaterialTheme.typography.labelLarge,
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PillSearchBar(
                        query = searchQuery,
                        onQueryChanged = onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role filter chips
                    RoleFilterRow(
                        selectedFilter = uiState.roleFilter,
                        onFilterSelected = onRoleFilterChanged,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sort controls + active filter indicator
                    SortAndFilterIndicatorRow(
                        uiState = uiState,
                        onSortFieldChanged = onSortFieldChanged,
                        onToggleSortOrder = onToggleSortOrder,
                        onClearFilters = onClearFilters,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                message = if (searchQuery.isBlank() && uiState.roleFilter == UserRoleFilter.ALL) {
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
                        onDelete = { onDelete(user) },
                        currentUserIsSuperuser = uiState.isSuperuser,
                        currentPermissions = uiState.currentPermissions,
                        isCurrentUser = user.id == uiState.currentUserId,
                        onManagePermissions = if (uiState.isSuperuser && user.role == UserRole.ADMIN) {
                            { onManagePermissions(user) }
                        } else {
                            null
                        }
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
 * Horizontal scrollable row of [FilterChip]s for [UserRoleFilter] options.
 */
@Composable
private fun RoleFilterRow(
    selectedFilter: UserRoleFilter,
    onFilterSelected: (UserRoleFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserRoleFilter.entries.forEach { filter ->
            val label = when (filter) {
                UserRoleFilter.ALL -> stringResource(R.string.admin_filter_role_all)
                UserRoleFilter.SUPERUSER -> stringResource(R.string.admin_filter_role_superuser)
                UserRoleFilter.ADMIN -> stringResource(R.string.admin_filter_role_admin)
                UserRoleFilter.USER -> stringResource(R.string.admin_filter_role_user)
                UserRoleFilter.BANNED -> stringResource(R.string.admin_filter_role_banned)
            }

            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = InterFamily
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Row containing the sort dropdown, ascending/descending toggle, and an
 * active filter count indicator with a clear button.
 */
@Composable
private fun SortAndFilterIndicatorRow(
    uiState: AdminUserManagementUiState,
    onSortFieldChanged: (UserSortField) -> Unit,
    onToggleSortOrder: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    // Count active filters (non-default values)
    val activeFilterCount = listOf(
        uiState.roleFilter != UserRoleFilter.ALL,
        uiState.sortField != UserSortField.NAME,
        !uiState.sortAscending,
        uiState.searchQuery.isNotBlank()
    ).count { it }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Sort controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Sort field dropdown
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.admin_sort_order),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortMenuItem(
                        text = stringResource(R.string.admin_sort_by_name),
                        selected = uiState.sortField == UserSortField.NAME,
                        onClick = {
                            onSortFieldChanged(UserSortField.NAME)
                            showSortMenu = false
                        }
                    )
                    SortMenuItem(
                        text = stringResource(R.string.admin_sort_by_email),
                        selected = uiState.sortField == UserSortField.EMAIL,
                        onClick = {
                            onSortFieldChanged(UserSortField.EMAIL)
                            showSortMenu = false
                        }
                    )
                    SortMenuItem(
                        text = stringResource(R.string.admin_sort_by_role),
                        selected = uiState.sortField == UserSortField.ROLE,
                        onClick = {
                            onSortFieldChanged(UserSortField.ROLE)
                            showSortMenu = false
                        }
                    )
                    SortMenuItem(
                        text = stringResource(R.string.admin_sort_by_date),
                        selected = uiState.sortField == UserSortField.DATE,
                        onClick = {
                            onSortFieldChanged(UserSortField.DATE)
                            showSortMenu = false
                        }
                    )
                }
            }

            // Sort order label
            Text(
                text = sortFieldLabel(uiState.sortField),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = InterFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Ascending / descending toggle
            IconButton(onClick = onToggleSortOrder) {
                Icon(
                    imageVector = if (uiState.sortAscending) {
                        Icons.Default.ArrowUpward
                    } else {
                        Icons.Default.ArrowDownward
                    },
                    contentDescription = if (uiState.sortAscending) {
                        stringResource(R.string.admin_sort_asc)
                    } else {
                        stringResource(R.string.admin_sort_desc)
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Active filter indicator + clear button
        if (activeFilterCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.admin_active_filters, activeFilterCount),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = InterFamily,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onClearFilters) {
                    Text(
                        text = stringResource(R.string.admin_clear_filters),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = InterFamily
                    )
                }
            }
        }
    }
}

/**
 * Returns the localised label for the current [UserSortField].
 */
@Composable
private fun sortFieldLabel(sortField: UserSortField): String = when (sortField) {
    UserSortField.NAME -> stringResource(R.string.admin_sort_by_name)
    UserSortField.EMAIL -> stringResource(R.string.admin_sort_by_email)
    UserSortField.ROLE -> stringResource(R.string.admin_sort_by_role)
    UserSortField.DATE -> stringResource(R.string.admin_sort_by_date)
}

/**
 * A single item inside the sort dropdown, with a visual selected indicator.
 */
@Composable
private fun SortMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        onClick = onClick,
        modifier = modifier
    )
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

/**
 * Dialog allowing a superuser to toggle individual [AdminPermission] values
 * for an admin user, then save the result.
 *
 * @param user The admin user whose permissions are being edited.
 * @param onSave Callback with the updated permission set when the user taps save.
 * @param onDismiss Callback when the dialog is dismissed without saving.
 * @param modifier Modifier for external styling.
 */
@Composable
private fun PermissionEditDialog(
    user: User,
    onSave: (Set<AdminPermission>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Initialise mutable permission state from the user's current permissions
    val permissionStates = remember(user.id) {
        AdminPermission.entries
            .map { it to (it in user.permissions) }
            .toMutableStateMap()
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.admin_permissions_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.displayName.ifBlank { user.email },
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = InterFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AdminPermission.entries.forEach { permission ->
                    val checked = permissionStates[permission] ?: false
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { permissionStates[permission] = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = permissionDisplayText(permission),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = InterFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = permissionStates
                        .filter { it.value }
                        .keys
                        .toSet()
                    onSave(selected)
                }
            ) {
                Text(
                    text = stringResource(R.string.admin_permissions_save),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontFamily = InterFamily
                )
            }
        }
    )
}

/**
 * Returns the localised display text for an [AdminPermission].
 */
@Composable
private fun permissionDisplayText(permission: AdminPermission): String = when (permission) {
    AdminPermission.MANAGE_USERS -> stringResource(R.string.admin_permission_manage_users)
    AdminPermission.CHANGE_USER_ROLES -> stringResource(R.string.admin_permission_change_user_roles)
    AdminPermission.DELETE_USERS -> stringResource(R.string.admin_permission_delete_users)
    AdminPermission.BAN_USERS -> stringResource(R.string.admin_permission_ban_users)
    AdminPermission.MANAGE_QUIZZES -> stringResource(R.string.admin_permission_manage_quizzes)
    AdminPermission.DELETE_QUIZZES -> stringResource(R.string.admin_permission_delete_quizzes)
    AdminPermission.PUBLISH_QUIZZES -> stringResource(R.string.admin_permission_publish_quizzes)
    AdminPermission.VIEW_REPORTS -> stringResource(R.string.admin_permission_view_reports)
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val previewUsers = listOf(
    User(
        id = "0",
        email = "superuser@example.com",
        displayName = "Sieu Quan Tri",
        role = UserRole.SUPERUSER
    ),
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
                uiState = AdminUserManagementUiState(
                    isLoading = false,
                    isSuperuser = true,
                    currentPermissions = AdminPermission.entries.toSet(),
                    currentUserId = "0"
                ),
                users = previewUsers,
                searchQuery = "",
                onSearchQueryChanged = {},
                onRoleChange = { _, _ -> },
                onBanToggle = {},
                onDelete = {},
                onLoadMore = {},
                onRoleFilterChanged = {},
                onSortFieldChanged = {},
                onToggleSortOrder = {},
                onClearFilters = {},
                onManagePermissions = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "User Management Content - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun UserManagementContentDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UserManagementContent(
                uiState = AdminUserManagementUiState(
                    isLoading = false,
                    roleFilter = UserRoleFilter.ADMIN,
                    sortField = UserSortField.ROLE,
                    isSuperuser = true,
                    currentPermissions = AdminPermission.entries.toSet(),
                    currentUserId = "0"
                ),
                users = previewUsers.filter { it.role == UserRole.ADMIN },
                searchQuery = "",
                onSearchQueryChanged = {},
                onRoleChange = { _, _ -> },
                onBanToggle = {},
                onDelete = {},
                onLoadMore = {},
                onRoleFilterChanged = {},
                onSortFieldChanged = {},
                onToggleSortOrder = {},
                onClearFilters = {},
                onManagePermissions = {},
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
                onRoleFilterChanged = {},
                onSortFieldChanged = {},
                onToggleSortOrder = {},
                onClearFilters = {},
                onManagePermissions = {},
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

@Preview(
    showBackground = true,
    name = "Pill Search Bar - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
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

@Preview(showBackground = true, name = "Permission Edit Dialog - Light")
@Composable
private fun PermissionEditDialogPreview() {
    QuizzezTheme(darkTheme = false) {
        PermissionEditDialog(
            user = User(
                id = "1",
                email = "admin@example.com",
                displayName = "Quan Tri Vien",
                role = UserRole.ADMIN,
                permissions = setOf(
                    AdminPermission.MANAGE_USERS,
                    AdminPermission.BAN_USERS
                )
            ),
            onSave = {},
            onDismiss = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Permission Edit Dialog - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PermissionEditDialogDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        PermissionEditDialog(
            user = User(
                id = "2",
                email = "admin2@example.com",
                displayName = "Quan Tri Hai",
                role = UserRole.ADMIN,
                permissions = setOf(
                    AdminPermission.MANAGE_QUIZZES,
                    AdminPermission.VIEW_REPORTS
                )
            ),
            onSave = {},
            onDismiss = {}
        )
    }
}
