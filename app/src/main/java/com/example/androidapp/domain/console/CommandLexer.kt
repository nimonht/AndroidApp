package com.example.androidapp.domain.console

/**
 * Lexical analyser that splits raw console input into a stream of [CommandToken]s.
 *
 * Handles:
 * - Double-quoted strings (`"hello world"`) and single-quoted strings (`'hello'`)
 * - Escape characters (`\"`, `\\`, `\ ` for escaped space)
 * - Long flags (`--flag`, `--key=value`)
 * - Short flags (`-q`) and compound short flags (`-abc` expanded to `-a -b -c`)
 * - Pipe `|` and semicolon `;` operators
 * - Bare words as keywords/arguments
 *
 * The lexer is a pure function with no side effects or Android dependencies,
 * residing in the domain layer.
 */
object CommandLexer {

    /**
     * Tokenise a raw console input string into an ordered list of [CommandToken]s.
     *
     * @param input The raw user input from the console text field.
     * @return An ordered list of tokens representing the parsed input.
     */
    fun tokenize(input: String): List<CommandToken> {
        val tokens = mutableListOf<CommandToken>()
        var i = 0
        val len = input.length

        while (i < len) {
            // Skip whitespace
            if (input[i].isWhitespace()) {
                i++
                continue
            }

            when {
                // Pipe operator
                input[i] == '|' -> {
                    tokens.add(CommandToken.Pipe())
                    i++
                }

                // Semicolon operator
                input[i] == ';' -> {
                    tokens.add(CommandToken.Semicolon)
                    i++
                }

                // Quoted string (double quotes)
                input[i] == '"' -> {
                    val result = readQuotedString(input, i, '"')
                    tokens.add(CommandToken.StringLiteral(result.value))
                    i = result.endIndex
                }

                // Quoted string (single quotes)
                input[i] == '\'' -> {
                    val result = readQuotedString(input, i, '\'')
                    tokens.add(CommandToken.StringLiteral(result.value))
                    i = result.endIndex
                }

                // Long flag (--flag or --key=value)
                input[i] == '-' && i + 1 < len && input[i + 1] == '-' -> {
                    val result = readLongFlag(input, i)
                    tokens.add(result.token)
                    i = result.endIndex
                }

                // Short flag (-q) or compound short flags (-abc)
                input[i] == '-' && i + 1 < len && input[i + 1].isLetter() -> {
                    val result = readShortFlags(input, i)
                    tokens.addAll(result.tokens)
                    i = result.endIndex
                }

                // Bare word (keyword or argument)
                else -> {
                    val result = readBareWord(input, i)
                    tokens.add(CommandToken.Keyword(result.value))
                    i = result.endIndex
                }
            }
        }

        return tokens
    }

    /**
     * Read a quoted string starting at position [start] (which must be the
     * opening quote character). Supports backslash escapes inside double quotes.
     * Single-quoted strings are treated as raw (no escape processing).
     */
    private fun readQuotedString(input: String, start: Int, quoteChar: Char): LexResult {
        val sb = StringBuilder()
        var i = start + 1 // skip opening quote
        val len = input.length
        val supportsEscapes = quoteChar == '"'

        while (i < len) {
            when {
                supportsEscapes && input[i] == '\\' && i + 1 < len -> {
                    val next = input[i + 1]
                    when (next) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        else -> {
                            sb.append('\\')
                            sb.append(next)
                        }
                    }
                    i += 2
                }

                input[i] == quoteChar -> {
                    // Closing quote found
                    return LexResult(sb.toString(), i + 1)
                }

                else -> {
                    sb.append(input[i])
                    i++
                }
            }
        }

        // Unterminated quote -- return what we have
        return LexResult(sb.toString(), i)
    }

    /**
     * Read a long flag starting at position [start] (which is the first `-`).
     * Handles `--flag` (boolean) and `--key=value` (valued) forms.
     */
    private fun readLongFlag(input: String, start: Int): FlagLexResult {
        var i = start + 2 // skip "--"
        val len = input.length
        val nameSb = StringBuilder()

        // Read the flag name (letters, digits, hyphens)
        while (i < len && (input[i].isLetterOrDigit() || input[i] == '-' || input[i] == '_')) {
            nameSb.append(input[i])
            i++
        }

        val flagName = nameSb.toString()

        // Check for =value
        if (i < len && input[i] == '=') {
            i++ // skip '='
            val valueResult = readFlagValue(input, i)
            return FlagLexResult(
                token = CommandToken.FlagValue(flagName, valueResult.value),
                endIndex = valueResult.endIndex
            )
        }

        return FlagLexResult(
            token = CommandToken.Flag(flagName),
            endIndex = i
        )
    }

    /**
     * Read the value part of a `--key=value` flag. The value can be a quoted
     * string or a bare word.
     */
    private fun readFlagValue(input: String, start: Int): LexResult {
        if (start >= input.length) return LexResult("", start)

        return when (input[start]) {
            '"' -> readQuotedString(input, start, '"')
            '\'' -> readQuotedString(input, start, '\'')
            else -> readBareWord(input, start)
        }
    }

    /**
     * Read short flags starting at position [start] (which is the `-`).
     *
     * A single character after `-` produces one [CommandToken.Flag].
     * Multiple characters (`-abc`) are expanded into separate flags:
     * `-a`, `-b`, `-c`. However, if the characters after `-` look like
     * a number (e.g. `-3`), it is treated as an argument instead.
     *
     * The pattern `-<letter><digits>` (e.g. `-n20`, `-c5`) is treated as
     * a flag with a value: `Flag("n")` with value `"20"`.
     */
    private fun readShortFlags(input: String, start: Int): ShortFlagLexResult {
        var i = start + 1 // skip '-'
        val len = input.length
        val tokens = mutableListOf<CommandToken>()
        val charsBuf = StringBuilder()

        // Read all contiguous letters/digits after the dash
        while (i < len && (input[i].isLetterOrDigit())) {
            charsBuf.append(input[i])
            i++
        }

        val chars = charsBuf.toString()

        // If it looks numeric (e.g. -3, -100), treat as a keyword/argument
        if (chars.all { it.isDigit() }) {
            return ShortFlagLexResult(
                tokens = listOf(CommandToken.Keyword("-$chars")),
                endIndex = i
            )
        }

        // Single letter flag might have a value attached (e.g. -n20 or -n=value)
        if (chars.length == 1) {
            // Check if next char is '=' for -k=value syntax
            if (i < len && input[i] == '=') {
                i++ // skip '='
                val valueResult = readFlagValue(input, i)
                tokens.add(CommandToken.FlagValue(chars, valueResult.value))
                return ShortFlagLexResult(tokens, valueResult.endIndex)
            }
            tokens.add(CommandToken.Flag(chars))
        } else {
            // Check for -<letter><digits> pattern (e.g. -n20, -c5)
            val firstChar = chars[0]
            val rest = chars.substring(1)
            if (firstChar.isLetter() && rest.all { it.isDigit() }) {
                tokens.add(CommandToken.FlagValue(firstChar.toString(), rest))
            } else {
                // Compound short flags: -abc -> Flag(a), Flag(b), Flag(c)
                for (ch in chars) {
                    tokens.add(CommandToken.Flag(ch.toString()))
                }
            }
        }

        return ShortFlagLexResult(tokens, i)
    }

    /**
     * Read a bare (unquoted) word starting at [start]. A word ends at
     * whitespace, `|`, `;`, or end of input. Backslash-space (`\ `) is
     * treated as an escaped space within the word.
     */
    private fun readBareWord(input: String, start: Int): LexResult {
        val sb = StringBuilder()
        var i = start
        val len = input.length

        while (i < len) {
            when {
                // Escaped character
                input[i] == '\\' && i + 1 < len -> {
                    sb.append(input[i + 1])
                    i += 2
                }

                // Word boundary
                input[i].isWhitespace() || input[i] == '|' || input[i] == ';' -> break

                else -> {
                    sb.append(input[i])
                    i++
                }
            }
        }

        return LexResult(sb.toString(), i)
    }

    // -- Internal result holders ------------------------------------------------

    /**
     * Intermediate result from reading a string or bare word.
     *
     * @property value The parsed text content.
     * @property endIndex The index in the input string immediately after the parsed region.
     */
    private data class LexResult(val value: String, val endIndex: Int)

    /**
     * Intermediate result from reading a single long flag token.
     *
     * @property token The parsed flag token.
     * @property endIndex The index in the input string immediately after the parsed region.
     */
    private data class FlagLexResult(val token: CommandToken, val endIndex: Int)

    /**
     * Intermediate result from reading one or more short flag tokens.
     *
     * @property tokens The list of parsed flag tokens (one per compound character).
     * @property endIndex The index in the input string immediately after the parsed region.
     */
    private data class ShortFlagLexResult(val tokens: List<CommandToken>, val endIndex: Int)
}
