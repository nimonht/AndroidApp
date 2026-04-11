package com.example.androidapp.ui.screens.advanced.console.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Theme-aware colors for console syntax highlighting.
 *
 * Constructed in composable scope where `MaterialTheme` is available,
 * so that the palette adapts to light and dark themes automatically.
 *
 * @property command Color for the first token in a pipeline segment (the command name).
 * @property subcommand Color for the second bare-word token (potential subcommand).
 * @property flag Color for short (`-x`) and long (`--flag`) flags.
 * @property flagValue Color for the value portion of `--key=value` flags.
 * @property string Color for quoted string literals (`"..."` / `'...'`).
 * @property operator Color for pipe (`|`) and semicolon (`;`) operators.
 * @property number Color for standalone numeric literals.
 * @property argument Color for other positional arguments.
 */
data class TokenColors(
    val command: Color,
    val subcommand: Color,
    val flag: Color,
    val flagValue: Color,
    val string: Color,
    val operator: Color,
    val number: Color,
    val argument: Color
) {
    companion object {
        /**
         * Color palette optimised for light backgrounds.
         *
         * Provides high contrast against `surfaceContainerLowest` or similar
         * light surface colors used by the console in light theme.
         */
        fun light() = TokenColors(
            command = Color(0xFF1565C0),
            subcommand = Color(0xFF00838F),
            flag = Color(0xFFE65100),
            flagValue = Color(0xFF4E342E),
            string = Color(0xFF2E7D32),
            operator = Color(0xFF7B1FA2),
            number = Color(0xFFC62828),
            argument = Color(0xFF37474F)
        )

        /**
         * Color palette optimised for dark backgrounds.
         *
         * Provides comfortable contrast against `surfaceContainerLowest` or
         * similar dark surface colors used by the console in dark theme.
         */
        fun dark() = TokenColors(
            command = Color(0xFF64B5F6),
            subcommand = Color(0xFF4DD0E1),
            flag = Color(0xFFFFB74D),
            flagValue = Color(0xFFBCAAA4),
            string = Color(0xFF81C784),
            operator = Color(0xFFCE93D8),
            number = Color(0xFFFF8A65),
            argument = Color(0xFFB0BEC5)
        )
    }
}

/**
 * [VisualTransformation] that applies theme-aware syntax-highlighting colors
 * to console input text.
 *
 * Token categorisation rules (applied per pipeline segment separated by `|` / `;`):
 * - **Command**: first bare word in a segment — colored with [TokenColors.command] + bold.
 * - **Subcommand**: second bare word if it looks like a subcommand name — [TokenColors.subcommand].
 * - **Flag**: `-x`, `--flag`, or the `--key=` portion of `--key=value` — [TokenColors.flag].
 * - **Flag value**: the value after `=` in `--key=value` — [TokenColors.flagValue].
 * - **String**: `"..."` or `'...'` quoted literals — [TokenColors.string].
 * - **Operator**: `|` and `;` — [TokenColors.operator].
 * - **Number**: standalone numeric tokens — [TokenColors.number].
 * - **Argument**: any other bare word — [TokenColors.argument].
 *
 * The transformation does not alter the text content or offset mapping;
 * it only annotates spans with color (and optionally font-weight) styles.
 *
 * @param colors The [TokenColors] instance providing the highlighting palette.
 */
class TokenHighlightTransformation(
    private val colors: TokenColors = TokenColors.dark()
) : VisualTransformation {

    /**
     * Applies token-based syntax highlighting to [text].
     *
     * The implementation re-lexes the input on every call. This is acceptable
     * because console input is typically short (< 200 chars) and the custom
     * lexer is lightweight.
     *
     * @param text The current input text as an [AnnotatedString].
     * @return A [TransformedText] with color spans applied, using identity offset mapping.
     */
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(raw)
        val spans = buildTokenSpans(raw)

        for (span in spans) {
            if (span.start < raw.length && span.end <= raw.length && span.start < span.end) {
                builder.addStyle(
                    style = span.style,
                    start = span.start,
                    end = span.end
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    /**
     * Builds a list of [StyledSpan]s by walking through the raw input character
     * by character, identifying token boundaries and assigning colors based on
     * the token's role within its pipeline segment.
     *
     * @param raw The raw console input string.
     * @return Ordered list of [StyledSpan]s covering recognised tokens.
     */
    private fun buildTokenSpans(raw: String): List<StyledSpan> {
        val spans = mutableListOf<StyledSpan>()
        var i = 0
        val len = raw.length
        // Tracks position within a pipeline segment: 0 = command, 1 = maybe subcommand, 2+ = argument
        var tokenIndexInSegment = 0

        while (i < len) {
            // Skip whitespace
            if (raw[i].isWhitespace()) {
                i++
                continue
            }

            when {
                // Pipe or semicolon operator — resets segment counter
                raw[i] == '|' || raw[i] == ';' -> {
                    spans.add(
                        StyledSpan(
                            i,
                            i + 1,
                            SpanStyle(color = colors.operator, fontWeight = FontWeight.Bold)
                        )
                    )
                    i++
                    tokenIndexInSegment = 0
                }

                // Double-quoted string
                raw[i] == '"' -> {
                    val end = findClosingQuote(raw, i, '"')
                    spans.add(StyledSpan(i, end, SpanStyle(color = colors.string)))
                    i = end
                    tokenIndexInSegment++
                }

                // Single-quoted string
                raw[i] == '\'' -> {
                    val end = findClosingQuote(raw, i, '\'')
                    spans.add(StyledSpan(i, end, SpanStyle(color = colors.string)))
                    i = end
                    tokenIndexInSegment++
                }

                // Long flag (--flag or --key=value)
                raw[i] == '-' && i + 1 < len && raw[i + 1] == '-' -> {
                    val flagNameEnd = findEndOfFlagName(raw, i + 2)
                    if (flagNameEnd < len && raw[flagNameEnd] == '=') {
                        // --key=value : color flag name (including =) and value separately
                        spans.add(
                            StyledSpan(i, flagNameEnd + 1, SpanStyle(color = colors.flag))
                        )
                        val valueEnd = findValueEnd(raw, flagNameEnd + 1)
                        if (valueEnd > flagNameEnd + 1) {
                            spans.add(
                                StyledSpan(
                                    flagNameEnd + 1,
                                    valueEnd,
                                    SpanStyle(color = colors.flagValue)
                                )
                            )
                        }
                        i = valueEnd
                    } else {
                        // Plain --flag
                        spans.add(
                            StyledSpan(i, flagNameEnd, SpanStyle(color = colors.flag))
                        )
                        i = flagNameEnd
                    }
                    tokenIndexInSegment++
                }

                // Short flag (-x) or compound short flags (-abc)
                raw[i] == '-' && i + 1 < len && raw[i + 1].isLetter() -> {
                    val end = findEndOfShortFlag(raw, i + 1)
                    spans.add(StyledSpan(i, end, SpanStyle(color = colors.flag)))
                    i = end
                    tokenIndexInSegment++
                }

                // Bare word — command, subcommand, number, or argument
                else -> {
                    val end = findEndOfBareWord(raw, i)
                    val word = raw.substring(i, end)
                    val style = when {
                        tokenIndexInSegment == 0 -> {
                            SpanStyle(
                                color = colors.command,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        tokenIndexInSegment == 1
                                && !word.startsWith("-")
                                && word.all { ch ->
                            ch.isLetter() || ch == '-' || ch == '_'
                        } -> {
                            SpanStyle(color = colors.subcommand)
                        }

                        word.toDoubleOrNull() != null -> {
                            SpanStyle(color = colors.number)
                        }

                        else -> {
                            SpanStyle(color = colors.argument)
                        }
                    }
                    spans.add(StyledSpan(i, end, style))
                    i = end
                    tokenIndexInSegment++
                }
            }
        }

        return spans
    }

    // -- Helper methods for token boundary detection --------------------------

    /**
     * Finds the index immediately after the closing [quoteChar], handling
     * backslash escapes inside double quotes.
     *
     * @param raw The full input string.
     * @param start The index of the opening quote character.
     * @param quoteChar The quote character (`"` or `'`).
     * @return The index immediately after the closing quote, or [raw].length
     *   if the quote is unterminated.
     */
    private fun findClosingQuote(raw: String, start: Int, quoteChar: Char): Int {
        var i = start + 1
        val supportsEscapes = quoteChar == '"'
        while (i < raw.length) {
            if (supportsEscapes && raw[i] == '\\' && i + 1 < raw.length) {
                i += 2
                continue
            }
            if (raw[i] == quoteChar) {
                return i + 1
            }
            i++
        }
        return raw.length
    }

    /**
     * Finds the end of a long flag **name** starting after the `--` prefix.
     *
     * Stops at `=`, whitespace, `|`, `;`, or end of input — unlike the old
     * `findEndOfFlag` this does **not** consume the `=value` portion.
     *
     * @param raw The full input string.
     * @param start The index of the first character after `--`.
     * @return The index immediately after the flag name.
     */
    private fun findEndOfFlagName(raw: String, start: Int): Int {
        var i = start
        while (i < raw.length &&
            (raw[i].isLetterOrDigit() || raw[i] == '-' || raw[i] == '_')
        ) {
            i++
        }
        return i
    }

    /**
     * Finds the end of a value portion after `=` in `--key=value`.
     *
     * If the value starts with a quote, delegates to [findClosingQuote].
     * Otherwise treats it as a bare word.
     *
     * @param raw The full input string.
     * @param start The index of the first character of the value (after `=`).
     * @return The index immediately after the value.
     */
    private fun findValueEnd(raw: String, start: Int): Int {
        if (start >= raw.length) return start
        return when (raw[start]) {
            '"' -> findClosingQuote(raw, start, '"')
            '\'' -> findClosingQuote(raw, start, '\'')
            else -> findEndOfBareWord(raw, start)
        }
    }

    /**
     * Finds the end of a short flag block starting after the `-`.
     * Handles compound flags (-abc) and -k=value syntax.
     *
     * @param raw The full input string.
     * @param start The index of the first character after `-`.
     * @return The index immediately after the flag(s).
     */
    private fun findEndOfShortFlag(raw: String, start: Int): Int {
        var i = start
        while (i < raw.length && raw[i].isLetterOrDigit()) {
            i++
        }
        if (i < raw.length && raw[i] == '=') {
            i++ // skip '='
            i = findValueEnd(raw, i)
        }
        return i
    }

    /**
     * Finds the end of a bare (unquoted) word. A word ends at whitespace,
     * `|`, `;`, or end of input. Backslash-space is treated as an escaped
     * space within the word.
     *
     * @param raw The full input string.
     * @param start The start index of the bare word.
     * @return The index immediately after the word.
     */
    private fun findEndOfBareWord(raw: String, start: Int): Int {
        var i = start
        while (i < raw.length) {
            when {
                raw[i] == '\\' && i + 1 < raw.length -> i += 2
                raw[i].isWhitespace() || raw[i] == '|' || raw[i] == ';' -> break
                else -> i++
            }
        }
        return i
    }

    /**
     * Represents a styled span within the input text.
     *
     * @property start Start index (inclusive) in the raw input.
     * @property end End index (exclusive) in the raw input.
     * @property style The [SpanStyle] to apply to this range.
     */
    private data class StyledSpan(
        val start: Int,
        val end: Int,
        val style: SpanStyle
    )
}
