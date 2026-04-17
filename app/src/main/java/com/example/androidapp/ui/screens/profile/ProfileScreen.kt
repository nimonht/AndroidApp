package com.example.androidapp.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Profile screen showing user information, activity stats, and a settings menu.
 * Stateless composable — all state is owned by [ProfileViewModel].
 *
 * @param onNavigateToLogin Callback to navigate to the login screen.
 * @param onNavigateToSettings Callback to navigate to the settings screen.
 * @param onNavigateToHistory Callback to navigate to attempt history.
 * @param onNavigateToTrash Callback to navigate to the recycle bin.
 * @param onNavigateToEditProfile Callback to navigate to the edit-profile screen.
 * @param onNavigateToQuestionPool Callback to navigate to the question pool.
 * @param onNavigateToAdminPanel Callback to navigate to the admin panel.
 * @param modifier Modifier for styling.
 */
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToQuestionPool: () -> Unit = {},
    onNavigateToAdminPanel: () -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(
                    authRepository = container.authRepository,
                    quizRepository = container.quizRepository,
                    attemptRepository = container.attemptRepository
                ) as T
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
            val avatarInitial = user.displayName.firstOrNull()?.toString()
                ?: user.email.firstOrNull()?.toString() ?: "?"

            UserProfileHeader(
                displayName = user.displayName,
                email = user.email,
                avatarInitial = avatarInitial,
                photoUrl = user.photoUrl,
                quizCount = uiState.quizCount,
                attemptCount = uiState.attemptCount,
                onEditClick = onNavigateToEditProfile
            )

            ProfileMenuSection(
                onHistoryClick = onNavigateToHistory,
                onTrashClick = onNavigateToTrash,
                onSettingsClick = onNavigateToSettings,
                onQuestionPoolClick = onNavigateToQuestionPool,
                onAdminPanelClick = onNavigateToAdminPanel,
                onAdvancedClick = onNavigateToAdvanced,
                isAdmin = user.isAdmin()
            )

            Button(
                onClick = { viewModel.onEvent(ProfileEvent.Logout) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.logout))
                }
            }
        } else {
            GuestPrompt(onLoginClick = onNavigateToLogin)
        }
    }
}

// ---------------------------------------------------------------------------
// Internal composables
// ---------------------------------------------------------------------------

/**
 * Profile header card showing the user's avatar, name, email, activity stats, and an edit button.
 *
 * @param displayName The user's display name.
 * @param email The user's email address.
 * @param avatarInitial Single character used as a fallback avatar.
 * @param photoUrl Optional remote URL for the user's avatar image.
 * @param quizCount Number of quizzes created by the user.
 * @param attemptCount Number of quiz attempts made by the user.
 * @param onEditClick Callback invoked when the edit icon is tapped.
 * @param modifier Modifier for styling.
 */
@Composable
private fun UserProfileHeader(
    displayName: String,
    email: String,
    avatarInitial: String,
    photoUrl: String?,
    quizCount: Int,
    attemptCount: Int,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AvatarImage(
                photoUrl = photoUrl,
                initial = avatarInitial,
                size = 80
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Name + email
            Column(modifier = Modifier.weight(1f)) {
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

            // Edit icon button
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.profile_edit_button_cd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Text(
            text = stringResource(R.string.profile_stats_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(label = stringResource(R.string.profile_stat_quizzes, quizCount))
            StatChip(label = stringResource(R.string.profile_stat_attempts, attemptCount))
        }
    }
}

/**
 * A small pill-shaped chip used to display a single statistic.
 *
 * @param label The text to display inside the chip.
 * @param modifier Modifier for styling.
 */
@Composable
private fun StatChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Circular avatar that shows a remote image when [photoUrl] is available,
 * or falls back to a coloured circle with the user's [initial].
 *
 * Coil's [coil.compose.AsyncImage] is used for remote image loading so it is
 * only imported when needed; when [photoUrl] is null we render the initials
 * fallback without loading any image at all.
 *
 * @param photoUrl Optional remote image URL.
 * @param initial Single character to display as fallback.
 * @param size Diameter in dp.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun AvatarImage(
    photoUrl: String?,
    initial: String,
    size: Int,
    modifier: Modifier = Modifier
) {
    val sizeDp = size.dp
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(sizeDp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = if (size >= 64) MaterialTheme.typography.headlineLarge
                else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ProfileMenuSection(
    onHistoryClick: () -> Unit,
    onTrashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onQuestionPoolClick: () -> Unit,
    onAdminPanelClick: () -> Unit,
    onAdvancedClick: () -> Unit,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Admin section (only for admins)
        if (isAdmin) {
            Text(
                text = stringResource(R.string.profile_section_admin),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            ProfileMenuItem(
                icon = Icons.Default.AdminPanelSettings,
                title = stringResource(R.string.admin_dashboard_title),
                onClick = onAdminPanelClick
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

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
            icon = Icons.Default.QuestionAnswer,
            title = stringResource(R.string.profile_menu_question_pool),
            onClick = onQuestionPoolClick
        )
        ProfileMenuItem(
            icon = Icons.Default.Delete,
            title = stringResource(R.string.profile_menu_recycle_bin),
            onClick = onTrashClick
        )
        ProfileMenuItem(
            icon = Icons.Default.Code,
            title = stringResource(R.string.profile_menu_developer_tools),
            onClick = onAdvancedClick
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
        Button(onClick = onLoginClick) {
            Text(stringResource(R.string.login))
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Profile Header – Light", showBackground = true)
@Composable
private fun UserProfileHeaderLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface {
            UserProfileHeader(
                displayName = "Nguyen Van A",
                email = "nguyenvana@example.com",
                avatarInitial = "N",
                photoUrl = null,
                quizCount = 12,
                attemptCount = 47,
                onEditClick = {}
            )
        }
    }
}

@Preview(name = "Profile Header – Dark", showBackground = true)
@Composable
private fun UserProfileHeaderDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface {
            UserProfileHeader(
                displayName = "Nguyen Van A",
                email = "nguyenvana@example.com",
                avatarInitial = "N",
                photoUrl = null,
                quizCount = 12,
                attemptCount = 47,
                onEditClick = {}
            )
        }
    }
}

@Preview(name = "Stat Chip – Light", showBackground = true)
@Composable
private fun StatChipLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface { StatChip(label = "12 Quizz") }
    }
}

@Preview(name = "Stat Chip – Dark", showBackground = true)
@Composable
private fun StatChipDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface { StatChip(label = "47 lượt làm") }
    }
}
