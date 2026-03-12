package com.example.androidapp.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.FullShape

/**
 * Pill-shaped tag chip with configurable colors and optional click action.
 *
 * @param text The label text displayed inside the chip.
 * @param modifier Modifier for styling and layout customization.
 * @param containerColor Background color of the chip.
 * @param labelColor Text color of the chip label.
 * @param onClick Optional click callback (chip is non-interactive if null).
 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(FullShape)
            .background(containerColor)
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
            color = labelColor
        )
    }
}