package com.example.androidapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R

/**
 * Profile screen showing user information and settings menu.
 * Shows login prompt for guests.
 *
 * @param onNavigateToLogin Callback to navigate to login screen.
 * @param onNavigateToSettings Callback to navigate to settings screen.
 * @param onNavigateToHistory Callback to navigate to attempt history.
 * @param onNavigateToTrash Callback to navigate to recycle bin.
 * @param modifier Modifier for styling.
 */
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTrash: () -> Unit,
    isLoggedIn: Boolean,
    displayName: String?,
    email: String?,
    avatarInitial: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (isLoggedIn) {
            // Logged in user view
            UserProfileHeader(
                displayName = displayName,
                email = email,
                avatarInitial = avatarInitial
            )

            // Menu items
            ProfileMenuSection(
                onHistoryClick = onNavigateToHistory,
                onTrashClick = onNavigateToTrash,
                onSettingsClick = onNavigateToSettings
            )

            // Logout button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.logout))
            }
        } else {
            // Guest view
            GuestPrompt(onLoginClick = onNavigateToLogin)
        }
    }
}

@Composable
private fun UserProfileHeader(
    displayName: String?,
    email: String?,
    avatarInitial: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarInitial ?: "",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = displayName.orEmpty(),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = email.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileMenuSection(
    onHistoryClick: () -> Unit,
    onTrashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_section_general),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ProfileMenuItem(
            icon = Icons.Default.History,
            title = stringResource(R.string.profile_menu_attempt_history),
            onClick = onHistoryClick
        )
        ProfileMenuItem(
            icon = Icons.Default.Delete,
            title = stringResource(R.string.profile_menu_recycle_bin),
            onClick = onTrashClick
        )
        ProfileMenuItem(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.profile_menu_settings),
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuestPrompt(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.profile_guest_prompt),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLoginClick) {
            Text(stringResource(R.string.login))
        }
    }
}
