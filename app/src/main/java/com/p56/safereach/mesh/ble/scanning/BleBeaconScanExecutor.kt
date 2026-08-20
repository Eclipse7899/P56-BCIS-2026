package com.p56.safereach.mesh.ble.scanning

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanEvent

private const val TAG = "BleBeaconScanExecutor"

class BleBeaconScanExecutor(
    val btManager: BluetoothManager,
    val onEvent: (BleScanEvent) -> Unit
) {
    private val mask = ByteArray(1650)

    private val settings = ScanSettings.Builder()
        .setLegacy(false)
        .setPhy(BluetoothDevice.PHY_LE_CODED)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    private val filter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(Constants.BLE_SERVICE_UUID))
        .build()

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun scan(): ScanCallback {
        val bluetoothLeScanner = btManager.adapter.bluetoothLeScanner
            ?: throw IllegalArgumentException("Bluetooth LE scanner is not available. Ensure Bluetooth is enabled.")
        Log.d(TAG, "Starting scan")
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.v(TAG, "Scan result received from ${result.device.address}")
                result.scanRecord
                    ?.serviceData?.get(
                        ParcelUuid(Constants.BLE_SERVICE_UUID)
                    )
                    ?.let {
                        Log.v(TAG, "Relevant service data found for ${result.device.address}")
                        onEvent(
                            BleScanEvent.Scanned(
                                BleScanResult(
                                    System.currentTimeMillis(),
                                    result.device.address,
                                    it
                                )
                            )
                        )
                    }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error code: $errorCode")
                onEvent(BleScanEvent.Failed)
            }
        }
        bluetoothLeScanner.startScan(
            listOf(filter),
            settings,
            scanCallback
        )
        Log.d(TAG, "Scan started successfully")
        return scanCallback
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stop(callback: ScanCallback) {
        Log.d(TAG, "Stopping scan")
        val bluetoothLeScanner = btManager.adapter.bluetoothLeScanner
            ?: throw IllegalArgumentException("Bluetooth LE scanner is not available. Ensure Bluetooth is enabled.").also {
                Log.e(
                    TAG,
                    "Bluetooth LE scanner is not available during stop"
                )
            }
        bluetoothLeScanner.stopScan(callback)
        Log.d(TAG, "Scan stopped successfully")
    }
}