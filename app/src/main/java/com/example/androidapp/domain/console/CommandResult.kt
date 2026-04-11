package com.example.androidapp.domain.console

/**
 * Represents the result of executing a console command.
 *
 * @property output List of styled output lines produced by the command.
 * @property isSuccess Whether the command completed successfully.
 * @property exitCode Numeric exit code (0 = success, non-zero = failure).
 */
data class CommandResult(
    val output: List<OutputLine>,
    val isSuccess: Boolean,
    val exitCode: Int = if (isSuccess) 0 else 1
) {
    companion object {
        /**
         * Creates a successful result with a single normal-styled line.
         *
         * @param message The output message text.
         * @return A successful [CommandResult] containing one [OutputLine].
         */
        fun success(message: String): CommandResult = CommandResult(
            output = listOf(OutputLine(message, OutputStyle.NORMAL)),
            isSuccess = true
        )

        /**
         * Creates a successful result with multiple output lines.
         *
         * @param lines The styled output lines.
         * @return A successful [CommandResult].
         */
        fun success(lines: List<OutputLine>): CommandResult = CommandResult(
            output = lines,
            isSuccess = true
        )

        /**
         * Creates a failed result with an error message.
         *
         * @param message The error message text.
         * @param exitCode Numeric exit code (defaults to 1).
         * @return A failed [CommandResult] with the message styled as [OutputStyle.ERROR].
         */
        fun error(message: String, exitCode: Int = 1): CommandResult = CommandResult(
            output = listOf(OutputLine(message, OutputStyle.ERROR)),
            isSuccess = false,
            exitCode = exitCode
        )

        /**
         * Creates an empty successful result (no output).
         *
         * @return A successful [CommandResult] with an empty output list.
         */
        fun empty(): CommandResult = CommandResult(
            output = emptyList(),
            isSuccess = true
        )
    }
}

/**
 * A single line of styled console output.
 *
 * @property text The text content of this output line.
 * @property style The visual style to apply when rendering this line.
 */
data class OutputLine(
    val text: String,
    val style: OutputStyle = OutputStyle.NORMAL
)

/**
 * Visual styles for console output lines.
 *
 * Each style maps to a distinct color or formatting treatment in the
 * console UI, enabling rich, semantically-colored command output.
 */
enum class OutputStyle {
    /** Default unstyled text. */
    NORMAL,

    /** Success messages (green). */
    SUCCESS,

    /** Error messages (red). */
    ERROR,

    /** Warning messages (amber/yellow). */
    WARNING,

    /** Informational messages (blue). */
    INFO,

    /** Section headers (bold, prominent). */
    HEADER,

    /** De-emphasized / secondary text (gray). */
    MUTED,

    /** Table column headers (bold, underlined). */
    TABLE_HEADER,

    /** Table data rows (monospace, aligned). */
    TABLE_ROW,

    /** Code or pre-formatted text (monospace, distinct background). */
    CODE
}
