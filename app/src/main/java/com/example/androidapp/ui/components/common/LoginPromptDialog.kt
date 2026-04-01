package com.example.androidapp.ui.components.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R

/**
 * Dialog prompting the user to log in when they attempt a restricted action as a guest.
 *
 * @param onLogin Called when the user chooses to log in.
 * @param onDismiss Called when the dialog is dismissed.
 * @param modifier Modifier for styling.
 */
@Composable
fun LoginPromptDialog(
    onLogin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.login_prompt_title)) },
        text = { Text(stringResource(R.string.login_prompt_message)) },
        confirmButton = {
            TextButton(onClick = onLogin) {
                Text(stringResource(R.string.login))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
