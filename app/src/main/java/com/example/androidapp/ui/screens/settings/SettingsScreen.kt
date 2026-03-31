package com.example.androidapp.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.data.preferences.SettingsPreferences
import com.example.androidapp.ui.components.forms.SwitchToggle
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Settings screen with grouped options: data/sync toggles, appearance,
 * and account management (including delete account).
 *
 * Stateless composable; all state is owned by [SettingsViewModel].
 *
 * @param viewModel The [SettingsViewModel] that owns screen state.
 * @param onNavigateBack Callback to navigate back.
 * @param onAccountDeleted Callback invoked after successful account deletion (navigate to login).
 * @param modifier Modifier for styling.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away after account deletion
    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) {
            onAccountDeleted()
        }
    }

    // Delete account confirmation dialog
    if (uiState.showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SettingsEvent.DeleteAccountDismissed) },
            title = { Text(stringResource(R.string.settings_delete_account_title)) },
            text = { Text(stringResource(R.string.settings_delete_account_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(SettingsEvent.DeleteAccountConfirmed) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.settings_delete_account_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(SettingsEvent.DeleteAccountDismissed) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete error snackbar dialog
    if (uiState.deleteError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SettingsEvent.ClearDeleteError) },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(uiState.deleteError ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(SettingsEvent.ClearDeleteError) }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ---- Data & Sync Section ----
                SettingsSection(title = stringResource(R.string.settings_section_data_sync)) {
                    SwitchToggle(
                        checked = uiState.autoSyncEnabled,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.AutoSyncToggled(it)) },
                        label = stringResource(R.string.settings_auto_sync),
                        description = stringResource(R.string.settings_auto_sync_desc)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SwitchToggle(
                        checked = uiState.wifiOnlySync,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.WifiOnlySyncToggled(it)) },
                        label = stringResource(R.string.settings_wifi_only_sync),
                        description = stringResource(R.string.settings_wifi_only_sync_desc),
                        enabled = uiState.autoSyncEnabled
                    )
                }

                // ---- Appearance Section ----
                SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                    ThemeModeSelector(
                        selectedMode = uiState.darkThemeMode,
                        onModeSelected = { mode ->
                            viewModel.onEvent(SettingsEvent.DarkThemeModeChanged(mode))
                        }
                    )
                }

                // ---- Account Section ----
                if (uiState.isLoggedIn) {
                    SettingsSection(title = stringResource(R.string.settings_section_account)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !uiState.isDeleting) {
                                    viewModel.onEvent(SettingsEvent.DeleteAccountRequested)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_delete_account),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = stringResource(R.string.settings_delete_account_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Loading overlay during account deletion
            if (uiState.isDeleting) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.settings_deleting_account),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A labeled section card for grouping related settings.
 */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                content = content
            )
        }
    }
}

/**
 * Three-option theme mode selector using segmented-style chips.
 *
 * @param selectedMode Current theme mode constant from [SettingsPreferences].
 * @param onModeSelected Callback when a mode is selected.
 * @param modifier Modifier for layout customization.
 */
@Composable
private fun ThemeModeSelector(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_dark_theme),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeModeChip(
                label = stringResource(R.string.settings_theme_system),
                selected = selectedMode == SettingsPreferences.THEME_MODE_SYSTEM,
                onClick = { onModeSelected(SettingsPreferences.THEME_MODE_SYSTEM) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeChip(
                label = stringResource(R.string.settings_theme_light),
                selected = selectedMode == SettingsPreferences.THEME_MODE_LIGHT,
                onClick = { onModeSelected(SettingsPreferences.THEME_MODE_LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeChip(
                label = stringResource(R.string.settings_theme_dark),
                selected = selectedMode == SettingsPreferences.THEME_MODE_DARK,
                onClick = { onModeSelected(SettingsPreferences.THEME_MODE_DARK) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A single selectable chip for theme mode selection.
 */
@Composable
private fun ThemeModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    // Preview only -- ViewModel is not available here.
    QuizzezTheme {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = "Cai dat",
                    canNavigateBack = true,
                    navigateUp = {}
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Preview placeholder", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
