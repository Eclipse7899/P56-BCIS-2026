package com.p56.safereach.mesh.ble.advertisement.fsm

sealed interface BleAdvertisingCommand {
    data class Start(
        val payload: ByteArray
    ) : BleAdvertisingCommand

    data object Stop : BleAdvertisingCommand
}