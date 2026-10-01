package com.p56.safereach.mesh.ble.scanning.fsm

sealed interface BleScanCommand {
    data object Start : BleScanCommand
    data object Stop : BleScanCommand
}