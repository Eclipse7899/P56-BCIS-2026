package com.p56.safereach.mesh.ble.scanning

sealed interface BleScanFail {
    data object ScanningTooFrequently : BleScanFail
    data object ApplicationRegistrationFailed : BleScanFail
    data object FeatureUnsupported : BleScanFail
    data object InternalError : BleScanFail
    data object OutOfHardwareResources : BleScanFail
    data class Unknown(val code: Int) : BleScanFail
}

/**
 * Whether trying again could plausibly succeed. Everything except [BleScanFail.FeatureUnsupported]
 * is treated as transient: unsupported hardware will not start working, but a busy or temporarily
 * broken BLE stack might.
 */
val BleScanFail.isRetryable: Boolean
    get() = when (this) {
        BleScanFail.FeatureUnsupported -> false
        else -> true
    }
