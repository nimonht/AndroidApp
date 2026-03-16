package com.example.androidapp.ui.screens.importt



import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.ui.components.quiz.CsvPreviewTable
// preview

import androidx.compose.ui.tooling.preview.Preview
import com.example.androidapp.ui.theme.QuizCodeTheme
//
@Composable
fun ImportCsvScreen(
    modifier: Modifier = Modifier,
    viewModel: ImportCsvViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {
            // TODO parse csv
            val preview = listOf(
                CsvRowPreview(
                    question = "What is Kotlin?",
                    choiceA = "Language",
                    choiceB = "Animal",
                    choiceC = "IDE",
                    choiceD = "Game",
                    correct = "A"
                )
            )

            viewModel.onFileLoaded(
                fileName = "sample.csv",
                rows = preview,
                errors = emptyList()
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Button(
            onClick = { filePicker.launch("text/*") }
        ) {
            Text("Chọn file CSV")
        }

        Spacer(modifier = Modifier.height(16.dp))

        state.fileName?.let {
            Text("File: $it")
        }

        if (state.errors.isNotEmpty()) {

            Text(
                text = "Lỗi:",
                style = MaterialTheme.typography.titleMedium
            )

            state.errors.forEach {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CsvPreviewTable(rows = state.rows)

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isReadyToImport) {
            Button(
                onClick = { viewModel.confirmImport() }
            ) {
                Text("Xác nhận import")
            }
        }
    }
}

 /// preview
@Preview(showBackground = true)
@Composable
fun ImportCsvScreenPreviewLight() {

    QuizCodeTheme(
        darkTheme = false
    ) {

        ImportCsvScreenPreviewContent()
    }
}

@Preview(showBackground = true)
@Composable
fun ImportCsvScreenPreviewDark() {

    QuizCodeTheme(
        darkTheme = true
    ) {

        ImportCsvScreenPreviewContent()
    }
}

@Composable
private fun ImportCsvScreenPreviewContent() {

    val fakeRows = listOf(
        CsvRowPreview(
            question = "What is Kotlin?",
            choiceA = "Language",
            choiceB = "Animal",
            choiceC = "IDE",
            choiceD = "Game",
            correct = "A"
        ),
        CsvRowPreview(
            question = "Android uses?",
            choiceA = "Java",
            choiceB = "Swift",
            choiceC = "Ruby",
            choiceD = "Go",
            correct = "A"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Button(onClick = {}) {
            Text("Chọn file CSV")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("File: sample.csv")

        Spacer(modifier = Modifier.height(16.dp))

        CsvPreviewTable(rows = fakeRows)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {}) {
            Text("Xác nhận import")
        }
    }
}