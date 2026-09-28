package com.p56.safereach.model

data class AlertBatch(
    val alerts: List<Alert>,
    val validUntil: Long,
    val timestamp: Long,
)

data class ReceivedMessage(
    val signedMessage: SignedMessage,
    val batch: AlertBatch,
    val receivedFrom: String,
    val receivedAt: Long,
)
