package com.p56.safereach.mesh.ble.scanning.fsm

import android.bluetooth.le.ScanCallback
import com.p56.safereach.mesh.ble.scanning.BleScanResult

sealed interface BleScanEvent {
    data class Started(val callback: ScanCallback) : BleScanEvent
    data class Scanned(val result: BleScanResult) : BleScanEvent
    data object Stopped : BleScanEvent
    data object Failed : BleScanEvent
    data object NoOp : BleScanEvent
}