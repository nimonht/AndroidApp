package com.example.androidapp.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain-layer sync state constants.
 *
 * Mirrors the data-layer `SyncState` enum to keep the domain layer
 * free of Android/data-layer imports.
 */
enum class ConsoleSyncState {
    /** No active or pending sync operations. */
    IDLE,

    /** A sync operation is currently in progress. */
    SYNCING,

    /** Operations are queued but not yet executing. */
    PENDING,

    /** The last sync operation failed. */
    ERROR
}

/**
 * Domain-layer interface for sync management operations.
 *
 * Commands use this to trigger and monitor background synchronisation
 * without coupling to the concrete [com.example.androidapp.data.sync.SyncManager].
 */
interface SyncService {
    /** Current sync state as a reactive stream (using domain-layer enum). */
    val consoleSyncState: StateFlow<ConsoleSyncState>

    /** Returns true if sync is currently permitted by user settings and network state. */
    suspend fun isSyncAllowed(): Boolean

    /** Processes all queued pending sync operations. */
    suspend fun processPendingOperations()

    /** Retries previously failed sync operations. */
    suspend fun retryFailedOperations()

    /** Returns the number of pending (unsynced) operations in the queue. */
    suspend fun getPendingCount(): Int
}
