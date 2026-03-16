package com.example.androidapp.ui.screens.importt



import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class CsvRowPreview(
    val question: String,
    val choiceA: String,
    val choiceB: String,
    val choiceC: String,
    val choiceD: String,
    val correct: String
)

data class CsvImportUiState(
    val rows: List<CsvRowPreview> = emptyList(),
    val errors: List<String> = emptyList(),
    val fileName: String? = null,
    val isReadyToImport: Boolean = false
)

class ImportCsvViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CsvImportUiState())
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    fun onFileLoaded(
        fileName: String,
        rows: List<CsvRowPreview>,
        errors: List<String>
    ) {
        _uiState.value = CsvImportUiState(
            rows = rows,
            errors = errors,
            fileName = fileName,
            isReadyToImport = rows.isNotEmpty() && errors.isEmpty()
        )
    }

    fun confirmImport() {
        // TODO: call repository later
    }
}