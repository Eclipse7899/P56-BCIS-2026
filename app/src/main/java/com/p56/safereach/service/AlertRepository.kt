package com.p56.safereach.service

import android.util.Log
import com.google.protobuf.InvalidProtocolBufferException
import com.p56.safereach.android.AlertNotifier
import com.p56.safereach.data.AlertUpsertResult
import com.p56.safereach.data.AppDatabase
import com.p56.safereach.data.toDomain
import com.p56.safereach.data.toEntity
import com.p56.safereach.model.Alert
import com.p56.safereach.model.ReceivedMessage
import com.p56.safereach.model.SignedMessage
import com.p56.safereach.model.isActive
import com.p56.safereach.model.toDomain
import com.p56.safereach.proto.MessageDataDto
import com.p56.safereach.util.ApplicationScope
import com.p56.safereach.verification.SigningManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AlertRepository"

@Singleton
class AlertRepository @Inject constructor(
    private val signingManager: SigningManager,
    private val database: AppDatabase,
    private val alertNotifier: AlertNotifier,
    @param:ApplicationScope
    private val scope: CoroutineScope,
) {

    private val receivedMessageState = MutableStateFlow<ReceivedMessage?>(null)

    val receivedMessage: StateFlow<ReceivedMessage?> = receivedMessageState.asStateFlow()

    private val activeAlertsState = MutableStateFlow<List<Alert>>(emptyList())

    val activeAlerts: StateFlow<List<Alert>> = activeAlertsState.asStateFlow()

    init {
        scope.launch {
            database.signedMessageDao().observeLatest().collect { entity ->
                if (entity == null) {
                    receivedMessageState.value = null
                    return@collect
                }
                val data = runCatching { MessageDataDto.parseFrom(entity.data) }.getOrNull()
                receivedMessageState.value = data?.let {
                    ReceivedMessage(
                        signedMessage = entity.toDomain(),
                        batch = it.toDomain(),
                        receivedFrom = entity.receivedFrom,
                        receivedAt = entity.receivedAt,
                    )
                }
            }
        }
        scope.launch {
            database.alertDao().observeAll().collect { entities ->
                activeAlertsState.value = entities.map { it.toDomain() }.filter { it.isActive() }
            }
        }
    }

    suspend fun addSigned(
        signedMessage: SignedMessage,
    ) {
        if (!signingManager.verify(signedMessage.payload, signedMessage.signature)) {
            Log.w(TAG, "Failed to verify alert")
            return
        }
        try {
            val innerData = MessageDataDto.parseFrom(signedMessage.payload)
            innerData.msgList
                .map{ it.toDomain() }
                .forEach { addAlert(it) }
            database.signedMessageDao().upsert(
                signedMessage.toEntity(timestamp = innerData.timestamp)
            )
        } catch (_: InvalidProtocolBufferException) {
            Log.w(TAG, "Failed to parse message data")
        }
    }

    private suspend fun addAlert(alert: Alert) {
        Log.d(TAG, "Upserting alert: ${alert.id}")
        val result = database.alertDao().upsertIfNewer(alert.toEntity())
        if (result != AlertUpsertResult.IGNORED && alert.isActive()) {
            alertNotifier.notify(alert)
        }
    }

    suspend fun removeSigned() {
        database.signedMessageDao().clear()
    }
}
