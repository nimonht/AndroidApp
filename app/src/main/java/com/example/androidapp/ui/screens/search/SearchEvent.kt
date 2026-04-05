package com.example.androidapp.ui.screens.search

/**
 * Tập hợp các sự kiện người dùng có thể kích hoạt trên thanh tìm kiếm.
 */
sealed class SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent()
    object OnClearSearch : SearchEvent()
    data class OnSearchClicked(val query: String) : SearchEvent()
    data class OnRecentSearchClicked(val query: String) : SearchEvent()
    object OnClearRecentSearches : SearchEvent()

    // Task 2
    data class OnTagToggle(val tag: String) : SearchEvent()

    // Discover page tag filter toggle (multi-select, AND logic)
    data class OnDiscoverTagToggle(val tag: String) : SearchEvent()

    // Task 4: Sự kiện nhấn nút chuyển đổi Grid/List
    object OnToggleViewMode : SearchEvent()

    // Task 5: Sự kiện khi người dùng chọn một tiêu chí sắp xếp
    data class OnSortOptionSelected(val option: SortOption) : SearchEvent()

    // Tag click navigation: nguoi dung nhan vao tag tu man hinh khac de tim kiem theo tag
    data class OnTagFilterFromNavigation(val tag: String) : SearchEvent()
}
