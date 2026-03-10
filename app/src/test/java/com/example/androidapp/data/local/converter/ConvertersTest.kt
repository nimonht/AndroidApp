package com.example.androidapp.data.local.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [Converters] Room type converters.
 */
class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    // ==================== String List ====================

    @Test
    fun `fromStringList converts list to JSON string`() {
        val list = listOf("a", "b", "c")
        val json = converters.fromStringList(list)
        assertEquals("[\"a\",\"b\",\"c\"]", json)
    }

    @Test
    fun `fromStringList handles null by returning empty list JSON`() {
        val json = converters.fromStringList(null)
        assertEquals("[]", json)
    }

    @Test
    fun `fromStringList handles empty list`() {
        val json = converters.fromStringList(emptyList())
        assertEquals("[]", json)
    }

    @Test
    fun `toStringList converts JSON string to list`() {
        val json = "[\"a\",\"b\",\"c\"]"
        val list = converters.toStringList(json)
        assertEquals(listOf("a", "b", "c"), list)
    }

    @Test
    fun `toStringList handles malformed JSON by returning empty list`() {
        val list = converters.toStringList("invalid json")
        assertTrue(list.isEmpty())
    }

    @Test
    fun `toStringList roundtrip preserves data`() {
        val original = listOf("tag1", "tag2", "tag3")
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)
        assertEquals(original, restored)
    }

    // ==================== String Map ====================

    @Test
    fun `fromStringMap converts map to JSON string`() {
        val map = mapOf("key1" to "value1", "key2" to "value2")
        val json = converters.fromStringMap(map)
        assertTrue(json.contains("\"key1\":\"value1\""))
        assertTrue(json.contains("\"key2\":\"value2\""))
    }

    @Test
    fun `fromStringMap handles null by returning empty map JSON`() {
        val json = converters.fromStringMap(null)
        assertEquals("{}", json)
    }

    @Test
    fun `toStringMap converts JSON string to map`() {
        val json = "{\"key1\":\"value1\",\"key2\":\"value2\"}"
        val map = converters.toStringMap(json)
        assertEquals("value1", map["key1"])
        assertEquals("value2", map["key2"])
    }

    @Test
    fun `toStringMap handles malformed JSON by returning empty map`() {
        val map = converters.toStringMap("invalid json")
        assertTrue(map.isEmpty())
    }

    @Test
    fun `toStringMap roundtrip preserves data`() {
        val original = mapOf("q1" to "a1", "q2" to "a2")
        val json = converters.fromStringMap(original)
        val restored = converters.toStringMap(json)
        assertEquals(original, restored)
    }

    // ==================== String List Map ====================

    @Test
    fun `fromStringListMap converts nested map to JSON`() {
        val map = mapOf("q1" to listOf("a1", "a2"), "q2" to listOf("a3"))
        val json = converters.fromStringListMap(map)
        assertTrue(json.contains("\"q1\""))
        assertTrue(json.contains("\"a1\""))
    }

    @Test
    fun `fromStringListMap handles null by returning empty map JSON`() {
        val json = converters.fromStringListMap(null)
        assertEquals("{}", json)
    }

    @Test
    fun `toStringListMap converts JSON to nested map`() {
        val json = "{\"q1\":[\"a1\",\"a2\"],\"q2\":[\"a3\"]}"
        val map = converters.toStringListMap(json)
        assertEquals(listOf("a1", "a2"), map["q1"])
        assertEquals(listOf("a3"), map["q2"])
    }

    @Test
    fun `toStringListMap handles malformed JSON by returning empty map`() {
        val map = converters.toStringListMap("invalid json")
        assertTrue(map.isEmpty())
    }

    @Test
    fun `toStringListMap roundtrip preserves data`() {
        val original = mapOf(
            "q1" to listOf("choiceA", "choiceB"),
            "q2" to listOf("choiceC")
        )
        val json = converters.fromStringListMap(original)
        val restored = converters.toStringListMap(json)
        assertEquals(original, restored)
    }

    // ==================== Timestamp ====================

    @Test
    fun `fromTimestamp converts Long to Date`() {
        val timestamp = 1700000000000L
        val date = converters.fromTimestamp(timestamp)
        assertEquals(timestamp, date?.time)
    }

    @Test
    fun `fromTimestamp handles null by returning null`() {
        val date = converters.fromTimestamp(null)
        assertEquals(null, date)
    }

    @Test
    fun `dateToTimestamp converts Date to Long`() {
        val timestamp = 1700000000000L
        val date = java.util.Date(timestamp)
        val result = converters.dateToTimestamp(date)
        assertEquals(timestamp, result)
    }

    @Test
    fun `dateToTimestamp handles null by returning null`() {
        val result = converters.dateToTimestamp(null)
        assertEquals(null, result)
    }

    @Test
    fun `timestamp roundtrip preserves data`() {
        val original = 1700000000000L
        val date = converters.fromTimestamp(original)
        val restored = converters.dateToTimestamp(date)
        assertEquals(original, restored)
    }
}
