package com.example.androidapp.ui.screens.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.forms.SwitchToggle
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.theme.QuizzezTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared quiz form body used by both [CreateQuizScreen] and [EditQuizScreen].
 *
 * Contains the title, thumbnail URL, description, tags, visibility toggles,
 * and the question editor card list inside a [LazyColumn]. This composable is
 * purely presentational and does not access any ViewModel directly.
 *
 * @param title Current quiz title.
 * @param onTitleChange Callback when the title changes.
 * @param thumbnailUrl Current thumbnail URL.
 * @param onThumbnailUrlChange Callback when the thumbnail URL changes.
 * @param description Current quiz description.
 * @param onDescriptionChange Callback when the description changes.
 * @param tags Raw comma-separated tags string.
 * @param onTagsChange Callback when the tags string changes.
 * @param onShowTagSuggestions Callback to open the tag suggestion dialog.
 * @param isPublic Whether the quiz is publicly visible.
 * @param onPublicToggle Callback when the public toggle changes.
 * @param shareToPool Whether to share questions to the community pool.
 * @param onShareToPoolToggle Callback when the share-to-pool toggle changes.
 * @param questions List of question drafts currently in the form.
 * @param onUpdateQuestion Callback to update a question at a given index.
 * @param onMoveQuestionUp Callback to move a question one position up.
 * @param onMoveQuestionDown Callback to move a question one position down.
 * @param onRemoveQuestion Callback to remove a question at a given index.
 * @param lastSavedAt Timestamp of the last draft save, or null if never saved.
 * @param modifier Modifier for styling.
 * @param questionsHeaderTrailingContent Optional trailing content rendered inside the
 *   questions section header [Row] (e.g. an "Add from Pool" button).
 */
@Composable
fun QuizFormContent(
    title: String,
    onTitleChange: (String) -> Unit,
    thumbnailUrl: String,
    onThumbnailUrlChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    onShowTagSuggestions: () -> Unit,
    isPublic: Boolean,
    onPublicToggle: (Boolean) -> Unit,
    shareToPool: Boolean,
    onShareToPoolToggle: (Boolean) -> Unit,
    questions: List<QuestionDraft>,
    onUpdateQuestion: (Int, QuestionDraft) -> Unit,
    onMoveQuestionUp: (Int) -> Unit,
    onMoveQuestionDown: (Int) -> Unit,
    onRemoveQuestion: (Int) -> Unit,
    lastSavedAt: Long?,
    modifier: Modifier = Modifier,
    questionsHeaderTrailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Last-saved indicator
        item {
            lastSavedAt?.let { savedAt ->
                val formatted = remember(savedAt) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(savedAt))
                }
                Text(
                    text = stringResource(R.string.create_last_saved, formatted),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Title
        item {
            TextInputField(
                value = title,
                onValueChange = onTitleChange,
                label = stringResource(R.string.create_quiz_title_label),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Thumbnail URL
        item {
            TextInputField(
                value = thumbnailUrl,
                onValueChange = onThumbnailUrlChange,
                label = stringResource(R.string.create_quiz_thumbnail_url_label),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Description
        item {
            TextInputField(
                value = description,
                onValueChange = onDescriptionChange,
                label = stringResource(R.string.create_quiz_description_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                singleLine = false
            )
        }

        // Tags
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextInputField(
                    value = tags,
                    onValueChange = onTagsChange,
                    label = stringResource(R.string.create_quiz_tags_label),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilledTonalIconButton(
                    onClick = onShowTagSuggestions
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = stringResource(R.string.create_quiz_pick_tags)
                    )
                }
            }
        }

        // Public toggle
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.create_quiz_public),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isPublic,
                        onCheckedChange = onPublicToggle
                    )
                }
                if (isPublic) {
                    Text(
                        text = stringResource(R.string.create_quiz_public_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                }
            }
        }

        // Share to pool toggle
        item {
            SwitchToggle(
                checked = shareToPool,
                onCheckedChange = onShareToPoolToggle,
                label = stringResource(R.string.create_quiz_share_to_pool),
                description = stringResource(R.string.create_quiz_share_to_pool_desc),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Section header
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.create_questions_header, questions.size),
                    style = MaterialTheme.typography.titleMedium
                )
                if (questionsHeaderTrailingContent != null) {
                    questionsHeaderTrailingContent()
                }
            }
        }

        // Question cards
        itemsIndexed(questions) { index, question ->
            QuestionEditorCard(
                questionNumber = index + 1,
                question = question,
                totalQuestions = questions.size,
                onQuestionChange = { updated ->
                    onUpdateQuestion(index, updated)
                },
                onMoveUp = { onMoveQuestionUp(index) },
                onMoveDown = { onMoveQuestionDown(index) },
                onRemove = { onRemoveQuestion(index) }
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "QuizFormContent - Light", showBackground = true)
@Composable
private fun QuizFormContentLightPreview() {
    QuizzezTheme(darkTheme = false) {
        QuizFormContent(
            title = "Lich su Viet Nam",
            onTitleChange = {},
            thumbnailUrl = "",
            onThumbnailUrlChange = {},
            description = "Bo cau hoi ve lich su Viet Nam",
            onDescriptionChange = {},
            tags = "lich su, viet nam",
            onTagsChange = {},
            onShowTagSuggestions = {},
            isPublic = true,
            onPublicToggle = {},
            shareToPool = false,
            onShareToPoolToggle = {},
            questions = listOf(
                QuestionDraft(
                    content = "Thu do cua Viet Nam la gi?",
                    choices = listOf(
                        ChoiceDraft(content = "Ha Noi"),
                        ChoiceDraft(content = "Ho Chi Minh"),
                        ChoiceDraft(content = "Da Nang"),
                        ChoiceDraft(content = "Hue")
                    ),
                    correctIndices = setOf(0)
                ),
                QuestionDraft(
                    content = "Viet Nam doc lap nam nao?",
                    choices = listOf(
                        ChoiceDraft(content = "1945"),
                        ChoiceDraft(content = "1954"),
                        ChoiceDraft(content = "1975"),
                        ChoiceDraft(content = "1930")
                    ),
                    correctIndices = setOf(0)
                )
            ),
            onUpdateQuestion = { _, _ -> },
            onMoveQuestionUp = {},
            onMoveQuestionDown = {},
            onRemoveQuestion = {},
            lastSavedAt = System.currentTimeMillis()
        )
    }
}

@Preview(name = "QuizFormContent - Dark", showBackground = true)
@Composable
private fun QuizFormContentDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        QuizFormContent(
            title = "",
            onTitleChange = {},
            thumbnailUrl = "",
            onThumbnailUrlChange = {},
            description = "",
            onDescriptionChange = {},
            tags = "",
            onTagsChange = {},
            onShowTagSuggestions = {},
            isPublic = false,
            onPublicToggle = {},
            shareToPool = false,
            onShareToPoolToggle = {},
            questions = listOf(QuestionDraft()),
            onUpdateQuestion = { _, _ -> },
            onMoveQuestionUp = {},
            onMoveQuestionDown = {},
            onRemoveQuestion = {},
            lastSavedAt = null
        )
    }
}
