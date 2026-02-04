package com.example.androidapp.ui.components.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int = 6, // Độ dài mặc định là 6
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // Tự động focus bàn phím khi màn hình hiện lên
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // 1. TRƯỜNG NHẬP LIỆU ẨN (Logic xử lý nhập)
        BasicTextField(
            value = value,
            onValueChange = {
                // Chỉ nhận số và tối đa 6 ký tự
                if (it.length <= length && it.all { char -> char.isDigit() }) {
                    onValueChange(it)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword // Bàn phím số
            ),
            // Làm cho textfield này vô hình nhưng vẫn chiếm chỗ để nhận sự kiện touch
            decorationBox = {
                // Không vẽ gì ở đây cả, ta vẽ UI bên dưới
            },
            modifier = Modifier
                .matchParentSize() // Phủ kín Box để bấm đâu cũng nhận focus
                .focusRequester(focusRequester),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary) // Giấu con trỏ mặc định đi nếu muốn
        )

        // 2. GIAO DIỆN HIỂN THỊ (6 Ô Vuông)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(length) { index ->
                val char = value.getOrNull(index)
                val isFocused = value.length == index // Ô đang chờ nhập

                // Vẽ từng ô
                CodeInputBox(
                    char = char,
                    isFocused = isFocused
                )
            }
        }
    }
}

@Composable
private fun CodeInputBox(
    char: Char?,
    isFocused: Boolean
) {
    // Màu viền: Nếu đang focus hoặc đã có chữ thì màu Đậm, ngược lại màu Nhạt
    val borderColor = if (isFocused || char != null)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    val backgroundColor = if (char != null)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .width(48.dp) // Kích thước ô
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (char != null) {
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        } else if (isFocused) {
            // Hiệu ứng con trỏ nhấp nháy (Optional) - Có thể thêm animation nếu cần
            // Hiện tại để trống hoặc vạch dưới
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }
}