package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R

/**
 * Component điều khiển hiển thị kết quả tìm kiếm.
 * Bao gồm tính năng sắp xếp (Sort) và chuyển đổi chế độ xem (Grid/List).
 *
 * @param currentSort Tùy chọn sắp xếp hiện tại.
 * @param isGridView Cờ xác định đang ở chế độ lưới hay danh sách.
 * @param onSortSelected Callback khi người dùng chọn một tiêu chí sắp xếp mới.
 * @param onToggleView Callback khi người dùng nhấn nút chuyển đổi chế độ xem.
 * @param modifier Modifier tùy chỉnh giao diện.
 */
@Composable
fun SearchControlsRow(
    currentSort: SortOption,
    isGridView: Boolean,
    onSortSelected: (SortOption) -> Unit,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Trạng thái đóng/mở của menu thả xuống (chỉ dùng cho UI nội bộ của component này)
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, // Đẩy 2 cụm ra 2 góc
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cụm bên trái: Nút Sắp xếp và Menu thả xuống
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.sort_tooltip),
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getSortOptionText(currentSort),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(getSortOptionText(option)) },
                        onClick = {
                            onSortSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Cụm bên phải: Nút chuyển đổi Grid/List
        IconButton(onClick = onToggleView) {
            Icon(
                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                contentDescription = stringResource(R.string.toggle_view_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Hàm phụ trợ lấy chuỗi văn bản tương ứng với tùy chọn sắp xếp.
 */
@Composable
private fun getSortOptionText(option: SortOption): String {
    return when (option) {
        SortOption.DATE -> stringResource(R.string.sort_date)
        SortOption.POPULARITY -> stringResource(R.string.sort_popularity)
        SortOption.RELEVANCE -> stringResource(R.string.sort_relevance)
    }
}