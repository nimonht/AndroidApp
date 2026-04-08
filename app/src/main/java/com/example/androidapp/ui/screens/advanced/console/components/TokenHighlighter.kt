package com.example.androidapp.ui.screens.advanced.console.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.androidapp.domain.console.CommandLexer
import com.example.androidapp.domain.console.CommandToken

/**
 * Colors used for syntax highlighting of console input tokens.
 *
 * These are intentionally hardcoded because they must be legible on the
 * dark console background regardless of the current Material theme.
 */
private object TokenColors {
    /** Commands and keywords — blue. */
    val KEYWORD = Color(0xFF42A5F5)

    /** Flags (short and long) — amber. */
    val FLAG = Color(0xFFFFC107)

    /** String literals — green. */
    val STRING = Color(0xFF66BB6A)

    /** Pipe and semicolon operators — magenta. */
    val OPERATOR = Color(0xFFCE93D8)

    /** Positional arguments — default light text. */
    val ARGUMENT = Color(0xFFE0E0E0)
}

/**
 * [VisualTransformation] that applies syntax-highlighting colors to console
 * input text based on token types produced by [CommandLexer].
 *
 * Token-to-color mapping:
 * - [CommandToken.Keyword] -> blue (commands / bare words)
 * - [CommandToken.Flag] / [CommandToken.FlagValue] -> amber
 * - [CommandToken.StringLiteral] -> green
 * - [CommandToken.Pipe] / [CommandToken.Semicolon] -> magenta
 * - Other tokens -> default light color
 *
 * The transformation does not alter the text content or offset mapping;
 * it only annotates spans with color styles.
 */
class TokenHighlightTransformation : VisualTransformation {

    /**
     * Applies token-based syntax highlighting to [text].
     *
     * The implementation re-lexes the input on every call. This is acceptable
     * because console input is typically short (< 200 chars) and [CommandLexer]
     * is lightweight.
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
                    style = SpanStyle(color = span.color),
                    start = span.start,
                    end = span.end
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    /**
     * Builds a list of [ColorSpan]s by lexing the raw input and mapping each
     * token back to its character range in the original string.
     *
     * Because [CommandLexer] strips quotes and normalises escape sequences,
     * we cannot rely on token values to find exact positions. Instead we
     * walk through the raw input character by character, mirroring the
     * lexer's logic at a high level to identify token boundaries.
     *
     * @param raw The raw console input string.
     * @return Ordered list of [ColorSpan]s covering recognised tokens.
     */
    private fun buildTokenSpans(raw: String): List<ColorSpan> {
        val spans = mutableListOf<ColorSpan>()
        var i = 0
        val len = raw.length
        var isFirstTokenInSegment = true

        while (i < len) {
            // Skip whitespace
            if (raw[i].isWhitespace()) {
                i++
                continue
            }

            when {
                // Pipe operator
                raw[i] == '|' -> {
                    spans.add(ColorSpan(i, i + 1, TokenColors.OPERATOR))
                    i++
                    isFirstTokenInSegment = true
                }

                // Semicolon operator
                raw[i] == ';' -> {
                    spans.add(ColorSpan(i, i + 1, TokenColors.OPERATOR))
                    i++
                    isFirstTokenInSegment = true
                }

                // Double-quoted string
                raw[i] == '"' -> {
                    val end = findClosingQuote(raw, i, '"')
                    spans.add(ColorSpan(i, end, TokenColors.STRING))
                    i = end
                    isFirstTokenInSegment = false
                }

                // Single-quoted string
                raw[i] == '\'' -> {
                    val end = findClosingQuote(raw, i, '\'')
                    spans.add(ColorSpan(i, end, TokenColors.STRING))
                    i = end
                    isFirstTokenInSegment = false
                }

                // Long flag (--flag or --key=value)
                raw[i] == '-' && i + 1 < len && raw[i + 1] == '-' -> {
                    val end = findEndOfFlag(raw, i + 2)
                    spans.add(ColorSpan(i, end, TokenColors.FLAG))
                    i = end
                    isFirstTokenInSegment = false
                }

                // Short flag (-x) or compound short flags (-abc)
                raw[i] == '-' && i + 1 < len && raw[i + 1].isLetter() -> {
                    val end = findEndOfShortFlag(raw, i + 1)
                    spans.add(ColorSpan(i, end, TokenColors.FLAG))
                    i = end
                    isFirstTokenInSegment = false
                }

                // Bare word — keyword (if first in segment) or argument
                else -> {
                    val end = findEndOfBareWord(raw, i)
                    val color = if (isFirstTokenInSegment) {
                        TokenColors.KEYWORD
                    } else {
                        TokenColors.ARGUMENT
                    }
                    spans.add(ColorSpan(i, end, color))
                    i = end
                    isFirstTokenInSegment = false
                }
            }
        }

        return spans
    }

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
     * Finds the end of a long flag starting after the `--` prefix.
     * Includes any `=value` portion.
     *
     * @param raw The full input string.
     * @param start The index of the first character after `--`.
     * @return The index immediately after the flag (and its value, if present).
     */
    private fun findEndOfFlag(raw: String, start: Int): Int {
        var i = start
        // Read flag name
        while (i < raw.length && (raw[i].isLetterOrDigit() || raw[i] == '-' || raw[i] == '_')) {
            i++
        }
        // Check for =value
        if (i < raw.length && raw[i] == '=') {
            i++ // skip '='
            i = when {
                i >= raw.length -> i
                raw[i] == '"' -> findClosingQuote(raw, i, '"')
                raw[i] == '\'' -> findClosingQuote(raw, i, '\'')
                else -> findEndOfBareWord(raw, i)
            }
        }
        return i
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
        // Read all contiguous letters/digits
        while (i < raw.length && raw[i].isLetterOrDigit()) {
            i++
        }
        // Check for =value on single-letter short flag
        if (i < raw.length && raw[i] == '=') {
            i++ // skip '='
            i = when {
                i >= raw.length -> i
                raw[i] == '"' -> findClosingQuote(raw, i, '"')
                raw[i] == '\'' -> findClosingQuote(raw, i, '\'')
                else -> findEndOfBareWord(raw, i)
            }
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
     * Represents a colored span within the input text.
     *
     * @property start Start index (inclusive) in the raw input.
     * @property end End index (exclusive) in the raw input.
     * @property color The highlight color for this span.
     */
    private data class ColorSpan(
        val start: Int,
        val end: Int,
        val color: Color
    )
}
