package com.example.androidapp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.navigation.AppTopBar

@Composable
fun SettingsScreen(
    navigateUp: () -> Unit,
    onLogout: () -> Unit
) {
    // Demo state
    var isDataSaverEnabled by remember { mutableStateOf(false) }
    var isDarkModeEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Cài đặt",
                canNavigateBack = true,
                navigateUp = navigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Nhóm: Chung
            SettingsGroupTitle("Chung")

            SettingsSwitchItem(
                title = "Tiết kiệm dữ liệu",
                subtitle = "Chỉ đồng bộ khi có Wi-Fi",
                checked = isDataSaverEnabled,
                onCheckedChange = { isDataSaverEnabled = it }
            )

            SettingsSwitchItem(
                title = "Giao diện tối",
                subtitle = "Luôn sử dụng nền tối",
                checked = isDarkModeEnabled,
                onCheckedChange = { isDarkModeEnabled = it }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Nhóm: Tài khoản
            SettingsGroupTitle("Tài khoản")

            SettingsActionItem(
                title = "Hồ sơ cá nhân",
                icon = Icons.Default.Person,
                onClick = { /* Navigate to profile edit */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Đăng xuất
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất")
            }
        }
    }
}

// Component con: Tiêu đề nhóm
@Composable
fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// Component con: Mục có nút gạt (Switch)
@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// Component con: Mục hành động (Button)
@Composable
fun SettingsActionItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}