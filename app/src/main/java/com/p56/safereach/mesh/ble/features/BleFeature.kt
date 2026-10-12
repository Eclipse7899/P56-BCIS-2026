package com.p56.safereach.mesh.ble.features


enum class BleFeature(
    val label: String,
    val description: String,
) {
    LeCodedPhy(
        label = "Bluetooth Coded PHY",
        description = "Long-range LE signalling required to advertise and scan for alerts.",
    ),

    LeExtendedAdvertising(
        label = "Bluetooth 5 (extended advertising)",
        description = "Extended advertising and scanning required to exchange alerts.",
    ),
}
