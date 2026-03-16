package com.example.androidapp.ui.screens.create



import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DraftSaver<T>(
    private val scope: CoroutineScope,
    private val delayMillis: Long = 2000,
    private val onSave: suspend (T) -> Unit
) {

    private val draftFlow = MutableStateFlow<T?>(null)

    init {
        scope.launch {
            draftFlow
                .debounce(delayMillis)
                .collectLatest { draft ->
                    draft?.let { onSave(it) }
                }
        }
    }

    fun scheduleSave(draft: T) {
        draftFlow.value = draft
    }
}