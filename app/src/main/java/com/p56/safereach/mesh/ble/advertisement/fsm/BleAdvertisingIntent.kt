package com.p56.safereach.mesh.ble.advertisement.fsm

sealed interface BleAdvertisingIntent {
    data class Start(
        val payload: ByteArray
    ) : BleAdvertisingIntent

    data object Stop : BleAdvertisingIntent
}