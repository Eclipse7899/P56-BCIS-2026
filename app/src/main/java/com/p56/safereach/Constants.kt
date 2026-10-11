package com.p56.safereach

import java.util.UUID

object Constants {
    val BLE_SERVICE_UUID_START: UUID =
        UUID.fromString("d5013063-3e8b-44f9-b051-040d01d97ac4")
    val BLE_SERVICE_UUID_PART_2: UUID =
        UUID.fromString("d5013063-3e8b-44f9-b051-040d01d97ac5")
    val BLE_SERVICE_UUID_PART_3: UUID =
        UUID.fromString("d5013063-3e8b-44f9-b051-040d01d97ac6")
    val BLE_SERVICE_UUID_PART_4: UUID =
        UUID.fromString("d5013063-3e8b-44f9-b051-040d01d97ac7")
    val BLE_SERVICE_UUID_PART_5: UUID =
        UUID.fromString("d5013063-3e8b-44f9-b051-040d01d97ac8")
    val BLE_SERVICE_UUID_PART_6: UUID =
        UUID.fromString("d5013063-3e8b-44f9-b051-040d01d97ac9")

    val SERVICE_UUIDS = listOf(
        BLE_SERVICE_UUID_START,
        BLE_SERVICE_UUID_PART_2,
        BLE_SERVICE_UUID_PART_3,
        BLE_SERVICE_UUID_PART_4,
        BLE_SERVICE_UUID_PART_5,
        BLE_SERVICE_UUID_PART_6
    )

    const val MAX_DATA_PAYLOAD = 220

    const val BLE_ADVERTISING_TIMEOUT: Long = 2000

    const val SCAN_RETRY_BACKOFF_MS = 1000L

    const val BLE_SCAN_INTENT_ACTION = "com.p56.safereach.BLE_SCAN_RESULT"

    const val APP_DISPLAY_NAME = "SafeReach"

    const val ALERTS_API_BASE_URL =
        "https://australia-southeast1-disaster-monitoring-2d085.cloudfunctions.net/alertsApi"

    const val ALERTS_API_PATH = "/alerts"

    const val ALERTS_API_DEFAULT_COUNT = 50
}
