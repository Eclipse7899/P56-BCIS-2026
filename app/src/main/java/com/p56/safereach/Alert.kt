@file:Suppress("MagicNumber")

package com.p56.safereach

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

enum class AlertType(val code: Int) {
    EARTHQUAKE(0),
    TSUNAMI(1),
    FIRE(2),
    FLOOD(3);

    companion object {
        private val map = entries.associateBy(AlertType::code)
        fun fromCode(code: Int) =
            map[code] ?: throw IllegalArgumentException("Unknown AlertType code: $code")
    }
}

enum class Severity(val code: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2);

    companion object {
        private val map = entries.associateBy(Severity::code)
        fun fromCode(code: Int) =
            map[code] ?: throw IllegalArgumentException("Unknown Severity code: $code")
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Alert(
    @ProtoNumber(1) val timestamp: Long,
    @ProtoNumber(2) val title: String,
    @ProtoNumber(3) val description: String,
    @ProtoNumber(4) val type: AlertType,
    @ProtoNumber(5) val severity: Severity
)
