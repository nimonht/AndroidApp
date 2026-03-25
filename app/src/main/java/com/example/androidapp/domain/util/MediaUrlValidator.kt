package com.example.androidapp.domain.util

import java.net.MalformedURLException
import java.net.URL

/**
 * Utility object for validating and sanitizing media URLs.
 */
object MediaUrlValidator {

    /**
     * Checks if a URL is a valid, secure HTTP/HTTPS URL.
     * For increased security, it verifies that the URL relies on Firebase Storage.
     *
     * @param url The string URL to validate.
     * @return True if valid, false otherwise.
     */
    fun isValidMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val parsedUrl = URL(url)
            val protocol = parsedUrl.protocol.lowercase()
            val host = parsedUrl.host.lowercase()
            
            protocol == "https" && (
                host == "firebasestorage.googleapis.com" || 
                host.endsWith("storage.googleapis.com") ||
                host == "10.0.2.2" // Emulator support
            )
        } catch (e: MalformedURLException) {
            false
        }
    }

    /**
     * Sanitizes a media URL by trimming whitespace. If invalid, returns null.
     */
    fun sanitizeMediaUrl(url: String?): String? {
        val trimmed = url?.trim()
        return if (isValidMediaUrl(trimmed)) trimmed else null
    }
}
