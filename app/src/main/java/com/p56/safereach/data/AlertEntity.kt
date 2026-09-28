package com.p56.safereach.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.p56.safereach.model.Alert
import com.p56.safereach.model.AlertType
import com.p56.safereach.model.Severity
import com.p56.safereach.model.Shape
import com.p56.safereach.model.toDto
import com.p56.safereach.model.toDomain
import com.p56.safereach.proto.ShapeDto

@Entity(tableName = "alerts")
@TypeConverters(ShapeTypeConverters::class)
data class AlertEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val title: String,
    val description: String,
    val type: Int,
    val severity: Int,
    val shape: Shape?,
    val version: Long,
    val expiresAt: Long,
    val invalidated: Boolean,
)

class ShapeTypeConverters {
    @TypeConverter
    fun shapeToByteArray(shape: Shape?): ByteArray? = shape?.toDto()?.toByteArray()

    @TypeConverter
    fun byteArrayToShape(bytes: ByteArray?): Shape? =
        bytes?.let { runCatching { ShapeDto.parseFrom(it).toDomain() }.getOrNull() }
}

fun AlertEntity.toDomain(): Alert = Alert(
    id = id,
    timestamp = timestamp,
    title = title,
    description = description,
    type = AlertType.fromValue(type),
    severity = Severity.fromValue(severity),
    shape = shape,
    version = version,
    expiresAt = expiresAt,
    invalidated = invalidated,
)

fun Alert.toEntity(): AlertEntity = AlertEntity(
    id = id,
    timestamp = timestamp,
    title = title,
    description = description,
    type = type.value,
    severity = severity.value,
    shape = shape,
    version = version,
    expiresAt = expiresAt,
    invalidated = invalidated,
)
