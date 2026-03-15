package com.example.androidapp.ui.screens.attempt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.example.androidapp.ui.theme.FullShape
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import java.util.concurrent.TimeUnit

/**
 * Attempt Detail screen showing detailed results of a single attempt.
 * Stateless composable; all state is owned by [AttemptDetailViewModel].
 *
 * @param attemptId The ID of the attempt to display.
 * @param onNavigateBack Callback to navigate back.
 * @param onReviewAnswers Callback to review answers for this attempt.
 * @param onRetryQuiz Callback to retry the quiz.
 * @param modifier Modifier for styling.
 */
@Composable
fun AttemptDetailScreen(
    attemptId: String,
    onNavigateBack: () -> Unit,
    onReviewAnswers: (quizId: String, attemptId: String) -> Unit,
    onRetryQuiz: (quizId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: AttemptDetailViewModel = viewModel(
        key = "attempt_detail_$attemptId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AttemptDetailViewModel(
                    attemptId,
                    container.attemptRepository,
                    container.quizRepository
                ) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.attempt_detail_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is AttemptDetailUiState.Loading -> LoadingSpinner(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            is AttemptDetailUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.onRetry() },
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            is AttemptDetailUiState.Success -> AttemptDetailContent(
                state = state,
                onReviewAnswers = { onReviewAnswers(state.attempt.quizId, state.attempt.id) },
                onRetryQuiz = { onRetryQuiz(state.attempt.quizId) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun AttemptDetailContent(
    state: AttemptDetailUiState.Success,
    onReviewAnswers: () -> Unit,
    onRetryQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(state.timeTakenMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(state.timeTakenMs) % 60
    val timeTakenStr = "%d:%02d".format(minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Overline
        Text(
            text = stringResource(R.string.attempt_detail_overline),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quiz title
        Text(
            text = state.quiz.title,
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Feedback phrase
        Text(
            text = when {
                state.percentage >= 80 -> stringResource(R.string.quiz_result_feedback_high)
                state.percentage >= 60 -> stringResource(R.string.quiz_result_feedback_medium)
                state.percentage >= 40 -> stringResource(R.string.quiz_result_feedback_low)
                else -> stringResource(R.string.quiz_result_feedback_min)
            },
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Italic,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Score display
        Text(
            text = stringResource(R.string.score_format, state.attempt.score, state.attempt.totalQuestions),
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 48.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.percentage_format, state.percentage),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                icon = Icons.Default.Check,
                value = state.attempt.score.toString(),
                label = stringResource(R.string.quiz_result_stat_correct),
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier.height(64.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            StatColumn(
                icon = Icons.Default.Close,
                value = (state.attempt.totalQuestions - state.attempt.score).toString(),
                label = stringResource(R.string.quiz_result_stat_wrong),
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier.height(64.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            StatColumn(
                icon = Icons.Default.Timer,
                value = timeTakenStr,
                label = stringResource(R.string.quiz_result_stat_time),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onReviewAnswers,
                modifier = Modifier.fillMaxWidth(),
                shape = FullShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.attempt_detail_review_button),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            OutlinedButton(
                onClick = onRetryQuiz,
                modifier = Modifier.fillMaxWidth(),
                shape = FullShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.attempt_detail_retry_button),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
