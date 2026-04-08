package com.example.androidapp.ui.components.forms

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Styled outlined text input field with built-in error and helper text support.
 *
 * @param value Current text value.
 * @param onValueChange Callback when the text changes.
 * @param label Label displayed above the field.
 * @param modifier Modifier for styling and layout customization.
 * @param errorMessage Optional error message shown below the field in red.
 * @param helperText Optional helper text shown below the field when there is no error.
 * @param visualTransformation Transformation for masking input (e.g., password fields).
 */
@Composable
fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,                  // Label (e.g., "Username")
    modifier: Modifier = Modifier,
    errorMessage: String? = null,   // Error message (e.g., "Must not be empty")
    helperText: String? = null,     // Helper text (e.g., "At least 6 characters")
    singleLine: Boolean = true,     // Single line by default
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None, // Used for password fields (masks characters)
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, // Keyboard configuration (number, email, etc.)
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,

            // 1. Error state (red border or not)
            isError = errorMessage != null,

            // 2. Supporting text below the field (helper or error)
            supportingText = {
                if (errorMessage != null) {
                    // Prioritize showing error in red
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (helperText != null) {
                    // If no error, show helper text in gray
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },

            // 3. Error icon (exclamation mark)
            trailingIcon = {
                if (errorMessage != null) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = stringResource(R.string.error),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun TextInputFieldPreview() {
    QuizzezTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TextInputField(
                value = "Nguyễn Văn A",
                onValueChange = {},
                label = "Tên người dùng",
                helperText = "Ít nhất 6 ký tự"
            )
            TextInputField(
                value = "",
                onValueChange = {},
                label = "Email",
                errorMessage = "Không được để trống"
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TextInputFieldDarkPreview() {
    QuizzezTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TextInputField(
                value = "Nguyễn Văn A",
                onValueChange = {},
                label = "Tên người dùng",
                helperText = "Ít nhất 6 ký tự"
            )
            TextInputField(
                value = "",
                onValueChange = {},
                label = "Email",
                errorMessage = "Không được để trống"
            )
        }
    }
}
