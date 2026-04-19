package com.example.androidapp.ui.components.quiz

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Displays quiz progress as a labeled linear progress bar.
 *
 * @param currentQuestionIndex Zero-based index of the current question.
 * @param totalQuestions Total number of questions in the quiz.
 * @param modifier Modifier for styling and layout customization.
 */
@Composable
fun QuizProgressIndicator(
    currentQuestionIndex: Int,
    totalQuestions: Int,
    modifier: Modifier = Modifier
) {
    val progress = (currentQuestionIndex + 1).toFloat() / totalQuestions.coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    id = R.string.question_progress,
                    currentQuestionIndex + 1,
                    totalQuestions
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun QuizProgressIndicatorPreview() {
    QuizzezTheme {
        QuizProgressIndicator(
            currentQuestionIndex = 4,
            totalQuestions = 10,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizProgressIndicatorDarkPreview() {
    QuizzezTheme {
        QuizProgressIndicator(
            currentQuestionIndex = 4,
            totalQuestions = 10,
            modifier = Modifier.padding(16.dp)
        )
    }
}
