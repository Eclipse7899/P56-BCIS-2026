package com.p56.safereach.mesh.ble.advertisement

sealed interface BleAdvertisingFail {
    data object MissingPermissions : BleAdvertisingFail
    data object TooManyAdvertisements : BleAdvertisingFail
    data object DataTooLarge : BleAdvertisingFail
    data object FeatureUnsupported : BleAdvertisingFail
    data object AlreadyStarted : BleAdvertisingFail
    data object InternalError : BleAdvertisingFail
    data class Unknown(val code: Int) : BleAdvertisingFail
    data object Timeout : BleAdvertisingFail
}

