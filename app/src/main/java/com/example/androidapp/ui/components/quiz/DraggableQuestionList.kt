package com.example.androidapp.ui.components.quiz


import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.androidapp.ui.screens.create.QuestionDraft
/// preview
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import com.example.androidapp.ui.theme.QuizCodeTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

import com.example.androidapp.ui.screens.create.ChoiceDraft

@Composable
fun DraggableQuestionList(
    questions: List<QuestionDraft>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (QuestionDraft, Int) -> Unit
) {

    var draggedIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(modifier = modifier) {

        itemsIndexed(
            items = questions,
            key = { _, q -> q.id }
        ) { index, question ->

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {

                        detectDragGesturesAfterLongPress(

                            onDragStart = {
                                draggedIndex = index
                            },

                            onDrag = { change, dragAmount ->

                                val from = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                val to = if (dragAmount.y > 0) from + 1 else from - 1

                                if (to in questions.indices) {
                                    onMove(from, to)
                                    draggedIndex = to
                                }

                                change.consume()
                            },

                            onDragEnd = {
                                draggedIndex = null
                            }
                        )
                    }
            ) {
                itemContent(question, index)
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun DraggableQuestionListPreview() {

    QuizCodeTheme {

        Surface {

            val questions = remember {

                mutableStateListOf(

                    QuestionDraft(
                        content = "Câu hỏi 1",
                        choices = listOf(
                            ChoiceDraft("A"),
                            ChoiceDraft("B"),
                            ChoiceDraft("C"),
                            ChoiceDraft("D")
                        )
                    ),

                    QuestionDraft(
                        content = "Câu hỏi 2",
                        choices = listOf(
                            ChoiceDraft("A"),
                            ChoiceDraft("B"),
                            ChoiceDraft("C"),
                            ChoiceDraft("D")
                        )
                    ),

                    QuestionDraft(
                        content = "Câu hỏi 3",
                        choices = listOf(
                            ChoiceDraft("A"),
                            ChoiceDraft("B"),
                            ChoiceDraft("C"),
                            ChoiceDraft("D")
                        )
                    )
                )
            }

            DraggableQuestionList(

                questions = questions,

                onMove = { from, to ->

                    val item = questions.removeAt(from)

                    questions.add(to, item)
                }

            ) { question, index ->

                androidx.compose.material3.Card {

                    androidx.compose.material3.Text(
                        text = "Câu ${index + 1}: ${question.content}"
                    )
                }
            }
        }
    }
}