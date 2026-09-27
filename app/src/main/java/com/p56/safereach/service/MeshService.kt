package com.p56.safereach.service

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
import com.p56.safereach.AlertContext
import com.p56.safereach.AlertMeta
import com.p56.safereach.Constants.APP_DISPLAY_NAME
import com.p56.safereach.R
import com.p56.safereach.mesh.BleResultQueue
import com.p56.safereach.mesh.BleStateReceiver
import com.p56.safereach.mesh.MeshStatusRepository
import com.p56.safereach.mesh.ble.BleOrchestrator
import com.p56.safereach.mesh.ble.advertisement.BleAdvController
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleIntent
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.BleScanController
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.proto.Alert
import com.p56.safereach.proto.MeshPacket
import com.p56.safereach.verification.SigningManager
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MeshService : Service() {
    private lateinit var orchestrator: BleOrchestrator
    private lateinit var advertiserController: BleAdvController
    private lateinit var scannerController: BleScanController
    private var bleStateReceiver: BleStateReceiver? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var signingManager: SigningManager

    @Inject
    lateinit var alertService: AlertService

    @Inject
    lateinit var meshStatusRepository: MeshStatusRepository

    override fun onCreate() {
        super.onCreate()
        meshStatusRepository.updateServiceRunning(true)
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        advertiserController = BleAdvController(serviceScope, btManager)
        scannerController = BleScanController(serviceScope, btManager, this)
        orchestrator = BleOrchestrator(serviceScope, advertiserController, scannerController)
        observeBluetoothState(btManager, advertiserController, scannerController)
        observeAlertStateRepository()
        observeIncomingData()
        observeOrchestratorState()
        observeSubsystemStates(advertiserController, scannerController)
    }

    private fun observeBluetoothState(
        btManager: BluetoothManager,
        advertiserController: BleAdvController,
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
        advertiserController: BleAdvController,
        scannerController: BleScanController
    ) {
        serviceScope.launch {
            advertiserController.state.collect {
                meshStatusRepository.updateAdvertiserState(it)
            }
        }
        serviceScope.launch {
            scannerController.state.collect {
                meshStatusRepository.updateScannerState(it)
            }
        }
    }

    private fun observeAlertStateRepository() {
        serviceScope.launch {
            alertService.broadcastableMessages.stateIn(serviceScope).collect { messages ->
                Log.d(TAG, "messages updated: ${messages.size}")
                if (messages.isEmpty()){
                    Log.d(TAG, "No alerts to send")
                    orchestrator.send(BleIntent.Passive)
                }
                else {
                    Log.d(TAG, "Sending ${messages.size} alerts")
                    val packet = MeshPacket.newBuilder()
                        .addAllMsg(messages)
                        .build()
                    orchestrator.send(BleIntent.Active(packet.toByteArray()))
                }
            }
        }
    }

    private fun observeIncomingData() {
        serviceScope.launch {
            for (message in BleResultQueue.channel) {
                try {
                    val packet = MeshPacket.parseFrom(message.data)
                    for (signedMessage in packet.msgList) {
                        val alert = Alert.parseFrom(signedMessage.data)
                        val ctx = AlertContext(
                            alert = alert,
                            signedMessage = signedMessage,
                            meta = AlertMeta(
                                receivedAt = message.receivedAt,
                                receivedFrom = message.receivedFrom
                            )
                        )
                        Log.d(TAG, "Received alert: $alert")
                        alertService.add(ctx)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode incoming mesh packet", e)
                    continue
                }
            }
        }
    }

    private fun observeOrchestratorState() {
        serviceScope.launch {
            orchestrator.state.collect {
                meshStatusRepository.updateOrchestratorState(it)
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
        orchestrator.send(BleIntent.Passive)
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        meshStatusRepository.updateServiceRunning(false)
        meshStatusRepository.updateOrchestratorState(BleState.Off)
        meshStatusRepository.updateAdvertiserState(BleAdvState.Idle)
        meshStatusRepository.updateScannerState(BleScanState.Idle)
        orchestrator.send(BleIntent.Off)
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
