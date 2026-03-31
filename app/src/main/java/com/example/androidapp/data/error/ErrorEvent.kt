package com.example.androidapp.data.error

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A thread-safe one-shot event bus for user-facing error messages.
 * ViewModels post errors; the top-level Composable consumes them as Snackbar/Dialog.
 */
object ErrorEvent {
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    fun post(message: String) {
        _errors.tryEmit(message)
    }
}
