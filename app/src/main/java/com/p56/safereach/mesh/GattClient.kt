package com.p56.safereach.mesh

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants

class GattClient(private val context: Context) {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectAndRetrieveData(device: BluetoothDevice, callback: (ByteArray) -> Unit) {
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
                        gatt.close()
                        return
                    }
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
                            callback(value)
                        }
                    } finally {
                        gatt.close()
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        gatt.close()
                        return
                    }
                    gatt.getService(Constants.BLE_SERVICE_UUID)
                        ?.getCharacteristic(Constants.BLE_CHARACTERISTIC_UUID)
                        ?.let { gatt.readCharacteristic(it) }
                        ?: gatt.close()
                }
            }
        )
    }
}
