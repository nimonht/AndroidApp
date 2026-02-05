package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.ui.components.quiz.DynamicChoiceList
import com.example.androidapp.ui.components.quiz.QuizProgressIndicator
import com.example.androidapp.ui.components.quiz.TimerDisplay

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
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedChoiceIds by remember { mutableStateOf(setOf<String>()) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    
    val totalQuestions = 10
    
    // Sample choices for demo
    val sampleChoices = remember {
        listOf(
            Choice(id = "a", content = "Option A", isCorrect = true, position = 0),
            Choice(id = "b", content = "Option B", isCorrect = false, position = 1),
            Choice(id = "c", content = "Option C", isCorrect = false, position = 2),
            Choice(id = "d", content = "Option D", isCorrect = false, position = 3)
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    TimerDisplay(secondsElapsed = elapsedSeconds)
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
        },
        bottomBar = {
            Button(
                onClick = {
                    if (currentQuestionIndex < totalQuestions - 1) {
                        currentQuestionIndex++
                        selectedChoiceIds = emptySet()
                    } else {
                        // Quiz complete - navigate to results
                        onQuizComplete(quizId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = selectedChoiceIds.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (currentQuestionIndex < totalQuestions - 1) 
                        stringResource(R.string.next) 
                    else 
                        stringResource(R.string.submit)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Progress indicator
            QuizProgressIndicator(
                currentQuestionIndex = currentQuestionIndex,
                totalQuestions = totalQuestions
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Question content
            Text(
                text = "Question ${currentQuestionIndex + 1}: Sample question text?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Choice buttons using reusable component
            DynamicChoiceList(
                choices = sampleChoices,
                selectedChoiceIds = selectedChoiceIds,
                allowMultipleCorrect = false,
                onChoiceSelected = { choiceId ->
                    selectedChoiceIds = setOf(choiceId)
                }
            )
        }
    }
}
