package com.p56.safereach.mesh.ble.scanning

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants

private const val TAG = "BleBeaconScanAdapter"

private const val REQUEST_CODE_BLE_SCAN = 100

class BleBeaconScanAdapter(
    val btManager: BluetoothManager,
    val context: Context
) {
    private val settings = ScanSettings.Builder()
        .setLegacy(false)
        .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setReportDelay(0)
        .build()

    private val filter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(Constants.BLE_SERVICE_UUID_START))
        .build()

    fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, BleScanReceiver::class.java).apply {
            action = Constants.BLE_SCAN_INTENT_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BLE_SCAN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return pendingIntent
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun scan() {
        val bluetoothLeScanner = btManager.adapter.bluetoothLeScanner
            ?: throw IllegalArgumentException("Bluetooth LE scanner is not available. Ensure Bluetooth is enabled.")
        Log.d(TAG, "Starting scan")
        val intent = buildPendingIntent()
        bluetoothLeScanner.startScan(
            listOf(filter),
            settings,
            intent
        )
        Log.d(TAG, "Scan started successfully")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stop() {
        Log.d(TAG, "Stopping scan")
        val bluetoothLeScanner = btManager.adapter.bluetoothLeScanner
            ?: throw IllegalArgumentException("Bluetooth LE scanner is not available. Ensure Bluetooth is enabled.").also {
                Log.e(
                    TAG,
                    "Bluetooth LE scanner is not available during stop"
                )
            }
        val intent = buildPendingIntent()
        bluetoothLeScanner.stopScan(intent)
        Log.d(TAG, "Scan stopped successfully")
    }
}