package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.quiz.QuizCard

/**
 * Mot muc kham pha gom tieu de section va mot hang cuon ngang cac [QuizCard].
 *
 * Duoc dung trong man hinh Tim kiem khi nguoi dung chua thuc hien tim kiem
 * nao, hien thi cac muc nhu "Top hom nay", "Noi bat", "Trending", v.v.
 *
 * @param title       Tieu de cua section (VD: "Top hom nay").
 * @param quizzes     Danh sach cac [QuizCardDraft] can hien thi.
 * @param onQuizClick Callback khi nguoi dung nhan vao mot the quiz; truyen quiz ID.
 * @param modifier    Modifier tuy chinh giao dien tu ben ngoai.
 */
@Composable
fun DiscoverSection(
    title: String,
    quizzes: List<QuizCardDraft>,
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (quizzes.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Tieu de section
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hang cuon ngang cac quiz card
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quizzes, key = { it.id }) { quiz ->
                DiscoverQuizCard(
                    quiz = quiz,
                    onQuizClick = onQuizClick,
                    modifier = Modifier.width(280.dp)
                )
            }
        }
    }
}

/**
 * The quiz nho dung trong hang cuon ngang cua [DiscoverSection].
 * Tai su dung [QuizCard] san co, chi gioi han chieu rong.
 *
 * @param quiz        Du lieu quiz can hien thi.
 * @param onQuizClick Callback khi nguoi dung nhan; truyen quiz ID.
 * @param modifier    Modifier tuy chinh tu ben ngoai.
 */
@Composable
private fun DiscoverQuizCard(
    quiz: QuizCardDraft,
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    QuizCard(
        quiz = quiz.toQuiz(isPublic = true),
        onClick = { onQuizClick(quiz.id) },
        modifier = modifier
    )
}
