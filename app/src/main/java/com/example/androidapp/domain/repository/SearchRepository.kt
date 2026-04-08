package com.example.androidapp.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface responsible for managing search data, including search history.
 */
interface SearchRepository {
    /**
     * Retrieves the list of recent search keywords.
     */
    fun getRecentSearches(): Flow<List<String>>

    /**
     * Saves a new search keyword to the search history.
     */
    suspend fun addRecentSearch(query: String)

    /**
     * Clears all search history.
     */
    suspend fun clearRecentSearches()
}
