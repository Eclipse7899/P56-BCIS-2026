package com.p56.safereach.mesh

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.p56.safereach.Alert
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.protobuf.ProtoBuf

class BleService : Service() {
    private val binder = LocalBinder()
    private var coordinator: BleCoordinator? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        if (!hasBlePermissions()) {
            Log.w(TAG, "Service started without required permissions; stopping")
            stopSelf()
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = btManager.adapter
        if (adapter == null) {
            stopSelf()
            return
        }
        coordinator = BleCoordinator(this, btManager, adapter)
        coordinator?.start { alert ->
            AlertStateRepository.update(alert)
        }

        serviceScope.launch {
            AlertStateRepository.message.collect { alert ->
                if (alert != null) {
                    if (hasBlePermissions()) {
                        coordinator?.updateAlert(alert)
                    } else {
                        Log.w(TAG, "Skipping updateAlert from repository; permissions were revoked")
                    }
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT])
    @OptIn(ExperimentalSerializationApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_MESSAGE) {
            val alert = intent.getStringExtra(EXTRA_DATA)
                ?.let { ProtoBuf.decodeFromHexString<Alert>(it) }
            if (alert == null) return START_STICKY

            if (!hasBlePermissions()) {
                Log.w(TAG, "Ignoring ACTION_UPDATE_MESSAGE; missing runtime permissions")
                return START_STICKY
            }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Missing Bluetooth permissions for update; ignoring")
                return START_STICKY
            }

            coordinator?.updateAlert(alert)
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        serviceScope.cancel()
        if (hasBlePermissions()) {
            coordinator?.stop()
        } else {
            Log.w(TAG, "Skipping coordinator.stop(); permissions were revoked")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        @SuppressLint("MissingPermission")
        fun updateMessage(alert: Alert) {
            if (hasBlePermissions()) {
                coordinator?.updateAlert(alert)
            } else {
                Log.w(TAG, "Ignoring bound updateMessage call; missing runtime permissions")
            }
        }
    }

    private fun hasBlePermissions(): Boolean =
        BlePermissions.missingRuntimePermissions(this).isEmpty()

    private fun buildNotification(): Notification {
        ensureNotificationChannel()
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BLE active")
            .setContentText("Scanning and advertising in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "BLE",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "BleService"
        const val ACTION_UPDATE_MESSAGE = "com.example.p56_demo.mesh.ACTION_UPDATE_MESSAGE"
        const val EXTRA_DATA = "extra_message"
        private const val NOTIFICATION_CHANNEL_ID = "ble_service"
        private const val NOTIFICATION_ID = 1001
    }
}
