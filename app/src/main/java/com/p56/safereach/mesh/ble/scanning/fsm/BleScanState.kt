package com.p56.safereach.mesh.ble.scanning.fsm

import android.bluetooth.le.ScanCallback

sealed interface BleScanState {
    data object Idle : BleScanState
    data object Disabled : BleScanState
    data object Scanning : BleScanState
    data object Failed : BleScanState
}