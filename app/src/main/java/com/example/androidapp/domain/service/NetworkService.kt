package com.example.androidapp.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain-layer interface for network connectivity status.
 *
 * Provides reactive streams for monitoring online status and WiFi
 * connectivity without coupling to the concrete
 * [com.example.androidapp.data.network.NetworkMonitor].
 */
interface NetworkService {
    /** Whether the device currently has internet connectivity. */
    val isOnline: StateFlow<Boolean>

    /** Whether the current active network uses an unmetered (WiFi) transport. */
    val isWifi: StateFlow<Boolean>
}
