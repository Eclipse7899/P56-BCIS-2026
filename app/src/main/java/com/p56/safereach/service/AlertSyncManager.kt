package com.p56.safereach.service

import android.util.Log
import com.p56.safereach.data.remote.AlertApiClient
import jakarta.inject.Inject
import jakarta.inject.Singleton

private const val TAG = "AlertSyncManager"


@Singleton
class AlertSyncManager @Inject constructor(
    private val alertApiClient: AlertApiClient,
    private val alertRepository: AlertRepository,
) {

    suspend fun sync() {
        runCatching { alertApiClient.fetchAlerts() }
            .onSuccess { alerts ->
                alertRepository.syncAlerts(alerts)
                Log.d(TAG, "Synced ${alerts.size} alerts from the alerts API")
            }
            .onFailure { Log.w(TAG, "Failed to sync alerts from the alerts API", it) }
    }
}
