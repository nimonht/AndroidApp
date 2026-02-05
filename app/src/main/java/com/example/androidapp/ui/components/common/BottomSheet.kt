package com.example.androidapp.ui.components.common

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizCodeTheme

/**
 * Modal bottom sheet wrapper with common functionality.
 * Provides a consistent bottom sheet experience throughout the app.
 *
 * @param showSheet Whether the sheet is visible.
 * @param onDismiss Callback when the sheet is dismissed.
 * @param modifier Modifier for styling.
 * @param skipPartiallyExpanded Whether to skip the partially expanded state.
 * @param title Optional title displayed at the top of the sheet.
 * @param content The content of the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 2.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Title section
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Content
                content()
            }
        }
    }
}

/**
 * Bottom sheet item with icon and text.
 *
 * @param text The text to display.
 * @param icon The leading icon.
 * @param onClick Callback when the item is clicked.
 * @param modifier Modifier for styling.
 * @param iconTint Tint color for the icon.
 * @param textColor Color for the text.
 * @param enabled Whether the item is enabled.
 * @param trailingContent Optional trailing content (e.g., badge, arrow).
 */
@Composable
fun BottomSheetItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) iconTint else iconTint.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) textColor else textColor.copy(alpha = 0.38f),
            modifier = Modifier.weight(1f)
        )
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

/**
 * Destructive bottom sheet item (e.g., delete, remove).
 */
@Composable
fun BottomSheetDestructiveItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    BottomSheetItem(
        text = text,
        icon = icon,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        iconTint = MaterialTheme.colorScheme.error,
        textColor = MaterialTheme.colorScheme.error
    )
}

/**
 * Bottom sheet section header.
 */
@Composable
fun BottomSheetSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

/**
 * Share bottom sheet with common sharing options.
 *
 * @param showSheet Whether the sheet is visible.
 * @param onDismiss Callback when dismissed.
 * @param shareCode The share code to display.
 * @param onCopyCode Callback when copy code is clicked.
 * @param onShareLink Callback when share link is clicked.
 * @param onShareQR Callback when share QR code is clicked.
 * @param onRegenerateCode Optional callback for regenerating the code.
 */
@Composable
fun ShareBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    shareCode: String,
    onCopyCode: () -> Unit,
    onShareLink: () -> Unit,
    onShareQR: (() -> Unit)? = null,
    onRegenerateCode: (() -> Unit)? = null
) {
    AppBottomSheet(
        showSheet = showSheet,
        onDismiss = onDismiss,
        title = stringResource(R.string.share_quiz_title)
    ) {
        // Share code display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.share_code_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shareCode,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Share options
        BottomSheetItem(
            text = stringResource(R.string.copy_code),
            icon = Icons.Outlined.ContentCopy,
            onClick = onCopyCode
        )

        BottomSheetItem(
            text = stringResource(R.string.share_link),
            icon = Icons.Outlined.Share,
            onClick = onShareLink
        )

        if (onShareQR != null) {
            BottomSheetItem(
                text = stringResource(R.string.create_qr_code),
                icon = Icons.Outlined.QrCode,
                onClick = onShareQR
            )
        }

        if (onRegenerateCode != null) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            BottomSheetItem(
                text = stringResource(R.string.regenerate_code),
                icon = Icons.Outlined.Refresh,
                onClick = onRegenerateCode,
                iconTint = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Options/actions bottom sheet for quiz or item options.
 *
 * @param showSheet Whether the sheet is visible.
 * @param onDismiss Callback when dismissed.
 * @param onEdit Callback for edit action.
 * @param onShare Callback for share action.
 * @param onDelete Callback for delete action.
 * @param onDuplicate Optional callback for duplicate action.
 * @param editEnabled Whether edit is enabled.
 * @param deleteEnabled Whether delete is enabled.
 */
@Composable
fun OptionsBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    editEnabled: Boolean = true,
    deleteEnabled: Boolean = true
) {
    AppBottomSheet(
        showSheet = showSheet,
        onDismiss = onDismiss,
        title = stringResource(R.string.options_title)
    ) {
        BottomSheetItem(
            text = stringResource(R.string.edit),
            icon = Icons.Outlined.Edit,
            onClick = onEdit,
            enabled = editEnabled
        )

        if (onDuplicate != null) {
            BottomSheetItem(
                text = stringResource(R.string.duplicate),
                icon = Icons.Outlined.ContentCopy,
                onClick = onDuplicate
            )
        }

        BottomSheetItem(
            text = stringResource(R.string.share),
            icon = Icons.Outlined.Share,
            onClick = onShare
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        BottomSheetDestructiveItem(
            text = stringResource(R.string.delete),
            icon = Icons.Outlined.Delete,
            onClick = onDelete,
            enabled = deleteEnabled
        )
    }
}

/**
 * Filter bottom sheet for list filtering.
 *
 * @param showSheet Whether the sheet is visible.
 * @param onDismiss Callback when dismissed.
 * @param title Title of the filter sheet.
 * @param content Filter options content.
 */
@Composable
fun FilterBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.filter_title),
    content: @Composable ColumnScope.() -> Unit
) {
    AppBottomSheet(
        showSheet = showSheet,
        onDismiss = onDismiss,
        title = title,
        skipPartiallyExpanded = false,
        content = content
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BottomSheetItemPreview() {
    QuizCodeTheme {
        Column {
            BottomSheetItem(
                text = stringResource(R.string.edit),
                icon = Icons.Outlined.Edit,
                onClick = { }
            )
            BottomSheetItem(
                text = stringResource(R.string.share),
                icon = Icons.Outlined.Share,
                onClick = { },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            BottomSheetDestructiveItem(
                text = stringResource(R.string.delete),
                icon = Icons.Outlined.Delete,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionsBottomSheetContentPreview() {
    QuizCodeTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.options_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            BottomSheetItem(
                text = stringResource(R.string.edit),
                icon = Icons.Outlined.Edit,
                onClick = { }
            )
            BottomSheetItem(
                text = stringResource(R.string.duplicate),
                icon = Icons.Outlined.ContentCopy,
                onClick = { }
            )
            BottomSheetItem(
                text = stringResource(R.string.share),
                icon = Icons.Outlined.Share,
                onClick = { }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            BottomSheetDestructiveItem(
                text = stringResource(R.string.delete),
                icon = Icons.Outlined.Delete,
                onClick = { }
            )
        }
    }
}
