package com.p56.safereach.mesh.ble.scanning.fsm

import com.p56.safereach.mesh.ble.scanning.BleScanFail


sealed interface BleScanState {

    data object Idle : BleScanState

    data class Starting(val attempt: Int) : BleScanState

    data object Scanning : BleScanState

    data object Stopping : BleScanState

    data class Retrying(
        val error: BleScanFail,
        val attempt: Int,
        val retryAtNanos: Long,
    ) : BleScanState
}
