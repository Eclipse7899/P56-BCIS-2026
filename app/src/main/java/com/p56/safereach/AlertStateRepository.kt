package com.p56.safereach

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AlertStateRepository {
    private val alertState = MutableStateFlow<Alert?>(null)

    private val alertHistoryState = MutableStateFlow<List<Alert>>(emptyList())

    private val messageState = MutableStateFlow<MeshMeta<SignedMessage>?>(null)

    val alert: StateFlow<Alert?> = alertState.asStateFlow()

    val alertHistory: StateFlow<List<Alert>> = alertHistoryState.asStateFlow()

    val message: StateFlow<MeshMeta<SignedMessage>?> = messageState.asStateFlow()

    fun updateAlert(value: Alert) {
        alertState.value?.let {previousAlert -> alertHistoryState.value =
        listOf(previousAlert) + alertHistoryState.value}
        alertState.value = value
    }

    fun updateMessage(value: MeshMeta<SignedMessage>) {
        messageState.value = value
    }

    fun clearAlert() {
        alertState.value?.let { previousAlert -> alertHistoryState.value =
        listOf(previousAlert) + alertHistoryState.value}
        alertState.value = null
    }

    fun clearAlertHistory(){
        alertHistoryState.value = emptyList()
    }

    fun clearMessage() {
        messageState.value = null
    }
}

