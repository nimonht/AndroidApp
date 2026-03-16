package com.example.androidapp.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ") }
            )
        }
    ) { padding ->

        Column(
            modifier = modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            AsyncImage(
                model = uiState.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )

            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = { Text("Tên hiển thị") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu thay đổi")
            }
        }
    }
}

