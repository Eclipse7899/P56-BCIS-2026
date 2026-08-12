package com.p56.safereach.mesh.ble.fsm

sealed interface BleState {
    data object Off : BleState
    data object Passive : BleState
    data class Active(val payload: ByteArray) : BleState
    data object Failed : BleState
}