package com.example.androidapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape system for QuizCode app.
 * Based on design-tokens.json radii values.
 */
val Shapes = Shapes(
    // Small components (chips, buttons with minimal radius)
    extraSmall = RoundedCornerShape(4.dp),

    // Small components (buttons, text fields)
    small = RoundedCornerShape(8.dp),

    // Medium components (cards, dialogs)
    medium = RoundedCornerShape(16.dp),

    // Large components (bottom sheets, large cards)
    large = RoundedCornerShape(28.dp),

    // Extra large components (full screen dialogs)
    extraLarge = RoundedCornerShape(32.dp)
)
