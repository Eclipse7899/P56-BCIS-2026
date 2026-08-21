package com.p56.safereach

import java.util.UUID

object Constants {
    val BLE_SERVICE_UUID: UUID =
        UUID.fromString("d5013063-3e8b-44f9-b051-040d01d97ac4")
    val BLE_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("a1b2c3d4-5678-90ab-cdef-1244567890ab")

    const val BLE_ADVERTISING_TIMEOUT: Long = 2000

    const val BLE_SCAN_INTENT_ACTION = "com.p56.safereach.BLE_SCAN_RESULT"

    const val APP_DISPLAY_NAME = "SafeReach"
}
