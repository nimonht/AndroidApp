package com.example.androidapp.ui.components.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceButton(
    label: String,          // Ví dụ: "A", "B"
    content: String,        // Nội dung: "Hà Nội"
    isSelected: Boolean,    // Trạng thái được chọn hay chưa
    isMultiSelect: Boolean = false, // True = Checkbox, False = Radio
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Xác định màu sắc dựa trên trạng thái chọn
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline

    val borderWidth = if (isSelected) 2.dp else 1.dp

    // 2. Khung thẻ bao quanh
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3. Phần hiển thị icon chọn (Radio hoặc Checkbox)
            if (isMultiSelect) {
                // Chế độ chọn nhiều (Checkbox)
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null // null để Card xử lý click
                )
            } else {
                // Chế độ chọn 1 (Radio)
                RadioButton(
                    selected = isSelected,
                    onClick = null // null để Card xử lý click
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 4. Nội dung text (Label + Content)
            Text(
                text = "$label. ", // "A. "
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = content, // "Hà Nội"
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f) // Đẩy icon check sang phải nếu cần
            )

            // Icon dấu tích nhỏ bên phải nếu đã chọn (Tăng trải nghiệm người dùng)
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}