package com.example.androidapp.data.session

import android.content.Context
import java.util.UUID

/**
 * Manages the guest session lifecycle.
 * Generates a stable guest UUID on first launch and persists it in SharedPreferences.
 * Exposes [isGuest] and [guestId] for UI and repository layers.
 */
class GuestSessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("guest_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_IS_GUEST = "is_guest"
        private const val GUEST_ID_PREFIX = "guest_"
    }

    /** Current guest mode flag. */
    val isGuest: Boolean get() = prefs.getBoolean(KEY_IS_GUEST, false)

    /** Stable guest UUID, or null if not in guest mode. */
    val guestId: String? get() = prefs.getString(KEY_GUEST_ID, null)

    /** Starts a guest session: generates UUID if needed, sets isGuest = true. */
    fun startGuestSession(): String {
        var id = prefs.getString(KEY_GUEST_ID, null)
        if (id == null) {
            id = "$GUEST_ID_PREFIX${UUID.randomUUID()}"
            prefs.edit()
                .putString(KEY_GUEST_ID, id)
                .putBoolean(KEY_IS_GUEST, true)
                .apply()
        } else {
            prefs.edit().putBoolean(KEY_IS_GUEST, true).apply()
        }
        return id
    }

    /** Ends the guest session and clears stored guest data. */
    fun clearGuestSession() {
        prefs.edit()
            .putBoolean(KEY_IS_GUEST, false)
            .remove(KEY_GUEST_ID)
            .apply()
    }
}
