package com.example.androidapp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.SearchRepository
import com.example.androidapp.domain.service.EmbeddingService
import com.example.androidapp.domain.util.SearchFilterLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel cho man hinh Tim kiem.
 *
 * Xu ly debounce tren o nhap tim kiem (400ms), truy van [QuizRepository] de lay
 * ket qua, luu lich su tim kiem qua [SearchRepository], va cung cap loc tag,
 * sap xep, chuyen doi che do xem (grid/list).
 *
 * Ngoai ra, khi khoi dong se tai du lieu Kham pha (discover): tag cloud, cac
 * muc "Top hom nay", "Noi bat", "Trending", "Top toan thoi gian" tu cac quiz
 * cong khai, giu phan man hinh luon co noi dung khi nguoi dung chua tim kiem.
 *
 * Pagination: Uses dynamic LIMIT Room queries. The limit increases when the
 * user scrolls near the bottom, and Room re-emits the full list up to the
 * new limit. This avoids loading all public quizzes into memory.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val quizRepository: QuizRepository,
    private val searchRepository: SearchRepository,
    private val embeddingService: EmbeddingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())

    /** Trang thai UI hien tai cho man hinh Tim kiem. */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * Danh sach ket qua tho tu repository, giu lai de loc tag va sap xep
     * ma khong can truy van lai.
     */
    private var allResults: List<Quiz> = emptyList()

    /**
     * Danh sach tat ca quiz cong khai (up to current limit), giu lai de loc
     * tag kham pha ma khong can truy van lai Firestore.
     */
    private var allPublicQuizzes: List<Quiz> = emptyList()

    /** Flow noi bo de debounce thay doi truy van. */
    private val _queryFlow = MutableStateFlow("")

    /** Dynamic limit for discover public quizzes. Increases on LoadMoreDiscover. */
    private val _discoverLimit = MutableStateFlow(INITIAL_DISCOVER_LIMIT)

    /** Dynamic limit for search results. Increases on LoadMoreSearchResults. */
    private val _searchLimit = MutableStateFlow(INITIAL_SEARCH_LIMIT)

    /** Job for the search results collector, cancelled and restarted on new searches. */
    private var searchJob: Job? = null

    private companion object {
        /** Initial number of public quizzes to load for the discover section. */
        const val INITIAL_DISCOVER_LIMIT = 50

        /** Number of additional public quizzes to load on each "load more" for discover. */
        const val DISCOVER_PAGE_SIZE = 50

        /** Initial number of search results to load. */
        const val INITIAL_SEARCH_LIMIT = 30

        /** Number of additional search results to load on each "load more". */
        const val SEARCH_PAGE_SIZE = 30
    }

    init {
        collectRecentSearches()
        observeQueryDebounce()
        loadDiscoverData()
        observeEmbeddingReadiness()
    }

    /**
     * Dieu phoi mot [SearchEvent] den ViewModel.
     */
    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange -> handleQueryChange(event.query)
            is SearchEvent.OnClearSearch -> handleClearSearch()
            is SearchEvent.OnSearchClicked -> handleSearchClicked(event.query)
            is SearchEvent.OnRecentSearchClicked -> handleRecentSearchClicked(event.query)
            is SearchEvent.OnClearRecentSearches -> handleClearRecentSearches()
            is SearchEvent.OnTagToggle -> handleTagToggle(event.tag)
            is SearchEvent.OnDiscoverTagToggle -> handleDiscoverTagToggle(event.tag)
            is SearchEvent.OnToggleViewMode -> handleToggleViewMode()
            is SearchEvent.OnSortOptionSelected -> handleSortOptionSelected(event.option)
            is SearchEvent.OnTagFilterFromNavigation -> handleTagFilterFromNavigation(event.tag)
            is SearchEvent.LoadMoreDiscover -> handleLoadMoreDiscover()
            is SearchEvent.LoadMoreSearchResults -> handleLoadMoreSearchResults()
        }
    }

    // ---------------------------------------------------------------------------
    // Init helpers
    // ---------------------------------------------------------------------------

    /**
     * Thu thap lich su tim kiem gan day tu [SearchRepository] va cap nhat
     * [SearchUiState.recentSearches].
     */
    private fun collectRecentSearches() {
        viewModelScope.launch {
            searchRepository.getRecentSearches().collectLatest { searches ->
                _uiState.update { it.copy(recentSearches = searches) }
            }
        }
    }

    /**
     * Lang nghe [_queryFlow] voi debounce 400ms. Khi nguoi dung ngung go,
     * tu dong thuc hien tim kiem.
     */
    private fun observeQueryDebounce() {
        viewModelScope.launch {
            _queryFlow
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        allResults = emptyList()
                        _searchLimit.value = INITIAL_SEARCH_LIMIT
                        _uiState.update {
                            it.copy(
                                searchResults = emptyList(),
                                availableTags = emptyList(),
                                selectedTags = emptyList(),
                                isSearching = false,
                                hasSearched = false,
                                hasMoreSearchResults = true
                            )
                        }
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    /**
     * Tai du lieu Kham pha phan man hinh tim kiem voi pagination.
     * Uses [_discoverLimit] via [flatMapLatest] so that increasing the limit
     * triggers a new Room query with the higher LIMIT value.
     */
    private fun loadDiscoverData() {
        _uiState.update { it.copy(isLoadingDiscover = true) }

        viewModelScope.launch {
            _discoverLimit.flatMapLatest { limit ->
                quizRepository.getPublicQuizzesLimited(limit)
            }.collectLatest { quizzes ->
                allPublicQuizzes = quizzes
                val limit = _discoverLimit.value

                // --- Tag cloud: dem tan suat, sap xep giam dan ---
                val tagFrequency: Map<String, Int> = quizzes
                    .flatMap { it.tags }
                    .groupingBy { it }
                    .eachCount()

                val discoverTags: List<String> = tagFrequency.entries
                    .sortedByDescending { it.value }
                    .map { it.key }

                val discoverData = withContext(Dispatchers.Default) {
                    deriveDiscoverData(quizzes, _uiState.value.selectedDiscoverTags)
                }

                _uiState.update { state ->
                    state.copy(
                        discoverTags = discoverTags,
                        todayTopQuizzes = discoverData.todayTop,
                        featuredQuizzes = discoverData.featured,
                        trendingQuizzes = discoverData.trending,
                        allTimeTopQuizzes = discoverData.allTimeTop,
                        browseAllQuizzes = discoverData.browseAll,
                        isLoadingDiscover = false,
                        isLoadingMore = false,
                        hasMoreDiscover = quizzes.size >= limit
                    )
                }
            }
        }
    }

    /**
     * Observes the embedding service and cache readiness, automatically
     * switching to HYBRID mode when both are ready.
     */
    private fun observeEmbeddingReadiness() {
        viewModelScope.launch {
            embeddingService.isReady
                .collectLatest { serviceReady ->
                    _uiState.update { state ->
                        state.copy(
                            isEmbeddingReady = serviceReady,
                            searchMode = if (serviceReady && state.searchMode == SearchMode.KEYWORD) {
                                SearchMode.HYBRID
                            } else {
                                state.searchMode
                            }
                        )
                    }
                    // Re-run active search now that the model is ready and mode has upgraded
                    // to HYBRID. The running searchJob listens on _searchLimit; it does NOT
                    // automatically restart when searchMode flips — we must restart it manually.
                    if (serviceReady) {
                        val currentQuery = _uiState.value.query
                        if (currentQuery.isNotBlank()) {
                            performSearch(currentQuery)
                        }
                    }
                }
        }
    }

    // ---------------------------------------------------------------------------
    // Pagination handlers
    // ---------------------------------------------------------------------------

    /** Increases the discover limit to load more public quizzes. */
    private fun handleLoadMoreDiscover() {
        if (!_uiState.value.hasMoreDiscover || _uiState.value.isLoadingMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        _discoverLimit.value += DISCOVER_PAGE_SIZE
    }

    /** Increases the search limit to load more search results. */
    private fun handleLoadMoreSearchResults() {
        if (!_uiState.value.hasMoreSearchResults || _uiState.value.isLoadingMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        _searchLimit.value += SEARCH_PAGE_SIZE
    }

    // ---------------------------------------------------------------------------
    // Event handlers
    // ---------------------------------------------------------------------------

    /**
     * Cap nhat query trong UI state va day gia tri moi vao [_queryFlow] de
     * kich hoat debounce.
     */
    private fun handleQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        _queryFlow.value = query
    }

    /** Xoa query, ket qua, va dat lai trang thai tim kiem. */
    private fun handleClearSearch() {
        _uiState.update {
            it.copy(
                query = "",
                searchResults = emptyList(),
                availableTags = emptyList(),
                selectedTags = emptyList(),
                isSearching = false,
                hasSearched = false,
                hasMoreSearchResults = true
            )
        }
        allResults = emptyList()
        _queryFlow.value = ""
        _searchLimit.value = INITIAL_SEARCH_LIMIT
    }

    /**
     * Xu ly khi nguoi dung nhan nut tim kiem. Luu query vao lich su
     * va thuc hien tim kiem ngay lap tuc (bo qua debounce).
     */
    private fun handleSearchClicked(query: String) {
        if (query.isBlank()) return
        _uiState.update { it.copy(query = query) }
        _queryFlow.value = query
        viewModelScope.launch {
            searchRepository.addRecentSearch(query)
            performSearch(query)
        }
    }

    /**
     * Xu ly khi nguoi dung chon mot tu khoa tim kiem gan day.
     * Dat query, luu vao lich su, va thuc hien tim kiem.
     */
    private fun handleRecentSearchClicked(query: String) {
        _uiState.update { it.copy(query = query) }
        _queryFlow.value = query
        viewModelScope.launch {
            searchRepository.addRecentSearch(query)
            performSearch(query)
        }
    }

    /** Xoa toan bo lich su tim kiem. */
    private fun handleClearRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }

    /**
     * Bat/tat mot tag trong danh sach [SearchUiState.selectedTags] va
     * cap nhat ket qua da loc.
     */
    private fun handleTagToggle(tag: String) {
        _uiState.update { state ->
            val updatedTags = if (tag in state.selectedTags) {
                state.selectedTags - tag
            } else {
                state.selectedTags + tag
            }
            state.copy(
                selectedTags = updatedTags,
                searchResults = deriveSearchResults(
                    allResults,
                    updatedTags,
                    state.sortOption
                )
            )
        }
    }

    /**
     * Bat/tat mot tag trong danh sach [SearchUiState.selectedDiscoverTags]
     * va cap nhat cac section kham pha da loc.
     */
    private fun handleDiscoverTagToggle(tag: String) {
        _uiState.update { state ->
            val updatedTags = if (tag in state.selectedDiscoverTags) {
                state.selectedDiscoverTags - tag
            } else {
                state.selectedDiscoverTags + tag
            }
            val discoverData = deriveDiscoverData(allPublicQuizzes, updatedTags)
            state.copy(
                selectedDiscoverTags = updatedTags,
                todayTopQuizzes = discoverData.todayTop,
                featuredQuizzes = discoverData.featured,
                trendingQuizzes = discoverData.trending,
                allTimeTopQuizzes = discoverData.allTimeTop,
                browseAllQuizzes = discoverData.browseAll
            )
        }
    }

    /** Chuyen doi giua che do xem Grid va List. */
    private fun handleToggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    /**
     * Thay doi tieu chi sap xep va cap nhat lai ket qua da sap xep.
     */
    private fun handleSortOptionSelected(option: SortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = option,
                searchResults = deriveSearchResults(
                    allResults,
                    state.selectedTags,
                    option
                )
            )
        }
    }

    /**
     * Xu ly khi nguoi dung nhan vao tag tu man hinh khac (VD: QuizCard, QuizDetailScreen)
     * de chuyen den man hinh Tim kiem voi ket qua da loc theo tag do.
     */
    private fun handleTagFilterFromNavigation(tag: String) {
        _uiState.update { it.copy(query = tag) }
        _queryFlow.value = tag
        viewModelScope.launch {
            searchRepository.addRecentSearch(tag)
            performSearch(tag)
        }
    }

    // ---------------------------------------------------------------------------
    // Search & transform helpers
    // ---------------------------------------------------------------------------

    /**
     * Executes a search query using the active search mode.
     * Cancels any previous search job and starts a new one that uses
     * [_searchLimit] via [flatMapLatest].
     */
    private fun performSearch(query: String) {
        _searchLimit.value = INITIAL_SEARCH_LIMIT
        _uiState.update { it.copy(isSearching = true) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchLimit.flatMapLatest { limit ->
                val mode = _uiState.value.searchMode
                val isReady = _uiState.value.isEmbeddingReady
                when {
                    isReady && mode == SearchMode.HYBRID ->
                        quizRepository.hybridSearchQuizzes(query, limit)

                    isReady && mode == SearchMode.SEMANTIC ->
                        quizRepository.semanticSearchQuizzes(query, limit)

                    else ->
                        quizRepository.searchQuizzesLimited(query, limit)
                }
            }.collectLatest { quizzes ->
                allResults = quizzes
                val limit = _searchLimit.value
                val availableTags = extractAvailableTags(quizzes)

                _uiState.update { state ->
                    val validSelectedTags = state.selectedTags.filter { it in availableTags }

                    state.copy(
                        isSearching = false,
                        hasSearched = true,
                        availableTags = availableTags,
                        selectedTags = validSelectedTags,
                        searchResults = deriveSearchResults(
                            quizzes,
                            validSelectedTags,
                            state.sortOption
                        ),
                        hasMoreSearchResults = quizzes.size >= limit,
                        isLoadingMore = false
                    )
                }
            }
        }
    }

    /**
     * Trich xuat danh sach tag duy nhat tu ket qua tim kiem, sap xep
     * theo thu tu bang chu cai.
     */
    private fun extractAvailableTags(quizzes: List<Quiz>): List<String> {
        return quizzes
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }

    /**
     * Loc va sap xep danh sach [Quiz] thanh [QuizCardDraft] dua tren
     * tag da chon va tieu chi sap xep hien tai.
     */
    private fun deriveSearchResults(
        quizzes: List<Quiz>,
        selectedTags: List<String>,
        sortOption: SortOption
    ): List<QuizCardDraft> {
        val filtered = SearchFilterLogic.filter(
            items = quizzes,
            queryTags = selectedTags.toSet().ifEmpty { null },
            isPublic = null,
            startDateMillis = null,
            endDateMillis = null,
            getTags = { it.tags },
            getIsPublic = { it.isPublic },
            getTimestampMillis = { it.createdAt }
        )

        val sorted = when (sortOption) {
            SortOption.RELEVANCE -> filtered
            SortOption.DATE -> filtered.sortedByDescending { it.updatedAt }
            SortOption.POPULARITY -> filtered.sortedByDescending { it.attemptCount }
        }

        return sorted.map { it.toCardDraft() }
    }

    /**
     * Map [Quiz] domain model sang [QuizCardDraft] UI model.
     */
    private fun Quiz.toCardDraft() = QuizCardDraft(
        id = id,
        title = title,
        authorName = authorName,
        questionCount = questionCount,
        attemptCount = attemptCount,
        coverImageUrl = thumbnailUrl,
        tags = tags,
        updatedAt = updatedAt
    )

    // ---------------------------------------------------------------------------
    // Discover data helper
    // ---------------------------------------------------------------------------

    /**
     * Ket qua du lieu kham pha da loc.
     */
    private data class DiscoverData(
        val todayTop: List<QuizCardDraft>,
        val featured: List<QuizCardDraft>,
        val trending: List<QuizCardDraft>,
        val allTimeTop: List<QuizCardDraft>,
        val browseAll: List<QuizCardDraft>
    )

    /**
     * Loc danh sach quiz cong khai theo [selectedTags] (AND logic) va tao
     * cac section kham pha. Neu [selectedTags] rong, tra ve toan bo.
     */
    private fun deriveDiscoverData(
        quizzes: List<Quiz>,
        selectedTags: List<String>
    ): DiscoverData {
        val filtered = if (selectedTags.isEmpty()) {
            quizzes
        } else {
            quizzes.filter { quiz ->
                selectedTags.all { tag -> tag in quiz.tags }
            }
        }

        // Sort by createdAt once; reuse for todayTop and browseAll
        val sortedByCreatedAt = filtered.sortedByDescending { it.createdAt }

        // Sort by attemptCount once; reuse for featured, trending, and allTimeTop
        val sortedByAttemptCount = filtered.sortedByDescending { it.attemptCount }

        return DiscoverData(
            todayTop = sortedByCreatedAt
                .take(10)
                .map { it.toCardDraft() },
            featured = sortedByAttemptCount
                .filter { it.isPublic }
                .take(8)
                .map { it.toCardDraft() },
            trending = sortedByAttemptCount
                .take(10)
                .map { it.toCardDraft() },
            allTimeTop = sortedByAttemptCount
                .take(10)
                .map { it.toCardDraft() },
            browseAll = sortedByCreatedAt
                .map { it.toCardDraft() }
        )
    }
}
