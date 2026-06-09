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
        remoteMessage.data["alert"]
            ?.let { ProtoBuf.decodeFromHexString<Alert>(it) }
            ?.let { AlertStateRepository.update(it) }
    }
}