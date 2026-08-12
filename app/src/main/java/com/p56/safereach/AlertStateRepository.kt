package com.p56.safereach

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AlertStateRepository {
    private val messageState = MutableStateFlow<MeshMeta<Alert>?>(null)
    val alert: StateFlow<MeshMeta<Alert>?> = messageState.asStateFlow()

    fun update(value: MeshMeta<Alert>) {
        messageState.value = value
    }

    fun clear() {
        messageState.value = null
    }
}

