package com.example.androidapp.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Shared UI state for admin screens that display [SystemStats].
 *
 * Used by both the dashboard and reports ViewModels via a `typealias`
 * in their respective packages.
 *
 * @property isLoading Whether stats are currently being fetched.
 * @property stats     The latest [SystemStats] snapshot, or `null` if not yet loaded.
 * @property error     A [UiError] code, or `null` when there is no error.
 * @property isOnline  Current network connectivity status.
 */
data class AdminStatsUiState(
    val isLoading: Boolean = true,
    val stats: SystemStats? = null,
    val error: UiError? = null,
    val isOnline: Boolean = true
)

/**
 * Abstract base ViewModel that loads and exposes [SystemStats].
 *
 * Subclasses (e.g. `AdminDashboardViewModel`, `AdminReportsViewModel`) inherit
 * the [uiState] flow, the [loadStats] refresh method, and the automatic
 * network-status observation so they can focus on screen-specific behaviour
 * that may be added in the future.
 *
 * @param adminRepository Repository for admin operations.
 * @param networkMonitor  Monitor for observing network connectivity state.
 */
abstract class BaseAdminStatsViewModel(
    private val adminRepository: AdminRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminStatsUiState())

    /** Observable UI state backed by [AdminStatsUiState]. */
    val uiState: StateFlow<AdminStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }
    }

    /**
     * Load (or reload) system statistics from [AdminRepository].
     *
     * Sets [AdminStatsUiState.isLoading] while the request is in-flight and
     * populates either [AdminStatsUiState.stats] or [AdminStatsUiState.error]
     * when it completes.
     */
    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getSystemStats()
                .catch {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UiError.LOAD_STATS_FAILED
                    )
                }
                .collect { stats ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stats = stats,
                        error = null
                    )
                }
        }
    }
}
