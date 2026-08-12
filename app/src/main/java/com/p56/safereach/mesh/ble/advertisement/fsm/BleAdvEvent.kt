package com.p56.safereach.mesh.ble.advertisement.fsm

import android.bluetooth.le.AdvertisingSet
import com.p56.safereach.mesh.ble.advertisement.BleAdvFail

sealed interface BleAdvEvent {
    data class Started(val set: AdvertisingSet, val data: ByteArray) : BleAdvEvent

    data object Stopped : BleAdvEvent

    data class Failed(
        val error: BleAdvFail
    ) : BleAdvEvent

    data object NoOp : BleAdvEvent
}