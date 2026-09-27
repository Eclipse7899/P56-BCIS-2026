package com.p56.safereach.service

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.p56.safereach.AlertContext
import com.p56.safereach.AlertMeta
import com.p56.safereach.proto.Alert
import com.p56.safereach.proto.SignedMessage
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
@AndroidEntryPoint
class FirebaseAlertService : FirebaseMessagingService() {

    @Inject
    lateinit var alertService: AlertService

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.data["alert"]?.let { encoded ->
            try {
                val signedMessage = SignedMessage.parseFrom(encoded.hexToByteArray())
                val alert = Alert.parseFrom(signedMessage.data)
                val ctx = AlertContext(
                    alert = alert,
                    signedMessage = signedMessage,
                    meta = AlertMeta(System.currentTimeMillis(), null)
                )
                alertService.add(ctx)
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
