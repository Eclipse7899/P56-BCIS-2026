package com.p56.safereach.mesh.ble.advertisement.fsm

import android.bluetooth.le.AdvertisingSet
import com.p56.safereach.mesh.ble.advertisement.BleAdvertisingFail

sealed interface BleAdvertisingState {

    data object Idle : BleAdvertisingState
    data object Disabled : BleAdvertisingState

    data class Advertising(
        val advertisingSet: AdvertisingSet,
        val payload: ByteArray
    ) : BleAdvertisingState

    data class Failed(
        val error: BleAdvertisingFail
    ) : BleAdvertisingState
}