package com.example.androidapp.ui.screens.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.quiz.ChoiceButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar

//// phần preview
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidapp.ui.theme.QuizCodeTheme
import com.example.androidapp.ui.screens.create.QuestionDraft
import com.example.androidapp.ui.screens.create.ChoiceDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPreviewScreen(
    title: String,
    questions: List<QuestionDraft>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title)
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Quay lại")
                    }
                }
            )
        }
    )


    { padding ->

        LazyColumn(
            modifier = modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            itemsIndexed(questions) { index, question ->

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Câu ${index + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = question.content,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    question.choices.forEachIndexed { choiceIndex, choice ->

                        ChoiceButton(
                            label = ('A' + choiceIndex).toString(),   // A B C D
                            content = choice.content,
                            isSelected = false,
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        )

                    }

                }
            }
        }
    }
}
//// preview sơ giao diện

@Preview(showBackground = true)
@Composable
fun QuizPreviewScreenPreview() {

    val mockQuestions = listOf(

        QuestionDraft(
            content = "Android được phát triển bởi công ty nào?",
            choices = listOf(
                ChoiceDraft(content = "Google"),
                ChoiceDraft(content = "Apple"),
                ChoiceDraft(content = "Microsoft"),
                ChoiceDraft(content = "Meta")
            ),
            correctIndices = setOf(0)
        ),

        QuestionDraft(
            content = "Ngôn ngữ chính của Android là gì?",
            choices = listOf(
                ChoiceDraft(content = "Kotlin"),
                ChoiceDraft(content = "Swift"),
                ChoiceDraft(content = "Python"),
                ChoiceDraft(content = "Go")
            ),
            correctIndices = setOf(0)
        )

    )

    QuizCodeTheme {
        QuizPreviewScreen(
            title = "Preview Quiz",
            questions = mockQuestions,
            onBack = {}
        )
    }
}