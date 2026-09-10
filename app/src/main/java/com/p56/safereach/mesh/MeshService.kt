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
import com.p56.safereach.Alert
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.Constants.APP_DISPLAY_NAME
import com.p56.safereach.MeshMeta
import com.p56.safereach.MeshPacket
import com.p56.safereach.R
import com.p56.safereach.SignedMessage
import com.p56.safereach.mesh.ble.BleOrchestrator
import com.p56.safereach.mesh.ble.advertisement.BleAdvActor
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleIntent
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.BleScanActor
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.verification.SigningManager
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.security.PublicKey

@OptIn(ExperimentalSerializationApi::class)
class MeshService : Service() {
    private lateinit var orchestrator: BleOrchestrator
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Inject
    lateinit var signingManager: SigningManager

    override fun onCreate() {
        super.onCreate()
        MeshStatusRepository.updateServiceRunning(true)
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val advActor = BleAdvActor(serviceScope, btManager)
        val scanActor = BleScanActor(serviceScope, btManager, this)
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
            AlertStateRepository.message.collect { msg ->
                if (msg != null) {
                    val newPacket = MeshPacket(
                        data = msg.data,
                        hopCount = msg.hopCount + 1
                    )
                    val payloadBytes: ByteArray = ProtoBuf.encodeToByteArray(newPacket)
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
                try{
                    val signedMsg = ProtoBuf.decodeFromByteArray<MeshPacket<SignedMessage>>(message.data)
                    if (!signingManager.verify(signedMsg.data))
                    {
                        Log.w(TAG, "Failed to verify incoming mesh packet")
                        continue
                    }

                    val alert = ProtoBuf.decodeFromByteArray<Alert>(signedMsg.data.data)
                    val meta = MeshMeta(
                        message.receivedAt,
                        message.receivedFrom,
                        signedMsg.hopCount,
                        signedMsg.data
                    )
                    Log.d(TAG, "Received mesh packet: $meta")
                    val current = AlertStateRepository.alert.value
                    if (current == null || current.timestamp < alert.timestamp) {
                        AlertStateRepository.updateAlert(alert)
                        AlertStateRepository.updateMessage(meta)
                        Log.d(TAG, "Updated alert state: $alert")
                        Log.d(TAG, "Updated message state: $meta")
                    }
                }
                catch (e: Exception){
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
