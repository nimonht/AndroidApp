package com.example.androidapp.ui.screens.admin.dashboard

import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.ui.screens.admin.BaseAdminStatsViewModel

/**
 * ViewModel for the admin dashboard screen.
 *
 * All stats-loading logic and UI state are provided by [BaseAdminStatsViewModel].
 * This subclass exists so the dashboard and reports screens retain distinct
 * ViewModel types, allowing them to diverge independently in the future.
 *
 * @param adminRepository Repository for admin operations.
 * @param networkMonitor Monitor for observing network connectivity state.
 */
class AdminDashboardViewModel(
    adminRepository: AdminRepository,
    networkMonitor: NetworkMonitor
) : BaseAdminStatsViewModel(adminRepository, networkMonitor)
