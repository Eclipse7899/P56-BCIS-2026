package com.p56.safereach.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.p56.safereach.Constants.APP_DISPLAY_NAME
import com.p56.safereach.R
import com.p56.safereach.mesh.BleScanResultQueue
import com.p56.safereach.mesh.BleStateReceiver
import com.p56.safereach.mesh.ble.BleOrchestrator
import com.p56.safereach.mesh.ble.advertisement.BleAdvertisingController
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState
import com.p56.safereach.mesh.ble.fsm.BleCommand
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.BleScanController
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.model.toDto
import com.p56.safereach.service.AlertRepository
import com.p56.safereach.service.MeshStatusStore
import com.p56.safereach.util.msgFromMesh
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MeshForegroundService : Service() {
    private lateinit var orchestrator: BleOrchestrator
    private lateinit var advertiserController: BleAdvertisingController
    private lateinit var scannerController: BleScanController
    private var bleStateReceiver: BleStateReceiver? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var alertRepository: AlertRepository

    @Inject
    lateinit var meshStatusStore: MeshStatusStore

    override fun onCreate() {
        super.onCreate()
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

        meshStatusStore.updateServiceRunning(true)
        advertiserController = BleAdvertisingController(serviceScope, btManager)
        scannerController = BleScanController(serviceScope, btManager, this)
        orchestrator = BleOrchestrator(serviceScope, advertiserController, scannerController)
        observeBluetoothState(btManager, advertiserController, scannerController)
        observeMessageState()
        observeIncomingData()
        observeOrchestratorState()
        observeSubsystemStates(advertiserController, scannerController)
    }

    private fun observeBluetoothState(
        btManager: BluetoothManager,
        advertiserController: BleAdvertisingController,
        scannerController: BleScanController
    ) {
        val receiver = BleStateReceiver { enabled ->
            advertiserController.onBluetoothStateChanged(enabled)
            scannerController.onBluetoothStateChanged(enabled)
        }

        val isBluetoothEnabled = btManager.adapter?.isEnabled == true
        advertiserController.onBluetoothStateChanged(isBluetoothEnabled)
        scannerController.onBluetoothStateChanged(isBluetoothEnabled)

        ContextCompat.registerReceiver(
            this,
            receiver,
            BleStateReceiver.filter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        bleStateReceiver = receiver
    }

    private fun observeSubsystemStates(
        advertiserController: BleAdvertisingController,
        scannerController: BleScanController
    ) {
        serviceScope.launch {
            advertiserController.state.collect {
                meshStatusStore.updateAdvertiserState(it)
            }
        }
        serviceScope.launch {
            scannerController.state.collect {
                meshStatusStore.updateScannerState(it)
            }
        }
    }

    private fun observeMessageState() {
        serviceScope.launch {
            alertRepository.receivedMessage.collect { wrapper ->
                if (wrapper == null) {
                    Log.d(TAG, "No alerts to send")
                    orchestrator.send(BleCommand.Passive)
                    return@collect
                }
                Log.d(TAG, "Sending alert intent")
                orchestrator.send(BleCommand.Active(wrapper.signedMessage.toDto().toByteArray()))
            }
        }
    }

    private fun observeIncomingData() {
        serviceScope.launch {
            for (packet in BleScanResultQueue.channel) {
                Log.d(TAG, "Received packet: $packet")
                msgFromMesh(packet.payload, packet.receivedFrom, packet.receivedAt)
                    .onFailure {
                        Log.w(TAG, "Failed to decode incoming mesh packet", it)
                        continue
                    }.onSuccess { signedMessage ->
                        Log.d(TAG, "Decoded incoming mesh packet: $signedMessage")
                        alertRepository.addSigned(signedMessage)
                    }
            }
        }
    }

    private fun observeOrchestratorState() {
        serviceScope.launch {
            orchestrator.state.collect {
                meshStatusStore.updateOrchestratorState(it)
                when (it) {
                    is BleState.Active -> {
                        updateNotification(
                            "$APP_DISPLAY_NAME is running",
                            "Scanning and advertising in the background"
                        )
                    }

                    BleState.Failed -> {
                        updateNotification(
                            "$APP_DISPLAY_NAME is stopped",
                            "Failed to start mesh"
                        )
                    }

                    BleState.Off -> {
                        updateNotification(
                            "$APP_DISPLAY_NAME is stopped",
                            "Mesh stopped"
                        )
                    }

                    BleState.Passive -> {
                        updateNotification(
                            "$APP_DISPLAY_NAME is running",
                            "Scanning in the background"
                        )
                    }

                    BleState.Disabled -> {
                        updateNotification(
                            "$APP_DISPLAY_NAME is stopped",
                            "Bluetooth has been disabled. Mesh stopped"
                        )
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        orchestrator.send(BleCommand.Passive)
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        meshStatusStore.updateServiceRunning(false)
        meshStatusStore.updateOrchestratorState(BleState.Off)
        meshStatusStore.updateAdvertiserState(BleAdvertisingState.Idle)
        meshStatusStore.updateScannerState(BleScanState.Idle)
        orchestrator.send(BleCommand.Off)
        bleStateReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "BLE state receiver was already unregistered", e)
            }
        }
        bleStateReceiver = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        ensureNotificationChannel()
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BLE starting")
            .setContentText("Initializing BLE services")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = buildNotification().apply {
            this.extras.putString(Notification.EXTRA_TITLE, title)
            this.extras.putString(Notification.EXTRA_TEXT, text)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
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
        private const val NOTIFICATION_CHANNEL_ID = "ble_service"
        private const val NOTIFICATION_ID = 1001
    }
}
