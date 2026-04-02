package com.example.androidapp.ui.screens.admin.reports

import com.example.androidapp.domain.model.SystemStats

/**
 * UI state for the admin reports screen.
 */
data class AdminReportsUiState(
    val isLoading: Boolean = true,
    val stats: SystemStats? = null,
    val error: String? = null
)
