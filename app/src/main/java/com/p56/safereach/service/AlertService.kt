package com.p56.safereach.service

import android.util.Log
import com.p56.safereach.AlertContext
import com.p56.safereach.Constants.MAX_ADVERTISING_ALERTS
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
import kotlinx.coroutines.flow.map

const val TAG = "AlertService"

@Singleton
class AlertService @Inject constructor(private val signingManager: SigningManager) {
    private val alertsState = MutableStateFlow<Map<String, AlertContext>>(mutableMapOf())

    val alerts = alertsState.asStateFlow()

    val activeAlerts: Flow<List<Alert>> =
        alerts.map { alerts ->
            alerts.values
                .filter { !it.alert.isExpired() }
                .filter { !it.alert.invalidated }
                .map { it.alert }
        }

    val broadcastableMessages: Flow<List<SignedMessage>> = alerts.map { alerts ->
        alerts.values
            .filter { !it.alert.isExpired() }
            .sortedWith(
                compareByDescending<AlertContext> { it.alert.severityValue }.thenByDescending { it.alert.timestamp }
            )
            .map { it.signedMessage }
            .take(MAX_ADVERTISING_ALERTS)
    }


    fun add(ctx: AlertContext) {
        Log.d(TAG, "Adding alert: ${ctx.alert.id}")
        if (!signingManager.verify(ctx.signedMessage)) {
            Log.w(TAG, "Failed to verify incoming mesh packet")
            return
        }
        val id = ctx.alert.id
        val existing = alerts.value[id]
        if (existing == null) {
            Log.d(TAG, "Adding new alert: ${ctx.alert.id}")
            alertsState.value = alerts.value.plus(id to ctx)
        } else if (ctx.alert.isSuperseded(existing.alert)) {
            Log.d(TAG, "Replacing superseded alert: ${ctx.alert.id}")
            alertsState.value = alerts.value.minus(id).plus(id to ctx)
        }
    }

    fun remove(id: String) {
        alertsState.value = alerts.value.minus(id)
    }

    fun clear() {
        alertsState.value = mutableMapOf()
    }
}