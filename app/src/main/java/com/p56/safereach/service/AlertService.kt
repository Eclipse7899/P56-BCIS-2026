package com.p56.safereach.service

import com.p56.safereach.AlertContext
import com.p56.safereach.isExpired
import com.p56.safereach.isSuperseded
import com.p56.safereach.proto.Alert
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class AlertService @Inject constructor() {
    private val alertsState = MutableStateFlow<Map<String, AlertContext>>(mutableMapOf())

    val alerts = alertsState.asStateFlow()

    val activeAlerts: Flow<List<Alert>> =
        alerts.map { alerts ->
            alerts.values
                .filter { it.alert.isExpired() }
                .filter { it.alert.invalidated }
                .map { it.alert }
        }

    fun add(newAlertCtx: AlertContext) {
        val id = newAlertCtx.alert.id
        val existing = alerts.value[id]
        if (existing == null) {
            alertsState.value = alerts.value.plus(id to newAlertCtx)
        } else if (newAlertCtx.alert.isSuperseded(existing.alert)) {
            alertsState.value = alerts.value.minus(id).plus(id to newAlertCtx)
        }
    }

    fun remove(id: String) {
        alertsState.value = alerts.value.minus(id)
    }

    fun clear() {
        alertsState.value = mutableMapOf()
    }
}