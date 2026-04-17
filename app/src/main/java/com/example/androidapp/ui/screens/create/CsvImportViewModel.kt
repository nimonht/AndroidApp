package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import com.example.androidapp.domain.util.CsvParser
import com.example.androidapp.domain.util.CsvValidator
import com.example.androidapp.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ---------------------------------------------------------------------------
// Phase
// ---------------------------------------------------------------------------

/**
 * Represents the current phase of the CSV import flow.
 */
enum class CsvImportPhase {
    /** Initial state – the user has not yet selected a file. */
    FilePicker,

    /** A file has been parsed; the user is reviewing a preview before confirming. */
    Preview,

    /** Import is in progress (conversion to [QuestionDraft] objects). */
    Importing,

    /** Import finished successfully. */
    Done
}

// ---------------------------------------------------------------------------
// Preview row
// ---------------------------------------------------------------------------

/**
 * A single row of the CSV preview table shown during the [CsvImportPhase.Preview] phase.
 *
 * @property rowNumber 1-based row number from the original CSV data rows.
 * @property question Text of the question column.
 * @property choices Ordered list of choice texts (A, B, C, D).
 * @property correctAnswer Letter indicating the correct choice ("a", "b", "c", or "d").
 * @property hasError Whether this row contains at least one validation error.
 * @property errorMessage Human-readable description of the error (null when [hasError] is false).
 */
data class CsvPreviewRow(
    val rowNumber: Int,
    val question: String,
    val choices: List<String>,
    val correctAnswer: String,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

/**
 * Immutable UI state for the CSV Import screen.
 *
 * @property phase Current phase of the import flow.
 * @property fileName Display name of the selected file.
 * @property rawCsvContent Raw string content of the selected file.
 * @property previewRows Parsed preview rows to display during [CsvImportPhase.Preview].
 * @property validationErrors Human-readable validation error messages.
 * @property isLoading True while an async operation is running.
 * @property isImported True once questions have been handed off via the callback.
 * @property error Non-null when an unexpected error should be surfaced to the user.
 * @property errorDetail Optional dynamic detail for parameterised error messages (e.g. exception text).
 */
data class CsvImportUiState(
    val phase: CsvImportPhase = CsvImportPhase.FilePicker,
    val fileName: String = "",
    val rawCsvContent: String = "",
    val previewRows: List<CsvPreviewRow> = emptyList(),
    val validationErrors: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isImported: Boolean = false,
    val error: UiError? = null,
    val errorDetail: String? = null
)

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

/**
 * Events that can be dispatched to [CsvImportViewModel].
 */
sealed class CsvImportEvent {
    /**
     * The user selected a file from the system picker.
     *
     * @property fileName Display name of the file.
     * @property content Raw string content of the file.
     */
    data class FileSelected(val fileName: String, val content: String) : CsvImportEvent()

    /** The user confirmed they want to import the previewed rows. */
    data object ConfirmImport : CsvImportEvent()

    /** Dismiss the current error banner. */
    data object ClearError : CsvImportEvent()

    /** Reset the screen back to [CsvImportPhase.FilePicker] so the user can start over. */
    data object Reset : CsvImportEvent()
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * ViewModel for the CSV Import screen.
 *
 * Responsibilities:
 * - Delegates CSV parsing to [CsvParser].
 * - Delegates validation to [CsvValidator].
 * - Builds [CsvPreviewRow] objects for the UI preview table.
 * - Converts validated rows to [QuestionDraft] objects and delivers them via [onQuestionsImported].
 *
 * @param onQuestionsImported Callback invoked with the list of imported [QuestionDraft] objects
 *   when the user confirms the import.
 */
class CsvImportViewModel(
    private val onQuestionsImported: (List<QuestionDraft>) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(CsvImportUiState())

    /** Current UI state exposed to the composable. */
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    /**
     * The required CSV column headers that must be present and non-blank.
     */
    private val requiredHeaders = listOf(
        "question", "choice_a", "choice_b", "choice_c", "choice_d", "correct"
    )

    /**
     * Dispatches a [CsvImportEvent] to the ViewModel for processing.
     */
    fun onEvent(event: CsvImportEvent) {
        when (event) {
            is CsvImportEvent.FileSelected -> handleFileSelected(event.fileName, event.content)
            is CsvImportEvent.ConfirmImport -> handleConfirmImport()
            is CsvImportEvent.ClearError -> _uiState.update { it.copy(error = null, errorDetail = null) }
            is CsvImportEvent.Reset -> _uiState.update { CsvImportUiState() }
        }
    }

    // -----------------------------------------------------------------------
    // Private handlers
    // -----------------------------------------------------------------------

    /**
     * Parses the raw CSV content and transitions to the [CsvImportPhase.Preview] phase.
     * Validation errors are collected so the UI can surface them without blocking the preview.
     */
    private fun handleFileSelected(fileName: String, content: String) {
        _uiState.update { it.copy(isLoading = true) }

        try {
            val parsedRows = CsvParser.parse(content)

            if (parsedRows.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError.CSV_NO_VALID_DATA
                    )
                }
                return
            }

            // Validate the parsed rows
            val validationErrors = CsvValidator.validate(parsedRows, requiredHeaders)

            // Build preview rows – collect per-row errors for inline display
            val errorsByLine: Map<Int, List<String>> = validationErrors
                .groupBy { it.lineNumber }
                .mapValues { entry -> entry.value.map { it.message } }

            val previewRows = parsedRows.mapIndexed { index, row ->
                // CSV line 1 is the header; data rows start at line 2
                val lineNumber = index + 2
                val rowErrors = errorsByLine[lineNumber]
                CsvPreviewRow(
                    rowNumber = index + 1,
                    question = row["question"].orEmpty(),
                    choices = listOf(
                        row["choice_a"].orEmpty(),
                        row["choice_b"].orEmpty(),
                        row["choice_c"].orEmpty(),
                        row["choice_d"].orEmpty()
                    ),
                    correctAnswer = row["correct"].orEmpty(),
                    hasError = rowErrors != null,
                    errorMessage = rowErrors?.joinToString("; ")
                )
            }

            val errorMessages = validationErrors.map { "Hàng ${it.lineNumber - 1}: ${it.message}" }

            _uiState.update {
                it.copy(
                    phase = CsvImportPhase.Preview,
                    fileName = fileName,
                    rawCsvContent = content,
                    previewRows = previewRows,
                    validationErrors = errorMessages,
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = UiError.CSV_READ_FAILED,
                    errorDetail = e.message
                )
            }
        }
    }

    /**
     * Converts the validated preview rows into [QuestionDraft] objects and fires
     * [onQuestionsImported]. Rows with errors are skipped.
     */
    private fun handleConfirmImport() {
        val state = _uiState.value
        if (state.validationErrors.isNotEmpty()) return

        _uiState.update { it.copy(phase = CsvImportPhase.Importing, isLoading = true) }

        try {
            val drafts = state.previewRows
                .map { row -> row.toQuestionDraft() }

            onQuestionsImported(drafts)

            _uiState.update {
                it.copy(
                    phase = CsvImportPhase.Done,
                    isLoading = false,
                    isImported = true,
                    error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    phase = CsvImportPhase.Preview,
                    isLoading = false,
                    error = UiError.CSV_IMPORT_FAILED,
                    errorDetail = e.message
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Conversion helpers
    // -----------------------------------------------------------------------

    /**
     * Converts a [CsvPreviewRow] into a [QuestionDraft] ready for use in
     * [CreateQuizViewModel].
     *
     * The correct index is derived from the "correct" column value:
     * "a" -> 0, "b" -> 1, "c" -> 2, "d" -> 3.
     * Defaults to 0 if the value is unrecognised.
     */
    private fun CsvPreviewRow.toQuestionDraft(): QuestionDraft {
        val correctIndex = when (correctAnswer.lowercase().trim()) {
            "a" -> 0
            "b" -> 1
            "c" -> 2
            "d" -> 3
            else -> 0
        }

        val choiceDrafts = choices.map { choiceText ->
            ChoiceDraft(content = choiceText)
        }

        return QuestionDraft(
            content = question,
            choices = choiceDrafts,
            correctIndices = setOf(correctIndex),
            isMultiSelect = false
        )
    }
}
