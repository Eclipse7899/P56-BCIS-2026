package com.p56.safereach.mesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.p56.safereach.Alert
import com.p56.safereach.parseReceivedData
import com.p56.safereach.prepareForTransmission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val SCAN_RETRY_MS = 2000L

class BleCoordinator(
    context: Context,
    btManager: BluetoothManager,
    adapter: BluetoothAdapter,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val advertiser = Advertiser(adapter)
    private val scanner = Scanner(adapter)
    private val gattServer = GattServer(context, btManager)
    private val gattClient = GattClient(context)
    private val stateMutex = Mutex()

    private var pendingPayload: ByteArray? = null
    private var isAdvertising = false
    private var scanJob: Job? = null

    private var lastAdvertisedPayload: ByteArray? = null

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ]
    )
    fun start(onUpdate: (Alert) -> Unit) {
        Log.i(TAG, "Coordinator start with payload length=${pendingPayload?.size ?: 0}")
        gattServer.start(pendingPayload ?: byteArrayOf())
        startScanLoop(onUpdate)
        updateAdvertisingAsync()
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ]
    )
    fun stop() {
        Log.i(TAG, "Coordinator stop")
        scanJob?.cancel()
        advertiser.stopAdvertising()
        gattServer.stop()
        scope.cancel()
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        ]
    )
    fun updateAlert(message: Alert) {
        val payload = prepareForTransmission(message)
        Log.i(TAG, "Update message length=${payload.size}")
        scope.launch {
            stateMutex.withLock {
                pendingPayload = payload
            }
            gattServer.start(payload)
            updateAdvertisingAsync()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun updateAdvertisingAsync() {
        Log.i(TAG, "Advertising update requested")
        scope.launch {
            updateAdvertising()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private suspend fun updateAdvertising() {
        stateMutex.withLock {
            val payload = pendingPayload
            if (payload == null) {
                if (isAdvertising) {
                    Log.i(TAG, "Stop advertising (no payload)")
                    advertiser.stopAdvertising()
                    isAdvertising = false
                    lastAdvertisedPayload = null
                } else {
                    Log.i(TAG, "Skip advertising (no payload)")
                }
                return
            }
            if (!isAdvertising) {
                Log.i(TAG, "Start advertising payloadSize=${payload.size}")
                startAdvertisingLocked(payload)
            } else if (lastAdvertisedPayload?.contentEquals(payload) == true) {
                Log.i(TAG, "Advertising already up-to-date")
            } else {
                Log.i(TAG, "Restart advertising payloadSize=${payload.size}")
                advertiser.stopAdvertising()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startAdvertisingLocked(payload: ByteArray) {
        isAdvertising = true
        lastAdvertisedPayload = payload.copyOf()
        advertiser.startAdvertising {
            Log.i(TAG, "Advertising ended")
            scope.launch { onAdvertisingEnded() }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private suspend fun onAdvertisingEnded() {
        stateMutex.withLock {
            isAdvertising = false
            val payload = pendingPayload
            if (payload != null && lastAdvertisedPayload?.contentEquals(payload) != true) {
                Log.i(TAG, "Advertising ended; restarting with payloadSize=${payload.size}")
                startAdvertisingLocked(payload)
            } else if (payload != null) {
                Log.i(TAG, "Advertising ended; payload unchanged")
            } else {
                Log.i(TAG, "Advertising ended; no payload to restart")
                lastAdvertisedPayload = null
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    private fun startScanLoop(onUpdate: (Alert) -> Unit) {
        scanJob?.cancel()
        scanJob = scope.launch {
            Log.i(TAG, "Scan loop started")
            while (isActive) {
                try {
                    scanner.scan().collect { result ->
                        Log.i(TAG, "Scan result device=${result.device.address}")
                        gattClient.connectAndRetrieveData(result.device) {
                            Log.i(TAG, "Gatt data received bytes=${it.size}")
                            onUpdate(parseReceivedData<Alert>(it))
                        }
                    }
                } catch (t: Throwable) {
                    Log.i(TAG, "Scan loop error; retrying", t)
                    delay(SCAN_RETRY_MS)
                }
            }
            Log.i(TAG, "Scan loop stopped")
        }
    }

    companion object {
        private const val TAG = "BleCoordinator"
    }
}
