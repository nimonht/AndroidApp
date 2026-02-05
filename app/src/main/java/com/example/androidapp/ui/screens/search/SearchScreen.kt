package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.feedback.EmptyState

/**
 * Search/Explore screen for discovering public quizzes.
 * Features:
 * - Search bar with text input
 * - Filter chips for categories
 * - Grid/List of quiz results
 *
 * @param onNavigateToQuiz Callback when a quiz is selected.
 * @param modifier Modifier for styling.
 */
@Composable
fun SearchScreen(
    onNavigateToQuiz: (String) -> Unit,
    filters: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val availableFilters = filters
    var selectedFilter by remember(availableFilters) {
        mutableStateOf(availableFilters.firstOrNull())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        // 2. Filter Chips
        FilterChipsRow(
            filters = availableFilters,
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        // 3. Results Header
        Text(
            text = stringResource(R.string.search_top_results),
            style = MaterialTheme.typography.titleMedium
        )

        // 4. Results Grid
        EmptyState(
            message = stringResource(R.string.search_empty),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        shape = MaterialTheme.shapes.medium,
        singleLine = true
    )
}

@Composable
private fun FilterChipsRow(
    filters: List<String>,
    selectedFilter: String?,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (filters.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                leadingIcon = if (filter == filters.first()) {
                    {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )
        }
    }
}

