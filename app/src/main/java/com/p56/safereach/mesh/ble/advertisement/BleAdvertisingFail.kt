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

/**
 * Whether the failure is expected to be transient. Permission, size, capability, and
 * already-started failures are not retryable; other failures (including timeouts and
 * unknown platform codes) are retried with backoff.
 */
val BleAdvertisingFail.isRetryable: Boolean
    get() = when (this) {
        BleAdvertisingFail.MissingPermissions,
        BleAdvertisingFail.DataTooLarge,
        BleAdvertisingFail.FeatureUnsupported,
        BleAdvertisingFail.AlreadyStarted -> false

        else -> true
    }

