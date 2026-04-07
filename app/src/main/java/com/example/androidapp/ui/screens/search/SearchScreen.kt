package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.common.TagChip
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.forms.QuizSearchBar
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

/**
 * Man hinh Tim kiem / Kham pha de tim cac bai kiem tra cong khai.
 *
 * Khi nguoi dung chua tim kiem, man hinh hien thi:
 *   - Tag cloud "Kham pha theo chu de" (tat ca tag tu quiz cong khai, sap xep theo tan suat)
 *   - Cac section doc: "Top hom nay", "Noi bat", "Trending", "Top toan thoi gian"
 *
 * Khi nguoi dung da thuc hien tim kiem, man hinh hien thi ket qua tim kiem
 * voi cac dieu khien sap xep va chuyen doi che do xem Grid/List.
 *
 * Day la composable phi trang thai (stateless); toan bo trang thai duoc
 * quan ly boi [SearchViewModel].
 *
 * @param onNavigateToQuiz Callback khi nguoi dung chon mot bai kiem tra, truyen quiz ID.
 * @param modifier Modifier tuy chinh giao dien.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onNavigateToQuiz: (String) -> Unit,
    initialTag: String? = null,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: SearchViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SearchViewModel(container.quizRepository, container.searchRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Khi nguoi dung dieu huong den man hinh Tim kiem voi tag cho truoc,
    // tu dong kich hoat tim kiem theo tag do.
    LaunchedEffect(initialTag) {
        if (!initialTag.isNullOrBlank()) {
            viewModel.onEvent(SearchEvent.OnTagFilterFromNavigation(initialTag))
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // 1. Thanh tim kiem voi lich su tim kiem gan day
        QuizSearchBar(
            query = uiState.query,
            recentSearches = uiState.recentSearches,
            onEvent = viewModel::onEvent,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Phan noi dung chinh: Kham pha (chua tim) hoac Ket qua (da tim)
        when {
            // Dang tai ket qua tim kiem
            uiState.isSearching -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Da tim kiem nhung khong co ket qua
            uiState.hasSearched && uiState.searchResults.isEmpty() -> {
                TagFilterRow(
                    tags = uiState.availableTags,
                    selectedTags = uiState.selectedTags,
                    onTagClick = { tag -> viewModel.onEvent(SearchEvent.OnTagToggle(tag)) },
                    modifier = Modifier.fillMaxWidth()
                )
                EmptyState(
                    message = stringResource(R.string.search_no_results_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            // Da tim kiem va co ket qua — hien thi theo che do xem hien tai
            uiState.hasSearched && uiState.searchResults.isNotEmpty() -> {
                TagFilterRow(
                    tags = uiState.availableTags,
                    selectedTags = uiState.selectedTags,
                    onTagClick = { tag -> viewModel.onEvent(SearchEvent.OnTagToggle(tag)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                SearchControlsRow(
                    currentSort = uiState.sortOption,
                    isGridView = uiState.isGridView,
                    onSortSelected = { option ->
                        viewModel.onEvent(SearchEvent.OnSortOptionSelected(option))
                    },
                    onToggleView = { viewModel.onEvent(SearchEvent.OnToggleViewMode) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isGridView) {
                    SearchResultsGrid(
                        results = uiState.searchResults,
                        onQuizClick = onNavigateToQuiz,
                        hasMoreSearchResults = uiState.hasMoreSearchResults,
                        isLoadingMore = uiState.isLoadingMore,
                        onLoadMore = { viewModel.onEvent(SearchEvent.LoadMoreSearchResults) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    SearchResultsList(
                        results = uiState.searchResults,
                        onQuizClick = onNavigateToQuiz,
                        hasMoreSearchResults = uiState.hasMoreSearchResults,
                        isLoadingMore = uiState.isLoadingMore,
                        onLoadMore = { viewModel.onEvent(SearchEvent.LoadMoreSearchResults) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            // Chua tim kiem — hien thi man hinh Kham pha
            else -> {
                DiscoverContent(
                    uiState = uiState,
                    onTagClick = { tag ->
                        viewModel.onEvent(SearchEvent.OnDiscoverTagToggle(tag))
                    },
                    onQuizClick = onNavigateToQuiz,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

/**
 * Phan noi dung Kham pha: hien thi tag cloud va cac section quiz
 * khi nguoi dung chua thuc hien bat ky tim kiem nao.
 *
 * @param uiState     Trang thai UI hien tai chua du lieu kham pha.
 * @param onTagClick  Callback khi nguoi dung nhan vao mot tag trong cloud.
 * @param onQuizClick Callback khi nguoi dung nhan vao mot the quiz.
 * @param modifier    Modifier tuy chinh giao dien tu ben ngoai.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverContent(
    uiState: SearchUiState,
    onTagClick: (String) -> Unit,
    onQuizClick: (String) -> Unit,
    onEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoadingDiscover) {
        LoadingSpinner(
            modifier = modifier,
            message = stringResource(R.string.loading)
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Tag cloud ---
        if (uiState.discoverTags.isNotEmpty()) {
            item(key = "discover_tags_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.search_discover_tags_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.discoverTags.forEach { tag ->
                            TagChip(
                                text = tag,
                                isSelected = tag in uiState.selectedDiscoverTags,
                                onClick = { onTagClick(tag) }
                            )
                        }
                    }
                }
            }
        }

        // --- Top hom nay ---
        if (uiState.todayTopQuizzes.isNotEmpty()) {
            item(key = "section_today_top") {
                DiscoverSection(
                    title = stringResource(R.string.search_section_today_top),
                    quizzes = uiState.todayTopQuizzes,
                    onQuizClick = onQuizClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- Noi bat ---
        if (uiState.featuredQuizzes.isNotEmpty()) {
            item(key = "section_featured") {
                DiscoverSection(
                    title = stringResource(R.string.search_section_featured),
                    quizzes = uiState.featuredQuizzes,
                    onQuizClick = onQuizClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- Trending ---
        if (uiState.trendingQuizzes.isNotEmpty()) {
            item(key = "section_trending") {
                DiscoverSection(
                    title = stringResource(R.string.search_section_trending),
                    quizzes = uiState.trendingQuizzes,
                    onQuizClick = onQuizClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- Top toan thoi gian ---
        if (uiState.allTimeTopQuizzes.isNotEmpty()) {
            item(key = "section_all_time_top") {
                DiscoverSection(
                    title = stringResource(R.string.search_section_all_time_top),
                    quizzes = uiState.allTimeTopQuizzes,
                    onQuizClick = onQuizClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Pagination: load more discover quizzes
        if (uiState.hasMoreDiscover) {
            item(key = "discover_load_more") {
                LaunchedEffect(Unit) {
                    onEvent(SearchEvent.LoadMoreDiscover)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}
