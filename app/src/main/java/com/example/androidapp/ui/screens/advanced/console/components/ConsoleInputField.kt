package com.example.androidapp.ui.screens.advanced.console.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Custom console input field with token highlighting, ghost text overlay,
 * and monospace font styling.
 *
 * Supports keyboard shortcuts for command submission (Enter/IME Done),
 * tab completion, and history navigation (Up/Down arrows).
 *
 * The input applies [TokenHighlightTransformation] for syntax colouring of
 * commands, flags, strings, and operators.
 *
 * @param value The current text in the input field.
 * @param onValueChange Callback with the new text and cursor position when the input changes.
 * @param ghostText Semi-transparent autocomplete suggestion displayed after the current input.
 * @param prompt The shell prompt string displayed before the input (e.g. "[user]$ ").
 * @param onSubmit Callback invoked when the user presses Enter or the IME Done action.
 * @param onTabPress Callback invoked when the user presses Tab (accept suggestion).
 * @param onUpPress Callback invoked when the user presses the Up arrow (history back).
 * @param onDownPress Callback invoked when the user presses the Down arrow (history forward).
 * @param isExecuting Whether a command is currently being executed (shows a spinner).
 * @param modifier Modifier for styling and layout.
 */
@Composable
fun ConsoleInputField(
    value: String,
    onValueChange: (String, Int) -> Unit,
    ghostText: String,
    prompt: String,
    onSubmit: () -> Unit,
    onTabPress: () -> Unit,
    onUpPress: () -> Unit,
    onDownPress: () -> Unit,
    isExecuting: Boolean,
    modifier: Modifier = Modifier
) {
    val tokenTransformation = remember { TokenHighlightTransformation() }

    val textFieldValue = remember(value) {
        TextFieldValue(
            text = value,
            selection = TextRange(value.length)
        )
    }

    val monoStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface
    )

    val promptColor = MaterialTheme.colorScheme.primary
    val ghostColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Prompt label
        Text(
            text = prompt,
            style = monoStyle.copy(
                color = promptColor,
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(end = 4.dp)
        )

        // Input area with ghost text overlay
        Box(
            modifier = Modifier.weight(1f)
        ) {
            // Ghost text layer (shown behind/after the actual input)
            if (ghostText.isNotEmpty() && value.isNotEmpty()) {
                Text(
                    text = ghostText,
                    style = monoStyle.copy(color = ghostColor),
                    maxLines = 1
                )
            }

            // Placeholder when empty
            if (value.isEmpty() && !isExecuting) {
                Text(
                    // TODO: move to strings.xml
                    text = "Nhap lenh...",
                    style = monoStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ),
                    maxLines = 1
                )
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    onValueChange(newValue.text, newValue.selection.start)
                },
                textStyle = monoStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                enabled = !isExecuting,
                visualTransformation = tokenTransformation,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onSubmit() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Enter -> {
                                    onSubmit()
                                    true
                                }

                                Key.Tab -> {
                                    onTabPress()
                                    true
                                }

                                Key.DirectionUp -> {
                                    onUpPress()
                                    true
                                }

                                Key.DirectionDown -> {
                                    onDownPress()
                                    true
                                }

                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            )
        }

        // Executing spinner
        if (isExecuting) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .then(Modifier.padding(2.dp))
            )
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(
    name = "ConsoleInputField - Light",
    showBackground = true
)
@Composable
private fun ConsoleInputFieldLightPreview() {
    QuizzezTheme(darkTheme = false) {
        ConsoleInputField(
            value = "help --verbose",
            onValueChange = { _, _ -> },
            ghostText = "help --verbose --all",
            prompt = "[user]\$ ",
            onSubmit = {},
            onTabPress = {},
            onUpPress = {},
            onDownPress = {},
            isExecuting = false
        )
    }
}

@Preview(
    name = "ConsoleInputField - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun ConsoleInputFieldDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        ConsoleInputField(
            value = "ban user@example.com",
            onValueChange = { _, _ -> },
            ghostText = "",
            prompt = "[admin]# ",
            onSubmit = {},
            onTabPress = {},
            onUpPress = {},
            onDownPress = {},
            isExecuting = true
        )
    }
}

@Preview(
    name = "ConsoleInputField - Empty",
    showBackground = true
)
@Composable
private fun ConsoleInputFieldEmptyPreview() {
    QuizzezTheme(darkTheme = false) {
        ConsoleInputField(
            value = "",
            onValueChange = { _, _ -> },
            ghostText = "",
            prompt = "[guest]\$ ",
            onSubmit = {},
            onTabPress = {},
            onUpPress = {},
            onDownPress = {},
            isExecuting = false
        )
    }
}
