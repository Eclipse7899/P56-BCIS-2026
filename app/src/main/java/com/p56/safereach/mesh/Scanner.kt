package com.p56.safereach.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class Scanner(
    bluetoothAdapter: BluetoothAdapter,
) {
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    private val settings = ScanSettings.Builder()
        .setLegacy(true)
        .setPhy(BluetoothDevice.PHY_LE_1M)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val filter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(Constants.BLE_SERVICE_UUID))
        .build()

    private val scanMutex = Mutex()

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    fun scan(durationSeconds: Long = 0L): Flow<ScanResult> {
        Log.d(TAG, "scan() called with durationSeconds=$durationSeconds")
        return startScan(durationSeconds)
    }

    private fun <T> callbackFlowWithScanLock(
        block: suspend ProducerScope<T>.(onClose: () -> Unit) -> Unit
    ): Flow<T> = callbackFlow {
        Log.d(TAG, "callbackFlowWithScanLock: Attempting to acquire scan lock")

        if (!scanMutex.tryLock()) {
            Log.e(
                TAG,
                "callbackFlowWithScanLock: Failed to acquire lock - scan already in progress"
            )
            close(IllegalStateException("Scan already in progress"))
            return@callbackFlow
        }

        Log.d(TAG, "callbackFlowWithScanLock: Lock acquired successfully")
        val unlock = {
            Log.d(TAG, "callbackFlowWithScanLock: Releasing scan lock")
            scanMutex.unlock()
        }

        try {
            Log.d(TAG, "callbackFlowWithScanLock: Starting block execution")
            block(unlock)
        } catch (t: Throwable) {
            Log.e(TAG, "callbackFlowWithScanLock: Exception in block", t)
            unlock()
            close(t)
        }
    }

    companion object {
        private const val TAG = "Scanner"
    }

    @Throws(IllegalStateException::class)
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    private fun startScan(durationSeconds: Long = 0L): Flow<ScanResult> =
        callbackFlowWithScanLock { onClose ->
            if (bluetoothLeScanner == null) {
                Log.e(TAG, "startScan: BLE scanner is null - unavailable")
                onClose()
                close(Throwable("BLE scanner unavailable"))
                return@callbackFlowWithScanLock
            }

            Log.i(
                TAG,
                "startScan: Starting BLE scan for ${if (durationSeconds > 0) "$durationSeconds seconds" else "indefinitely"}"
            )

            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    Log.d(
                        TAG,
                        "scanCallback.onScanResult: Received scan result - device: ${result.device.address}, RSSI: ${result.rssi}, callbackType: $callbackType"
                    )
                    if (!trySend(result).isSuccess) {
                        Log.w(
                            TAG,
                            "scanCallback.onScanResult: Failed to send result - flow may be closed"
                        )
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "scanCallback.onScanFailed: Scan failed with error code: $errorCode")
                    close(Throwable("Scan failed with error: $errorCode"))
                }
            }

            Log.d(TAG, "startScan: Starting BLE scan with filter and settings")
            try {
                bluetoothLeScanner.startScan(
                    listOf(filter),
                    settings,
                    scanCallback
                )
                Log.d(TAG, "startScan: BLE scan started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "startScan: Exception when starting scan", e)
                onClose()
                close(e)
                return@callbackFlowWithScanLock
            }

            val timeoutJob = if (durationSeconds > 0) {
                Log.d(TAG, "startScan: Setting up timeout for $durationSeconds seconds")
                launch {
                    delay(durationSeconds * 1000)
                    Log.i(
                        TAG,
                        "startScan: Timeout reached - stopping BLE scan"
                    )
                    bluetoothLeScanner.stopScan(scanCallback)
                    close()
                }
            } else {
                Log.d(TAG, "startScan: Scan duration is indefinite (0 seconds)")
                null
            }

            awaitClose {
                Log.d(TAG, "awaitClose: Cleaning up scan")
                Log.d(TAG, "awaitClose: Cancelling timeout job if it exists")
                timeoutJob?.cancel()
                Log.d(TAG, "awaitClose: Stopping BLE scan callback")
                try {
                    bluetoothLeScanner.stopScan(scanCallback)
                    Log.d(TAG, "awaitClose: BLE scan stopped successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "awaitClose: Exception when stopping scan", e)
                }
                Log.d(TAG, "awaitClose: Calling onClose release handler")
                onClose()
                Log.d(TAG, "awaitClose: Scan cleanup completed")
            }
        }
}
