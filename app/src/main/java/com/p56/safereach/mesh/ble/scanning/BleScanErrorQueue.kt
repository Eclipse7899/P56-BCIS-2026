package com.p56.safereach.mesh.ble.scanning

import kotlinx.coroutines.channels.Channel

object BleScanErrorQueue {
    val channel: Channel<BleScanFail> = Channel(Channel.UNLIMITED)
}
