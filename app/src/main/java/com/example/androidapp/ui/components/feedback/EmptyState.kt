package com.example.androidapp.ui.components.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
    actionLabel: String? = null, // Ví dụ: "Tạo mới ngay"
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon mờ nhạt
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Thông báo
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Nút hành động (Optional - chỉ hiện nếu có truyền vào)
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}
