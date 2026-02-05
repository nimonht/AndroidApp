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
                title = "Settings",
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
            SettingsSection(title = "Notifications") {
                SwitchToggle(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    label = "Push Notifications",
                    description = "Receive updates about quiz results and new features"
                )
            }

            // Appearance Section
            SettingsSection(title = "Appearance") {
                SwitchToggle(
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it },
                    label = "Dark Mode",
                    description = "Use dark theme throughout the app"
                )
            }

            // Data Section
            SettingsSection(title = "Data & Sync") {
                SwitchToggle(
                    checked = autoSyncEnabled,
                    onCheckedChange = { autoSyncEnabled = it },
                    label = "Auto Sync",
                    description = "Automatically sync quizzes to cloud when connected"
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
