package com.p56.safereach

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.protobuf.ProtoBuf

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseAlertService : FirebaseMessagingService() {

    @OptIn(ExperimentalSerializationApi::class)
    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.data["alert"]?.let { encoded ->
            try {
                val msg = ProtoBuf.decodeFromHexString<SignedMessage>(encoded)
                val meta = MeshMeta(
                    receivedAt = System.currentTimeMillis(),
                    receivedFrom = null,
                    hopCount = 0,
                    data = msg
                )
                val alert = ProtoBuf.decodeFromByteArray<Alert>(msg.data)
                AlertStateRepository.updateAlert(alert)
                AlertStateRepository.updateMessage(meta)
            } catch (e: SerializationException) {
                Log.w("FirebaseAlertService", "Failed to decode incoming alert", e)
            } catch (e: IllegalArgumentException) {
                Log.w(
                    "FirebaseAlertService",
                    "Failed to decode incoming alert - invalid hex string",
                    e
                )
            }
        }
    }
}