package com.example.androidapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape system for Quizzez app.
 * Follows Material Design 3 shape scale:
 *   None  -> 0 dp
 *   Extra-Small -> 4 dp
 *   Small -> 8 dp
 *   Medium -> 12 dp
 *   Large -> 16 dp
 *   Extra-Large -> 28 dp
 *
 * MaterialTheme.shapes exposes the standard five MD3 slots above.
 * Use [FullShape] when a fixed 50 dp pill/capsule shape is needed.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val FullShape = RoundedCornerShape(50.dp)
