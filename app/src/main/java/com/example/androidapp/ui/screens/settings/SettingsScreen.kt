package com.example.androidapp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.ui.components.forms.SwitchToggle
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Settings screen with grouped options and toggles.
 * Stateless composable; all state is owned by [SettingsViewModel].
 *
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

            // Data Section
            SettingsSection(title = stringResource(R.string.settings_section_data_sync)) {
                SwitchToggle(
                    checked = uiState.autoSyncEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.AutoSyncToggled(it)) },
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
