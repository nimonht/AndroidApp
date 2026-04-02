package com.example.androidapp.ui.components.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.androidapp.R
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * User card component for admin user management screen.
 *
 * @param user The user to display.
 * @param onRoleChange Callback when role change is requested.
 * @param onBanToggle Callback when ban/unban is requested.
 * @param onDelete Callback when delete is requested.
 * @param modifier Modifier for styling.
 */
@Composable
fun AdminUserCard(
    user: User,
    onRoleChange: (UserRole) -> Unit,
    onBanToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (user.isBanned) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Avatar + Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
                if (!user.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Name + Email + Role
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = user.displayName.ifBlank { "Không có tên" },
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = user.email,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Role badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val roleColor = when (user.role) {
                            UserRole.ADMIN -> MaterialTheme.colorScheme.error
                            UserRole.USER -> MaterialTheme.colorScheme.primary
                            UserRole.GUEST -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        val roleText = when (user.role) {
                            UserRole.ADMIN -> stringResource(R.string.admin_role_admin)
                            UserRole.USER -> stringResource(R.string.admin_role_user)
                            UserRole.GUEST -> stringResource(R.string.admin_role_guest)
                        }

                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = roleColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = roleText,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = roleColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Banned badge
                        if (user.isBanned) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.admin_user_banned),
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right: Actions menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.admin_user_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Role change options
                    if (user.role != UserRole.ADMIN) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_promote_admin)) },
                            onClick = {
                                onRoleChange(UserRole.ADMIN)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            }
                        )
                    }

                    if (user.role != UserRole.USER) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_demote_user)) },
                            onClick = {
                                onRoleChange(UserRole.USER)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        )
                    }

                    HorizontalDivider()

                    // Ban/Unban
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (user.isBanned) stringResource(R.string.admin_unban_user)
                                else stringResource(R.string.admin_ban_user)
                            )
                        },
                        onClick = {
                            onBanToggle()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (user.isBanned) Icons.Default.CheckCircle else Icons.Default.Block,
                                contentDescription = null
                            )
                        }
                    )

                    HorizontalDivider()

                    // Delete
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.admin_delete_user), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminUserCardPreview() {
    QuizzezTheme {
        AdminUserCard(
            user = User(
                id = "user1",
                email = "user@example.com",
                displayName = "Nguyễn Văn A",
                role = UserRole.USER
            ),
            onRoleChange = {},
            onBanToggle = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
