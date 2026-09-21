package com.p56.safereach

import com.p56.safereach.proto.Alert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AlertStateRepository {
    private val alertState = MutableStateFlow<Alert?>(null)

    private val messageState = MutableStateFlow<MeshMeta?>(null)

    val alert: StateFlow<Alert?> = alertState.asStateFlow()

    val message: StateFlow<MeshMeta?> = messageState.asStateFlow()

    fun updateAlert(value: Alert) {
        alertState.value = value
    }

    fun updateMessage(value: MeshMeta) {
        messageState.value = value
    }

    fun clearAlert() {
        alertState.value = null
    }

    fun clearMessage() {
        messageState.value = null
    }
}
