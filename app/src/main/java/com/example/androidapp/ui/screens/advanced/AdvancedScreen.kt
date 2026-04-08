package com.example.androidapp.ui.screens.advanced

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.ui.screens.advanced.console.ConsoleScreen
import com.example.androidapp.ui.screens.advanced.logviewer.LogViewerScreen
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Container screen for the Advanced Developer Tools feature.
 *
 * Presents a [TabRow] with two tabs:
 * - **Console** (index 0): Interactive command-line interface.
 * - **Logs** (index 1): Real-time log viewer with filtering.
 *
 * Tab state is owned by [AdvancedViewModel].
 *
 * @param onNavigateBack Callback invoked when the user taps the back button.
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AdvancedViewModel = viewModel()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    // TODO: move to strings.xml
    val tabTitles = listOf("Console", "Nhat ky")

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    // TODO: move to strings.xml
                    Text(text = "Cong cu nang cao")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            // TODO: move to strings.xml
                            contentDescription = "Quay lai"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> ConsoleScreen(modifier = Modifier.fillMaxSize())
                1 -> LogViewerScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(
    name = "AdvancedScreen - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun AdvancedScreenLightPreview() {
    QuizzezTheme(darkTheme = false) {
        AdvancedScreen(onNavigateBack = {})
    }
}

@Preview(
    name = "AdvancedScreen - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdvancedScreenDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        AdvancedScreen(onNavigateBack = {})
    }
}
