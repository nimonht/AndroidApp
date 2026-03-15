package com.example.androidapp.ui.components.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizzezTheme
import com.example.androidapp.ui.theme.Warning as WarningColor

/**
 * Reusable alert dialog with title, message, and confirm/cancel actions.
 * Follows Material 3 design guidelines.
 *
 * @param title The title of the dialog.
 * @param message The message/description displayed in the dialog.
 * @param onDismiss Callback when the dialog is dismissed (cancel, outside tap, back button).
 * @param onConfirm Callback when the confirm button is clicked.
 * @param modifier Modifier for styling.
 * @param icon Optional icon displayed above the title.
 * @param confirmText Text for the confirm button.
 * @param dismissText Text for the dismiss/cancel button.
 * @param confirmButtonColors Colors for the confirm button.
 * @param isDestructive Whether this is a destructive action (shows error colors).
 */
@Composable
fun AppAlertDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    confirmText: String? = null,
    dismissText: String? = null,
    confirmButtonColors: ButtonColors? = null,
    isDestructive: Boolean = false
) {
    val confirmLabel = confirmText ?: stringResource(R.string.confirm)
    val dismissLabel = dismissText ?: stringResource(R.string.cancel)

    val buttonColors = confirmButtonColors ?: if (isDestructive) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    } else {
        ButtonDefaults.buttonColors()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = buttonColors
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        }
    )
}

/**
 * Confirmation dialog for delete/destructive actions.
 *
 * @param title The title of the dialog.
 * @param message The warning message.
 * @param onDismiss Callback when dismissed.
 * @param onConfirm Callback when confirmed.
 * @param modifier Modifier for styling.
 * @param confirmText Text for the confirm button.
 */
@Composable
fun DeleteConfirmDialog(
    title: String? = null,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String? = null
) {
    val resolvedTitle = title ?: stringResource(R.string.delete_confirm_title)
    val resolvedConfirmText = confirmText ?: stringResource(R.string.delete)
    AppAlertDialog(
        title = resolvedTitle,
        message = message,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        modifier = modifier,
        icon = Icons.Default.Delete,
        confirmText = resolvedConfirmText,
        dismissText = stringResource(R.string.cancel),
        isDestructive = true
    )
}

/**
 * Information dialog with just an OK button.
 *
 * @param title The title of the dialog.
 * @param message The information message.
 * @param onDismiss Callback when dismissed.
 * @param modifier Modifier for styling.
 * @param icon Optional icon (defaults to info icon).
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

/**
 * Warning dialog for important notices.
 *
 * @param title The title of the dialog.
 * @param message The warning message.
 * @param onDismiss Callback when dismissed.
 * @param onConfirm Callback when confirmed.
 * @param modifier Modifier for styling.
 * @param confirmText Text for the confirm button.
 */
@Composable
fun WarningDialog(
    title: String? = null,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String? = null
) {
    val resolvedTitle = title ?: stringResource(R.string.warning_title)
    val resolvedConfirmText = confirmText ?: stringResource(R.string.continue_action)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningColor
            )
        },
        title = {
            Text(
                text = resolvedTitle,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarningColor
                )
            ) {
                Text(resolvedConfirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Loading dialog with a progress indicator.
 *
 * @param message The loading message to display.
 * @param modifier Modifier for styling.
 */
@Composable
fun LoadingDialog(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    val displayMessage = message ?: stringResource(R.string.loading_processing)
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        modifier = modifier,
        title = null,
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    text = displayMessage,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = { }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppAlertDialogPreview() {
    QuizzezTheme {
        AppAlertDialog(
            title = "Lưu thay đổi?",
            message = "Bạn có muốn lưu các thay đổi trước khi thoát không?",
            onDismiss = { },
            onConfirm = { },
            icon = Icons.Default.Info
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteConfirmDialogPreview() {
    QuizzezTheme {
        DeleteConfirmDialog(
            title = "Xóa bài kiểm tra?",
            message = "Bạn có chắc chắn muốn xóa bài kiểm tra này? Hành động này không thể hoàn tác.",
            onDismiss = { },
            onConfirm = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoDialogPreview() {
    QuizzezTheme {
        InfoDialog(
            title = "Thông tin",
            message = "Bài kiểm tra này có 10 câu hỏi và thời gian làm bài là 15 phút.",
            onDismiss = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WarningDialogPreview() {
    QuizzezTheme {
        WarningDialog(
            title = "Cảnh báo",
            message = "Bạn sẽ mất tiến trình hiện tại nếu thoát khỏi bài kiểm tra.",
            onDismiss = { },
            onConfirm = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingDialogPreview() {
    QuizzezTheme {
        LoadingDialog(
            message = "Đang tải bài kiểm tra..."
        )
    }
}
