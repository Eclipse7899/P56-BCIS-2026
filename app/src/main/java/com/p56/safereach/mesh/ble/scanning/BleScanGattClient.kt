package com.p56.safereach.mesh.ble.scanning

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants

private const val TAG = "GattClient"

/**
 * A GATT client used to connect to a BLE device and retrieve service data.
 */
class BleScanGattClient(private val context: Context) {
    /**
     * Connects to a GATT server on the given device, discovers services, and reads a specific characteristic.
     *
     * @param device The Bluetooth device to connect to.
     * @param callback Callback invoked with the retrieved data if successful.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectAndRetrieveData(device: BluetoothDevice, callback: (ByteArray) -> Unit) {
        Log.d(TAG, "Connecting to GATT on device ${device.address}")
        device.connectGatt(
            context,
            false,
            object : BluetoothGattCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    if (status != BluetoothGatt.GATT_SUCCESS || newState != BluetoothGatt.STATE_CONNECTED) {
                        Log.w(
                            TAG,
                            "GATT connection state changed for ${device.address}: status=$status, newState=$newState. Closing GATT."
                        )
                        gatt.close()
                        return
                    }
                    Log.d(TAG, "GATT connected to ${device.address}. Discovering services...")
                    gatt.discoverServices()
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int
                ) {
                    try {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.d(
                                TAG,
                                "GATT characteristic read successfully from ${device.address}"
                            )
                            callback(value)
                        } else {
                            Log.e(
                                TAG,
                                "GATT characteristic read failed for ${device.address} with status: $status"
                            )
                        }
                    } finally {
                        gatt.close()
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.e(
                            TAG,
                            "GATT services discovery failed for ${device.address} with status: $status"
                        )
                        gatt.close()
                        return
                    }
                    Log.d(
                        TAG,
                        "GATT services discovered for ${device.address}. Reading characteristic..."
                    )
                    gatt.getService(Constants.BLE_SERVICE_UUID)
                        ?.getCharacteristic(Constants.BLE_CHARACTERISTIC_UUID)
                        ?.let { gatt.readCharacteristic(it) }
                        ?: run {
                            Log.e(
                                TAG,
                                "GATT service/characteristic not found for ${device.address}"
                            )
                            gatt.close()
                        }
                }
            }
        )
    }
}