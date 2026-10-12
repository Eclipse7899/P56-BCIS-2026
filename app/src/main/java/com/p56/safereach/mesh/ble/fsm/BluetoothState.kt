package com.p56.safereach.mesh.ble.fsm

sealed interface BluetoothState {
    data object Disabled : BluetoothState
    data object Enabled : BluetoothState
}