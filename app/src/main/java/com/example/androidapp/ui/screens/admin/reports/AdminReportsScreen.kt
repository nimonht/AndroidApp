package com.example.androidapp.ui.screens.admin.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.ui.components.admin.StatisticCard
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Admin reports screen displaying detailed statistics and analytics.
 *
 * @param viewModel The ViewModel for managing reports state.
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
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
                        text = stringResource(R.string.admin_view_reports),
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
        ) {
            when {
                uiState.isLoading -> {
                    LoadingSpinner(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
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

@Composable
private fun ReportsContent(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // User Analytics Section
        Text(
            text = stringResource(R.string.admin_reports_user_analytics),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatisticCard(
                    title = stringResource(R.string.admin_stat_total_users),
                    value = stats.totalUsers.toString(),
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = stringResource(R.string.admin_stat_active_users),
                    value = stats.activeUsers.toString(),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            ReportCard(
                title = stringResource(R.string.admin_stat_active_user_percentage),
                value = String.format("%.1f%%", stats.activeUserPercentage),
                description = stringResource(R.string.admin_reports_active_user_desc),
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        // Content Analytics Section
        Text(
            text = stringResource(R.string.admin_reports_content_analytics),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatisticCard(
                    title = stringResource(R.string.admin_stat_total_quizzes),
                    value = stats.totalQuizzes.toString(),
                    icon = Icons.Default.Quiz,
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = stringResource(R.string.admin_stat_public_quizzes),
                    value = stats.publicQuizzes.toString(),
                    icon = Icons.Default.Public,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatisticCard(
                    title = stringResource(R.string.admin_stat_private_quizzes),
                    value = stats.privateQuizzes.toString(),
                    icon = Icons.Default.Lock,
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = stringResource(R.string.admin_stat_draft_quizzes),
                    value = stats.draftQuizzes.toString(),
                    icon = Icons.Default.Edit,
                    modifier = Modifier.weight(1f)
                )
            }

            ReportCard(
                title = stringResource(R.string.admin_stat_public_quiz_percentage),
                value = String.format("%.1f%%", stats.publicQuizPercentage),
                description = stringResource(R.string.admin_reports_public_quiz_desc),
                icon = Icons.Default.BarChart,
                color = MaterialTheme.colorScheme.secondary
            )

            StatisticCard(
                title = stringResource(R.string.admin_stat_total_questions),
                value = stats.totalQuestionsInPool.toString(),
                icon = Icons.Default.QuestionAnswer,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()

        // Engagement Analytics Section
        Text(
            text = stringResource(R.string.admin_reports_engagement_analytics),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatisticCard(
                title = stringResource(R.string.admin_stat_total_attempts),
                value = stats.totalAttempts.toString(),
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.fillMaxWidth()
            )

            ReportCard(
                title = stringResource(R.string.admin_stat_avg_attempts_per_quiz),
                value = String.format("%.1f", stats.averageAttemptsPerQuiz),
                description = stringResource(R.string.admin_reports_avg_attempts_desc),
                icon = Icons.Default.Analytics,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        HorizontalDivider()

        // Community Content Section
        Text(
            text = stringResource(R.string.admin_reports_community_content),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatisticCard(
                title = stringResource(R.string.admin_stat_question_pool),
                value = stats.totalQuestionsInPool.toString(),
                icon = Icons.Default.Folder,
                modifier = Modifier.fillMaxWidth()
            )

            ReportCard(
                title = stringResource(R.string.admin_stat_deleted_quizzes),
                value = stats.deletedQuizzes.toString(),
                description = stringResource(R.string.admin_reports_deleted_quizzes_desc),
                icon = Icons.Default.DeleteOutline,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ReportCard(
    title: String,
    value: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = color
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = value,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = color
                )

                Text(
                    text = description,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportsContentPreview() {
    QuizzezTheme {
        ReportsContent(
            stats = SystemStats(
                totalUsers = 1234,
                activeUsers = 567,
                totalQuizzes = 890,
                publicQuizzes = 456,
                privateQuizzes = 434,
                draftQuizzes = 123,
                deletedQuizzes = 45,
                totalAttempts = 67890,
                totalQuestionsInPool = 234
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}
