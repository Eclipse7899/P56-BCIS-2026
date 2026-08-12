package com.p56.safereach.mesh.ble.scanning.fsm

import android.bluetooth.le.ScanCallback

sealed interface BleScanState {
    data object Idle : BleScanState
    data class Scanning(val callback: ScanCallback) : BleScanState
    data object Failed : BleScanState
}