package com.p56.safereach.mesh.ble.advertisement.fsm


sealed interface BleAdvertisingDesiredState {
    data object Idle : BleAdvertisingDesiredState
    data class Advertising(val payload: ByteArray) : BleAdvertisingDesiredState
}
