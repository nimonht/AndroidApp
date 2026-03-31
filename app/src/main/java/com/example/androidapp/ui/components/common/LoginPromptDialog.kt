package com.example.androidapp.ui.components.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidapp.R
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Material3 AlertDialog prompting guest users to log in or register.
 * Shows a message explaining the restricted action requires an account.
 *
 * @param onDismiss Callback when the dialog is dismissed.
 * @param onLoginClick Callback to navigate to the login screen.
 * @param onRegisterClick Callback to navigate to the registration screen.
 * @param modifier Modifier for styling.
 */
@Composable
fun LoginPromptDialog(
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.login_prompt_title),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.login_prompt_message),
                fontFamily = InterFamily
            )
        },
        confirmButton = {
            TextButton(onClick = onLoginClick) {
                Text(text = stringResource(R.string.login_prompt_login_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onRegisterClick) {
                Text(text = stringResource(R.string.login_prompt_register_button))
            }
        },
        modifier = modifier
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun LoginPromptDialogPreviewLight() {
    QuizzezTheme(darkTheme = false) {
        LoginPromptDialog(onDismiss = {}, onLoginClick = {}, onRegisterClick = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun LoginPromptDialogPreviewDark() {
    QuizzezTheme(darkTheme = true) {
        LoginPromptDialog(onDismiss = {}, onLoginClick = {}, onRegisterClick = {})
    }
}
