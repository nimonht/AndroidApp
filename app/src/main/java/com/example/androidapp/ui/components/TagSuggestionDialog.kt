package com.example.androidapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizzezTheme

/** Internal keys used by [QuizTags.categories] — not displayed to users. */
private const val CAT_STUDY = "study"
private const val CAT_ENTERTAINMENT = "entertainment"
private const val CAT_GENERAL = "general_knowledge"
private const val CAT_PROFESSIONAL = "professional"

/**
 * Predefined quiz tag categories with their tags.
 * These are the suggested tags available to users when creating or editing quizzes.
 */
object QuizTags {
    /** All available predefined tags grouped by category. */
    val categories: Map<String, List<String>> = linkedMapOf(
        CAT_STUDY to listOf(
            "Toan hoc", "Vat ly", "Hoa hoc", "Sinh hoc",
            "Lich su", "Dia ly", "Ngu van", "Tieng Anh",
            "Tin hoc", "Khoa hoc", "Cong nghe"
        ),
        CAT_ENTERTAINMENT to listOf(
            "Phim anh", "Am nhac", "The thao", "Game",
            "Anime", "Truyen tranh", "Nghe thuat"
        ),
        CAT_GENERAL to listOf(
            "Van hoa", "Xa hoi", "Kinh te", "Phap luat",
            "Y te", "Moi truong", "Am thuc", "Du lich"
        ),
        CAT_PROFESSIONAL to listOf(
            "Lap trinh", "Thiet ke", "Marketing", "Quan ly",
            "Tai chinh", "Y hoc", "Ky thuat", "Ngoai ngu"
        )
    )

    /** Flat list of all available tags. */
    val allTags: List<String> = categories.values.flatten()
}

/**
 * Dialog that shows predefined tag suggestions grouped by category.
 * Users can tap chips to toggle tag selection, then confirm their choices.
 *
 * @param currentTags The tags already selected (from the text input, comma-separated).
 * @param onTagsConfirmed Callback with the updated comma-separated tag string.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSuggestionDialog(
    currentTags: String,
    availableTags: List<String> = emptyList(),
    onTagsConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialSelected = remember(currentTags) {
        currentTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }
    var selectedTags by remember { mutableStateOf(initialSelected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_suggestion_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.tag_suggestion_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val categoryDisplayNames = mapOf(
                    CAT_STUDY to stringResource(R.string.tag_category_study),
                    CAT_ENTERTAINMENT to stringResource(R.string.tag_category_entertainment),
                    CAT_GENERAL to stringResource(R.string.tag_category_general_knowledge),
                    CAT_PROFESSIONAL to stringResource(R.string.tag_category_professional)
                )
                val communityLabel = stringResource(R.string.tag_category_community)

                val otherTags =
                    availableTags.map { it.trim() }.filter { it.isNotBlank() && !QuizTags.allTags.contains(it) }
                        .distinct()
                val displayCategories = QuizTags.categories.toMutableMap()
                if (otherTags.isNotEmpty()) {
                    displayCategories[communityLabel] = otherTags
                }

                displayCategories.forEach { (category, tags) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = categoryDisplayNames[category] ?: category,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.forEach { tag ->
                                val isSelected = tag in selectedTags
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTags = if (isSelected) {
                                            selectedTags - tag
                                        } else {
                                            selectedTags + tag
                                        }
                                    },
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = selectedTags.joinToString(", ")
                    onTagsConfirmed(result)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun TagSuggestionDialogPreview() {
    QuizzezTheme {
        TagSuggestionDialog(
            currentTags = "Toan hoc, Vat ly",
            availableTags = listOf("Toan hoc", "Vat ly", "Hoa hoc", "Lap trinh"),
            onTagsConfirmed = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TagSuggestionDialogDarkPreview() {
    QuizzezTheme {
        TagSuggestionDialog(
            currentTags = "Toan hoc, Vat ly",
            availableTags = listOf("Toan hoc", "Vat ly", "Hoa hoc", "Lap trinh"),
            onTagsConfirmed = {},
            onDismiss = {}
        )
    }
}
