package com.p56.safereach.mesh.ble.scanning.fsm

import com.p56.safereach.mesh.ble.scanning.BleScanFail


sealed interface BleScanEvent {

    data object ScanStarted : BleScanEvent

    data object ScanStopped : BleScanEvent

    data class Failed(val error: BleScanFail) : BleScanEvent
}
