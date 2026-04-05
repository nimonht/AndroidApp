package com.example.androidapp.ui.components.admin

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Quick Stats card for the admin dashboard.
 *
 * Displays a single metric inside a vertically-centred card with a
 * gradient-backed icon container, a large bold value, a descriptive title,
 * and an optional subtitle. Designed for use in a 2- or 3-column grid.
 *
 * @param title       Statistic label displayed below the value (e.g., "Tong nguoi dung").
 * @param value       Formatted metric value displayed prominently (e.g., "1,234").
 * @param subtitle    Additional context beneath the title (e.g., "nguoi dung").
 *                    Hidden when blank.
 * @param icon        [ImageVector] rendered inside the gradient container.
 * @param gradientColors Two or more colours for the icon container linear gradient.
 *                       Falls back to [MaterialTheme.colorScheme.primary] when empty.
 * @param modifier    Modifier for external layout customisation.
 */
@Composable
fun StatisticCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val fallbackColor = MaterialTheme.colorScheme.primary
    val resolvedGradientColors = when {
        gradientColors.size >= 2 -> gradientColors
        gradientColors.size == 1 -> listOf(gradientColors.first(), gradientColors.first())
        else -> listOf(fallbackColor, fallbackColor)
    }

    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // -- Gradient icon container (48dp rounded square) --
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush = Brush.linearGradient(resolvedGradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            // -- Large value number --
            Text(
                text = value,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // -- Title + optional subtitle --
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatisticCardPreview() {
    QuizzezTheme {
        StatisticCard(
            title = "Tổng người dùng",
            value = "1,234",
            subtitle = "người dùng",
            icon = Icons.Default.Person,
            gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
            modifier = Modifier
                .padding(16.dp)
                .width(160.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatisticCardNoSubtitlePreview() {
    QuizzezTheme {
        StatisticCard(
            title = "Tổng số câu đố",
            value = "567",
            subtitle = "",
            icon = Icons.Default.Star,
            gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
            modifier = Modifier
                .padding(16.dp)
                .width(160.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatisticCardAttemptsPreview() {
    QuizzezTheme {
        StatisticCard(
            title = "Tổng số lượt làm",
            value = "8,901",
            subtitle = "lượt chơi",
            icon = Icons.Default.PlayArrow,
            gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53)),
            modifier = Modifier
                .padding(16.dp)
                .width(160.dp)
        )
    }
}
