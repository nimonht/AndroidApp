package com.example.androidapp.domain.service

import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer interface for application settings access.
 *
 * Exposes reactive [Flow]s for reading preferences and suspending setters
 * for writing changes, without coupling to the concrete implementation in the data layer.
 */
interface SettingsService {

    /** Dark theme mode constants. */
    companion object {
        /** Follow the device system setting. */
        const val THEME_MODE_SYSTEM = 0

        /** Force light theme. */
        const val THEME_MODE_LIGHT = 1

        /** Force dark theme. */
        const val THEME_MODE_DARK = 2
    }

    /** Current dark theme mode ([THEME_MODE_SYSTEM], [THEME_MODE_LIGHT], or [THEME_MODE_DARK]). */
    val darkThemeMode: Flow<Int>

    /** Whether automatic background sync is enabled. */
    val autoSyncEnabled: Flow<Boolean>

    /** Whether sync should only happen over WiFi. */
    val wifiOnlySync: Flow<Boolean>

    /** Persists the dark theme mode. */
    suspend fun setDarkThemeMode(mode: Int)

    /** Persists the auto-sync enabled preference. */
    suspend fun setAutoSyncEnabled(enabled: Boolean)

    /** Persists the WiFi-only sync preference. */
    suspend fun setWifiOnlySync(enabled: Boolean)
}
