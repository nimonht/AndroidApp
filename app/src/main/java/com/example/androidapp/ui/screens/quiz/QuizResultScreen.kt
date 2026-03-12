package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.theme.FullShape
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import java.util.concurrent.TimeUnit

/**
 * Quiz result screen — Editorial Minimalist design.
 * Stateless composable; all state is owned by [QuizResultViewModel].
 *
 * @param quizId The ID of the completed quiz.
 * @param attemptId The ID of the attempt (for loading results).
 * @param onNavigateHome Callback to go back to home.
 * @param onRetryQuiz Callback to retry the quiz.
 * @param onReviewAnswers Callback to review answers.
 * @param modifier Modifier for styling.
 */
@Composable
fun QuizResultScreen(
    quizId: String,
    attemptId: String,
    onNavigateHome: () -> Unit,
    onRetryQuiz: () -> Unit,
    onReviewAnswers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: QuizResultViewModel = viewModel(
        key = "result_${attemptId}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuizResultViewModel(quizId, attemptId, container.quizRepository, container.attemptRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is QuizResultUiState.Loading -> LoadingSpinner(modifier = Modifier.fillMaxSize())
            is QuizResultUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.onRetry() },
                modifier = Modifier.fillMaxSize()
            )
            is QuizResultUiState.Success -> ResultContent(
                quiz = state.quiz,
                attempt = state.attempt,
                percentage = state.percentage,
                onNavigateHome = onNavigateHome,
                onRetryQuiz = onRetryQuiz,
                onReviewAnswers = onReviewAnswers,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ResultContent(
    quiz: Quiz,
    attempt: Attempt,
    percentage: Int,
    onNavigateHome: () -> Unit,
    onRetryQuiz: () -> Unit,
    onReviewAnswers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeTakenMs = if (attempt.endTimeMillis != null)
        attempt.endTimeMillis - attempt.startTimeMillis
    else 0L
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeTakenMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeTakenMs) % 60
    val timeTakenStr = "%d:%02d".format(minutes, seconds)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // ── Overline label ─────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.quiz_result_performance_overline),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Score display ─────────────────────────────────────────────────
        val scoreAnnotated = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 72.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            ) {
                append("%02d".format(attempt.score))
            }
            withStyle(
                SpanStyle(
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 60.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                append(" / ")
            }
            withStyle(
                SpanStyle(
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 72.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            ) {
                append("%02d".format(attempt.totalQuestions))
            }
        }
        Text(text = scoreAnnotated, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(20.dp))

        // ── Decorative accent divider ──────────────────────────────────────
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Italic feedback phrase ─────────────────────────────────────────
        Text(
            text = when {
                percentage >= 80 -> stringResource(R.string.quiz_result_feedback_high)
                percentage >= 60 -> stringResource(R.string.quiz_result_feedback_medium)
                percentage >= 40 -> stringResource(R.string.quiz_result_feedback_low)
                else -> stringResource(R.string.quiz_result_feedback_min)
            },
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = quiz.title,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Stats grid ────────────────────────────────────────────────────
        ResultStatsGrid(
            correctCount = attempt.score,
            wrongCount = attempt.totalQuestions - attempt.score,
            timeTaken = timeTakenStr,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Action buttons ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth(),
                shape = FullShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.quiz_result_go_home),
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
                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.quiz_result_try_again),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            TextButton(
                onClick = onReviewAnswers,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.quiz_result_review_answers),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Footer watermark ──────────────────────────────────────────────
        Text(
            text = stringResource(R.string.quiz_result_watermark),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.outlineVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun ResultStatsGrid(
    correctCount: Int,
    wrongCount: Int,
    timeTaken: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ResultStatColumn(
            icon = Icons.Default.Check,
            value = correctCount.toString(),
            label = stringResource(R.string.quiz_result_stat_correct),
            modifier = Modifier.weight(1f)
        )

        VerticalDivider(
            modifier = Modifier.height(64.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        ResultStatColumn(
            icon = Icons.Default.Close,
            value = wrongCount.toString(),
            label = stringResource(R.string.quiz_result_stat_wrong),
            modifier = Modifier.weight(1f)
        )

        VerticalDivider(
            modifier = Modifier.height(64.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        ResultStatColumn(
            icon = Icons.Default.Timer,
            value = timeTaken,
            label = stringResource(R.string.quiz_result_stat_time),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ResultStatColumn(
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

