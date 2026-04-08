package com.example.androidapp.ui.screens.advanced

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minimal ViewModel for the [AdvancedScreen] container.
 *
 * Tracks which tab (Console or Logs) is currently selected.
 * Tab indices:
 * - `0` = Console
 * - `1` = Logs
 */
class AdvancedViewModel : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)

    /** Currently selected tab index (0 = Console, 1 = Logs). */
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    /**
     * Selects the tab at the given [index].
     *
     * @param index Tab index to select (0 = Console, 1 = Logs).
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index.coerceIn(0, 1)
    }
}
