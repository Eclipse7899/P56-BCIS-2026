package com.p56.safereach.mesh.ble.advertisement.fsm

sealed interface BleAdvIntent {
    data class Start(
        val payload: ByteArray
    ) : BleAdvIntent

    data object Stop : BleAdvIntent
}