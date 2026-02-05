package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R

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
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val allFilter = stringResource(R.string.filter_all)
    var selectedFilter by remember(allFilter) { mutableStateOf(allFilter) }

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
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        // 3. Results Header
        Text(
            text = stringResource(R.string.search_top_results),
            style = MaterialTheme.typography.titleMedium
        )

        // 4. Results Grid
        SearchResultsGrid(
            onQuizClick = onNavigateToQuiz
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
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allFilter = stringResource(R.string.filter_all)
    val filters = listOf(
        allFilter,
        stringResource(R.string.filter_math),
        stringResource(R.string.filter_science),
        stringResource(R.string.filter_history),
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                leadingIcon = if (filter == allFilter) {
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

@Composable
private fun SearchResultsGrid(
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with actual data from ViewModel
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) { index ->
            SearchResultCard(
                title = stringResource(R.string.search_quiz_number, index + 1),
                onClick = { onQuizClick("quiz_$index") }
            )
        }
    }
}

/**
 * Temporary result card for search results.
 * TODO: Replace with actual QuizCard component when integrated with ViewModel.
 */
@Composable
private fun SearchResultCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(160.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(title)
        }
    }
}
