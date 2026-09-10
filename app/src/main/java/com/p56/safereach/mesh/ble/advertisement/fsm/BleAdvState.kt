package com.p56.safereach.mesh.ble.advertisement.fsm

import android.bluetooth.le.AdvertisingSet
import com.p56.safereach.mesh.ble.advertisement.BleAdvFail

sealed interface BleAdvState {

    data object Idle : BleAdvState
    data object Disabled : BleAdvState

    data class Advertising(
        val advertisingSet: AdvertisingSet,
        val data: ByteArray
    ) : BleAdvState

    data class Failed(
        val error: BleAdvFail
    ) : BleAdvState
}