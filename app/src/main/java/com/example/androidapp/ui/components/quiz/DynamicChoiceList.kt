package com.example.androidapp.ui.components.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.Choice

/**
 * Render danh sách nút lựa chọn động.
 * Hỗ trợ 2-10 lựa chọn mỗi câu với chế độ đa chọn tùy chọn.
 */
@Composable
fun DynamicChoiceList(
    choices: List<Choice>,              // Danh sách các lựa chọn (A, B, C...)
    selectedChoiceIds: Set<String>,     // Các ID đang được chọn (Dùng Set để hỗ trợ chọn nhiều)
    allowMultipleCorrect: Boolean = false, // Chế độ chọn nhiều (True/False)
    onChoiceSelected: (String) -> Unit, // Hàm xử lý khi bấm vào 1 lựa chọn
    modifier: Modifier = Modifier
) {
    // 1. Kiểm tra an toàn (Validation)
    // Nếu dữ liệu sai (ít hơn 2 hoặc nhiều hơn 10), báo lỗi ngay để Developer biết sửa
    require(choices.size in 2..10) {
        "Câu hỏi phải có từ 2 đến 10 lựa chọn, nhưng nhận được ${choices.size}"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Khoảng cách giữa các nút
    ) {
        // 2. Hiển thị bộ đếm (Chỉ hiện nếu có quá nhiều lựa chọn, giúp user đỡ ngợp)
        if (choices.size > 4) {
            Text(
                text = stringResource(
                    id = R.string.quiz_choice_count,
                    selectedChoiceIds.size,
                    choices.size
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 3. Vòng lặp tạo nút (Magic Loop)
        choices.forEachIndexed { index, choice ->
            // Tự động tính nhãn: 0->A, 1->B, 2->C... dựa vào bảng mã ASCII
            val label = ('A' + index).toString()

            // Kiểm tra xem nút này có đang được chọn không
            val isSelected = choice.id in selectedChoiceIds

            // Gọi lại Component ChoiceButton bạn đã làm ở Bước 4
            ChoiceButton(
                label = label,
                content = choice.content,
                isSelected = isSelected,
                isMultiSelect = allowMultipleCorrect,
                onClick = { onChoiceSelected(choice.id) }
            )
        }
    }
}