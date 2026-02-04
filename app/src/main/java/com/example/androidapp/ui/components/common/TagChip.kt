package com.example.androidapp.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    // Thêm các tham số tùy chỉnh màu sắc (có giá trị mặc định)
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    // Thêm sự kiện click (mặc định là null nếu không muốn click)
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50)) // Bo tròn tối đa để thành hình viên thuốc (Pill-style)
            .background(containerColor)
            // Xử lý sự kiện click nếu có truyền vào
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp) // Tăng padding một chút cho dễ bấm
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontSize = 12.sp
        )
    }
}