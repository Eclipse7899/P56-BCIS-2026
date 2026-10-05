package com.p56.safereach.android

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.p56.safereach.model.toDomain
import com.p56.safereach.proto.SignedMessageDto
import com.p56.safereach.service.AlertRepository
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "FirebaseAlertService"

private const val ALERT_TOPIC = "alerts"

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
@AndroidEntryPoint
class FirebaseAlertMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var alertRepository: AlertRepository

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        subscribeToDefaultTopics()
    }

    private fun subscribeToDefaultTopics() {
        val targetTopic = ALERT_TOPIC
        FirebaseMessaging.getInstance().subscribeToTopic(targetTopic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Automatically subscribed to '$targetTopic' topic.")
                } else {
                    Log.e(TAG, "Auto-subscription to '$targetTopic' failed: ${task.exception?.message}")
                }
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        remoteMessage.data[ALERT_TOPIC]?.let { encoded ->
            try {
                val dto = SignedMessageDto.parseFrom(encoded.hexToByteArray())
                val signed = dto.toDomain("Firebase", System.currentTimeMillis())
                serviceScope.launch {
                    alertRepository.addSigned(signed)
                }
            } catch (e: IllegalArgumentException) {
                Log.w(
                    TAG,
                    "Failed to decode incoming alert - invalid hex string",
                    e
                )
            }
        }
    }
}
