package com.example.androidapp.ui.components.feedback

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Full-screen centered loading indicator with an optional message.
 *
 * @param modifier Modifier for styling and layout customization.
 * @param message Optional text displayed below the spinner (e.g., "Dang dang nhap...").
 */
@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    message: String? = null // Optional message (e.g., "Logging in...")
) {
    // Box centers content both vertically and horizontally
    Box(
        modifier = modifier.fillMaxSize(), // Fills the entire parent space by default
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Main spinner
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            // 2. Message text (only shown if provided)
            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp)) // Spacing between spinner and text
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun LoadingSpinnerPreview() {
    QuizzezTheme {
        LoadingSpinner(
            message = "Dang tai du lieu..."
        )
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingSpinnerDarkPreview() {
    QuizzezTheme {
        LoadingSpinner(
            message = "Dang tai du lieu..."
        )
    }
}
