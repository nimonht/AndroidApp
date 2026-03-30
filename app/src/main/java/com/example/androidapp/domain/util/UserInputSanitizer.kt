package com.example.androidapp.domain.util

/**
 * Utility object for sanitizing user inputs.
 * Designed to clean text and prevent basic injection attacks (e.g., XSS)
 * before data is processed or saved to the database.
 */
object UserInputSanitizer {

    private val HTML_TAG_REGEX = Regex("<[^>]*>")

    /**
     * Sanitizes a given text input by removing leading/trailing whitespaces
     * and stripping out HTML/XML tags.
     *
     * @param input The raw text input from the user.
     * @return The sanitized text. Returns an empty string if the input is null or blank.
     */
    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) {
            return ""
        }

        return input.trim().replace(HTML_TAG_REGEX, "")
    }
}