package com.example.androidapp.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.navigation.AppTopBar

@Composable
fun HistoryScreen(
    navigateUp: () -> Unit
) {
    // Giả lập dữ liệu
    val historyList = listOf(
        HistoryItemData("Toán Học Đại Cương", "8/10", "12/05/2025"),
        HistoryItemData("Lịch Sử Việt Nam", "5/10", "10/05/2025"),
        HistoryItemData("Tiếng Anh B1", "10/10", "09/05/2025")
    )

    Scaffold(
        topBar = {
            AppTopBar(title = "Lịch sử làm bài", canNavigateBack = true, navigateUp = navigateUp)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyList.size) { index ->
                HistoryItemCard(historyList[index])
            }
        }
    }
}

data class HistoryItemData(val title: String, val score: String, val date: String)

@Composable
fun HistoryItemCard(data: HistoryItemData) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = data.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = data.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Hiển thị điểm số nổi bật
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = data.score,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}