package com.p56.safereach.mesh.ble.scanning

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
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

        if (intent.hasExtra(BluetoothLeScanner.EXTRA_ERROR_CODE)) {
            val errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1)
            Log.e(TAG, "Background scan failed with error code: $errorCode")

            if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
                Log.d(TAG, "Scan already started, ignoring error")
                return
            }

            val fail = when (errorCode) {
                ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> BleScanFail.ScanningTooFrequently
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> BleScanFail.ApplicationRegistrationFailed
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> BleScanFail.FeatureUnsupported
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> BleScanFail.InternalError
                ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> BleScanFail.OutOfHardwareResources
                else -> BleScanFail.Unknown(errorCode)
            }
            Log.e(TAG, "Reporting scan failure to controller: $fail")
            BleScanErrorQueue.channel.trySend(fail)
            return
        }

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