package com.example.androidapp.ui.components.quiz

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Renders a dynamic list of choice buttons.
 * Supports 2-10 choices per question with optional multi-select mode.
 */
@Composable
fun DynamicChoiceList(
    choices: List<Choice>,              // List of choices (A, B, C...)
    selectedChoiceIds: Set<String>,     // Currently selected IDs (Set to support multi-select)
    allowMultipleCorrect: Boolean = false, // Multi-select mode (True/False)
    onChoiceSelected: (String) -> Unit, // Callback when a choice is tapped
    modifier: Modifier = Modifier
) {
    // 1. Safety check (Validation)
    // If data is invalid (fewer than 2 or more than 10), fail immediately so the developer can fix it
    require(choices.size in 2..10) {
        "Câu hỏi phải có từ 2 đến 10 lựa chọn, nhưng nhận được ${choices.size}"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between buttons
    ) {
        // 2. Show selection counter (only when there are many choices, to avoid overwhelming the user)
        if (choices.size > 4) {
            Text(
                text = stringResource(
                    id = R.string.quiz_choice_count,
                    selectedChoiceIds.size,
                    choices.size
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 3. Loop to create choice buttons
        choices.forEachIndexed { index, choice ->
            // Auto-calculate label: 0->A, 1->B, 2->C... based on ASCII values
            val label = ('A' + index).toString()

            // Check whether this button is currently selected
            val isSelected = choice.id in selectedChoiceIds

            // Render the ChoiceButton component
            ChoiceButton(
                label = label,
                content = choice.content,
                isSelected = isSelected,
                isMultiSelect = allowMultipleCorrect,
                onClick = { onChoiceSelected(choice.id) }
            )
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun DynamicChoiceListPreview() {
    QuizzezTheme {
        DynamicChoiceList(
            choices = listOf(
                Choice(id = "1", content = "Hà Nội", isCorrect = true, position = 0),
                Choice(id = "2", content = "Hồ Chí Minh", isCorrect = false, position = 1),
                Choice(id = "3", content = "Đà Nẵng", isCorrect = false, position = 2),
                Choice(id = "4", content = "Huế", isCorrect = false, position = 3)
            ),
            selectedChoiceIds = setOf("1"),
            allowMultipleCorrect = false,
            onChoiceSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DynamicChoiceListDarkPreview() {
    QuizzezTheme {
        DynamicChoiceList(
            choices = listOf(
                Choice(id = "1", content = "Hà Nội", isCorrect = true, position = 0),
                Choice(id = "2", content = "Hồ Chí Minh", isCorrect = false, position = 1),
                Choice(id = "3", content = "Đà Nẵng", isCorrect = false, position = 2),
                Choice(id = "4", content = "Huế", isCorrect = false, position = 3)
            ),
            selectedChoiceIds = setOf("1"),
            allowMultipleCorrect = false,
            onChoiceSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
