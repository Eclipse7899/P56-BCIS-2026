package com.p56.safereach.mesh.ble.scanning

data class BleScanResult (
    val timestamp: Long,
    val receivedFrom: String,
    val data: ByteArray
)