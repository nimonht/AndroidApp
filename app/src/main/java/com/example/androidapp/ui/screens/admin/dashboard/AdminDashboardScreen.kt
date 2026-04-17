package com.example.androidapp.ui.screens.admin.dashboard

import android.content.res.Configuration
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.ui.components.admin.AdminInsightCard
import com.example.androidapp.ui.components.admin.BarChartItem
import com.example.androidapp.ui.components.admin.EngagementLineChart
import com.example.androidapp.ui.components.admin.EngagementRingChart
import com.example.androidapp.ui.components.admin.HorizontalBarChart
import com.example.androidapp.ui.components.admin.StatisticCard
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Admin dashboard screen displaying system statistics, charts, insights,
 * and quick-action navigation tiles.
 *
 * The screen is wrapped in a [Scaffold] with a translucent [TopAppBar] and
 * delegates to [DashboardContent] once data has loaded successfully.
 *
 * @param viewModel          ViewModel that owns the dashboard UI state.
 * @param onNavigateBack     Callback invoked when the user presses the back arrow.
 * @param onNavigateToUsers  Callback invoked when "Manage Users" is tapped.
 * @param onNavigateToQuizzes Callback invoked when "Manage Quizzes" is tapped.
 * @param onNavigateToReports Callback invoked when "View Reports" is tapped.
 * @param modifier           Modifier for external layout customisation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val hasManageUsers = uiState.isSuperuser ||
            uiState.currentPermissions.contains(AdminPermission.MANAGE_USERS)
    val hasManageQuizzes = uiState.isSuperuser ||
            uiState.currentPermissions.contains(AdminPermission.MANAGE_QUIZZES)
    val hasViewReports = uiState.isSuperuser ||
            uiState.currentPermissions.contains(AdminPermission.VIEW_REPORTS)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_dashboard_title),
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingSpinner(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!.toMessage(),
                        onRetry = { viewModel.loadStats() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.stats != null -> {
                    DashboardContent(
                        stats = uiState.stats!!,
                        onNavigateToUsers = onNavigateToUsers,
                        onNavigateToQuizzes = onNavigateToQuizzes,
                        onNavigateToReports = onNavigateToReports,
                        hasManageUsers = hasManageUsers,
                        hasManageQuizzes = hasManageQuizzes,
                        hasViewReports = hasViewReports,
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
 * Scrollable dashboard body containing all visual sections.
 *
 * @param stats              Loaded [SystemStats] from the repository.
 * @param onNavigateToUsers  Quick-action callback for user management.
 * @param onNavigateToQuizzes Quick-action callback for quiz management.
 * @param onNavigateToReports Quick-action callback for reports.
 * @param hasManageUsers     Whether the current admin may manage users.
 * @param hasManageQuizzes   Whether the current admin may manage quizzes.
 * @param hasViewReports     Whether the current admin may view reports.
 * @param modifier           Modifier for external layout customisation.
 */
@Composable
private fun DashboardContent(
    stats: SystemStats,
    onNavigateToUsers: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToReports: () -> Unit,
    hasManageUsers: Boolean,
    hasManageQuizzes: Boolean,
    hasViewReports: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1 -- Greeting header
        GreetingHeader(modifier = Modifier.fillMaxWidth())

        // 2 -- Quick stats
        QuickStatsSection(stats = stats, modifier = Modifier.fillMaxWidth())

        // 3 -- Engagement line chart
        EngagementChartSection(stats = stats, modifier = Modifier.fillMaxWidth())

        // 4 -- Quiz distribution bar chart
        QuizDistributionSection(stats = stats, modifier = Modifier.fillMaxWidth())

        // 5 -- User engagement ring
        UserEngagementSection(stats = stats, modifier = Modifier.fillMaxWidth())

        // 6 -- AI-powered insights
        InsightsSection(stats = stats, modifier = Modifier.fillMaxWidth())

        // 7 -- Quick actions
        QuickActionsSection(
            onNavigateToUsers = onNavigateToUsers,
            onNavigateToQuizzes = onNavigateToQuizzes,
            onNavigateToReports = onNavigateToReports,
            hasManageUsers = hasManageUsers,
            hasManageQuizzes = hasManageQuizzes,
            hasViewReports = hasViewReports,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------------------
// 1. Greeting Header
// ---------------------------------------------------------------------------

/**
 * Top greeting row with a personalised welcome message and notification bell.
 *
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun GreetingHeader(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.admin_greeting, stringResource(R.string.admin_role_admin)),
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------
// 2. Quick Stats
// ---------------------------------------------------------------------------

/**
 * Section title plus a row of three [StatisticCard]s for headline metrics.
 *
 * @param stats  Current [SystemStats].
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun QuickStatsSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(text = stringResource(R.string.admin_quick_stats))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatisticCard(
                title = stringResource(R.string.admin_stat_total_users),
                value = stats.totalUsers.toString(),
                subtitle = stringResource(R.string.admin_users_subtitle),
                icon = Icons.Default.People,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            StatisticCard(
                title = stringResource(R.string.admin_stat_active_users),
                value = stats.activeUsers.toString(),
                subtitle = stringResource(R.string.admin_current_subtitle),
                icon = Icons.Default.CheckCircle,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            StatisticCard(
                title = stringResource(R.string.admin_stat_total_quizzes),
                value = stats.totalQuizzes.toString(),
                subtitle = stringResource(R.string.admin_quizzes_subtitle),
                icon = Icons.Default.Quiz,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Engagement Line Chart
// ---------------------------------------------------------------------------

/**
 * Section containing a card with a weekly engagement line chart and a
 * trend-increase badge.
 *
 * Synthetic data points are derived from [SystemStats.activeUsers] to
 * simulate weekly variation.
 *
 * @param stats  Current [SystemStats].
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun EngagementChartSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(text = stringResource(R.string.admin_engagement_chart))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header row with title and trend badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.admin_active_users_weekly),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TrendBadge(
                        text = stringResource(R.string.admin_trend_increase, "12"),
                        modifier = Modifier
                    )
                }

                // Chart
                val dataPoints = listOf(
                    stats.activeUsers * 0.7f,
                    stats.activeUsers * 0.8f,
                    stats.activeUsers * 0.9f,
                    stats.activeUsers * 1.0f,
                    stats.activeUsers * 0.85f,
                    stats.activeUsers * 0.95f,
                    stats.activeUsers * 1.1f
                )
                val labels = listOf(
                    stringResource(R.string.admin_chart_weekday_mon),
                    stringResource(R.string.admin_chart_weekday_tue),
                    stringResource(R.string.admin_chart_weekday_wed),
                    stringResource(R.string.admin_chart_weekday_thu),
                    stringResource(R.string.admin_chart_weekday_fri),
                    stringResource(R.string.admin_chart_weekday_sat),
                    stringResource(R.string.admin_chart_weekday_sun)
                )

                EngagementLineChart(
                    dataPoints = dataPoints,
                    labels = labels,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Quiz Distribution
// ---------------------------------------------------------------------------

/**
 * Section with a horizontal bar chart showing quiz counts by status.
 *
 * @param stats  Current [SystemStats].
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun QuizDistributionSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
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
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalBarChart(
                items = listOf(
                    BarChartItem(
                        label = stringResource(R.string.admin_public_label),
                        value = stats.publicQuizzes,
                        color = primaryColor
                    ),
                    BarChartItem(
                        label = stringResource(R.string.admin_private_label),
                        value = stats.privateQuizzes,
                        color = secondaryColor
                    ),
                    BarChartItem(
                        label = stringResource(R.string.admin_deleted_label),
                        value = stats.deletedQuizzes,
                        color = errorColor
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 5. User Engagement Ring
// ---------------------------------------------------------------------------

/**
 * Section with a donut ring chart showing active-user percentage and
 * supplementary text information.
 *
 * @param stats  Current [SystemStats].
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun UserEngagementSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_user_engagement),
                fontFamily = PlayfairDisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                EngagementRingChart(
                    percentage = stats.activeUserPercentage.toFloat(),
                    centerLabel = stringResource(R.string.admin_active_ratio),
                    centerValue = String.format(Locale.US, "%.0f%%", stats.activeUserPercentage),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.admin_active_ratio),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(
                            R.string.admin_active_of_total,
                            stats.activeUsers,
                            stats.totalUsers
                        ),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Small supplementary stat chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            label = stringResource(R.string.admin_stat_active_users),
                            value = stats.activeUsers.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                        )
                        StatChip(
                            label = stringResource(R.string.admin_stat_total_users),
                            value = stats.totalUsers.toString(),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. Insights
// ---------------------------------------------------------------------------

/**
 * Section containing four [AdminInsightCard]s with data-driven descriptions.
 *
 * @param stats  Current [SystemStats].
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun InsightsSection(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(text = stringResource(R.string.admin_ai_insights))

        AdminInsightCard(
            title = stringResource(R.string.admin_insight_engagement_title),
            description = stringResource(
                R.string.admin_insight_engagement_desc,
                String.format("%.1f", stats.averageAttemptsPerQuiz)
            ),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )

        AdminInsightCard(
            title = stringResource(R.string.admin_insight_growth_title),
            description = stringResource(
                R.string.admin_insight_growth_desc,
                stats.activeUsers.toString(),
                String.format("%.0f", stats.activeUserPercentage)
            ),
            icon = Icons.Default.Group,
            accentColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth()
        )

        AdminInsightCard(
            title = stringResource(R.string.admin_insight_content_title),
            description = stringResource(
                R.string.admin_insight_content_desc,
                stats.publicQuizzes.toString()
            ),
            icon = Icons.Default.Public,
            accentColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth()
        )

        AdminInsightCard(
            title = stringResource(R.string.admin_insight_community_title),
            description = stringResource(
                R.string.admin_insight_community_desc,
                stats.totalQuestionsInPool.toString()
            ),
            icon = Icons.Default.Folder,
            accentColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------------------------------------------------------------------
// 7. Quick Actions
// ---------------------------------------------------------------------------

/**
 * Section with elevated navigation tiles for common admin operations.
 *
 * Tiles are shown conditionally based on the current admin's permissions.
 * If no permission flags are set the entire section is hidden.
 *
 * @param onNavigateToUsers   Callback for the "Manage Users" action.
 * @param onNavigateToQuizzes Callback for the "Manage Quizzes" action.
 * @param onNavigateToReports Callback for the "View Reports" action.
 * @param hasManageUsers      Whether the "Manage Users" tile should be visible.
 * @param hasManageQuizzes    Whether the "Manage Quizzes" tile should be visible.
 * @param hasViewReports      Whether the "View Reports" tile should be visible.
 * @param modifier            Modifier for external layout customisation.
 */
@Composable
private fun QuickActionsSection(
    onNavigateToUsers: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToReports: () -> Unit,
    hasManageUsers: Boolean,
    hasManageQuizzes: Boolean,
    hasViewReports: Boolean,
    modifier: Modifier = Modifier
) {
    // Hide the entire section if the admin has no navigation permissions.
    if (!hasManageUsers && !hasManageQuizzes && !hasViewReports) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(text = stringResource(R.string.admin_quick_actions))

        if (hasManageUsers) {
            QuickActionCard(
                title = stringResource(R.string.admin_manage_users),
                description = stringResource(R.string.admin_action_manage_users_desc),
                icon = Icons.Default.People,
                onClick = onNavigateToUsers,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (hasManageQuizzes) {
            QuickActionCard(
                title = stringResource(R.string.admin_manage_quizzes),
                description = stringResource(R.string.admin_action_manage_quizzes_desc),
                icon = Icons.Default.Quiz,
                onClick = onNavigateToQuizzes,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (hasViewReports) {
            QuickActionCard(
                title = stringResource(R.string.admin_view_reports),
                description = stringResource(R.string.admin_action_view_reports_desc),
                icon = Icons.Default.Assessment,
                onClick = onNavigateToReports,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared helper composables
// ---------------------------------------------------------------------------

/**
 * Consistent section heading used across all dashboard sections.
 *
 * @param text     The heading string to display.
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

/**
 * Small pill-shaped badge displaying a trend percentage such as "+12%".
 *
 * @param text     Formatted trend string.
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun TrendBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Tiny labelled chip used inside the engagement section to show a key
 * metric alongside its colour indicator.
 *
 * @param label    Short descriptor.
 * @param value    Numeric string to display.
 * @param color    Colour dot for visual association.
 * @param modifier Modifier for external layout customisation.
 */
@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            text = value,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Elevated navigation card for quick-action items. Displays an icon,
 * title, description, and a trailing chevron arrow.
 *
 * @param title       Action title.
 * @param description Short explanatory text.
 * @param icon        Leading icon.
 * @param onClick     Callback invoked on tap.
 * @param modifier    Modifier for external layout customisation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val previewStats = SystemStats(
    totalUsers = 1234,
    activeUsers = 567,
    totalQuizzes = 890,
    publicQuizzes = 456,
    privateQuizzes = 234,
    deletedQuizzes = 45,
    totalAttempts = 6789,
    totalQuestionsInPool = 312,
    adminUsers = 3
)

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "AdminDashboard - Light"
)
@Composable
private fun AdminDashboardContentLightPreview() {
    QuizzezTheme(darkTheme = false) {
        DashboardContent(
            stats = previewStats,
            onNavigateToUsers = {},
            onNavigateToQuizzes = {},
            onNavigateToReports = {},
            hasManageUsers = true,
            hasManageQuizzes = true,
            hasViewReports = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "AdminDashboard - Dark"
)
@Composable
private fun AdminDashboardContentDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        DashboardContent(
            stats = previewStats,
            onNavigateToUsers = {},
            onNavigateToQuizzes = {},
            onNavigateToReports = {},
            hasManageUsers = true,
            hasManageQuizzes = true,
            hasViewReports = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}
