package com.example.androidapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.androidapp.domain.service.SettingsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quizzez_settings")

/**
 * DataStore-backed persistence for application settings.
 *
 * Exposes reactive [Flow]s for each preference and suspending setters
 * that write changes to disk asynchronously.
 *
 * @param context Application context used to access the DataStore instance.
 */
class SettingsPreferences(private val context: Context) : SettingsService {

    private object Keys {
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val WIFI_ONLY_SYNC = booleanPreferencesKey("wifi_only_sync")
        val DARK_THEME_MODE = intPreferencesKey("dark_theme_mode")
    }

    /**
     * Dark theme mode constants.
     * - [THEME_MODE_SYSTEM] follows the device system setting.
     * - [THEME_MODE_LIGHT] forces light theme.
     * - [THEME_MODE_DARK] forces dark theme.
     */
    companion object {
        const val THEME_MODE_SYSTEM = 0
        const val THEME_MODE_LIGHT = 1
        const val THEME_MODE_DARK = 2
    }

    /** Whether automatic background sync is enabled. Defaults to `true`. */
    override val autoSyncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.AUTO_SYNC_ENABLED] ?: true }

    /** Whether sync should only happen over WiFi (data-saving mode). Defaults to `false`. */
    override val wifiOnlySync: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.WIFI_ONLY_SYNC] ?: false }

    /**
     * Current dark theme mode. Defaults to [THEME_MODE_SYSTEM].
     * @see THEME_MODE_SYSTEM
     * @see THEME_MODE_LIGHT
     * @see THEME_MODE_DARK
     */
    override val darkThemeMode: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[Keys.DARK_THEME_MODE] ?: THEME_MODE_SYSTEM }

    /** Persists the auto-sync enabled preference. */
    override suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTO_SYNC_ENABLED] = enabled
        }
    }

    /** Persists the WiFi-only sync preference. */
    override suspend fun setWifiOnlySync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.WIFI_ONLY_SYNC] = enabled
        }
    }

    /** Persists the dark theme mode preference. */
    override suspend fun setDarkThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DARK_THEME_MODE] = mode
        }
    }
}
