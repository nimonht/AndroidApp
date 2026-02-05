package com.example.androidapp.ui.components.forms

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.QuizCodeTheme

/**
 * Styled toggle switch for boolean settings with optional label and description.
 *
 * @param checked Whether the switch is checked/on.
 * @param onCheckedChange Callback when the switch state changes.
 * @param modifier Modifier for styling and layout customization.
 * @param enabled Whether the switch is enabled and interactive.
 * @param label Primary label text displayed next to the switch.
 * @param description Secondary description text displayed below the label.
 */
@Composable
fun SwitchToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    description: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Label and description column
        if (label != null || description != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
                if (description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }

        // Switch component
        Switch(
            checked = checked,
            onCheckedChange = null, // Handled by row click
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                disabledCheckedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            )
        )
    }
}

/**
 * Switch toggle inside a card container for settings screens.
 *
 * @param checked Whether the switch is checked/on.
 * @param onCheckedChange Callback when the switch state changes.
 * @param label Primary label text.
 * @param modifier Modifier for styling and layout customization.
 * @param enabled Whether the switch is enabled.
 * @param description Secondary description text.
 * @param icon Optional leading icon composable.
 */
@Composable
fun SwitchToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    role = Role.Switch,
                    onClick = { onCheckedChange(!checked) }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            if (icon != null) {
                Box(
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    icon()
                }
            }

            // Label and description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            // Switch
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SwitchTogglePreview() {
    QuizCodeTheme {
        var checked by remember { mutableStateOf(true) }
        Column(modifier = Modifier.padding(16.dp)) {
            SwitchToggle(
                checked = checked,
                onCheckedChange = { checked = it },
                label = "Chế độ tối",
                description = "Sử dụng giao diện tối để bảo vệ mắt"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SwitchToggleUncheckedPreview() {
    QuizCodeTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SwitchToggle(
                checked = false,
                onCheckedChange = { },
                label = "Thông báo",
                description = "Nhận thông báo khi có bài kiểm tra mới"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SwitchToggleDisabledPreview() {
    QuizCodeTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SwitchToggle(
                checked = true,
                onCheckedChange = { },
                enabled = false,
                label = "Tính năng Premium",
                description = "Nâng cấp để mở khóa tính năng này"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SwitchToggleCardPreview() {
    QuizCodeTheme {
        var checked by remember { mutableStateOf(true) }
        Column(modifier = Modifier.padding(16.dp)) {
            SwitchToggleCard(
                checked = checked,
                onCheckedChange = { checked = it },
                label = "Công khai bài kiểm tra",
                description = "Cho phép mọi người tìm kiếm và làm bài kiểm tra của bạn"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SwitchToggleSimplePreview() {
    QuizCodeTheme {
        var checked by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Xáo trộn câu hỏi", modifier = Modifier.weight(1f))
            SwitchToggle(
                checked = checked,
                onCheckedChange = { checked = it }
            )
        }
    }
}
