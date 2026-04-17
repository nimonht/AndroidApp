package com.example.androidapp.ui.screens.search

/**
 * Events the user can trigger on the Search screen.
 */
sealed class SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent()
    data object OnClearSearch : SearchEvent()
    data class OnSearchClicked(val query: String) : SearchEvent()
    data class OnRecentSearchClicked(val query: String) : SearchEvent()
    data object OnClearRecentSearches : SearchEvent()

    // Tag filter
    data class OnTagToggle(val tag: String) : SearchEvent()
    data class OnDiscoverTagToggle(val tag: String) : SearchEvent()

    // View mode
    data object OnToggleViewMode : SearchEvent()

    // Sort
    data class OnSortOptionSelected(val option: SortOption) : SearchEvent()

    // Tag navigation from other screens
    data class OnTagFilterFromNavigation(val tag: String) : SearchEvent()

    // Pagination
    data object LoadMoreDiscover : SearchEvent()
    data object LoadMoreSearchResults : SearchEvent()
}
