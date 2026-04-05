package com.example.androidapp.ui.screens.admin.reports

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.ui.components.admin.AdminInsightCard
import com.example.androidapp.ui.components.admin.BarChartItem
import com.example.androidapp.ui.components.admin.EngagementRingChart
import com.example.androidapp.ui.components.admin.HorizontalBarChart
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import com.example.androidapp.ui.theme.QuizzezTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * Admin reports screen displaying advanced analytics, charts, and AI-powered insights.
 *
 * Presents a comprehensive dashboard with hero stat cards, quiz distribution chart,
 * user and content analytics sections, and intelligent insight cards derived from
 * [SystemStats].
 *
 * @param viewModel The [AdminReportsViewModel] managing the reports UI state.
 * @param onNavigateBack Callback invoked when the user taps the back button.
 * @param modifier Modifier for external layout customization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    viewModel: AdminReportsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_advanced_analytics),
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingSpinner(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error,
                        onRetry = { viewModel.loadStats() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.stats != null -> {
                    ReportsContent(
                        stats = uiState.stats!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main scrollable content
// ---------------------------------------------------------------------------

/**
 * Scrollable column containing all analytics sections.
 *
 * @param stats The [SystemStats] domain model to render.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun ReportsContent(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Hero Stats Row
        HeroStatsRow(stats = stats)

        // 2. Quiz Distribution Chart
        QuizDistributionSection(stats = stats)

        // 3. User Analytics
        UserAnalyticsSection(stats = stats)

        // 4. Content Analytics
        ContentAnalyticsSection(stats = stats)

        // 5. AI Insights
        AiInsightsSection(stats = stats)

        // 6. Bottom scroll padding
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ---------------------------------------------------------------------------
// Section 1 -- Hero Stats Row
// ---------------------------------------------------------------------------

/**
 * Two large metric cards displayed side-by-side: total plays and completion rate.
 *
 * @param stats The [SystemStats] providing the metric values.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun HeroStatsRow(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Plays card
        HeroStatCard(
            title = stringResource(R.string.admin_total_plays),
            value = numberFormat.format(stats.totalAttempts),
            valueColor = MaterialTheme.colorScheme.primary,
            badgeText = stringResource(
                R.string.admin_increase_this_month,
                String.format(Locale.US, "%.1f", stats.averageAttemptsPerQuiz)
            ),
            modifier = Modifier.weight(1f)
        )

        // Completion Rate card
        HeroStatCard(
            title = stringResource(R.string.admin_completion_rate),
            value = String.format(Locale.US, "%.1f%%", stats.activeUserPercentage),
            valueColor = MaterialTheme.colorScheme.secondary,
            badgeText = stringResource(
                R.string.admin_stat_avg_attempts_per_quiz
            ) + ": " + String.format(Locale.US, "%.1f", stats.averageAttemptsPerQuiz),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * A single hero-sized stat card with title, large value, and a small pill badge.
 *
 * @param title The metric label displayed at the top.
 * @param value The formatted metric value.
 * @param valueColor The [Color] applied to the value text.
 * @param badgeText Short contextual text rendered in a pill below the value.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun HeroStatCard(
    title: String,
    value: String,
    valueColor: Color,
    badgeText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = badgeText,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section 2 -- Quiz Distribution Chart
// ---------------------------------------------------------------------------

/**
 * Card displaying quiz counts by status as a horizontal bar chart.
 *
 * @param stats The [SystemStats] providing quiz breakdown values.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun QuizDistributionSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_quiz_distribution),
                fontFamily = PlayfairDisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalBarChart(
                items = listOf(
                    BarChartItem(
                        label = stringResource(R.string.admin_public_label),
                        value = stats.publicQuizzes,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    BarChartItem(
                        label = stringResource(R.string.admin_private_label),
                        value = stats.privateQuizzes,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    BarChartItem(
                        label = stringResource(R.string.admin_deleted_label),
                        value = stats.deletedQuizzes,
                        color = MaterialTheme.colorScheme.error
                    )
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Section 3 -- User Analytics
// ---------------------------------------------------------------------------

/**
 * User analytics section with an engagement ring chart and summary stat rows.
 *
 * @param stats The [SystemStats] providing user metric values.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun UserAnalyticsSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.admin_reports_user_analytics),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EngagementRingChart(
                    percentage = stats.activeUserPercentage.toFloat(),
                    centerLabel = stringResource(R.string.admin_active_ratio),
                    centerValue = String.format(
                        Locale.US, "%.0f%%", stats.activeUserPercentage
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserStatRow(
                        icon = Icons.Default.Person,
                        label = stringResource(R.string.admin_stat_total_users),
                        value = stats.totalUsers.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    UserStatRow(
                        icon = Icons.Default.Group,
                        label = stringResource(R.string.admin_stat_active_users),
                        value = stats.activeUsers.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    UserStatRow(
                        icon = Icons.Default.Shield,
                        label = "Admin",
                        value = stats.adminUsers.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * A single row inside the user analytics summary showing an icon, label, and value.
 *
 * @param icon The leading icon.
 * @param label The stat description.
 * @param value The formatted stat value.
 * @param color The tint for the icon and value.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun UserStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Section 4 -- Content Analytics
// ---------------------------------------------------------------------------

/**
 * Content analytics section showing quiz counts in a 2x2 grid of accent-topped cards.
 *
 * @param stats The [SystemStats] providing quiz counts.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun ContentAnalyticsSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.admin_reports_content_analytics),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccentStatCard(
                label = stringResource(R.string.admin_stat_total_quizzes),
                value = stats.totalQuizzes.toString(),
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            AccentStatCard(
                label = stringResource(R.string.admin_stat_public_quizzes),
                value = stats.publicQuizzes.toString(),
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccentStatCard(
                label = stringResource(R.string.admin_stat_private_quizzes),
                value = stats.privateQuizzes.toString(),
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            AccentStatCard(
                label = stringResource(R.string.admin_stat_deleted_quizzes),
                value = stats.deletedQuizzes.toString(),
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A small outlined card with a colored accent strip at the top, a large value,
 * and a descriptive label beneath it.
 *
 * @param label The descriptive text for the metric.
 * @param value The formatted metric value.
 * @param accentColor The color of the top accent bar.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun AccentStatCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = accentColor
                )

                Text(
                    text = label,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section 5 -- AI Insights
// ---------------------------------------------------------------------------

/**
 * AI insights section presenting four [AdminInsightCard]s with data-driven summaries.
 *
 * @param stats The [SystemStats] used to populate insight descriptions.
 * @param modifier Modifier for external layout customization.
 */
@Composable
private fun AiInsightsSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    val purpleAccent = Color(0xFF9C27B0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.admin_ai_insights),
                fontFamily = PlayfairDisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Engagement insight
        AdminInsightCard(
            title = stringResource(R.string.admin_insight_engagement_title),
            description = stringResource(
                R.string.admin_insight_engagement_desc,
                String.format(Locale.US, "%.1f", stats.averageAttemptsPerQuiz)
            ),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            accentColor = MaterialTheme.colorScheme.primary
        )

        // Growth insight
        AdminInsightCard(
            title = stringResource(R.string.admin_insight_growth_title),
            description = stringResource(
                R.string.admin_insight_growth_desc,
                stats.activeUsers.toString(),
                String.format(Locale.US, "%.0f", stats.activeUserPercentage)
            ),
            icon = Icons.Default.Group,
            accentColor = MaterialTheme.colorScheme.secondary
        )

        // Content insight
        AdminInsightCard(
            title = stringResource(R.string.admin_insight_content_title),
            description = stringResource(
                R.string.admin_insight_content_desc,
                stats.publicQuizzes.toString()
            ),
            icon = Icons.Default.Public,
            accentColor = MaterialTheme.colorScheme.tertiary
        )

        // Community insight
        AdminInsightCard(
            title = stringResource(R.string.admin_insight_community_title),
            description = stringResource(
                R.string.admin_insight_community_desc,
                stats.totalQuestionsInPool.toString()
            ),
            icon = Icons.Default.Folder,
            accentColor = purpleAccent
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val previewStats = SystemStats(
    totalUsers = 1234,
    totalQuizzes = 890,
    totalAttempts = 67890,
    totalQuestionsInPool = 234,
    activeUsers = 567,
    publicQuizzes = 456,
    privateQuizzes = 312,
    deletedQuizzes = 45,
    adminUsers = 3
)

@Preview(
    showBackground = true,
    name = "AdminReportsContent - Light"
)
@Composable
private fun ReportsContentLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ReportsContent(
                stats = previewStats,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "AdminReportsContent - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ReportsContentDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ReportsContent(
                stats = previewStats,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, name = "HeroStatsRow - Light")
@Composable
private fun HeroStatsRowLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HeroStatsRow(
                stats = previewStats,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "HeroStatsRow - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HeroStatsRowDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HeroStatsRow(
                stats = previewStats,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
