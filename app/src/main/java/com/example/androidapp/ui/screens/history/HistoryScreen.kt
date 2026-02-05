package com.example.androidapp.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * History screen showing past quiz attempts.
 *
 * @param onNavigateBack Callback to navigate back.
 * @param onAttemptClick Callback when an attempt is clicked.
 * @param modifier Modifier for styling.
 */
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onAttemptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.history_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        EmptyState(
            message = stringResource(R.string.history_empty),
            actionLabel = stringResource(R.string.history_action_explore),
            onActionClick = onNavigateBack,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        )
    }
}
