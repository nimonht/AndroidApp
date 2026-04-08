package com.example.androidapp.ui.screens.advanced.console.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.ui.screens.advanced.console.StyledOutputLine
import com.example.androidapp.ui.theme.QuizzezTheme

// -- ColorScheme extensions for console output colors -------------------------

/** Green used for success output lines. */
val ColorScheme.consoleSuccess: Color
    get() = Color(0xFF4CAF50)

/** Red used for error output lines. */
val ColorScheme.consoleError: Color
    get() = Color(0xFFEF5350)

/** Amber used for warning output lines. */
val ColorScheme.consoleWarning: Color
    get() = Color(0xFFFFC107)

/** Blue used for informational output lines. */
val ColorScheme.consoleInfo: Color
    get() = Color(0xFF42A5F5)

/** Green-ish tone used for code/preformatted output lines. */
val ColorScheme.consoleCode: Color
    get() = Color(0xFF66BB6A)

// -- Composable ---------------------------------------------------------------

/**
 * Renders a single console output line with monospace font and color
 * determined by the [OutputStyle] of the line.
 *
 * Color mapping:
 * - [OutputStyle.NORMAL]: onSurface
 * - [OutputStyle.SUCCESS]: [ColorScheme.consoleSuccess]
 * - [OutputStyle.ERROR]: [ColorScheme.consoleError]
 * - [OutputStyle.WARNING]: [ColorScheme.consoleWarning]
 * - [OutputStyle.INFO]: [ColorScheme.consoleInfo]
 * - [OutputStyle.HEADER]: primary, bold
 * - [OutputStyle.MUTED]: onSurfaceVariant
 * - [OutputStyle.TABLE_HEADER]: primary, bold
 * - [OutputStyle.TABLE_ROW]: onSurface
 * - [OutputStyle.CODE]: [ColorScheme.consoleCode] with subtle background
 *
 * @param line The [StyledOutputLine] to render.
 * @param modifier Modifier for external layout customisation.
 */
@Composable
fun ConsoleOutputLine(
    line: StyledOutputLine,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val textColor: Color = when (line.style) {
        OutputStyle.NORMAL -> colorScheme.onSurface
        OutputStyle.SUCCESS -> colorScheme.consoleSuccess
        OutputStyle.ERROR -> colorScheme.consoleError
        OutputStyle.WARNING -> colorScheme.consoleWarning
        OutputStyle.INFO -> colorScheme.consoleInfo
        OutputStyle.HEADER -> colorScheme.primary
        OutputStyle.MUTED -> colorScheme.onSurfaceVariant
        OutputStyle.TABLE_HEADER -> colorScheme.primary
        OutputStyle.TABLE_ROW -> colorScheme.onSurface
        OutputStyle.CODE -> colorScheme.consoleCode
    }

    val fontWeight: FontWeight = when (line.style) {
        OutputStyle.HEADER, OutputStyle.TABLE_HEADER -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    val backgroundModifier = when (line.style) {
        OutputStyle.CODE -> Modifier.background(
            colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )

        else -> Modifier
    }

    Text(
        text = line.text,
        color = textColor,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .padding(horizontal = 8.dp, vertical = 1.dp)
    )
}

// region Previews

@Preview(name = "Normal Line - Light", showBackground = true)
@Preview(
    name = "Normal Line - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineNormalPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "Ket qua: Thanh cong",
                    style = OutputStyle.NORMAL
                )
            )
        }
    }
}

@Preview(name = "Success Line - Light", showBackground = true)
@Preview(
    name = "Success Line - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineSuccessPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "[OK] Da tao quiz thanh cong.",
                    style = OutputStyle.SUCCESS
                )
            )
        }
    }
}

@Preview(name = "Error Line - Light", showBackground = true)
@Preview(
    name = "Error Line - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineErrorPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "Loi: Khong tim thay lenh 'xyz'",
                    style = OutputStyle.ERROR
                )
            )
        }
    }
}

@Preview(name = "Warning Line - Light", showBackground = true)
@Preview(
    name = "Warning Line - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineWarningPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "Canh bao: Lenh nay thuc hien thao tac khong the hoan tac!",
                    style = OutputStyle.WARNING
                )
            )
        }
    }
}

@Preview(name = "Info Line - Light", showBackground = true)
@Preview(
    name = "Info Line - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineInfoPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "Thong tin: Dang ket noi toi may chu...",
                    style = OutputStyle.INFO
                )
            )
        }
    }
}

@Preview(name = "Header Line - Light", showBackground = true)
@Preview(
    name = "Header Line - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineHeaderPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "HELP - DANH SACH LENH",
                    style = OutputStyle.HEADER
                )
            )
        }
    }
}

@Preview(name = "Code Line - Light", showBackground = true)
@Preview(
    name = "Code Line - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineCodePreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "  ls -q --sort=date | grep \"kotlin\"",
                    style = OutputStyle.CODE
                )
            )
        }
    }
}

@Preview(name = "Muted Line - Light", showBackground = true)
@Preview(
    name = "Muted Line - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineMutedPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "  Bi danh: list, dir",
                    style = OutputStyle.MUTED
                )
            )
        }
    }
}

@Preview(name = "Table Header - Light", showBackground = true)
@Preview(
    name = "Table Header - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineTableHeaderPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "ID          TIEU DE               NGAY TAO",
                    style = OutputStyle.TABLE_HEADER
                )
            )
        }
    }
}

@Preview(name = "Table Row - Light", showBackground = true)
@Preview(
    name = "Table Row - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ConsoleOutputLineTableRowPreview() {
    QuizzezTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConsoleOutputLine(
                line = StyledOutputLine(
                    text = "abc123      Toan lop 10            2024-01-15",
                    style = OutputStyle.TABLE_ROW
                )
            )
        }
    }
}

// endregion
