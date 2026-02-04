package com.example.androidapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.androidapp.navigation.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                // Item Home
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { /* Đang ở Home nên ko làm gì */ }
                )
                // Item Search
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                    selected = false,
                    onClick = { navController.navigate(Screen.Search.route) }
                )
                // Item Profile
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { navController.navigate(Screen.Profile.route) }
                )
            }
        }
    ) { innerPadding ->
        // Phần nội dung chính (Cho phép cuộn)
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Cho phép cuộn dọc
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Chào mừng
            Text(
                text = "Hello, Thanh! 👋",
                style = MaterialTheme.typography.headlineMedium
            )

            // 2. Ô nhập mã Quiz (Card placeholder)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Enter quiz code", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* Xử lý sau */ }) {
                        Text("Join Quiz")
                    }
                }
            }

            // 3. Recently Played
            SectionTitle(title = "Recently Played")
            // Placeholder cho list quiz
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizCardPlaceholder("Quiz 1")
                QuizCardPlaceholder("Quiz 2")
            }

            // 4. My Quizzes
            SectionTitle(title = "My Quizzes")
            QuizCardPlaceholder("Math Quiz 101", modifier = Modifier.fillMaxWidth())

            // 5. Trending
            SectionTitle(title = "Trending Quizzes")
            QuizCardPlaceholder("Science Trivia", modifier = Modifier.fillMaxWidth())
        }
    }
}

// Các Component con để tái sử dụng (Viết tạm ở đây)
@Composable
fun SectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = "See All →", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun QuizCardPlaceholder(name: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp).width(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(name)
        }
    }
}