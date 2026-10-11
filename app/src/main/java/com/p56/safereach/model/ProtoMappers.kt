package com.p56.safereach.model

import com.google.protobuf.ByteString
import com.p56.safereach.proto.AlertDto
import com.p56.safereach.proto.LatLongDto
import com.p56.safereach.proto.MessageDataDto
import com.p56.safereach.proto.ShapeDto
import com.p56.safereach.proto.SignedMessageDto

fun LatLongDto.toDomain(): LatLong = LatLong(
    latitude = latitude.toDouble(),
    longitude = longitude.toDouble(),
)

fun LatLong.toDto(): LatLongDto = LatLongDto.newBuilder()
    .setLatitude(latitude.toFloat())
    .setLongitude(longitude.toFloat())
    .build()

fun ShapeDto.toDomain(): Shape? = when (kindCase) {
    ShapeDto.KindCase.CIRCLE -> Shape.Circle(
        center = circle.center.toDomain(),
        radius = circle.radius.toDouble(),
    )

    ShapeDto.KindCase.POLYGON -> Shape.Polygon(
        points = polygon.pointsList.map { it.toDomain() },
    )

    else -> null
}

fun Shape.toDto(): ShapeDto {
    val builder = ShapeDto.newBuilder()
    when (this) {
        is Shape.Circle -> builder.setCircle(
            ShapeDto.CircleDto.newBuilder()
                .setCenter(center.toDto())
                .setRadius(radius.toFloat())
                .build()
        )

        is Shape.Polygon -> builder.setPolygon(
            ShapeDto.PolygonDto.newBuilder()
                .addAllPoints(points.map { it.toDto() })
                .build()
        )
    }
    return builder.build()
}

fun AlertDto.toDomain(): Alert = Alert(
    id = id,
    timestamp = timestamp,
    title = title,
    description = description,
    type = AlertType.fromValue(typeValue),
    severity = Severity.fromValue(severityValue),
    shape = if (hasShape()) shape.toDomain() else null,
    version = version,
    expiresAt = expiresAt,
    invalidated = invalidated,
)

fun Alert.toDto(): AlertDto = AlertDto.newBuilder()
    .setId(id)
    .setTimestamp(timestamp)
    .setTitle(title)
    .setDescription(description)
    .setTypeValue(type.value)
    .setSeverityValue(severity.value)
    .setShape(shape?.toDto())
    .setVersion(version)
    .setExpiresAt(expiresAt)
    .setInvalidated(invalidated)
    .build()

fun SignedMessageDto.toDomain(receivedFrom: String, receivedAt: Long): SignedMessage =
    SignedMessage(
        payload = data.toByteArray(),
        signature = signature.toByteArray(),
        receivedFrom = receivedFrom,
        receivedAt = receivedAt,
        cleared = false,
    )

fun SignedMessage.toDto(): SignedMessageDto = SignedMessageDto.newBuilder()
    .setData(ByteString.copyFrom(payload))
    .setSignature(ByteString.copyFrom(signature))
    .build()

fun MessageDataDto.toDomain(): AlertBatch = AlertBatch(
    alerts = msgList.map { it.toDomain() },
    validUntil = validUntil,
    timestamp = timestamp,
)

fun AlertBatch.toDto(): MessageDataDto = MessageDataDto.newBuilder()
    .addAllMsg(alerts.map { it.toDto() })
    .setValidUntil(validUntil)
    .setTimestamp(timestamp)
    .build()
