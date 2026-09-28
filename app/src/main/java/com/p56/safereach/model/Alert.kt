package com.p56.safereach.model

enum class AlertType(val value: Int, val displayName: String) {
    EARTHQUAKE(0, "Earthquake"),
    TSUNAMI(1, "Tsunami"),
    FIRE(2, "Fire"),
    FLOOD(3, "Flood"),
    UNKNOWN(-1, "Unknown");

    companion object {
        fun fromValue(value: Int): AlertType =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}

enum class Severity(val value: Int, val displayName: String) {
    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High"),
    UNKNOWN(-1, "Unknown");

    companion object {
        fun fromValue(value: Int): Severity =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}

data class Alert(
    val id: String,
    val timestamp: Long,
    val title: String,
    val description: String,
    val type: AlertType,
    val severity: Severity,
    val shape: Shape?,
    val version: Long,
    val expiresAt: Long,
    val invalidated: Boolean,
)
