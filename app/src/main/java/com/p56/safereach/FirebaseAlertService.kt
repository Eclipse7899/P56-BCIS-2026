package com.p56.safereach

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.protobuf.ProtoBuf

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseAlertService : FirebaseMessagingService() {

    @OptIn(ExperimentalSerializationApi::class)
    override fun onMessageReceived(
        remoteMessage:
        RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.data["alert"]?.let { encoded ->
            try {
                AlertStateRepository.update(ProtoBuf.decodeFromHexString<Alert>(encoded))
            } catch (e: Exception) {
                android.util.Log.w("FirebaseAlertService", "Failed to decode incoming alert", e)
            }
        }
    }
}