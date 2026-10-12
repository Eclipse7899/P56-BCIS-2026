package com.p56.safereach.mesh.ble.scanning.fsm


sealed interface BleScanDesiredState {
    data object Idle : BleScanDesiredState
    data object Scanning : BleScanDesiredState
}
