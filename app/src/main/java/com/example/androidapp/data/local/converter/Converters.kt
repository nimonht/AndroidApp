package com.example.androidapp.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database.
 * Handles conversion between complex types and primitive types that Room can store.
 */
class Converters {

    private val gson = Gson()

    /**
     * Convert a List<String> to a JSON string for storage.
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    /**
     * Convert a JSON string back to a List<String>.
     */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Convert a Map<String, String> to a JSON string for storage.
     */
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return gson.toJson(value ?: emptyMap<String, String>())
    }

    /**
     * Convert a JSON string back to a Map<String, String>.
     */
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return try {
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Convert a Map<String, List<String>> to a JSON string for storage.
     * Used for choiceOrders and multiAnswers fields.
     */
    @TypeConverter
    fun fromStringListMap(value: Map<String, List<String>>?): String {
        return gson.toJson(value ?: emptyMap<String, List<String>>())
    }

    /**
     * Convert a JSON string back to a Map<String, List<String>>.
     */
    @TypeConverter
    fun toStringListMap(value: String): Map<String, List<String>> {
        val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
        return try {
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
