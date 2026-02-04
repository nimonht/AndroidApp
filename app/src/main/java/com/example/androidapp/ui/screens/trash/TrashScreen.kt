package com.example.androidapp.ui.screens.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.navigation.AppTopBar

@Composable
fun TrashScreen(
    navigateUp: () -> Unit
) {
    val deletedQuizzes = listOf("Quiz Nháp 1", "Bài kiểm tra cũ")

    Scaffold(
        topBar = { AppTopBar("Thùng rác", true, navigateUp) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(deletedQuizzes.size) { index ->
                TrashItem(deletedQuizzes[index])
            }
        }
    }
}

@Composable
fun TrashItem(title: String) {
    Card {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, modifier = Modifier.weight(1f))
            Row {
                // Nút Khôi phục
                IconButton(onClick = { /* Restore logic */ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Khôi phục", tint = MaterialTheme.colorScheme.primary)
                }
                // Nút Xóa vĩnh viễn
                IconButton(onClick = { /* Delete forever logic */ }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa vĩnh viễn", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}