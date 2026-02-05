package com.example.androidapp.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.forms.CodeInputField

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
    val (joinCode, setJoinCode) = remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Header
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineMedium
        )

        // 2. Quiz Code Input Card
        QuizCodeCard(
            code = joinCode,
            onCodeChange = setJoinCode,
            onJoinQuiz = { code ->
                if (code.length == 6) {
                    onNavigateToQuiz(code)
                }
            }
        )

        // 3. Recently Played Section
        SectionHeader(
            title = stringResource(R.string.home_recently_played),
            onSeeAllClick = onNavigateToSearch
        )
        EmptyState(
            message = stringResource(R.string.home_recently_played_empty),
            modifier = Modifier.fillMaxWidth()
        )

        // 4. My Quizzes Section
        SectionHeader(
            title = stringResource(R.string.home_my_quizzes),
            onSeeAllClick = onNavigateToSearch
        )
        EmptyState(
            message = stringResource(R.string.home_my_quizzes_empty),
            modifier = Modifier.fillMaxWidth()
        )

        // 5. Trending Section
        SectionHeader(
            title = stringResource(R.string.home_trending_quizzes),
            onSeeAllClick = onNavigateToSearch
        )
        EmptyState(
            message = stringResource(R.string.home_trending_empty),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuizCodeCard(
    code: String,
    onCodeChange: (String) -> Unit,
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
            CodeInputField(
                value = code,
                onValueChange = onCodeChange
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onJoinQuiz(code) },
                enabled = code.length == 6
            ) {
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
            Text(text = stringResource(R.string.home_see_all))
        }
    }
}
