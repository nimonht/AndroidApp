package com.example.androidapp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.forms.SwitchToggle
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Settings screen with grouped options and toggles.
 *
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var autoSyncEnabled by remember { mutableStateOf(true) }

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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Notifications Section
            SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                SwitchToggle(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    label = stringResource(R.string.settings_push_notifications),
                    description = stringResource(R.string.settings_push_notifications_desc)
                )
            }

            // Appearance Section
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                SwitchToggle(
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it },
                    label = stringResource(R.string.settings_dark_mode),
                    description = stringResource(R.string.settings_dark_mode_desc)
                )
            }

            // Data Section
            SettingsSection(title = stringResource(R.string.settings_section_data_sync)) {
                SwitchToggle(
                    checked = autoSyncEnabled,
                    onCheckedChange = { autoSyncEnabled = it },
                    label = stringResource(R.string.settings_auto_sync),
                    description = stringResource(R.string.settings_auto_sync_desc)
                )
            }
        }
    }
}

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
