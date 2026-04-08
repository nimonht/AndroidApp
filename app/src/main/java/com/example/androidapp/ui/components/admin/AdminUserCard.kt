package com.example.androidapp.ui.components.admin

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.androidapp.R
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Redesigned user card for admin user management.
 *
 * Shows a circular avatar with a role-colored border ring, user info with
 * an inline role badge (pill shape), an optional banned badge, and a
 * three-dot overflow menu for admin actions.
 *
 * Action menu items are conditionally displayed based on the current user's
 * permissions ([currentPermissions]) and superuser status
 * ([currentUserIsSuperuser]). If the target user is a SUPERUSER, no action
 * menu is rendered at all. The same applies when the card represents the
 * currently logged-in user ([isCurrentUser]).
 *
 * @param user The user to display.
 * @param onRoleChange Callback when a role change is requested.
 * @param onBanToggle Callback when ban/unban is requested.
 * @param onDelete Callback when delete is requested.
 * @param modifier Modifier for external styling.
 * @param currentUserIsSuperuser Whether the logged-in admin holds SUPERUSER role.
 * @param currentPermissions Effective permissions of the logged-in admin.
 * @param isCurrentUser Whether this card represents the logged-in user.
 * @param onManagePermissions Optional callback to open the permission editor
 *   (only shown when the current user is a superuser and the target is ADMIN).
 */
@Composable
fun AdminUserCard(
    user: User,
    onRoleChange: (UserRole) -> Unit,
    onBanToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    currentUserIsSuperuser: Boolean = false,
    currentPermissions: Set<AdminPermission> = emptySet(),
    isCurrentUser: Boolean = false,
    onManagePermissions: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    // Gold / amber tint for superuser
    val superuserColor = Color(0xFFD4A017)

    val roleColor = when (user.role) {
        UserRole.SUPERUSER -> superuserColor
        UserRole.ADMIN -> MaterialTheme.colorScheme.error
        UserRole.USER -> MaterialTheme.colorScheme.primary
        UserRole.GUEST -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val avatarBorderColor = when (user.role) {
        UserRole.SUPERUSER -> superuserColor
        UserRole.ADMIN -> MaterialTheme.colorScheme.error
        UserRole.USER -> MaterialTheme.colorScheme.primary
        UserRole.GUEST -> MaterialTheme.colorScheme.outline
    }

    val avatarBackgroundColor = when (user.role) {
        UserRole.SUPERUSER -> superuserColor.copy(alpha = 0.15f)
        UserRole.ADMIN -> MaterialTheme.colorScheme.errorContainer
        UserRole.USER -> MaterialTheme.colorScheme.primaryContainer
        UserRole.GUEST -> MaterialTheme.colorScheme.surfaceVariant
    }

    val avatarTextColor = when (user.role) {
        UserRole.SUPERUSER -> superuserColor
        UserRole.ADMIN -> MaterialTheme.colorScheme.onErrorContainer
        UserRole.USER -> MaterialTheme.colorScheme.onPrimaryContainer
        UserRole.GUEST -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Whether to show the overflow menu at all:
    // - Never for SUPERUSER targets (protected account)
    // - Never for the currently logged-in user (no self-actions)
    val showActions = !isCurrentUser && user.role != UserRole.SUPERUSER

    // Individual permission checks (superuser bypasses all)
    val canChangeRole = currentUserIsSuperuser ||
            currentPermissions.contains(AdminPermission.CHANGE_USER_ROLES)
    val canBan = currentUserIsSuperuser ||
            currentPermissions.contains(AdminPermission.BAN_USERS)
    val canDeleteUser = currentUserIsSuperuser ||
            currentPermissions.contains(AdminPermission.DELETE_USERS)
    val canManagePermissions = currentUserIsSuperuser &&
            user.role == UserRole.ADMIN &&
            onManagePermissions != null

    // If the user has zero available actions, hide the menu entirely
    val hasAnyAction = canChangeRole || canBan || canDeleteUser || canManagePermissions

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // -- Avatar --
            UserAvatar(
                photoUrl = user.photoUrl,
                displayName = user.displayName,
                borderColor = avatarBorderColor,
                backgroundColor = avatarBackgroundColor,
                textColor = avatarTextColor
            )

            Spacer(modifier = Modifier.width(14.dp))

            // -- Info column --
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Name row with inline role badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.displayName.ifBlank { user.email },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    RoleBadge(role = user.role, roleColor = roleColor)
                }

                // Email
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Banned badge
                if (user.isBanned) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = stringResource(R.string.admin_user_banned),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Superuser protected hint
                if (user.role == UserRole.SUPERUSER) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = superuserColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = superuserColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.admin_superuser_protected),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = superuserColor
                            )
                        }
                    }
                }
            }

            // -- Overflow menu --
            if (showActions && hasAnyAction) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.admin_user_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    UserActionsMenu(
                        expanded = showMenu,
                        user = user,
                        onDismiss = { showMenu = false },
                        onRoleChange = { role ->
                            onRoleChange(role)
                            showMenu = false
                        },
                        onBanToggle = {
                            onBanToggle()
                            showMenu = false
                        },
                        onDelete = {
                            onDelete()
                            showMenu = false
                        },
                        canChangeRole = canChangeRole,
                        canBan = canBan,
                        canDelete = canDeleteUser,
                        canManagePermissions = canManagePermissions,
                        onManagePermissions = if (canManagePermissions) {
                            {
                                onManagePermissions?.invoke()
                                showMenu = false
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private helper composables
// ---------------------------------------------------------------------------

/**
 * Circular avatar with a colored border ring.
 * Shows the user photo when available; otherwise displays the first initial.
 */
@Composable
private fun UserAvatar(
    photoUrl: String?,
    displayName: String,
    borderColor: Color,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"

    if (!photoUrl.isNullOrEmpty()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(56.dp)
                .border(width = 2.dp, color = borderColor, shape = CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(56.dp)
                .border(width = 2.dp, color = borderColor, shape = CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

/**
 * Small pill-shaped badge that displays the user role.
 */
@Composable
private fun RoleBadge(
    role: UserRole,
    roleColor: Color,
    modifier: Modifier = Modifier
) {
    val superuserColor = Color(0xFFD4A017)

    val roleText = when (role) {
        UserRole.SUPERUSER -> stringResource(R.string.admin_user_role_superuser)
        UserRole.ADMIN -> stringResource(R.string.admin_role_admin)
        UserRole.USER -> stringResource(R.string.admin_role_user)
        UserRole.GUEST -> stringResource(R.string.admin_role_guest)
    }

    val badgeBackground = when (role) {
        UserRole.SUPERUSER -> superuserColor.copy(alpha = 0.15f)
        UserRole.ADMIN -> roleColor.copy(alpha = 0.15f)
        UserRole.USER -> roleColor.copy(alpha = 0.15f)
        UserRole.GUEST -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = badgeBackground,
        modifier = modifier
    ) {
        Text(
            text = roleText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = roleColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Dropdown menu with promote / demote / ban / unban / delete / manage-permissions
 * actions. Each item is conditionally shown based on the caller's permission flags.
 */
@Composable
private fun UserActionsMenu(
    expanded: Boolean,
    user: User,
    onDismiss: () -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onBanToggle: () -> Unit,
    onDelete: () -> Unit,
    canChangeRole: Boolean,
    canBan: Boolean,
    canDelete: Boolean,
    canManagePermissions: Boolean,
    onManagePermissions: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        // -- Role change items --
        if (canChangeRole) {
            // Promote to admin (only if not already admin)
            if (user.role != UserRole.ADMIN) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.admin_promote_admin)
                        )
                    },
                    onClick = { onRoleChange(UserRole.ADMIN) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null
                        )
                    }
                )
            }

            // Demote to user (only if not already a regular user)
            if (user.role != UserRole.USER) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.admin_demote_user)
                        )
                    },
                    onClick = { onRoleChange(UserRole.USER) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        // -- Manage permissions (superuser -> admin target only) --
        if (canManagePermissions && onManagePermissions != null) {
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(R.string.admin_permissions_edit))
                },
                onClick = onManagePermissions,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null
                    )
                }
            )
        }

        // Divider between role actions and destructive actions
        if (canChangeRole || canManagePermissions) {
            HorizontalDivider()
        }

        // -- Ban / Unban toggle --
        if (canBan) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (user.isBanned) {
                            stringResource(R.string.admin_unban_user)
                        } else {
                            stringResource(R.string.admin_ban_user)
                        }
                    )
                },
                onClick = onBanToggle,
                leadingIcon = {
                    Icon(
                        imageVector = if (user.isBanned) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Block
                        },
                        contentDescription = null
                    )
                }
            )
        }

        if (canBan && canDelete) {
            HorizontalDivider()
        }

        // -- Delete (destructive) --
        if (canDelete) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.admin_delete_user),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = onDelete,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Admin User Card - Light")
@Composable
private fun AdminUserCardLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminUserCard(
                user = User(
                    id = "0",
                    email = "superuser@example.com",
                    displayName = "Sieu Quan Tri",
                    role = UserRole.SUPERUSER
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = true,
                currentPermissions = AdminPermission.entries.toSet()
            )
            AdminUserCard(
                user = User(
                    id = "1",
                    email = "admin@example.com",
                    displayName = "Quan Tri Vien",
                    role = UserRole.ADMIN
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = true,
                currentPermissions = AdminPermission.entries.toSet(),
                onManagePermissions = {}
            )
            AdminUserCard(
                user = User(
                    id = "2",
                    email = "user@example.com",
                    displayName = "Nguyen Van A",
                    photoUrl = "https://example.com/avatar.jpg",
                    role = UserRole.USER
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = false,
                currentPermissions = setOf(AdminPermission.BAN_USERS)
            )
            AdminUserCard(
                user = User(
                    id = "3",
                    email = "banned@example.com",
                    displayName = "Nguoi Dung Bi Cam",
                    role = UserRole.USER,
                    isBanned = true
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = false,
                currentPermissions = setOf(
                    AdminPermission.BAN_USERS,
                    AdminPermission.DELETE_USERS
                )
            )
            AdminUserCard(
                user = User(
                    id = "4",
                    email = "guest@example.com",
                    displayName = "Khach",
                    role = UserRole.GUEST
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = false,
                currentPermissions = emptySet()
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Admin User Card - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdminUserCardDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminUserCard(
                user = User(
                    id = "0",
                    email = "superuser@example.com",
                    displayName = "Sieu Quan Tri",
                    role = UserRole.SUPERUSER
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = false,
                currentPermissions = emptySet()
            )
            AdminUserCard(
                user = User(
                    id = "1",
                    email = "admin@example.com",
                    displayName = "Quan Tri Vien",
                    role = UserRole.ADMIN
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = true,
                currentPermissions = AdminPermission.entries.toSet(),
                onManagePermissions = {}
            )
            AdminUserCard(
                user = User(
                    id = "2",
                    email = "user@example.com",
                    displayName = "Nguyen Van A",
                    photoUrl = "https://example.com/avatar.jpg",
                    role = UserRole.USER
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = true,
                currentPermissions = AdminPermission.entries.toSet()
            )
            AdminUserCard(
                user = User(
                    id = "3",
                    email = "banned@example.com",
                    displayName = "Nguoi Dung Bi Cam",
                    role = UserRole.USER,
                    isBanned = true
                ),
                onRoleChange = {},
                onBanToggle = {},
                onDelete = {},
                currentUserIsSuperuser = false,
                currentPermissions = setOf(AdminPermission.BAN_USERS)
            )
        }
    }
}
