package com.p56.safereach.mesh.ble.advertisement.fsm

import android.bluetooth.le.AdvertisingSet
import com.p56.safereach.mesh.ble.advertisement.BleAdvertisingFail

sealed interface BleAdvertisingState {

    data object Idle : BleAdvertisingState

    data class Starting(val attempt: Int) : BleAdvertisingState

    data class Advertising(
        val advertisingSet: AdvertisingSet,
        val payload: ByteArray
    ) : BleAdvertisingState

    data object Stopping : BleAdvertisingState

    data class Retrying(
        val error: BleAdvertisingFail,
        val attempt: Int,
        val retryAtNanos: Long,
    ) : BleAdvertisingState
}
