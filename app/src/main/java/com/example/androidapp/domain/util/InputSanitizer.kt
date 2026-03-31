package com.example.androidapp.domain.util

/**
 * Utility object for sanitizing text inputs to prevent injection and XSS attacks.
 * All methods are pure Kotlin with no Android dependencies.
 */
object InputSanitizer {

    private const val DEFAULT_MAX_LENGTH = 1000
    private const val FIRESTORE_MAX_LENGTH = 10000

    private val SCRIPT_TAG_PATTERN = Regex("<\\s*script", RegexOption.IGNORE_CASE)
    private val JAVASCRIPT_URL_PATTERN = Regex("javascript\\s*:", RegexOption.IGNORE_CASE)
    private val DATA_URL_PATTERN = Regex("data\\s*:", RegexOption.IGNORE_CASE)

    /**
     * Sanitizes a text input by trimming whitespace, removing control characters,
     * and limiting the length to [maxLength].
     *
     * @param input The raw text input, may be null.
     * @param maxLength The maximum allowed length of the output. Defaults to [DEFAULT_MAX_LENGTH].
     * @return A sanitized string, or an empty string if input is null.
     */
    fun sanitizeText(input: String?, maxLength: Int = DEFAULT_MAX_LENGTH): String {
        if (input == null) return ""
        val trimmed = input.trim()
        val cleaned = trimmed.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
        return if (cleaned.length > maxLength) cleaned.substring(0, maxLength) else cleaned
    }

    /**
     * Escapes HTML special characters to prevent XSS when rendering user input.
     * Handles &amp;, &lt;, &gt;, &quot;, and &#39; characters.
     *
     * @param input The raw text input, may be null.
     * @return A string with HTML special characters escaped, or an empty string if input is null.
     */
    fun sanitizeHtml(input: String?): String {
        if (input == null) return ""
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * Checks whether the input contains common injection patterns such as
     * script tags, javascript: URLs, or data: URLs.
     *
     * @param input The text to check.
     * @return True if prohibited content is detected, false otherwise.
     */
    fun containsProhibitedContent(input: String): Boolean {
        return SCRIPT_TAG_PATTERN.containsMatchIn(input) ||
            JAVASCRIPT_URL_PATTERN.containsMatchIn(input) ||
            DATA_URL_PATTERN.containsMatchIn(input)
    }

    /**
     * Sanitizes input for safe storage in Firestore by trimming whitespace,
     * removing null bytes, and limiting length to [FIRESTORE_MAX_LENGTH] characters.
     *
     * @param input The raw text input, may be null.
     * @return A sanitized string suitable for Firestore, or an empty string if input is null.
     */
    fun sanitizeForFirestore(input: String?): String {
        if (input == null) return ""
        val trimmed = input.trim()
        val cleaned = trimmed.replace("\u0000", "")
        return if (cleaned.length > FIRESTORE_MAX_LENGTH) {
            cleaned.substring(0, FIRESTORE_MAX_LENGTH)
        } else {
            cleaned
        }
    }
}
