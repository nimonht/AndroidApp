package com.example.androidapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer



/**
 * Profile screen showing user information and settings menu.
 * Stateless composable; all state is owned by [ProfileViewModel].
 */
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(container.authRepository) as T
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        if (uiState.isLoggedIn && uiState.user != null) {

            val user = uiState.user!!

            val avatarInitial =
                user.displayName.firstOrNull()?.toString()
                    ?: user.email.firstOrNull()?.toString()
                    ?: "?"

            UserProfileHeader(
                displayName = user.displayName,
                email = user.email,
                avatarInitial = avatarInitial
            )

            // ===== STATS DISPLAY =====
            ProfileStats(
                quizCount = 0,
                attemptCount = 0,
                score = 0
            )

            ProfileMenuSection(
                onHistoryClick = onNavigateToHistory,
                onTrashClick = onNavigateToTrash,
                onSettingsClick = onNavigateToSettings
            )

            Button(
                onClick = { viewModel.onEvent(ProfileEvent.Logout) },
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

            GuestPrompt(
                onLoginClick = onNavigateToLogin
            )
        }
    }
}


@Composable
private fun UserProfileHeader(
    displayName: String,
    email: String,
    avatarInitial: String,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = avatarInitial,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {

            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/**
 * ===== USER STATS =====
 */
@Composable
private fun ProfileStats(
    quizCount: Int,
    attemptCount: Int,
    score: Int,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        StatItem(
            value = quizCount.toString(),
            label = stringResource(R.string.profile_stat_quiz)
        )

        StatItem(
            value = attemptCount.toString(),
            label = stringResource(R.string.profile_stat_attempts)
        )

        StatItem(
            value = score.toString(),
            label = stringResource(R.string.profile_stat_score)
        )
    }
}


@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.profile_guest_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLoginClick
        ) {

            Text(
                stringResource(R.string.login)
            )
        }
    }
}
