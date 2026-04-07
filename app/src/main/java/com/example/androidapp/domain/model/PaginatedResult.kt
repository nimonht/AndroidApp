package com.example.androidapp.domain.model

/**
 * Generic wrapper for paginated query results.
 *
 * Used across the app to represent a page of data with a flag indicating
 * whether more data is available beyond this page.
 *
 * @param T The type of items in the page.
 * @property items The items in the current page.
 * @property hasMore Whether more items are available beyond this page.
 */
data class PaginatedResult<T>(
    val items: List<T>,
    val hasMore: Boolean
)
