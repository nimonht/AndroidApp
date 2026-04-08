package com.example.androidapp.ui.screens.admin.reports

import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.ui.screens.admin.BaseAdminStatsViewModel

/**
 * ViewModel for the admin reports screen.
 *
 * Delegates all statistics loading and network monitoring to
 * [BaseAdminStatsViewModel]. Screen-specific behaviour can be
 * added here in the future without affecting the dashboard.
 *
 * @param adminRepository Repository for admin operations.
 * @param authRepository Repository for authentication and current-user queries.
 * @param networkMonitor Monitor for observing network connectivity state.
 */
class AdminReportsViewModel(
    adminRepository: AdminRepository,
    authRepository: AuthRepository,
    networkMonitor: NetworkMonitor
) : BaseAdminStatsViewModel(adminRepository, authRepository, networkMonitor)
