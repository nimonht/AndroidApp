package com.example.androidapp.domain.util

/**
 * Utility object for validating and normalizing quiz tags.
 * Ensures tags conform to length, character, and uniqueness constraints.
 */
object TagValidator {

    private const val MIN_TAG_LENGTH = 1
    private const val MAX_TAG_LENGTH = 50

    private val ALLOWED_TAG_PATTERN = Regex("^[\\w\\s-]+$")
    private val MULTI_SPACE_PATTERN = Regex("[\\s_]+")

    /**
     * Normalizes a tag by trimming whitespace, converting to lowercase,
     * and collapsing consecutive spaces or underscores into a single space.
     *
     * @param tag The raw tag string.
     * @return The normalized tag.
     */
    fun normalizeTag(tag: String): String {
        return tag.trim()
            .lowercase()
            .replace(MULTI_SPACE_PATTERN, " ")
            .trim()
    }

    /**
     * Validates a tag against length and character constraints.
     *
     * @param tag The tag string to validate (should be normalized first).
     * @return A [TagValidationResult] indicating whether the tag is valid.
     */
    fun validateTag(tag: String): TagValidationResult {
        val normalized = tag.trim()
        if (normalized.length < MIN_TAG_LENGTH) {
            return TagValidationResult(isValid = false, errorMessage = "Tag must not be empty")
        }
        if (normalized.length > MAX_TAG_LENGTH) {
            return TagValidationResult(
                isValid = false,
                errorMessage = "Tag must not exceed $MAX_TAG_LENGTH characters"
            )
        }
        if (!ALLOWED_TAG_PATTERN.matches(normalized)) {
            return TagValidationResult(
                isValid = false,
                errorMessage = "Tag may only contain letters, numbers, spaces, hyphens, and underscores"
            )
        }
        return TagValidationResult(isValid = true)
    }

    /**
     * Normalizes a list of tags, removes blank entries and duplicates,
     * and preserves the original insertion order.
     *
     * @param tags The list of raw tag strings.
     * @return A deduplicated list of normalized, non-blank tags.
     */
    fun normalizeTags(tags: List<String>): List<String> {
        return tags
            .map { normalizeTag(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * Result of a tag validation check.
     *
     * @property isValid Whether the tag passed all validation rules.
     * @property errorMessage A description of the validation failure, or null if valid.
     */
    data class TagValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )
}
