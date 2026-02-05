package com.example.androidapp.ui.components.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import com.example.androidapp.R

@Composable
fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,                  // Nhãn (VD: "Tên đăng nhập")
    modifier: Modifier = Modifier,
    errorMessage: String? = null,   // Thông báo lỗi (VD: "Không được để trống")
    helperText: String? = null,     // Hướng dẫn (VD: "Ít nhất 6 ký tự")
    singleLine: Boolean = true,     // Mặc định là 1 dòng
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None, // Dùng cho mật khẩu (ẩn ký tự)
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, // Cấu hình bàn phím (số, email...)
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

            // 1. Trạng thái lỗi (Đỏ hay không đỏ?)
            isError = errorMessage != null,

            // 2. Dòng chữ nhỏ bên dưới (Hỗ trợ hoặc Báo lỗi)
            supportingText = {
                if (errorMessage != null) {
                    // Ưu tiên hiện lỗi màu đỏ
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (helperText != null) {
                    // Nếu không lỗi thì hiện hướng dẫn màu xám
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },

            // 3. Icon báo lỗi (Dấu chấm than)
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