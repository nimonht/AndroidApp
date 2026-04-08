package com.example.androidapp.ui.components.quiz

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * A selectable answer choice card for quiz questions, displaying a label, content text,
 * and a radio button or checkbox depending on the selection mode.
 *
 * @param label The choice label (e.g., "A", "B").
 * @param content The choice text content.
 * @param isSelected Whether this choice is currently selected.
 * @param isMultiSelect If true, displays a checkbox; otherwise a radio button.
 * @param onClick Callback when the choice is clicked.
 * @param modifier Modifier for styling and layout customization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceButton(
    label: String,          // e.g., "A", "B"
    content: String,        // The choice text content
    isSelected: Boolean,    // Whether this choice is currently selected
    isMultiSelect: Boolean = false, // True = Checkbox, False = Radio
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Determine colors based on selection state
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline

    val borderWidth = if (isSelected) 2.dp else 1.dp

    // 2. Wrapping card container
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3. Selection indicator (Radio or Checkbox)
            if (isMultiSelect) {
                // Multi-select mode (Checkbox)
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null // null so the Card handles clicks
                )
            } else {
                // Single-select mode (Radio)
                RadioButton(
                    selected = isSelected,
                    onClick = null // null so the Card handles clicks
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 4. Text content (Label + Content)
            Text(
                text = "$label. ", // "A. "
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f) // Push the check icon to the right if needed
            )

            // Small check icon on the right when selected (improves UX)
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun ChoiceButtonPreview() {
    QuizzezTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceButton(
                label = "A",
                content = "Thủ đô của Việt Nam là Hà Nội",
                isSelected = true,
                onClick = {}
            )
            ChoiceButton(
                label = "B",
                content = "Thủ đô của Việt Nam là Đà Nẵng",
                isSelected = false,
                onClick = {}
            )
            ChoiceButton(
                label = "C",
                content = "Chọn nhiều đáp án",
                isSelected = true,
                isMultiSelect = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChoiceButtonDarkPreview() {
    QuizzezTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceButton(
                label = "A",
                content = "Thủ đô của Việt Nam là Hà Nội",
                isSelected = true,
                onClick = {}
            )
            ChoiceButton(
                label = "B",
                content = "Thủ đô của Việt Nam là Đà Nẵng",
                isSelected = false,
                onClick = {}
            )
            ChoiceButton(
                label = "C",
                content = "Chọn nhiều đáp án",
                isSelected = true,
                isMultiSelect = true,
                onClick = {}
            )
        }
    }
}
