package com.p56.safereach.data

import androidx.compose.ui.geometry.CornerRadius
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.p56.safereach.model.Alert
import com.p56.safereach.model.AlertType
import com.p56.safereach.model.Severity

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: Int,
    val timestamp: Long,
    val title: String,
    val description: String,
    val type: Int,
    val severity: Int,
    val longitude: Double,
    val latitude: Double,
    val radius: Int,
    val expiresAt: Long,
    val invalidated: Boolean,
)

fun AlertEntity.toDomain(): Alert = Alert(
    id = id,
    timestamp = timestamp,
    title = title,
    description = description,
    type = AlertType.fromValue(type),
    severity = Severity.fromValue(severity),
    expiresAt = expiresAt,
    longitude = longitude,
    latitude = latitude,
    radius = radius,
    invalidated = invalidated
)

fun Alert.toEntity(): AlertEntity = AlertEntity(
    id = id,
    timestamp = timestamp,
    title = title,
    description = description,
    type = type.value,
    severity = severity.value,
    expiresAt = expiresAt,
    longitude = longitude,
    latitude = latitude,
    radius = radius,
    invalidated = invalidated,
)
