package com.p56.safereach

import com.p56.safereach.proto.SignedMessage

data class MeshMeta(
    val receivedAt: Long,
    val receivedFrom: String?,
    val hopCount: Long,
    val data: SignedMessage
)
