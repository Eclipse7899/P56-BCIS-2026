package com.p56.safereach.mesh

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
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.Constants.APP_DISPLAY_NAME
import com.p56.safereach.MeshMeta
import com.p56.safereach.R
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MeshService : Service() {
    private lateinit var orchestrator: BleOrchestrator
    private var bleStateReceiver: BleStateReceiver? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var signingManager: SigningManager

    override fun onCreate() {
        super.onCreate()
        MeshStatusRepository.updateServiceRunning(true)
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val advertiserController = BleAdvController(serviceScope, btManager)
        val scannerController = BleScanController(serviceScope, btManager, this)
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
                MeshStatusRepository.updateAdvertiserState(it)
            }
        }
        serviceScope.launch {
            scannerController.state.collect {
                MeshStatusRepository.updateScannerState(it)
            }
        }
    }

    private fun observeAlertStateRepository() {
        serviceScope.launch {
            AlertStateRepository.message.collect { message ->
                if (message != null) {
                    val packet = MeshPacket.newBuilder()
                        .setHopCount(message.hopCount + 1)
                        .setMsg(message.data)
                        .build()
                    val payloadBytes = packet.toByteArray()
                    orchestrator.send(BleIntent.Active(payloadBytes))
                } else {
                    orchestrator.send(BleIntent.Passive)
                }
            }
        }
    }

    private fun observeIncomingData() {
        serviceScope.launch {
            for (message in BleResultQueue.channel) {
                try {
                    val packet = MeshPacket.parseFrom(message.data)
                    if (!signingManager.verify(packet.msg)) {
                        Log.w(TAG, "Failed to verify incoming mesh packet")
                        continue
                    }

                    val alert = Alert.parseFrom(packet.msg.data)
                    val meta = MeshMeta(
                        message.receivedAt,
                        message.receivedFrom,
                        packet.hopCount,
                        packet.msg
                    )
                    Log.d(TAG, "Received mesh packet: $meta")
                    if (AlertStateRepository.updateAlert(alert)) {
                        AlertStateRepository.updateMessage(meta)
                        Log.d(TAG, "Updated alert state: $alert")
                        Log.d(TAG, "Updated message state: $meta")
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
                MeshStatusRepository.updateOrchestratorState(it)
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
        MeshStatusRepository.updateServiceRunning(false)
        MeshStatusRepository.updateOrchestratorState(BleState.Off)
        MeshStatusRepository.updateAdvertiserState(BleAdvState.Idle)
        MeshStatusRepository.updateScannerState(BleScanState.Idle)
        orchestrator.send(BleIntent.Off)
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
