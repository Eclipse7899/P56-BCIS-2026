package com.p56.safereach.mesh.ble.scanning

import kotlinx.coroutines.channels.Channel

object BleScanResultQueue {
    val channel: Channel<BleScanResult> = Channel(Channel.UNLIMITED)
}