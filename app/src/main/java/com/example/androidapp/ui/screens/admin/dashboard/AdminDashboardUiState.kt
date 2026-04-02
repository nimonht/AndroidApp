package com.example.androidapp.ui.screens.admin.dashboard

import com.example.androidapp.domain.model.SystemStats

/**
 * UI state for the admin dashboard screen.
 */
data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val stats: SystemStats? = null,
    val error: String? = null
)
