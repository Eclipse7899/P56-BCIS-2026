package com.p56.safereach.model

import com.google.protobuf.ByteString
import com.p56.safereach.proto.AlertBatchDto
import com.p56.safereach.proto.AlertDto
import com.p56.safereach.proto.SignedMessageDto

fun AlertDto.toDomain(): Alert = Alert(
    id = id,
    timestamp = timestamp,
    title = title,
    description = description,
    type = AlertType.fromValue(typeValue),
    severity = Severity.fromValue(severityValue),
    latitude = latitudeE5 / 1E5,
    longitude = longitudeE5 / 1E5,
    expiresAt = getExpiresAt(expiresInS, timestamp),
    radius = radiusM,
    invalidated = invalidated
)

fun getExpiresAt(expiresInSec: Int, timestamp: Long): Long {
    val expiresInMs = expiresInSec * 1000L
    return timestamp + expiresInMs
}

fun getExpiresInSeconds(expiresAtMs: Long, timestampMs: Long): Int {
    val expiresIn = (expiresAtMs - timestampMs).coerceAtLeast(0)
    return (expiresIn / 1000).toInt()
}

fun Alert.toDto(): AlertDto = AlertDto.newBuilder()
    .setId(id)
    .setTimestamp(timestamp)
    .setTitle(title)
    .setDescription(description)
    .setTypeValue(type.value)
    .setSeverityValue(severity.value)
    .setExpiresInS(getExpiresInSeconds(expiresAt, timestamp))
    .build()

fun SignedMessageDto.toDomain(receivedFrom: String, receivedAt: Long): SignedMessage =
    SignedMessage(
        payload = data.toByteArray(),
        signature = signature.toByteArray(),
        receivedFrom = receivedFrom,
        receivedAt = receivedAt,
        dismissed = false,
    )

fun SignedMessage.toDto(): SignedMessageDto = SignedMessageDto.newBuilder()
    .setData(ByteString.copyFrom(payload))
    .setSignature(ByteString.copyFrom(signature))
    .build()

fun AlertBatchDto.toDomain(): AlertBatch = AlertBatch(
    alerts = alertsList.map { it.toDomain() },
    validUntil = getExpiresAt(expiresInS, timestamp),
    timestamp = timestamp,
)

fun AlertBatch.toDto(): AlertBatchDto = AlertBatchDto.newBuilder()
    .addAllAlerts(alerts.map { it.toDto() })
    .setExpiresInS(getExpiresInSeconds(validUntil, timestamp))
    .setTimestamp(timestamp)
    .build()
