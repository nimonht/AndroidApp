package com.example.androidapp.ui.components.quiz


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.screens.create.ChoiceDraft
import androidx.compose.ui.tooling.preview.Preview
//// phần preview
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@Composable
fun ChoiceEditor(
    choices: List<ChoiceDraft>,
    onChoiceChange: (Int, String) -> Unit,
    onAddChoice: () -> Unit,
    onRemoveChoice: (Int) -> Unit,
    onMarkCorrect: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier) {

        LazyColumn {
            itemsIndexed(choices) { index, choice ->

                ChoiceItem(
                    index = index,
                    choice = choice,
                    onTextChange = { onChoiceChange(index, it) },
                    onRemove = { onRemoveChoice(index) },
                    onMarkCorrect = { onMarkCorrect(index) },
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (choices.size < 10) {
            Button(
                onClick = onAddChoice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Thêm lựa chọn")
            }
        }
    }
}
@Composable
private fun ChoiceItem(
    index: Int,
    choice: ChoiceDraft,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
    onMarkCorrect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Choice ${index + 1}")

                Checkbox(
                    checked = false,
                    onCheckedChange = { onMarkCorrect() }
                )
            }

            OutlinedTextField(
                value = choice.content,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth()
            )

            Row {

                TextButton(onClick = onMoveUp) {
                    Text("Up")
                }

                TextButton(onClick = onMoveDown) {
                    Text("Down")
                }

                TextButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun ChoiceEditorPreview() {

    val choices = listOf(
        ChoiceDraft(content = "Choice A"),
        ChoiceDraft(content = "Choice B"),
        ChoiceDraft(content = "Choice C"),
        ChoiceDraft(content = "Choice D")
    )

    ChoiceEditor(
        choices = choices,
        onChoiceChange = { _, _ -> },
        onAddChoice = { },
        onRemoveChoice = { },
        onMarkCorrect = { },
        onMoveUp = { },
        onMoveDown = { }
    )
}