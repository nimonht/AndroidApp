package com.example.androidapp.ui.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

// Sửa Enum để chứa String trực tiếp
enum class AppTab(
    val route: String,
    val icon: ImageVector,
    val label: String // Đổi từ Int sang String
) {
    HOME("home", Icons.Default.Home, "Trang chủ"),
    SEARCH("search", Icons.Default.Search, "Tìm kiếm"),
    PROFILE("profile", Icons.Default.Person, "Hồ sơ")
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        AppTab.values().forEach { tab ->
            val isSelected = currentRoute == tab.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(text = tab.label) },
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}