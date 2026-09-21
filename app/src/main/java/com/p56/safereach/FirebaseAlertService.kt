package com.p56.safereach

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.p56.safereach.proto.Alert
import com.p56.safereach.proto.SignedMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseAlertService : FirebaseMessagingService() {

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.data["alert"]?.let { encoded ->
            try {
                val msg = SignedMessage.parseFrom(encoded.hexToByteArray())
                val meta = MeshMeta(
                    receivedAt = System.currentTimeMillis(),
                    receivedFrom = null,
                    hopCount = 0,
                    data = msg
                )
                val alert = Alert.parseFrom(msg.data)
                AlertStateRepository.updateAlert(alert)
                AlertStateRepository.updateMessage(meta)
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