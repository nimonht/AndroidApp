package com.example.androidapp.ui.components.quiz



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.screens.importt.CsvRowPreview

@Composable
fun CsvPreviewTable(
    rows: List<CsvRowPreview>,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {

        items(rows) { row ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {

                Column(
                    modifier = Modifier.padding(12.dp)
                ) {

                    Text("Question: ${row.question}")
                    Text("A: ${row.choiceA}")
                    Text("B: ${row.choiceB}")
                    Text("C: ${row.choiceC}")
                    Text("D: ${row.choiceD}")
                    Text("Correct: ${row.correct}")
                }
            }
        }
    }
}