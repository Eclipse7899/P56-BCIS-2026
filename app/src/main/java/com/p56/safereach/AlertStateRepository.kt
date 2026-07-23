package com.p56.safereach

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AlertStateRepository {
    private val messageState = MutableStateFlow<Alert?>(null)
    val message: StateFlow<Alert?> = messageState.asStateFlow()

    fun update(value: Alert) {
        if (value.timestamp <= (messageState.value?.timestamp ?: 0L)) {
            return
        }
        messageState.value = value
    }
}

