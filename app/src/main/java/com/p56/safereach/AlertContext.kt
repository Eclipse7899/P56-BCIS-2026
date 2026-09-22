package com.p56.safereach

import com.p56.safereach.proto.Alert
import com.p56.safereach.proto.SignedMessage

data class AlertContext (
    val meta: AlertMeta,
    val alert: Alert,
    val signedMessage: SignedMessage
)