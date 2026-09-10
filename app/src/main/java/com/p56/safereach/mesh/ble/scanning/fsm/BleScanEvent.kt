package com.p56.safereach.mesh.ble.scanning.fsm

import com.p56.safereach.mesh.ble.scanning.BleScanResult

sealed interface BleScanEvent {
    data object Started : BleScanEvent
    data class Scanned(val result: BleScanResult) : BleScanEvent
    data object Stopped : BleScanEvent
    data object Failed : BleScanEvent
    data object NoOp : BleScanEvent
}