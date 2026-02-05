package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.feedback.EmptyState

/**
 * Take Quiz screen where user answers questions.
 *
 * @param quizId The ID of the quiz being taken.
 * @param onNavigateBack Callback to exit the quiz.
 * @param onQuizComplete Callback when quiz is submitted.
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeQuizScreen(
    quizId: String,
    onNavigateBack: () -> Unit,
    onQuizComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.take_quiz_title))
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        EmptyState(
            message = stringResource(R.string.take_quiz_empty),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        )
    }
}
