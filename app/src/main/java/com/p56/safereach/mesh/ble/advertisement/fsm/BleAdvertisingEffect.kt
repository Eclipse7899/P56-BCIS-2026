package com.p56.safereach.mesh.ble.advertisement.fsm

import android.bluetooth.le.AdvertisingSet


sealed interface BleAdvertisingEffect {

    data class Start(val payload: ByteArray) : BleAdvertisingEffect

    data class Update(
        val advertisingSet: AdvertisingSet,
        val payload: ByteArray,
    ) : BleAdvertisingEffect

    data object Stop : BleAdvertisingEffect

    data object NoOp : BleAdvertisingEffect
}
