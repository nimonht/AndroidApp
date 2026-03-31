package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.quiz.QuizCard

/**
 * Component hiển thị kết quả tìm kiếm dưới dạng Lưới (Grid 2 cột).
 *
 * @param results Danh sách các bài kiểm tra cần hiển thị.
 * @param onQuizClick Callback khi người dùng nhấn vào một bài kiểm tra.
 * @param modifier Modifier tùy chỉnh giao diện.
 */
@Composable
fun SearchResultsGrid(
    results: List<QuizCardDraft>,
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(results, key = { it.id }) { quiz ->
            QuizCard(
                quiz = quiz.toQuiz(),
                onClick = { onQuizClick(quiz.id) },
                modifier = Modifier
            )
        }
    }
}

/**
 * Chuyển đổi [QuizCardDraft] sang [Quiz] để truyền cho [QuizCard].
 */
private fun QuizCardDraft.toQuiz() = Quiz(
    id = id,
    ownerId = "",
    title = title,
    authorName = authorName,
    thumbnailUrl = coverImageUrl,
    questionCount = questionCount,
    attemptCount = attemptCount,
    tags = tags
)
