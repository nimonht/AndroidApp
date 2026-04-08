package com.example.androidapp.ui.components.feedback

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Displays a placeholder UI when a list or content area has no data.
 * Shows a faded icon, a descriptive message, and an optional action button.
 *
 * @param message Text explaining why the area is empty.
 * @param icon Icon displayed above the message.
 * @param actionLabel Optional label for a call-to-action button.
 * @param onActionClick Optional callback invoked when the action button is tapped.
 * @param modifier Modifier for styling and layout customization.
 */
@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Default.Search,
    actionLabel: String? = null, // e.g., "Create new"
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Faded icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Message
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Action button (optional - only shown if provided)
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun EmptyStatePreview() {
    QuizzezTheme {
        EmptyState(
            message = "Khong tim thay ket qua nao",
            icon = Icons.Default.Search,
            actionLabel = "Tao moi",
            onActionClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyStateDarkPreview() {
    QuizzezTheme {
        EmptyState(
            message = "Chua co bai quiz nao",
            icon = Icons.Default.Inbox
        )
    }
}
