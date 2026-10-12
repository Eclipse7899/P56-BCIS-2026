package com.p56.safereach.mesh.ble.fsm

sealed interface BleIntent {
    data object Off : BleIntent
    data object Passive : BleIntent
    data class Active(val payload: ByteArray) : BleIntent
}