package com.example.androidapp.domain.util

/**
 * Utility for unified input validation rules.
 */
object InputValidator {
    
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_REGEX.matches(email)
    }

    fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }
}
