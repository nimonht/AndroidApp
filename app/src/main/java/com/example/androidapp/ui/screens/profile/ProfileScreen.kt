package com.example.androidapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
// Import Icon thủ công nếu cần
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    // Giả lập trạng thái đăng nhập (Sau này sẽ lấy từ Firebase)
    var isLoggedIn by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                    selected = false,
                    onClick = { navController.navigate("search") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = true, // Đang ở Profile nên sáng lên
                    onClick = { /* Đang ở đây rồi */ }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isLoggedIn) {
                // --- PHẦN 1: THÔNG TIN USER ---
                UserProfileHeader()

                // --- PHẦN 2: MENU CHỨC NĂNG ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("General", style = MaterialTheme.typography.titleSmall, color = Color.Gray)

                    ProfileMenuItem(
                        icon = Icons.Default.History,
                        title = "Attempt History",
                        onClick = { navController.navigate("history") }
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.Delete,
                        title = "Recycle Bin",
                        onClick = { navController.navigate("trash") }
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        onClick = { navController.navigate("settings") }
                    )
                }

                // --- PHẦN 3: ĐĂNG XUẤT ---
                Button(
                    onClick = { isLoggedIn = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out")
                }

            } else {
                // --- GIAO DIỆN KHI CHƯA ĐĂNG NHẬP ---
                GuestView(onLoginClick = { navController.navigate("login") })
            }
        }
    }
}

// Component hiển thị Header thông tin User
@Composable
fun UserProfileHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Avatar giả lập
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("T", style = MaterialTheme.typography.headlineLarge)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text("Thanh Student", style = MaterialTheme.typography.headlineSmall)
            Text("thanh@huflit.edu.vn", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

// Component từng dòng Menu
@Composable
fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

// Component cho khách (Chưa đăng nhập)
@Composable
fun GuestView(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("You are not logged in", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLoginClick) {
            Text("Login to continue")
        }
    }
}