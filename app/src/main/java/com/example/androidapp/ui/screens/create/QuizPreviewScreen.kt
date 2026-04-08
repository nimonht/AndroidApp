package com.example.androidapp.ui.screens.create

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.common.TagChip
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.example.androidapp.ui.theme.QuizzezTheme

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * Quiz Preview screen that displays all quiz metadata and questions in read-only mode.
 *
 * Loads the quiz and questions from [QuizPreviewViewModel] by [quizId].
 * Correct answers are highlighted in green so the creator can verify them before publishing.
 *
 * @param quizId The ID of the quiz to preview.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onPublish Callback invoked when the user taps the publish button.
 *   Only shown when the quiz is not yet public.
 * @param modifier Modifier for styling.
 */
@Composable
fun QuizPreviewScreen(
    quizId: String,
    onNavigateBack: () -> Unit,
    onPublish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: QuizPreviewViewModel = viewModel(
        key = "preview_$quizId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuizPreviewViewModel(quizId, container.quizRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.quiz_preview_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is QuizPreviewUiState.Loading -> {
                LoadingSpinner(
                    modifier = Modifier.padding(innerPadding),
                    message = stringResource(R.string.loading)
                )
            }

            is QuizPreviewUiState.Error -> {
                ErrorState(
                    message = state.error.toMessage(state.errorDetail),
                    onRetry = viewModel::onRetry,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is QuizPreviewUiState.Success -> {
                QuizPreviewContent(
                    quiz = state.quiz,
                    questions = state.questions,
                    onPublish = onPublish,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------

/**
 * Stateless content for the Quiz Preview screen.
 *
 * Renders quiz header (title, description, badges, tags) followed by
 * individual read-only question cards and a bottom publish/status button.
 *
 * @param quiz The quiz metadata to display.
 * @param questions The ordered list of questions to render.
 * @param onPublish Callback invoked when the user taps the publish button.
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuizPreviewContent(
    quiz: Quiz,
    questions: List<Question>,
    onPublish: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // ---- Quiz header card ----
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Description
                    if (!quiz.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = quiz.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badges row: public/private + question count
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VisibilityBadge(isPublic = quiz.isPublic)
                        Text(
                            text = stringResource(R.string.quiz_preview_question_count, questions.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    // Tags
                    if (quiz.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quiz.tags.forEach { tag ->
                                TagChip(text = tag)
                            }
                        }
                    }
                }
            }
        }

        // ---- Question cards ----
        itemsIndexed(questions) { index, question ->
            QuestionPreviewCard(
                questionNumber = index + 1,
                question = question
            )
        }

        // ---- Publish / Already-published button ----
        item {
            Spacer(modifier = Modifier.height(4.dp))
            if (quiz.isPublic) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.quiz_preview_already_published))
                }
            } else {
                Button(
                    onClick = onPublish,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.quiz_preview_publish_button))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Visibility badge
// ---------------------------------------------------------------------------

/**
 * Small pill badge indicating whether a quiz is public or private.
 *
 * @param isPublic Whether the quiz is publicly visible.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun VisibilityBadge(
    isPublic: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isPublic) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isPublic) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = if (isPublic) {
        stringResource(R.string.quiz_preview_public_badge)
    } else {
        stringResource(R.string.quiz_preview_private_badge)
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

// ---------------------------------------------------------------------------
// Question preview card
// ---------------------------------------------------------------------------

/**
 * Read-only card rendering a single [Question] with all its choices.
 *
 * Correct choices are highlighted with a green tinted background and a check icon.
 * If the question has an explanation it is shown at the bottom of the card.
 *
 * @param questionNumber 1-based display number for this question.
 * @param question The domain question to render.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun QuestionPreviewCard(
    questionNumber: Int,
    question: Question,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Question number + content
            Text(
                text = stringResource(R.string.create_question_title, questionNumber),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = question.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Choices
            if (question.choices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.choices
                        .sortedBy { it.position }
                        .forEach { choice ->
                            ChoicePreviewRow(choice = choice)
                        }
                }
            }

            // Explanation
            if (!question.explanation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.quiz_preview_explanation_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Choice preview row
// ---------------------------------------------------------------------------

/**
 * Renders a single [Choice] row in read-only mode.
 *
 * Correct choices get a green-tinted background, a check icon, and a
 * "Đúng" label. Incorrect choices use the standard surface colour.
 *
 * @param choice The choice to render.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun ChoicePreviewRow(
    choice: Choice,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (choice.isCorrect) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (choice.isCorrect) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = choice.content,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        if (choice.isCorrect) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.quiz_preview_correct_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val previewQuiz = Quiz(
    id = "preview-quiz-1",
    ownerId = "user-1",
    title = "Kiểm tra Lịch sử Việt Nam",
    description = "Bài kiểm tra về các sự kiện lịch sử quan trọng của Việt Nam từ thế kỷ XX.",
    authorName = "Nguyen Van A",
    tags = listOf("lịch sử", "việt nam", "thế kỷ xx"),
    isPublic = false,
    questionCount = 2
)

private val previewQuestions = listOf(
    Question(
        id = "q1",
        quizId = "preview-quiz-1",
        content = "Năm nào Việt Nam tuyên bố độc lập?",
        choices = listOf(
            Choice(id = "c1", content = "1945", isCorrect = true, position = 0),
            Choice(id = "c2", content = "1954", isCorrect = false, position = 1),
            Choice(id = "c3", content = "1975", isCorrect = false, position = 2),
            Choice(id = "c4", content = "1930", isCorrect = false, position = 3)
        ),
        explanation = "Ngày 2 tháng 9 năm 1945, Chủ tịch Hồ Chí Minh đọc Tuyên ngôn Độc lập.",
        position = 0
    ),
    Question(
        id = "q2",
        quizId = "preview-quiz-1",
        content = "Chiến dịch nào kết thúc chiến tranh giải phóng miền Nam?",
        choices = listOf(
            Choice(id = "c5", content = "Chiến dịch Điện Biên Phủ", isCorrect = false, position = 0),
            Choice(id = "c6", content = "Chiến dịch Hồ Chí Minh", isCorrect = true, position = 1),
            Choice(id = "c7", content = "Chiến dịch Mậu Thân", isCorrect = false, position = 2),
            Choice(id = "c8", content = "Chiến dịch Tây Nguyên", isCorrect = false, position = 3)
        ),
        position = 1
    )
)

@Preview(showBackground = true, name = "Quiz Preview - Light")
@Composable
private fun QuizPreviewContentLightPreview() {
    QuizzezTheme {
        Surface {
            QuizPreviewContent(
                quiz = previewQuiz,
                questions = previewQuestions,
                onPublish = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Quiz Preview - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuizPreviewContentDarkPreview() {
    QuizzezTheme {
        Surface {
            QuizPreviewContent(
                quiz = previewQuiz,
                questions = previewQuestions,
                onPublish = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Quiz Preview - Already Published")
@Composable
private fun QuizPreviewPublishedPreview() {
    QuizzezTheme {
        Surface {
            QuizPreviewContent(
                quiz = previewQuiz.copy(isPublic = true),
                questions = previewQuestions,
                onPublish = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Visibility Badge - Public")
@Composable
private fun VisibilityBadgePublicPreview() {
    QuizzezTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            VisibilityBadge(isPublic = true)
        }
    }
}

@Preview(showBackground = true, name = "Visibility Badge - Private")
@Composable
private fun VisibilityBadgePrivatePreview() {
    QuizzezTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            VisibilityBadge(isPublic = false)
        }
    }
}

@Preview(showBackground = true, name = "Question Preview Card")
@Composable
private fun QuestionPreviewCardPreview() {
    QuizzezTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            QuestionPreviewCard(
                questionNumber = 1,
                question = previewQuestions.first()
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Question Preview Card - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuestionPreviewCardDarkPreview() {
    QuizzezTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            QuestionPreviewCard(
                questionNumber = 1,
                question = previewQuestions.first()
            )
        }
    }
}
