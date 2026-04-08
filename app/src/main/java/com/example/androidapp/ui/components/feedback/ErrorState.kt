package com.example.androidapp.ui.components.feedback

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Displays a full-screen error state with an icon, message, and retry button.
 *
 * @param message The error message to display; falls back to a default if null.
 * @param icon The icon shown above the message.
 * @param onRetry Callback invoked when the user taps the retry button.
 * @param modifier Modifier for styling and layout customization.
 */
@Composable
fun ErrorState(
    message: String? = null,
    icon: ImageVector = Icons.Default.Warning,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayMessage = message ?: stringResource(R.string.error_default_message)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Red warning icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message text
        Text(
            text = displayMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Retry button
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun ErrorStatePreview() {
    QuizzezTheme {
        ErrorState(
            message = "Khong the tai du lieu. Vui long kiem tra ket noi mang.",
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorStateDarkPreview() {
    QuizzezTheme {
        ErrorState(
            message = "Khong the tai du lieu. Vui long kiem tra ket noi mang.",
            onRetry = {}
        )
    }
}
