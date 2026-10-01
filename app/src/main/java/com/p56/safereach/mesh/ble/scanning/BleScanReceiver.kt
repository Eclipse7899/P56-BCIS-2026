package com.p56.safereach.mesh.ble.scanning

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.p56.safereach.Constants.SERVICE_UUIDS
import com.p56.safereach.mesh.BleScanResultQueue

private const val TAG = "BleScanReceiver"

class BleScanReceiver : BroadcastReceiver() {

    fun defrag(record: ScanRecord): ByteArray {
        val fragments = mutableListOf<ByteArray>()
        for (uuid in SERVICE_UUIDS) {
            Log.d(TAG, "Checking for service data with UUID: $uuid")
            val fragment = record.getServiceData(ParcelUuid(uuid))
            if (fragment != null) {
                Log.d(TAG, "Found service data for UUID $uuid: ${fragment.size}")
                fragments.add(fragment)
            }
        }
        val totalSize = fragments.sumOf { it.size }
        val data = ByteArray(totalSize)
        var offset = 0
        for (fragment in fragments) {
            fragment.copyInto(data, destinationOffset = offset)
            offset += fragment.size
        }
        return data
    }

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
            val record = result.scanRecord ?: continue
            val data = defrag(record)
            val bleResult = BleScanResult(
                receivedAt = System.currentTimeMillis(),
                receivedFrom = result.device.address,
                payload = data
            )
            Log.d(TAG, "Sending scan result to queue: $bleResult")
            BleScanResultQueue.channel.trySend(bleResult)
        }
    }
}