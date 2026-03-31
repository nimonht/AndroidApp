package com.example.androidapp.ui.screens.search

/**
 * Các tùy chọn sắp xếp kết quả tìm kiếm (Task 5).
 * Định nghĩa Enum ngay tại đây để SearchUiState có thể tham chiếu tới.
 */
enum class SortOption { DATE, POPULARITY, RELEVANCE }

/**
 * Trạng thái UI cho màn hình Tìm kiếm (Gồm Task 1 đến Task 5).
 */
data class SearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false,

    // Task 2: Tag Filter (dùng khi đang trong chế độ kết quả tìm kiếm)
    val availableTags: List<String> = emptyList(),
    val selectedTags: List<String> = emptyList(),

    // Task 3: Danh sách kết quả tìm kiếm
    val searchResults: List<QuizCardDraft> = emptyList(),

    // Task 4: Cờ xác định chế độ xem hiện tại
    val isGridView: Boolean = true,

    // Task 5: Cờ xác định tùy chọn sắp xếp hiện tại
    val sortOption: SortOption = SortOption.RELEVANCE,

    // Task 6: Cờ xác định đã bấm tìm kiếm chưa (Dùng để hiện EmptyState)
    val hasSearched: Boolean = false,

    // Discover — hiển thị khi chưa tìm kiếm
    /** Tất cả tag từ các quiz công khai, sắp xếp theo tần suất giảm dần. */
    val discoverTags: List<String> = emptyList(),

    /** Top 10 quiz mới nhất hôm nay (theo createdAt giảm dần). */
    val todayTopQuizzes: List<QuizCardDraft> = emptyList(),

    /** Top 8 quiz nổi bật (theo attemptCount giảm dần, isPublic = true). */
    val featuredQuizzes: List<QuizCardDraft> = emptyList(),

    /** Top 10 quiz đang trending (theo attemptCount giảm dần). */
    val trendingQuizzes: List<QuizCardDraft> = emptyList(),

    /** Top 10 quiz mọi thời đại (theo attemptCount giảm dần toàn bộ). */
    val allTimeTopQuizzes: List<QuizCardDraft> = emptyList(),

    /** Đang tải dữ liệu khám phá. */
    val isLoadingDiscover: Boolean = false,

    /** Tag dang duoc chon de loc tren man hinh Kham pha (multi-select, AND logic). */
    val selectedDiscoverTags: List<String> = emptyList()
)
