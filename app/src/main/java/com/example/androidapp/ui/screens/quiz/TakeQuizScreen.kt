package com.example.androidapp.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
// Import Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeQuizScreen(navController: NavController, quizId: String) {
    // Biến lưu câu hỏi hiện tại (Bắt đầu từ 0)
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    // Biến lưu đáp án đang chọn
    var selectedOption by remember { mutableStateOf("") }

    val totalQuestions = 10
    val progress = (currentQuestionIndex + 1).toFloat() / totalQuestions

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("14:30", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.popBackStack() }) { // Nút thoát
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (currentQuestionIndex < totalQuestions - 1) {
                        currentQuestionIndex++ // Qua câu tiếp theo
                        selectedOption = ""    // Reset lựa chọn
                    } else {
                        // Hết câu hỏi -> Chuyển sang màn hình kết quả (Nhiệm vụ 7 sẽ làm)
                        navController.navigate("quiz_result/$quizId")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = selectedOption.isNotEmpty(), // Phải chọn đáp án mới sáng nút
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (currentQuestionIndex < totalQuestions - 1) "Next Question" else "Submit Quiz")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 1. Thanh tiến độ
            Text("Question ${currentQuestionIndex + 1}/$totalQuestions", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Nội dung câu hỏi
            Text(
                text = "Câu hỏi số ${currentQuestionIndex + 1}: Tại sao lá cây lại có màu xanh?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Các đáp án (Radio Buttons)
            val options = listOf("Do diệp lục", "Do ánh sáng", "Do nước", "Do đất")

            Column {
                options.forEach { text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (text == selectedOption),
                                onClick = { selectedOption = text },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == selectedOption),
                            onClick = null // null vì đã xử lý ở Row click
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}