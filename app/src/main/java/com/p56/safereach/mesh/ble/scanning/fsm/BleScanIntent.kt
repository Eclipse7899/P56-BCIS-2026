package com.p56.safereach.mesh.ble.scanning.fsm

sealed interface BleScanIntent {
    data object Start : BleScanIntent
    data object Stop : BleScanIntent
}