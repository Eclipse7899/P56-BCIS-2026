package com.p56.safereach.service

import android.util.Log
import com.p56.safereach.AlertContext
import com.p56.safereach.Constants.MAX_ADVERTISING_ALERTS
import com.p56.safereach.expiryTicks
import com.p56.safereach.isExpired
import com.p56.safereach.isSuperseded
import com.p56.safereach.proto.Alert
import com.p56.safereach.proto.SignedMessage
import com.p56.safereach.verification.SigningManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

const val TAG = "AlertService"

@Singleton
class AlertService @Inject constructor(private val signingManager: SigningManager) {
    private val alertsState = MutableStateFlow<Map<String, AlertContext>>(mutableMapOf())

    val alerts = alertsState.asStateFlow()

    private val alertsOnExpiry: Flow<Map<String, AlertContext>> =
        combine(
            alertsState,
            expiryTicks(alertsState.map { alerts -> alerts.values.map { it.alert } })
        ) { alerts, _ -> alerts }

    val activeAlerts: Flow<List<Alert>> =
        alertsOnExpiry.map { alerts ->
            alerts.values
                .filter { !it.alert.isExpired() }
                .filter { !it.alert.invalidated }
                .map { it.alert }
        }

    val broadcastableMessages: Flow<List<SignedMessage>> = alertsOnExpiry.map { alerts ->
        alerts.values
            .filter { !it.alert.isExpired() }
            .sortedWith(
                compareByDescending<AlertContext> { it.alert.severityValue }.thenByDescending { it.alert.timestamp }
            )
            .map { it.signedMessage }
            .take(MAX_ADVERTISING_ALERTS)
    }


    fun add(newCtx: AlertContext) {
        Log.d(TAG, "Adding alert: ${newCtx.alert.id}")
        if (!signingManager.verify(newCtx.signedMessage)) {
            Log.w(TAG, "Failed to verify incoming mesh packet")
            return
        }
        val id = newCtx.alert.id
        val existing = alerts.value[id]
        if (existing == null) {
            Log.d(TAG, "Adding new alert: ${newCtx.alert.id}")
            alertsState.value = alerts.value.plus(id to newCtx)
        } else if (existing.alert.isSuperseded(newCtx.alert)) {
            Log.d(TAG, "Replacing superseded alert: ${existing.alert.id}")
            alertsState.value = alerts.value.minus(id).plus(id to newCtx)
        }
    }

    fun remove(id: String) {
        alertsState.value = alerts.value.minus(id)
    }

    fun clear() {
        alertsState.value = mutableMapOf()
    }
}