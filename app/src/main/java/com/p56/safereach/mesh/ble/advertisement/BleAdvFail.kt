package com.p56.safereach.mesh.ble.advertisement

sealed interface BleAdvFail {
    data object MissingPermissions : BleAdvFail
    data object TooManyAdvertisements : BleAdvFail
    data object DataTooLarge : BleAdvFail
    data object FeatureUnsupported : BleAdvFail
    data object AlreadyStarted : BleAdvFail
    data object InternalError : BleAdvFail
    data class Unknown(val code: Int) : BleAdvFail
    data object Timeout : BleAdvFail
}

