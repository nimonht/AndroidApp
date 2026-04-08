package com.example.androidapp.ui.screens.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.ui.common.toMessage
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.example.androidapp.ui.theme.QuizzezTheme

// ---------------------------------------------------------------------------
// Screen entry point
// ---------------------------------------------------------------------------

/**
 * Entry point for the CSV Import screen.
 *
 * Instantiates [CsvImportViewModel] with a [ViewModelProvider.Factory] that injects the
 * [onQuestionsImported] callback, then delegates rendering to [CsvImportContent].
 *
 * @param onNavigateBack Invoked when the user presses the back arrow or after a successful import.
 * @param onQuestionsImported Invoked with the imported [QuestionDraft] list once the user confirms.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun CsvImportScreen(
    onNavigateBack: () -> Unit,
    onQuestionsImported: (List<QuestionDraft>) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CsvImportViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CsvImportViewModel(onQuestionsImported) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CsvImportContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------
// Stateless content
// ---------------------------------------------------------------------------

/**
 * Stateless content composable for the CSV Import screen.
 *
 * Renders a different body depending on the current [CsvImportUiState.phase]:
 * - [CsvImportPhase.FilePicker] – instructions and file picker button.
 * - [CsvImportPhase.Preview] – preview table, validation errors, confirm/reset buttons.
 * - [CsvImportPhase.Importing] – full-screen loading indicator.
 * - [CsvImportPhase.Done] – success message.
 *
 * @param uiState Current UI state.
 * @param onEvent Dispatcher for [CsvImportEvent] values.
 * @param onNavigateBack Invoked to close the screen.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun CsvImportContent(
    uiState: CsvImportUiState,
    onEvent: (CsvImportEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface errors via Snackbar
    val errorMessage = uiState.error?.toMessage(detail = uiState.errorDetail)
    LaunchedEffect(uiState.error) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onEvent(CsvImportEvent.ClearError)
        }
    }

    // Navigate back automatically once import is confirmed
    LaunchedEffect(uiState.isImported) {
        if (uiState.isImported) onNavigateBack()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.csv_import_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (uiState.phase) {
                CsvImportPhase.FilePicker -> FilePickerPhase(
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxSize()
                )

                CsvImportPhase.Preview -> PreviewPhase(
                    uiState = uiState,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxSize()
                )

                CsvImportPhase.Importing -> ImportingPhase(
                    modifier = Modifier.fillMaxSize()
                )

                CsvImportPhase.Done -> DonePhase(
                    importedCount = uiState.previewRows.count { !it.hasError },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Phase – FilePicker
// ---------------------------------------------------------------------------

/**
 * Phase 1: prompts the user to pick a CSV file using the system file picker.
 *
 * Reads the file content from the returned [Uri] and dispatches [CsvImportEvent.FileSelected].
 *
 * @param onEvent Dispatcher for [CsvImportEvent] values.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun FilePickerPhase(
    onEvent: (CsvImportEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "file.csv"
            val content = context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            onEvent(CsvImportEvent.FileSelected(fileName = fileName, content = content))
        } catch (_: Exception) {
            // Content resolver failure is surfaced via ViewModel error state
        }
    }

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FileOpen,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.csv_import_instructions),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Template format hint
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = stringResource(R.string.csv_import_template_format),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { launcher.launch("text/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.csv_import_pick_file),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Phase – Preview
// ---------------------------------------------------------------------------

/**
 * Phase 2: displays a paginated preview of parsed CSV rows, any validation errors,
 * and action buttons to confirm or reset.
 *
 * @param uiState Current UI state containing [CsvImportUiState.previewRows] and
 *   [CsvImportUiState.validationErrors].
 * @param onEvent Dispatcher for [CsvImportEvent] values.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun PreviewPhase(
    uiState: CsvImportUiState,
    onEvent: (CsvImportEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasErrors = uiState.validationErrors.isNotEmpty()
    // Show only the first 5 rows in the preview
    val previewRows = uiState.previewRows.take(5)

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // File name badge
        item {
            Spacer(modifier = Modifier.height(8.dp))
            FileNameBadge(fileName = uiState.fileName)
        }

        // Section header
        item {
            Text(
                text = stringResource(R.string.csv_import_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Preview table header row
        item {
            PreviewTableHeader()
        }

        // Data rows
        items(previewRows) { row ->
            PreviewRowCard(row = row)
        }

        // "No data" fallback
        if (previewRows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.csv_import_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Validation errors section
        if (hasErrors) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ValidationErrorsSection(errors = uiState.validationErrors)
            }
        }

        // Action buttons
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ActionButtons(
                hasErrors = hasErrors,
                onConfirm = { onEvent(CsvImportEvent.ConfirmImport) },
                onReset = { onEvent(CsvImportEvent.Reset) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Displays the selected file name in a pill-shaped surface badge.
 *
 * @param fileName Name of the selected file.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun FileNameBadge(
    fileName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.csv_import_file_selected, fileName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Renders a sticky header row for the preview table with column labels.
 *
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun PreviewTableHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = stringResource(R.string.csv_import_column_question),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.csv_import_column_correct),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.width(48.dp)
        )
    }
}

/**
 * Card representing a single parsed CSV data row in the preview list.
 *
 * Rows with errors are highlighted using the error colour scheme.
 *
 * @param row The [CsvPreviewRow] to display.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun PreviewRowCard(
    row: CsvPreviewRow,
    modifier: Modifier = Modifier
) {
    val containerColor = if (row.hasError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (row.hasError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Row number
                Text(
                    text = stringResource(R.string.csv_import_row_number, row.rowNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (row.hasError) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp)
                )

                // Question text
                Text(
                    text = row.question.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.hasError) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Correct answer badge
                CorrectAnswerBadge(
                    letter = row.correctAnswer.uppercase(),
                    hasError = row.hasError,
                    modifier = Modifier.width(48.dp)
                )
            }

            // Choices preview (condensed)
            if (row.choices.any { it.isNotBlank() }) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    color = if (row.hasError) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val labels = listOf("A", "B", "C", "D")
                    row.choices.forEachIndexed { index, choice ->
                        ChoiceChip(
                            label = labels.getOrElse(index) { "${index + 1}" },
                            text = choice,
                            isCorrect = row.correctAnswer.lowercase().trim() ==
                                    labels.getOrElse(index) { "" }.lowercase(),
                            hasError = row.hasError,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Inline error message
            if (row.hasError && row.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = row.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Small badge displaying the correct answer letter (A, B, C, or D).
 *
 * @param letter Uppercase letter of the correct answer.
 * @param hasError Whether the parent row has a validation error.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun CorrectAnswerBadge(
    letter: String,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (hasError) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary
    val contentColor = if (hasError) MaterialTheme.colorScheme.onError
    else MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.ifBlank { "?" },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * Small chip representing a single answer choice within a preview row.
 *
 * Correct choices are visually highlighted.
 *
 * @param label Single letter label (A, B, C, D).
 * @param text Choice text content.
 * @param isCorrect Whether this chip represents the correct answer.
 * @param hasError Whether the parent row has a validation error.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun ChoiceChip(
    label: String,
    text: String,
    isCorrect: Boolean,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isCorrect && !hasError -> MaterialTheme.colorScheme.primaryContainer
        hasError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isCorrect && !hasError -> MaterialTheme.colorScheme.onPrimaryContainer
        hasError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(4.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        Text(
            text = text.ifBlank { "—" },
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Expandable section listing all validation errors collected from [CsvValidator].
 *
 * @param errors List of human-readable error messages.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun ValidationErrorsSection(
    errors: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.csv_import_errors_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            errors.forEach { error ->
                Text(
                    text = "• $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Bottom action buttons for the Preview phase.
 *
 * The confirm button is disabled when [hasErrors] is true.
 *
 * @param hasErrors Whether validation errors exist that should block importing.
 * @param onConfirm Callback for the confirm/import action.
 * @param onReset Callback for the reset/re-pick action.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun ActionButtons(
    hasErrors: Boolean,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onConfirm,
            enabled = !hasErrors,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        ) {
            Text(
                text = stringResource(R.string.csv_import_confirm),
                style = MaterialTheme.typography.labelLarge
            )
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = stringResource(R.string.csv_import_reset),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Phase – Importing
// ---------------------------------------------------------------------------

/**
 * Full-screen loading indicator shown while [CsvImportPhase.Importing] is active.
 *
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun ImportingPhase(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.loading_processing),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Phase – Done
// ---------------------------------------------------------------------------

/**
 * Success state shown after questions have been imported successfully.
 *
 * @param importedCount Number of questions that were imported.
 * @param modifier Modifier for styling and layout customisation.
 */
@Composable
fun DonePhase(
    importedCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.csv_import_success, importedCount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/** Light theme preview of the FilePicker phase. */
@Preview(name = "FilePicker – Light", showBackground = true)
@Composable
private fun FilePickerPhaseLightPreview() {
    QuizzezTheme(darkTheme = false) {
        CsvImportContent(
            uiState = CsvImportUiState(phase = CsvImportPhase.FilePicker),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

/** Dark theme preview of the FilePicker phase. */
@Preview(
    name = "FilePicker – Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun FilePickerPhaseDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            CsvImportContent(
                uiState = CsvImportUiState(phase = CsvImportPhase.FilePicker),
                onEvent = {},
                onNavigateBack = {}
            )
        }
    }
}

/** Light theme preview of the Preview phase with sample data. */
@Preview(name = "Preview Phase – Light", showBackground = true)
@Composable
private fun PreviewPhaseLightPreview() {
    val sampleRows = listOf(
        CsvPreviewRow(
            rowNumber = 1,
            question = "What is the capital of Vietnam?",
            choices = listOf("Hanoi", "Ho Chi Minh City", "Da Nang", "Hue"),
            correctAnswer = "a"
        ),
        CsvPreviewRow(
            rowNumber = 2,
            question = "2 + 2 = ?",
            choices = listOf("3", "4", "5", "6"),
            correctAnswer = "b"
        ),
        CsvPreviewRow(
            rowNumber = 3,
            question = "",
            choices = listOf("", "", "", ""),
            correctAnswer = "x",
            hasError = true,
            errorMessage = "Missing or blank value for required column: 'question'."
        )
    )
    QuizzezTheme(darkTheme = false) {
        CsvImportContent(
            uiState = CsvImportUiState(
                phase = CsvImportPhase.Preview,
                fileName = "questions.csv",
                previewRows = sampleRows,
                validationErrors = listOf("Hàng 4: Missing or blank value for required column: 'question'.")
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

/** Dark theme preview of the Preview phase with sample data. */
@Preview(
    name = "Preview Phase – Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewPhaseDarkPreview() {
    val sampleRows = listOf(
        CsvPreviewRow(
            rowNumber = 1,
            question = "What is the capital of Vietnam?",
            choices = listOf("Hanoi", "Ho Chi Minh City", "Da Nang", "Hue"),
            correctAnswer = "a"
        ),
        CsvPreviewRow(
            rowNumber = 2,
            question = "2 + 2 = ?",
            choices = listOf("3", "4", "5", "6"),
            correctAnswer = "b"
        )
    )
    QuizzezTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            CsvImportContent(
                uiState = CsvImportUiState(
                    phase = CsvImportPhase.Preview,
                    fileName = "math_questions.csv",
                    previewRows = sampleRows,
                    validationErrors = emptyList()
                ),
                onEvent = {},
                onNavigateBack = {}
            )
        }
    }
}

/** Light theme preview of the Done phase. */
@Preview(name = "Done Phase – Light", showBackground = true)
@Composable
private fun DonePhaseLightPreview() {
    QuizzezTheme(darkTheme = false) {
        DonePhase(importedCount = 10)
    }
}

/** Dark theme preview of the Done phase. */
@Preview(
    name = "Done Phase – Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DonePhaseDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DonePhase(importedCount = 10)
        }
    }
}
