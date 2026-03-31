package com.example.androidapp.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TagValidator] utility functions.
 */
class TagValidatorTest {

    // ==================== normalizeTag ====================

    @Test
    fun `normalizeTag trims whitespace`() {
        assertEquals("kotlin", TagValidator.normalizeTag("  kotlin  "))
    }

    @Test
    fun `normalizeTag converts to lowercase`() {
        assertEquals("kotlin", TagValidator.normalizeTag("Kotlin"))
    }

    @Test
    fun `normalizeTag converts mixed case to lowercase`() {
        assertEquals("android dev", TagValidator.normalizeTag("Android DEV"))
    }

    @Test
    fun `normalizeTag collapses multiple spaces to single space`() {
        assertEquals("a b", TagValidator.normalizeTag("a   b"))
    }

    @Test
    fun `normalizeTag replaces underscores with single space`() {
        assertEquals("a b", TagValidator.normalizeTag("a__b"))
    }

    @Test
    fun `normalizeTag collapses mixed spaces and underscores`() {
        assertEquals("a b", TagValidator.normalizeTag("a _ _ b"))
    }

    // ==================== validateTag ====================

    @Test
    fun `validateTag accepts valid simple tag`() {
        val result = TagValidator.validateTag("kotlin")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateTag accepts tag with spaces`() {
        val result = TagValidator.validateTag("android dev")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateTag accepts tag with hyphens`() {
        val result = TagValidator.validateTag("well-known")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateTag accepts tag with underscores`() {
        val result = TagValidator.validateTag("my_tag")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateTag accepts tag with numbers`() {
        val result = TagValidator.validateTag("level2")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateTag rejects empty tag`() {
        val result = TagValidator.validateTag("")
        assertFalse(result.isValid)
        assertEquals("Tag must not be empty", result.errorMessage)
    }

    @Test
    fun `validateTag rejects blank tag`() {
        val result = TagValidator.validateTag("   ")
        assertFalse(result.isValid)
        assertEquals("Tag must not be empty", result.errorMessage)
    }

    @Test
    fun `validateTag rejects tag exceeding max length`() {
        val longTag = "a".repeat(51)
        val result = TagValidator.validateTag(longTag)
        assertFalse(result.isValid)
        assertEquals("Tag must not exceed 50 characters", result.errorMessage)
    }

    @Test
    fun `validateTag accepts tag at max length boundary`() {
        val tag = "a".repeat(50)
        val result = TagValidator.validateTag(tag)
        assertTrue(result.isValid)
    }

    @Test
    fun `validateTag rejects tag with special characters`() {
        val result = TagValidator.validateTag("hello@world")
        assertFalse(result.isValid)
        assertEquals(
            "Tag may only contain letters, numbers, spaces, hyphens, and underscores",
            result.errorMessage
        )
    }

    @Test
    fun `validateTag rejects tag with exclamation mark`() {
        val result = TagValidator.validateTag("wow!")
        assertFalse(result.isValid)
    }

    @Test
    fun `validateTag rejects tag with hash symbol`() {
        val result = TagValidator.validateTag("#trending")
        assertFalse(result.isValid)
    }

    // ==================== normalizeTags ====================

    @Test
    fun `normalizeTags normalizes and deduplicates tags`() {
        val tags = listOf("Kotlin", "kotlin", "KOTLIN")
        val result = TagValidator.normalizeTags(tags)
        assertEquals(listOf("kotlin"), result)
    }

    @Test
    fun `normalizeTags removes blank entries`() {
        val tags = listOf("kotlin", "", "   ", "android")
        val result = TagValidator.normalizeTags(tags)
        assertEquals(listOf("kotlin", "android"), result)
    }

    @Test
    fun `normalizeTags preserves order of first occurrence`() {
        val tags = listOf("Android", "Kotlin", "android")
        val result = TagValidator.normalizeTags(tags)
        assertEquals(listOf("android", "kotlin"), result)
    }

    @Test
    fun `normalizeTags handles empty list`() {
        val result = TagValidator.normalizeTags(emptyList())
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `normalizeTags collapses whitespace in tags`() {
        val tags = listOf("  android   dev  ", "android dev")
        val result = TagValidator.normalizeTags(tags)
        assertEquals(listOf("android dev"), result)
    }
}
