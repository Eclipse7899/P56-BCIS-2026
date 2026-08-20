package com.p56.safereach.mesh.ble.scanning

data class BleScanResult(
    val receivedAt: Long,
    val receivedFrom: String,
    val data: ByteArray
)