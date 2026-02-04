package com.example.androidapp.ui.components.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    message: String? = null // Dòng thông báo tùy chọn (VD: "Đang đăng nhập...")
) {
    // Box giúp căn giữa nội dung theo cả chiều dọc và ngang
    Box(
        modifier = modifier.fillMaxSize(), // Mặc định chiếm toàn bộ không gian cha
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Vòng xoay chính
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            // 2. Dòng thông báo (chỉ hiện nếu có truyền vào)
            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp)) // Khoảng cách giữa vòng xoay và chữ
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}