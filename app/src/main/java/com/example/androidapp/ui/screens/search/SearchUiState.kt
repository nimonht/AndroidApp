package com.example.androidapp.ui.screens.search

/**
 * Search mode controlling which backend is used for query execution.
 */
enum class SearchMode {
    /** Pure FTS keyword matching (always available). */
    KEYWORD,

    /** Pure semantic (vector similarity) search. Requires model to be loaded. */
    SEMANTIC,

    /** FTS + semantic merged via Reciprocal Rank Fusion (default when model is ready). */
    HYBRID
}

/**
 * Sort options for search results.
 */
enum class SortOption { DATE, POPULARITY, RELEVANCE }

/**
 * UI state for the Search screen.
 */
data class SearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false,

    // Tag filter (search results mode, OR logic)
    val availableTags: List<String> = emptyList(),
    val selectedTags: List<String> = emptyList(),

    // Search results
    val searchResults: List<QuizCardDraft> = emptyList(),

    // View mode
    val isGridView: Boolean = true,

    // Sort
    val sortOption: SortOption = SortOption.RELEVANCE,

    // Whether a search has been performed
    val hasSearched: Boolean = false,

    // Discover sections (shown when not searching)
    val discoverTags: List<String> = emptyList(),
    val todayTopQuizzes: List<QuizCardDraft> = emptyList(),
    val featuredQuizzes: List<QuizCardDraft> = emptyList(),
    val trendingQuizzes: List<QuizCardDraft> = emptyList(),
    val allTimeTopQuizzes: List<QuizCardDraft> = emptyList(),
    val browseAllQuizzes: List<QuizCardDraft> = emptyList(),
    val isLoadingDiscover: Boolean = false,
    val selectedDiscoverTags: List<String> = emptyList(),

    // Pagination
    val hasMoreDiscover: Boolean = true,
    val hasMoreSearchResults: Boolean = true,
    val isLoadingMore: Boolean = false,

    // Semantic search state
    val searchMode: SearchMode = SearchMode.KEYWORD,
    val isEmbeddingReady: Boolean = false
)
