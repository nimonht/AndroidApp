package com.example.androidapp.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [InputSanitizer] utility functions.
 */
class InputSanitizerTest {

    // ==================== sanitizeText ====================

    @Test
    fun `sanitizeText returns empty string for null input`() {
        assertEquals("", InputSanitizer.sanitizeText(null))
    }

    @Test
    fun `sanitizeText returns empty string for blank input`() {
        assertEquals("", InputSanitizer.sanitizeText("   "))
    }

    @Test
    fun `sanitizeText trims leading and trailing whitespace`() {
        assertEquals("hello world", InputSanitizer.sanitizeText("  hello world  "))
    }

    @Test
    fun `sanitizeText preserves normal text`() {
        assertEquals("Hello, World!", InputSanitizer.sanitizeText("Hello, World!"))
    }

    @Test
    fun `sanitizeText removes control characters`() {
        assertEquals("abc", InputSanitizer.sanitizeText("a\u0001b\u0002c"))
    }

    @Test
    fun `sanitizeText preserves newlines and tabs`() {
        val input = "line1\nline2\ttab"
        assertEquals(input, InputSanitizer.sanitizeText(input))
    }

    @Test
    fun `sanitizeText truncates text exceeding default max length`() {
        val longText = "a".repeat(1500)
        val result = InputSanitizer.sanitizeText(longText)
        assertEquals(1000, result.length)
    }

    @Test
    fun `sanitizeText truncates text exceeding custom max length`() {
        val result = InputSanitizer.sanitizeText("abcdefghij", maxLength = 5)
        assertEquals("abcde", result)
    }

    @Test
    fun `sanitizeText treats negative maxLength as zero`() {
        val result = InputSanitizer.sanitizeText("hello", maxLength = -1)
        assertEquals("", result)
    }

    // ==================== sanitizeHtml ====================

    @Test
    fun `sanitizeHtml returns empty string for null input`() {
        assertEquals("", InputSanitizer.sanitizeHtml(null))
    }

    @Test
    fun `sanitizeHtml escapes ampersand`() {
        assertEquals("a &amp; b", InputSanitizer.sanitizeHtml("a & b"))
    }

    @Test
    fun `sanitizeHtml escapes angle brackets`() {
        assertEquals("&lt;div&gt;", InputSanitizer.sanitizeHtml("<div>"))
    }

    @Test
    fun `sanitizeHtml escapes double quotes`() {
        assertEquals("say &quot;hello&quot;", InputSanitizer.sanitizeHtml("say \"hello\""))
    }

    @Test
    fun `sanitizeHtml escapes single quotes`() {
        assertEquals("it&#39;s", InputSanitizer.sanitizeHtml("it's"))
    }

    @Test
    fun `sanitizeHtml escapes all special characters together`() {
        val input = "<b>\"Tom & Jerry's\"</b>"
        val expected = "&lt;b&gt;&quot;Tom &amp; Jerry&#39;s&quot;&lt;/b&gt;"
        assertEquals(expected, InputSanitizer.sanitizeHtml(input))
    }

    // ==================== containsProhibitedContent ====================

    @Test
    fun `containsProhibitedContent detects script tags`() {
        assertTrue(InputSanitizer.containsProhibitedContent("<script>alert('xss')</script>"))
    }

    @Test
    fun `containsProhibitedContent detects script tags case insensitive`() {
        assertTrue(InputSanitizer.containsProhibitedContent("<SCRIPT>alert('xss')</SCRIPT>"))
    }

    @Test
    fun `containsProhibitedContent detects script tags with spaces`() {
        assertTrue(InputSanitizer.containsProhibitedContent("<  script>alert('xss')</script>"))
    }

    @Test
    fun `containsProhibitedContent detects javascript URLs`() {
        assertTrue(InputSanitizer.containsProhibitedContent("javascript:alert('xss')"))
    }

    @Test
    fun `containsProhibitedContent detects javascript URLs case insensitive`() {
        assertTrue(InputSanitizer.containsProhibitedContent("JAVASCRIPT:alert('xss')"))
    }

    @Test
    fun `containsProhibitedContent detects data URLs`() {
        assertTrue(InputSanitizer.containsProhibitedContent("data:text/html,<script>alert('xss')</script>"))
    }

    @Test
    fun `containsProhibitedContent returns false for safe text`() {
        assertFalse(InputSanitizer.containsProhibitedContent("Hello, this is safe text!"))
    }

    @Test
    fun `containsProhibitedContent returns false for empty string`() {
        assertFalse(InputSanitizer.containsProhibitedContent(""))
    }

    // ==================== sanitizeForStorage ====================

    @Test
    fun `sanitizeForStorage returns empty string for null input`() {
        assertEquals("", InputSanitizer.sanitizeForStorage(null))
    }

    @Test
    fun `sanitizeForStorage trims whitespace`() {
        assertEquals("hello", InputSanitizer.sanitizeForStorage("  hello  "))
    }

    @Test
    fun `sanitizeForStorage removes null bytes`() {
        assertEquals("abc", InputSanitizer.sanitizeForStorage("a\u0000b\u0000c"))
    }

    @Test
    fun `sanitizeForStorage truncates text exceeding 10000 characters`() {
        val longText = "x".repeat(15000)
        val result = InputSanitizer.sanitizeForStorage(longText)
        assertEquals(10000, result.length)
    }

    @Test
    fun `sanitizeForStorage preserves text within limit`() {
        val text = "Normal text content"
        assertEquals(text, InputSanitizer.sanitizeForStorage(text))
    }
}
