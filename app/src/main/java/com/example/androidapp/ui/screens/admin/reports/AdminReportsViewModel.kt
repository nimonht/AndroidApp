package com.example.androidapp.ui.screens.admin.reports

import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.ui.screens.admin.BaseAdminStatsViewModel

/**
 * ViewModel for the admin reports screen.
 *
 * Delegates all statistics loading and network monitoring to
 * [BaseAdminStatsViewModel]. Screen-specific behaviour can be
 * added here in the future without affecting the dashboard.
 *
 * @param adminRepository Repository for admin operations.
 * @param networkMonitor Monitor for observing network connectivity state.
 */
class AdminReportsViewModel(
    adminRepository: AdminRepository,
    networkMonitor: NetworkMonitor
) : BaseAdminStatsViewModel(adminRepository, networkMonitor)
