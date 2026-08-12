package com.p56.safereach

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
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
                val alert = ProtoBuf.decodeFromHexString<Alert>(encoded)
                val meshPacket = MeshPacket(
                    hopCount = 0,
                    data = alert
                )
                val meshMeta = MeshMeta(
                    receivedAt = System.currentTimeMillis(),
                    receivedFrom = null,
                    packet = meshPacket
                )
                AlertStateRepository.update(meshMeta)
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