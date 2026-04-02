package com.example.androidapp.ui.screens.admin.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 * Admin dashboard screen displaying system statistics and quick actions.
 *
 * @param viewModel The ViewModel for managing dashboard state.
 * @param onNavigateBack Callback to navigate back.
 * @param onNavigateToUsers Callback to navigate to user management.
 * @param onNavigateToQuizzes Callback to navigate to quiz management.
 * @param onNavigateToReports Callback to navigate to reports.
 * @param modifier Modifier for styling.
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
                    DashboardContent(
                        stats = uiState.stats!!,
                        onNavigateToUsers = onNavigateToUsers,
                        onNavigateToQuizzes = onNavigateToQuizzes,
                        onNavigateToReports = onNavigateToReports,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    stats: SystemStats,
    onNavigateToUsers: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Overview Section
        Text(
            text = stringResource(R.string.admin_overview),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Primary Stats Grid
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
                    title = stringResource(R.string.admin_stat_total_attempts),
                    value = stats.totalAttempts.toString(),
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = stringResource(R.string.admin_stat_deleted_quizzes),
                    value = stats.deletedQuizzes.toString(),
                    icon = Icons.Default.Delete,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calculated Metrics Section
        Text(
            text = stringResource(R.string.admin_insights),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                title = stringResource(R.string.admin_stat_avg_attempts_per_quiz),
                value = String.format("%.1f", stats.averageAttemptsPerQuiz),
                description = "Trung bình số lượt chơi trên mỗi quiz"
            )

            InsightCard(
                title = stringResource(R.string.admin_stat_active_user_percentage),
                value = String.format("%.1f%%", stats.activeUserPercentage),
                description = "Tỷ lệ người dùng hoạt động trong 30 ngày qua"
            )

            InsightCard(
                title = stringResource(R.string.admin_stat_public_quiz_percentage),
                value = String.format("%.1f%%", stats.publicQuizPercentage),
                description = "Tỷ lệ quiz công khai so với tổng quiz"
            )
        }

        // Quick Actions Section
        Text(
            text = stringResource(R.string.admin_quick_actions),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = stringResource(R.string.admin_manage_users),
                description = "Quản lý tài khoản, vai trò và trạng thái người dùng",
                icon = Icons.Default.People,
                onClick = onNavigateToUsers
            )

            QuickActionCard(
                title = stringResource(R.string.admin_manage_quizzes),
                description = "Quản lý quiz, xuất bản và xóa nội dung",
                icon = Icons.Default.Quiz,
                onClick = onNavigateToQuizzes
            )

            QuickActionCard(
                title = stringResource(R.string.admin_view_reports),
                description = "Xem báo cáo chi tiết và phân tích hệ thống",
                icon = Icons.Default.Assessment,
                onClick = onNavigateToReports
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InsightCard(
    title: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = description,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    QuizzezTheme {
        DashboardContent(
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
            onNavigateToUsers = {},
            onNavigateToQuizzes = {},
            onNavigateToReports = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
