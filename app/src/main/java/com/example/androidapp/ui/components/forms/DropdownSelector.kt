@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.androidapp.ui.components.forms

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizCodeTheme

/**
 * Reusable dropdown selector component for selecting a single option from a list.
 * Uses ExposedDropdownMenuBox for proper Material 3 dropdown behavior.
 *
 * @param label The label for the dropdown field.
 * @param options List of available options to select from.
 * @param selectedOption The currently selected option (null if none selected).
 * @param onOptionSelected Callback when an option is selected.
 * @param modifier Modifier for styling and layout customization.
 * @param enabled Whether the dropdown is enabled.
 * @param placeholder Text to show when no option is selected.
 * @param errorMessage Error message to display (shows error state if not null).
 * @param helperText Helper text to display below the field.
 */
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Chọn một tùy chọn",
    errorMessage: String? = null,
    helperText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedOption ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                enabled = enabled,
                isError = errorMessage != null,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                supportingText = {
                    when {
                        errorMessage != null -> {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        helperText != null -> {
                            Text(
                                text = helperText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        leadingIcon = if (option == selectedOption) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null,
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

/**
 * Multi-select dropdown selector for selecting multiple options.
 *
 * @param label The label for the dropdown field.
 * @param options List of available options to select from.
 * @param selectedOptions List of currently selected options.
 * @param onSelectionChanged Callback when selection changes.
 * @param modifier Modifier for styling and layout customization.
 * @param enabled Whether the dropdown is enabled.
 * @param placeholder Text to show when no options are selected.
 * @param maxDisplayItems Maximum number of items to display in the text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    label: String,
    options: List<String>,
    selectedOptions: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Chọn các tùy chọn",
    maxDisplayItems: Int = 3
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = when {
        selectedOptions.isEmpty() -> ""
        selectedOptions.size <= maxDisplayItems -> selectedOptions.joinToString(", ")
        else -> "${selectedOptions.take(maxDisplayItems).joinToString(", ")} +${selectedOptions.size - maxDisplayItems}"
    }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = { },
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                enabled = enabled,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    val isSelected = option in selectedOptions
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            val newSelection = if (isSelected) {
                                selectedOptions - option
                            } else {
                                selectedOptions + option
                            }
                            onSelectionChanged(newSelection)
                        },
                        leadingIcon = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null // Handled by onClick
                            )
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        // Display selected chips
        if (selectedOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedOptions.forEach { option ->
                    InputChip(
                        selected = true,
                        onClick = {
                            onSelectionChanged(selectedOptions - option)
                        },
                        label = { Text(option) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.remove_option, option),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DropdownSelectorPreview() {
    QuizCodeTheme {
        var selected by remember { mutableStateOf<String?>(null) }
        DropdownSelector(
            label = "Danh mục",
            options = listOf("Toán học", "Khoa học", "Lịch sử", "Địa lý", "Văn học"),
            selectedOption = selected,
            onOptionSelected = { selected = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DropdownSelectorWithSelectionPreview() {
    QuizCodeTheme {
        DropdownSelector(
            label = "Độ khó",
            options = listOf("Dễ", "Trung bình", "Khó", "Rất khó"),
            selectedOption = "Trung bình",
            onOptionSelected = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DropdownSelectorWithErrorPreview() {
    QuizCodeTheme {
        DropdownSelector(
            label = "Chủ đề",
            options = listOf("Option 1", "Option 2"),
            selectedOption = null,
            onOptionSelected = { },
            errorMessage = "Vui lòng chọn một chủ đề",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MultiSelectDropdownPreview() {
    QuizCodeTheme {
        var selected by remember { mutableStateOf(listOf("Toán học", "Khoa học")) }
        MultiSelectDropdown(
            label = "Tags",
            options = listOf("Toán học", "Khoa học", "Lịch sử", "Địa lý", "Văn học"),
            selectedOptions = selected,
            onSelectionChanged = { selected = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}
