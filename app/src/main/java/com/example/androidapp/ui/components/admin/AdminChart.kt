package com.example.androidapp.ui.components.admin

import android.content.res.Configuration
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.ui.theme.FullShape
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Data class representing a single item in [HorizontalBarChart].
 *
 * @property label Display label shown to the left of the bar.
 * @property value Numeric value determining bar length and displayed to the right.
 * @property color Fill colour for this bar.
 */
data class BarChartItem(
    val label: String,
    val value: Int,
    val color: Color
)

/**
 * A smooth area / line chart drawn with Canvas using cubic bezier curves.
 *
 * Renders a gradient-filled area beneath a curved line connecting the data
 * points. The peak data point is highlighted with a prominent dot and a value
 * label drawn above it. X-axis labels are displayed below the chart area.
 *
 * @param dataPoints Y-axis values for each data point (at least one entry).
 * @param labels     X-axis labels aligned one-to-one with [dataPoints].
 * @param modifier   Modifier for external layout customisation.
 */
@Composable
fun EngagementLineChart(
    dataPoints: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.heightIn(min = 200.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val paddingTop = 28.dp.toPx()
            val paddingBottom = 4.dp.toPx()
            val paddingHorizontal = 16.dp.toPx()

            val chartWidth = canvasWidth - 2f * paddingHorizontal
            val chartHeight = canvasHeight - paddingTop - paddingBottom

            val maxVal = dataPoints.max()
            val minVal = dataPoints.min()
            val valRange = maxVal - minVal

            // Map data points to pixel coordinates
            val points = dataPoints.mapIndexed { index, value ->
                val x = paddingHorizontal +
                        index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1) * chartWidth
                val normalizedY = if (valRange > 0f) {
                    (value - minVal) / valRange
                } else {
                    0.5f
                }
                val y = paddingTop + chartHeight * (1f - normalizedY)
                Offset(x, y)
            }

            val bottomY = canvasHeight - paddingBottom

            // Horizontal grid lines (3 subtle lines)
            for (i in 0..2) {
                val y = paddingTop + chartHeight * (i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(paddingHorizontal, y),
                    end = Offset(canvasWidth - paddingHorizontal, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Build smooth line path using cubic bezier curves
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }

            // Build filled area path (same top curve, closed at the bottom)
            val fillPath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
                lineTo(points.last().x, bottomY)
                lineTo(points.first().x, bottomY)
                close()
            }

            val progress = animationProgress.value

            // Draw gradient fill beneath the line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f * progress),
                        primaryColor.copy(alpha = 0f)
                    ),
                    startY = paddingTop,
                    endY = bottomY
                )
            )

            // Draw the main curve line
            drawPath(
                path = linePath,
                color = primaryColor.copy(alpha = progress),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw small dots on every data point
            points.forEach { point ->
                drawCircle(
                    color = surfaceColor,
                    radius = 4.dp.toPx() * progress,
                    center = point
                )
                drawCircle(
                    color = primaryColor,
                    radius = 3.dp.toPx() * progress,
                    center = point
                )
            }

            // Highlight peak point with a larger ring dot
            val peakIndex = dataPoints.indices.maxByOrNull { dataPoints[it] } ?: 0
            val peakPoint = points[peakIndex]

            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx() * progress,
                center = peakPoint
            )
            drawCircle(
                color = surfaceColor,
                radius = 3.5.dp.toPx() * progress,
                center = peakPoint
            )

            // Draw peak value label above the dot
            if (progress > 0.5f) {
                val peakText = dataPoints[peakIndex].toInt().toString()
                val textPaint = android.graphics.Paint().apply {
                    color = onSurfaceColor.toArgb()
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    peakText,
                    peakPoint.x,
                    peakPoint.y - 14.dp.toPx(),
                    textPaint
                )
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = onSurfaceVariantColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Horizontal bar chart for showing categorical distributions.
 *
 * Each [BarChartItem] is rendered as a row containing a label on the left,
 * a proportionally-sized coloured bar with pill-shaped ends in the middle,
 * and the numeric value on the right.
 *
 * @param items    List of [BarChartItem] entries to display.
 * @param modifier Modifier for external layout customisation.
 */
@Composable
fun HorizontalBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier
) {
    val maxValue = items.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Label on the left
                Text(
                    text = item.label,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = onSurfaceVariantColor,
                    modifier = Modifier.width(72.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Bar in the middle (track + fill)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                ) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(FullShape)
                            .background(trackColor)
                    )

                    // Filled bar (proportional width)
                    val fraction = (item.value.toFloat() / maxValue *
                            animationProgress.value).coerceIn(0f, 1f)
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = fraction.coerceAtLeast(0.03f))
                                .clip(FullShape)
                                .background(item.color)
                        )
                    }
                }

                // Value on the right
                Text(
                    text = item.value.toString(),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = onSurfaceColor,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Circular progress ring chart for displaying a single percentage metric.
 *
 * A thick arc is drawn over a [MaterialTheme.colorScheme.surfaceVariant] track
 * to represent progress. The centre of the ring shows [centerValue] in bold
 * with [centerLabel] below it.
 *
 * @param percentage  Fill percentage (0 -- 100). Clamped to the valid range.
 * @param centerLabel Descriptive label below the value inside the ring.
 * @param centerValue Formatted value shown in the ring centre (e.g., "75%").
 * @param color       Fill colour for the progress arc.
 * @param modifier    Modifier for external layout customisation.
 */
@Composable
fun EngagementRingChart(
    percentage: Float,
    centerLabel: String,
    centerValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val clampedPercentage = percentage.coerceIn(0f, 100f)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(percentage) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = Size(
                width = size.width - strokeWidth,
                height = size.height - strokeWidth
            )
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Background track (full circle)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Filled progress arc (starts at 12 o'clock)
            val sweepAngle = (clampedPercentage / 100f) * 360f * animationProgress.value
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Centre text: value on top, label below
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = centerLabel,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EngagementLineChartPreview() {
    QuizzezTheme {
        EngagementLineChart(
            dataPoints = listOf(120f, 180f, 150f, 220f, 310f, 280f, 350f),
            labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN"),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EngagementLineChartFlatPreview() {
    QuizzezTheme {
        EngagementLineChart(
            dataPoints = listOf(50f, 50f, 50f, 50f),
            labels = listOf("T2", "T3", "T4", "T5"),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HorizontalBarChartPreview() {
    QuizzezTheme {
        HorizontalBarChart(
            items = listOf(
                BarChartItem("Công khai", 245, Color(0xFF4CAF50)),
                BarChartItem("Riêng tư", 123, Color(0xFF2196F3)),
                BarChartItem("Nháp", 67, Color(0xFFFFC107)),
                BarChartItem("Đã xóa", 15, Color(0xFFEF5350))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EngagementRingChartPreview() {
    QuizzezTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EngagementRingChart(
                percentage = 72f,
                centerLabel = "Hoạt động",
                centerValue = "72%",
                color = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(16.dp))
            EngagementRingChart(
                percentage = 45f,
                centerLabel = "Công khai",
                centerValue = "45%",
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EngagementRingChartZeroPreview() {
    QuizzezTheme {
        EngagementRingChart(
            percentage = 0f,
            centerLabel = "Trống",
            centerValue = "0%",
            color = Color(0xFFEF5350),
            modifier = Modifier.padding(16.dp)
        )
    }
}
