package com.p56.safereach.mesh.ble.fsm

sealed interface BleCommand {
    data object Off : BleCommand
    data object Passive : BleCommand
    data class Active(val payload: ByteArray) : BleCommand
}