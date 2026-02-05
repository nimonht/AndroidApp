package com.example.androidapp.ui.screens.trash

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.navigation.AppTopBar

/**
 * Trash/Recycle Bin screen showing deleted quizzes.
 * Allows restore or permanent deletion.
 *
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
 */
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.trash_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        EmptyState(
            message = stringResource(R.string.trash_empty),
            icon = Icons.Default.Delete,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        )
    }
}
