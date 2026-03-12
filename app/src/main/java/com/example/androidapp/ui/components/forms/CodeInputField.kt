package com.example.androidapp.ui.components.forms

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizCodeTheme

/**
 * Single-field code input for joining a quiz session via share code.
 * Accepts up to 6 uppercase alphanumeric characters matching the
 * share-code pattern `[A-Z0-9]{6}`.
 *
 * @param value The current input value.
 * @param onValueChange Callback when the input changes.
 * @param length Maximum character count (defaults to 6).
 * @param modifier Modifier for styling and layout customization.
 */
@Composable
fun CodeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int = 6,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = newValue
                .filter { it.isLetterOrDigit() }
                .uppercase()
                .take(length)
            onValueChange(filtered)
        },
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(R.string.home_code_placeholder),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        textStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = MaterialTheme.typography.titleMedium.letterSpacing
        ),
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = remember {
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = false
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CodeInputFieldEmptyPreview() {
    QuizCodeTheme {
        CodeInputField(
            value = "",
            onValueChange = { }
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CodeInputFieldDarkPreview() {
    QuizCodeTheme {
        CodeInputField(
            value = "ABC1",
            onValueChange = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CodeInputFieldFilledPreview() {
    QuizCodeTheme {
        CodeInputField(
            value = "XY9Z3K",
            onValueChange = { }
        )
    }
}