package com.example.androidapp.ui.components.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.FullShape
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Pill-shaped tag chip with configurable colors and optional click action.
 *
 * @param text The label text displayed inside the chip.
 * @param modifier Modifier for styling and layout customization.
 * @param isSelected Whether the chip is in a selected state. When true, the chip uses
 *   primary/onPrimary colors, overriding [containerColor] and [labelColor].
 * @param containerColor Background color of the chip (ignored when [isSelected] is true).
 * @param labelColor Text color of the chip label (ignored when [isSelected] is true).
 * @param onClick Optional click callback (chip is non-interactive if null).
 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    labelColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    onClick: (() -> Unit)? = null
) {
    val actualContainerColor = if (isSelected) MaterialTheme.colorScheme.primary else containerColor
    val actualLabelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else labelColor

    Box(
        modifier = modifier
            .clip(FullShape)
            .background(actualContainerColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = actualLabelColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true, name = "Light")
@Composable
private fun TagChipPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(text = "Khoa hoc")
                    TagChip(text = "Lich su")
                    TagChip(text = "Toan hoc", isSelected = true)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(text = "Dia ly", onClick = {})
                    TagChip(text = "Van hoc", isSelected = true, onClick = {})
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TagChipDarkPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(text = "Khoa hoc")
                    TagChip(text = "Lich su")
                    TagChip(text = "Toan hoc", isSelected = true)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(text = "Dia ly", onClick = {})
                    TagChip(text = "Van hoc", isSelected = true, onClick = {})
                }
            }
        }
    }
}
