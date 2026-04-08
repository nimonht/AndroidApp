package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.quiz.QuizCard

/**
 * Component hiển thị kết quả tìm kiếm dưới dạng Danh sách dọc (List).
 *
 * @param results Danh sách các bài kiểm tra cần hiển thị.
 * @param onQuizClick Callback khi người dùng nhấn vào một bài kiểm tra.
 * @param modifier Modifier tùy chỉnh giao diện.
 */
@Composable
fun SearchResultsList(
    results: List<QuizCardDraft>,
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    hasMoreSearchResults: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(results, key = { it.id }) { quiz ->
            QuizCard(
                quiz = quiz.toQuiz(),
                onClick = { onQuizClick(quiz.id) },
                modifier = Modifier
            )
        }

        // Pagination: load more search results
        if (hasMoreSearchResults) {
            item(key = "load_more_search_results") {
                LaunchedEffect(isLoadingMore) {
                    if (!isLoadingMore) onLoadMore()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

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
