package com.example.androidapp.domain.console

/**
 * Lightweight JSON builder for console command output.
 *
 * Generates pretty-printed JSON as a list of [OutputLine]s styled
 * with [OutputStyle.CODE]. All string values are escaped via
 * [CommandFormatUtils.escapeJson] to prevent injection and ensure
 * valid JSON output.
 *
 * Usage:
 * ```
 * val lines = ConsoleJsonBuilder.buildObject {
 *     field("name", "value")
 *     field("count", 42)
 *     field("active", true)
 *     array("items") {
 *         addObject {
 *             field("id", "abc")
 *         }
 *     }
 * }
 * ```
 */
object ConsoleJsonBuilder {

    /**
     * Builds a JSON object and returns its lines as [OutputLine]s.
     *
     * @param indent Base indentation level (number of spaces).
     * @param block Builder lambda to populate the object fields.
     * @return List of styled output lines representing the JSON object.
     */
    fun buildObject(
        indent: Int = 0,
        block: JsonObjectScope.() -> Unit
    ): List<OutputLine> {
        val scope = JsonObjectScope(indent)
        scope.block()
        return scope.build()
    }

    /**
     * Builds a JSON array and returns its lines as [OutputLine]s.
     *
     * @param indent Base indentation level (number of spaces).
     * @param block Builder lambda to populate the array elements.
     * @return List of styled output lines representing the JSON array.
     */
    fun buildArray(
        indent: Int = 0,
        block: JsonArrayScope.() -> Unit
    ): List<OutputLine> {
        val scope = JsonArrayScope(indent)
        scope.block()
        return scope.build()
    }

    /**
     * Scope for building a JSON object with named fields.
     *
     * @property indent Number of leading spaces for this object's braces.
     */
    class JsonObjectScope(private val indent: Int) {
        private val entries = mutableListOf<JsonEntry>()

        /** Adds a string field, escaping the value for JSON safety. */
        fun field(key: String, value: String?) {
            val escaped = if (value != null) "\"${CommandFormatUtils.escapeJson(value)}\"" else "null"
            entries.add(JsonEntry.Literal("\"${CommandFormatUtils.escapeJson(key)}\": $escaped"))
        }

        /** Adds an integer field. */
        fun field(key: String, value: Int) {
            entries.add(JsonEntry.Literal("\"${CommandFormatUtils.escapeJson(key)}\": $value"))
        }

        /** Adds a long field. */
        fun field(key: String, value: Long) {
            entries.add(JsonEntry.Literal("\"${CommandFormatUtils.escapeJson(key)}\": $value"))
        }

        /** Adds a double field. */
        fun field(key: String, value: Double) {
            entries.add(JsonEntry.Literal("\"${CommandFormatUtils.escapeJson(key)}\": $value"))
        }

        /** Adds a boolean field. */
        fun field(key: String, value: Boolean) {
            entries.add(JsonEntry.Literal("\"${CommandFormatUtils.escapeJson(key)}\": $value"))
        }

        /** Adds a nested object field. */
        fun obj(key: String, block: JsonObjectScope.() -> Unit) {
            val nested = JsonObjectScope(indent + 2)
            nested.block()
            entries.add(JsonEntry.Nested("\"${CommandFormatUtils.escapeJson(key)}\": ", nested.buildRaw()))
        }

        /** Adds a nested array field. */
        fun array(key: String, block: JsonArrayScope.() -> Unit) {
            val nested = JsonArrayScope(indent + 2)
            nested.block()
            entries.add(JsonEntry.Nested("\"${CommandFormatUtils.escapeJson(key)}\": ", nested.buildRaw()))
        }

        internal fun build(): List<OutputLine> {
            return buildRaw().map { OutputLine(it, OutputStyle.CODE) }
        }

        internal fun buildRaw(): List<String> {
            val prefix = " ".repeat(indent)
            val innerPrefix = " ".repeat(indent + 2)
            if (entries.isEmpty()) return listOf("$prefix{}")

            val lines = mutableListOf<String>()
            lines.add("$prefix{")
            entries.forEachIndexed { i, entry ->
                val comma = if (i < entries.size - 1) "," else ""
                when (entry) {
                    is JsonEntry.Literal -> {
                        lines.add("$innerPrefix${entry.text}$comma")
                    }

                    is JsonEntry.Nested -> {
                        val nestedLines = entry.lines.toMutableList()
                        if (nestedLines.size == 1) {
                            lines.add("$innerPrefix${entry.prefix}${nestedLines[0].trimStart()}$comma")
                        } else if (nestedLines.isNotEmpty()) {
                            lines.add("$innerPrefix${entry.prefix}${nestedLines[0].trimStart()}")
                            for (j in 1 until nestedLines.size - 1) {
                                lines.add(nestedLines[j])
                            }
                            lines.add("${nestedLines.last()}$comma")
                        }
                    }
                }
            }
            lines.add("$prefix}")
            return lines
        }
    }

    /**
     * Scope for building a JSON array.
     *
     * @property indent Number of leading spaces for this array's brackets.
     */
    class JsonArrayScope(private val indent: Int) {
        private val elements = mutableListOf<JsonEntry>()

        /** Adds a string element. */
        fun addString(value: String) {
            elements.add(JsonEntry.Literal("\"${CommandFormatUtils.escapeJson(value)}\""))
        }

        /** Adds an integer element. */
        fun addInt(value: Int) {
            elements.add(JsonEntry.Literal("$value"))
        }

        /** Adds a nested object element. */
        fun addObject(block: JsonObjectScope.() -> Unit) {
            val nested = JsonObjectScope(indent + 2)
            nested.block()
            elements.add(JsonEntry.Nested("", nested.buildRaw()))
        }

        internal fun build(): List<OutputLine> {
            return buildRaw().map { OutputLine(it, OutputStyle.CODE) }
        }

        internal fun buildRaw(): List<String> {
            val prefix = " ".repeat(indent)
            val innerPrefix = " ".repeat(indent + 2)
            if (elements.isEmpty()) return listOf("$prefix[]")

            val lines = mutableListOf<String>()
            lines.add("$prefix[")
            elements.forEachIndexed { i, entry ->
                val comma = if (i < elements.size - 1) "," else ""
                when (entry) {
                    is JsonEntry.Literal -> {
                        lines.add("$innerPrefix${entry.text}$comma")
                    }

                    is JsonEntry.Nested -> {
                        val nestedLines = entry.lines.toMutableList()
                        if (nestedLines.size == 1) {
                            lines.add("$innerPrefix${entry.prefix}${nestedLines[0].trimStart()}$comma")
                        } else if (nestedLines.isNotEmpty()) {
                            lines.add("$innerPrefix${nestedLines[0].trimStart()}")
                            for (j in 1 until nestedLines.size - 1) {
                                lines.add(nestedLines[j])
                            }
                            lines.add("${nestedLines.last()}$comma")
                        }
                    }
                }
            }
            lines.add("$prefix]")
            return lines
        }
    }

    /**
     * Internal sealed type representing a single entry in a JSON structure.
     */
    private sealed class JsonEntry {
        /** A simple key-value or scalar text (already formatted). */
        data class Literal(val text: String) : JsonEntry()

        /** A nested structure (object or array) with its prefix and rendered lines. */
        data class Nested(val prefix: String, val lines: List<String>) : JsonEntry()
    }
}
