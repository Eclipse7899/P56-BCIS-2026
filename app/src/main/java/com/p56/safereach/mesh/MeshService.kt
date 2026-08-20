package com.p56.safereach.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.p56.safereach.Alert
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.Constants.APP_DISPLAY_NAME
import com.p56.safereach.MeshPacket
import com.p56.safereach.MeshMeta
import com.p56.safereach.R
import com.p56.safereach.mesh.ble.BleOrchestrator
import com.p56.safereach.mesh.ble.advertisement.BleAdvActor
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleIntent
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.BleScanActor
import com.p56.safereach.mesh.ble.scanning.BleScanResult
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class MeshService : Service() {
    private lateinit var orchestrator: BleOrchestrator
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val dataChannel = Channel<BleScanResult>(Channel.UNLIMITED)

    override fun onCreate() {
        super.onCreate()
        MeshStatusRepository.updateServiceRunning(true)
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val advActor = BleAdvActor(serviceScope, btManager)
        val scanActor = BleScanActor(serviceScope, btManager, onScan = {
            dataChannel.trySend(it)
        })
        orchestrator = BleOrchestrator(serviceScope, advActor, scanActor)
        observeAlertStateRepository()
        observeIncomingData()
        observeOrchestratorState()
        observeSubsystemStates(advActor, scanActor)
    }

    private fun observeSubsystemStates(advActor: BleAdvActor, scanActor: BleScanActor) {
        serviceScope.launch {
            advActor.state.collect {
                MeshStatusRepository.updateAdvertiserState(it)
            }
        }
        serviceScope.launch {
            scanActor.state.collect {
                MeshStatusRepository.updateScannerState(it)
            }
        }
    }

    private fun observeAlertStateRepository() {
        serviceScope.launch {
            AlertStateRepository.alert.collect { meshMeta ->
                if (meshMeta != null) {
                    val newPacket = meshMeta.packet.copy(hopCount = meshMeta.packet.hopCount + 1)
                    val payloadBytes: ByteArray = ProtoBuf.encodeToByteArray(newPacket)
                    orchestrator.send(BleIntent.Active(payloadBytes))
                }
                else {
                    orchestrator.send(BleIntent.Passive)
                }
            }
        }
    }

    private fun observeIncomingData() {
        serviceScope.launch {
            for (message in dataChannel) {
                val meshPacket = ProtoBuf.decodeFromByteArray<MeshPacket<Alert>>(message.data)
                val meta = MeshMeta(
                    message.receivedAt,
                    message.receivedFrom,
                    meshPacket
                )

                val current = AlertStateRepository.alert.value
                if (current == null){
                    AlertStateRepository.update(meta)
                }
                else if (current.packet.data.timestamp < meta.packet.data.timestamp) {
                    AlertStateRepository.update(meta)
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
                            "Failed to start mesh")
                    }

                    BleState.Off -> {
                        updateNotification(
                            "$APP_DISPLAY_NAME is stopped",
                            "Mesh stopped")
                    }

                    BleState.Passive -> {
                        updateNotification(
                            "$APP_DISPLAY_NAME is running",
                            "Scanning in the background")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
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
