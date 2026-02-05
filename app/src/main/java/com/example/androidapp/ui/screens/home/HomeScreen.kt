package com.example.androidapp.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R

/**
 * Home dashboard screen displaying:
 * - Welcome message
 * - Quiz join code input
 * - Recently played quizzes
 * - User's own quizzes
 * - Trending public quizzes
 *
 * @param onNavigateToQuiz Callback to navigate to quiz detail screen.
 * @param onNavigateToSearch Callback to navigate to search screen.
 * @param modifier Modifier for styling.
 */
@Composable
fun HomeScreen(
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Header
        Text(
            text = "Hello! 👋",
            style = MaterialTheme.typography.headlineMedium
        )

        // 2. Quiz Code Input Card
        QuizCodeCard(
            onJoinQuiz = { code ->
                // TODO: Validate code and navigate
            }
        )

        // 3. Recently Played Section
        SectionHeader(
            title = "Recently Played",
            onSeeAllClick = onNavigateToSearch
        )
        RecentlyPlayedRow(onQuizClick = onNavigateToQuiz)

        // 4. My Quizzes Section
        SectionHeader(
            title = "My Quizzes",
            onSeeAllClick = onNavigateToSearch
        )
        MyQuizzesSection(onQuizClick = onNavigateToQuiz)

        // 5. Trending Section
        SectionHeader(
            title = "Trending Quizzes",
            onSeeAllClick = onNavigateToSearch
        )
        TrendingSection(onQuizClick = onNavigateToQuiz)
    }
}

@Composable
private fun QuizCodeCard(
    onJoinQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.quiz_code_hint),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            // TODO: Integrate CodeInputField when state management is ready
            Button(onClick = { onJoinQuiz("") }) {
                Text(stringResource(R.string.quiz_join))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onSeeAllClick) {
            Text(text = "See All →")
        }
    }
}

@Composable
private fun RecentlyPlayedRow(
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with actual data from ViewModel
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            QuizPreviewCard(
                title = "Quiz ${index + 1}",
                onClick = { onQuizClick("quiz_$index") }
            )
        }
    }
}

@Composable
private fun MyQuizzesSection(
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with actual QuizCard components and data
    QuizPreviewCard(
        title = "Math Quiz 101",
        onClick = { onQuizClick("math_101") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TrendingSection(
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with actual QuizCard components and data
    QuizPreviewCard(
        title = "Science Trivia",
        onClick = { onQuizClick("science_trivia") },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Temporary preview card for quizzes.
 * TODO: Replace with actual QuizCard component when integrated with ViewModel.
 */
@Composable
private fun QuizPreviewCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(100.dp)
            .widthIn(min = 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(title)
        }
    }
}
