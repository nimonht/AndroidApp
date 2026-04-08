package com.example.androidapp.ui.screens.advanced.console.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.ui.theme.QuizzezTheme

// -- ColorScheme extensions for suggestion icon tints -------------------------

/** Blue tint for command suggestion icons. */
val ColorScheme.suggestionCommand: Color
    get() = Color(0xFF42A5F5)

/** Green tint for subcommand suggestion icons. */
val ColorScheme.suggestionSubcommand: Color
    get() = Color(0xFF66BB6A)

/** Amber tint for flag suggestion icons. */
val ColorScheme.suggestionFlag: Color
    get() = Color(0xFFFFC107)

/** Purple tint for user suggestion icons. */
val ColorScheme.suggestionUser: Color
    get() = Color(0xFFAB47BC)

/** Orange tint for quiz suggestion icons. */
val ColorScheme.suggestionQuiz: Color
    get() = Color(0xFFFF7043)

/** Teal tint for tag suggestion icons. */
val ColorScheme.suggestionTag: Color
    get() = Color(0xFF26A69A)

/**
 * Maximum number of suggestions visible in the dropdown at once.
 */
private const val MAX_VISIBLE_SUGGESTIONS = 8

/**
 * Autocomplete suggestion dropdown displayed above the console input field.
 *
 * Shows up to [MAX_VISIBLE_SUGGESTIONS] items, each with an icon determined
 * by [SuggestionType], the suggestion display text, and an optional description.
 * The currently selected item is highlighted with a distinct background.
 *
 * @param suggestions The list of autocomplete suggestions to display.
 * @param selectedIndex The index of the currently highlighted suggestion, or -1 if none.
 * @param onSelect Callback invoked when the user taps a suggestion, with the item's index.
 * @param onDismiss Callback invoked when the dropdown should be dismissed.
 * @param modifier Modifier for external styling and layout.
 */
@Composable
fun SuggestionDropdown(
    suggestions: List<CompletionSuggestion>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    BackHandler(enabled = true, onBack = onDismiss)

    val visibleSuggestions = suggestions.take(MAX_VISIBLE_SUGGESTIONS)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = (visibleSuggestions.size * 48).dp)
        ) {
            itemsIndexed(
                items = visibleSuggestions,
                key = { index, suggestion -> "${index}_${suggestion.text}" }
            ) { index, suggestion ->
                SuggestionItem(
                    suggestion = suggestion,
                    isSelected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * A single row inside the [SuggestionDropdown].
 *
 * Displays an icon representing the [SuggestionType], the suggestion's
 * display text in monospace font, and an optional description muted to
 * the right.
 *
 * @param suggestion The completion suggestion data to render.
 * @param isSelected Whether this item is currently highlighted/selected.
 * @param onClick Callback when the item is tapped.
 * @param modifier Modifier for external styling and layout.
 */
@Composable
private fun SuggestionItem(
    suggestion: CompletionSuggestion,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .background(backgroundColor)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Type icon
        val (icon, iconTint) = suggestionTypeVisuals(suggestion.type)
        Icon(
            imageVector = icon,
            contentDescription = suggestion.type.name,
            modifier = Modifier.size(16.dp),
            tint = iconTint
        )

        // Display text
        Text(
            text = suggestion.displayText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        // Description (if present)
        if (suggestion.description.isNotEmpty()) {
            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Returns the icon and tint color for a given [SuggestionType].
 *
 * Colors are resolved from [ColorScheme] extension properties so they
 * participate in theming and remain consistent with other console components.
 *
 * @param type The suggestion type to resolve visuals for.
 * @return A pair of (ImageVector, Color) for the icon and its tint.
 */
@Composable
private fun suggestionTypeVisuals(type: SuggestionType): Pair<ImageVector, Color> {
    val colorScheme = MaterialTheme.colorScheme
    return when (type) {
        SuggestionType.COMMAND -> Icons.Filled.Terminal to colorScheme.suggestionCommand
        SuggestionType.SUBCOMMAND -> Icons.Filled.Segment to colorScheme.suggestionSubcommand
        SuggestionType.FLAG -> Icons.Filled.Flag to colorScheme.suggestionFlag
        SuggestionType.ARGUMENT -> Icons.Filled.Code to colorScheme.onSurfaceVariant
        SuggestionType.USER -> Icons.Filled.Person to colorScheme.suggestionUser
        SuggestionType.QUIZ -> Icons.Filled.Quiz to colorScheme.suggestionQuiz
        SuggestionType.TAG -> Icons.AutoMirrored.Filled.Label to colorScheme.suggestionTag
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "SuggestionDropdown - Light", showBackground = true)
@Preview(
    name = "SuggestionDropdown - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SuggestionDropdownPreview() {
    val sampleSuggestions = listOf(
        CompletionSuggestion(
            text = "help",
            displayText = "help",
            description = "Hien thi danh sach lenh",
            type = SuggestionType.COMMAND
        ),
        CompletionSuggestion(
            text = "history",
            displayText = "history",
            description = "Xem lich su lam bai",
            type = SuggestionType.COMMAND
        ),
        CompletionSuggestion(
            text = "--verbose",
            displayText = "--verbose",
            description = "Hien thi chi tiet",
            type = SuggestionType.FLAG
        ),
        CompletionSuggestion(
            text = "user@example.com",
            displayText = "user@example.com",
            description = "Nguoi dung",
            type = SuggestionType.USER
        ),
        CompletionSuggestion(
            text = "quiz-123",
            displayText = "quiz-123",
            description = "Bai kiem tra",
            type = SuggestionType.QUIZ
        )
    )

    QuizzezTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SuggestionDropdown(
                suggestions = sampleSuggestions,
                selectedIndex = 1,
                onSelect = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "SuggestionDropdown Empty - Light", showBackground = true)
@Preview(
    name = "SuggestionDropdown Empty - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SuggestionDropdownEmptyPreview() {
    QuizzezTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SuggestionDropdown(
                suggestions = emptyList(),
                selectedIndex = -1,
                onSelect = {},
                onDismiss = {}
            )
        }
    }
}
