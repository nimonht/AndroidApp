package com.example.androidapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.forms.TextInputField

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), // Cho phép cuộn nếu màn hình nhỏ
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Tạo tài khoản", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(32.dp))

        TextInputField(value = username, onValueChange = { username = it }, label = "Tên người dùng")
        Spacer(modifier = Modifier.height(16.dp))
        TextInputField(value = email, onValueChange = { email = it }, label = "Email")
        Spacer(modifier = Modifier.height(16.dp))
        TextInputField(
            value = password,
            onValueChange = { password = it },
            label = "Mật khẩu",
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextInputField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Xác nhận mật khẩu",
            visualTransformation = PasswordVisualTransformation(),
            errorMessage = if (confirmPassword.isNotEmpty() && password != confirmPassword) "Mật khẩu không khớp" else null
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onSignupSuccess, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Đăng ký")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Đã có tài khoản? Đăng nhập")
        }
    }
}