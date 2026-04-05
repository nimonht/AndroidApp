package com.example.androidapp.ui.components.admin

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Insight card with a pastel-tinted background for the admin "AI Insights" section.
 *
 * The card renders a horizontal layout containing a circular icon container on
 * the left and title + description text on the right. The background and icon
 * container colours are derived from [accentColor] at reduced alpha values,
 * producing a soft pastel appearance that works in both light and dark themes.
 *
 * @param title       Bold headline text (e.g., "Tuong tac nguoi dung").
 * @param description Explanatory body text below the title.
 * @param icon        [ImageVector] displayed inside the circular container.
 * @param accentColor Base colour used for background tint, icon container, and icon tint.
 * @param modifier    Modifier for external layout customisation.
 */
@Composable
fun AdminInsightCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(accentColor.copy(alpha = 0.15f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // -- Circular icon container (48dp) --
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = accentColor
            )
        }

        // -- Text content --
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = description,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminInsightCardEngagementPreview() {
    QuizzezTheme {
        AdminInsightCard(
            title = "Tương tác người dùng",
            description = "Trung bình 5.2 lượt chơi mỗi quiz cho thấy mức độ tương tác tốt từ cộng đồng.",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            accentColor = Color(0xFF4CAF50),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminInsightCardGrowthPreview() {
    QuizzezTheme {
        AdminInsightCard(
            title = "Tăng trưởng tốt",
            description = "567 người dùng đang hoạt động, chiếm 45% tổng số người dùng.",
            icon = Icons.Default.People,
            accentColor = Color(0xFF2196F3),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminInsightCardContentPreview() {
    QuizzezTheme {
        AdminInsightCard(
            title = "Nội dung phong phú",
            description = "245 quiz công khai giúp cộng đồng tiếp cận nội dung dễ dàng hơn.",
            icon = Icons.Default.Lightbulb,
            accentColor = Color(0xFFFF9800),
            modifier = Modifier.padding(16.dp)
        )
    }
}
