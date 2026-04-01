package com.example.androidapp

import com.example.androidapp.data.remote.model.QuestionPoolItemDto
import org.junit.Test

/**
 * Verifies that [QuestionPoolItemDto] exposes the expected "active" related
 * methods after Kotlin compilation (e.g. `getIsActive`, `isActive`, `component*`).
 */
class FirebaseModelTest {

    @Test
    fun questionPoolItemDto_hasActiveRelatedMethods() {
        val activeMethods = QuestionPoolItemDto::class.java.declaredMethods
            .filter { it.name.contains("active", ignoreCase = true) }

        // Ensure at least one "active" accessor exists on the DTO
        assert(activeMethods.isNotEmpty()) {
            "Expected QuestionPoolItemDto to have at least one method related to 'active', but found none."
        }

        activeMethods.forEach { method ->
            println("${method.name} -> annotations: ${method.annotations.contentToString()}")
        }
    }
}
