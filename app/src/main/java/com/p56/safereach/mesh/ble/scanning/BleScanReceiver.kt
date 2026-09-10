package com.p56.safereach.mesh.ble.scanning

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.p56.safereach.Constants
import com.p56.safereach.mesh.BleResultQueue

private const val TAG = "BleScanReceiver"

class BleScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received scan results broadcast")
        val results: List<ScanResult>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(
                    BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                    ScanResult::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
            }
        for (result in results.orEmpty()) {
            when (val data =
                result.scanRecord?.serviceData?.get(ParcelUuid(Constants.BLE_SERVICE_UUID))) {
                null -> continue
                else -> {
                    val bleResult = BleScanResult(
                        receivedAt = System.currentTimeMillis(),
                        receivedFrom = result.device.address,
                        data = data
                    )
                    BleResultQueue.channel.trySend(bleResult)
                }
            }
        }
    }
}