package com.p56.safereach.mesh.ble.advertisement.fsm

import android.bluetooth.le.AdvertisingSet
import com.p56.safereach.mesh.ble.advertisement.BleAdvertisingFail

sealed interface BleAdvertisingEvent {

    data class Started(val set: AdvertisingSet, val payload: ByteArray) : BleAdvertisingEvent

    data object Stopped : BleAdvertisingEvent

    data class Failed(
        val error: BleAdvertisingFail
    ) : BleAdvertisingEvent
}
