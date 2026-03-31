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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Edit Profile screen that allows the current user to update their display name
 * and avatar image via URL.
 *
 * Users can either paste an image URL directly or use the "Random Avatar" button
 * to fetch a random anime/artwork image from the Wallhaven API.
 * When the save operation completes successfully, the composable navigates back
 * via [onNavigateBack].
 *
 * @param onNavigateBack Callback invoked when the user presses the back button
 *   or after a successful save.
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: EditProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditProfileViewModel(
                    authRepository = container.authRepository
                ) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate back once the save completes successfully
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    // Show error in a Snackbar and then clear it from state
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(EditProfileEvent.ClearError)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_edit_title),
                        style = MaterialTheme.typography.titleLarge
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
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(EditProfileEvent.SaveProfile) },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.save),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        EditProfileContent(
            uiState = uiState,
            onDisplayNameChanged = { viewModel.onEvent(EditProfileEvent.DisplayNameChanged(it)) },
            onAvatarUrlChanged = { viewModel.onEvent(EditProfileEvent.AvatarUrlChanged(it)) },
            onFetchRandomAvatar = { viewModel.onEvent(EditProfileEvent.FetchRandomAvatar) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/**
 * Stateless content area for the Edit Profile screen.
 *
 * Displays the avatar preview, a text field for the avatar URL, a "Random Avatar"
 * button, a [TextInputField] for the display name, and a read-only email field.
 *
 * @param uiState The current [EditProfileUiState] to render.
 * @param onDisplayNameChanged Callback for display-name text changes.
 * @param onAvatarUrlChanged Callback for avatar URL text changes.
 * @param onFetchRandomAvatar Callback invoked when the user taps the random avatar button.
 * @param modifier Modifier for the root layout.
 */
@Composable
private fun EditProfileContent(
    uiState: EditProfileUiState,
    onDisplayNameChanged: (String) -> Unit,
    onAvatarUrlChanged: (String) -> Unit,
    onFetchRandomAvatar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Avatar preview
        AvatarPreview(
            photoUrl = uiState.photoUrl,
            initial = uiState.displayName.firstOrNull()?.toString()
                ?: uiState.email.firstOrNull()?.toString()
                ?: "?",
            isLoading = uiState.isLoadingAvatar
        )

        // Random Avatar button
        Button(
            onClick = onFetchRandomAvatar,
            enabled = !uiState.isLoading && !uiState.isLoadingAvatar,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.profile_edit_random_avatar))
        }

        // Avatar URL input
        TextInputField(
            value = uiState.photoUrl ?: "",
            onValueChange = onAvatarUrlChanged,
            label = stringResource(R.string.profile_edit_avatar_url),
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Display name input
        TextInputField(
            value = uiState.displayName,
            onValueChange = onDisplayNameChanged,
            label = stringResource(R.string.profile_edit_display_name),
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        // Read-only email field
        TextInputField(
            value = uiState.email,
            onValueChange = {},
            label = stringResource(R.string.profile_edit_email),
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Circular avatar composable that renders either a remote image (via Coil's
 * [AsyncImage]) or a coloured circle containing the user's [initial].
 *
 * A [CircularProgressIndicator] covers the avatar while a random avatar fetch
 * is in progress.
 *
 * @param photoUrl Optional remote URL for the avatar image.
 * @param initial Fallback character shown when no image is available.
 * @param isLoading Whether a random avatar fetch is currently running.
 * @param modifier Modifier for styling.
 */
@Composable
private fun AvatarPreview(
    photoUrl: String?,
    initial: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Loading overlay for random avatar fetch
        if (isLoading) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(
    name = "Edit Profile Content – Light",
    showBackground = true
)
@Composable
private fun EditProfileContentLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState(
                    displayName = "Nguyen Van A",
                    email = "nguyenvana@example.com",
                    photoUrl = null
                ),
                onDisplayNameChanged = {},
                onAvatarUrlChanged = {},
                onFetchRandomAvatar = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Edit Profile Content – Dark",
    showBackground = true
)
@Composable
private fun EditProfileContentDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState(
                    displayName = "Nguyen Van A",
                    email = "nguyenvana@example.com",
                    photoUrl = null
                ),
                onDisplayNameChanged = {},
                onAvatarUrlChanged = {},
                onFetchRandomAvatar = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Edit Profile Content – Loading Avatar",
    showBackground = true
)
@Composable
private fun EditProfileContentLoadingPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState(
                    displayName = "Nguyen Van A",
                    email = "nguyenvana@example.com",
                    isLoadingAvatar = true
                ),
                onDisplayNameChanged = {},
                onAvatarUrlChanged = {},
                onFetchRandomAvatar = {}
            )
        }
    }
}
