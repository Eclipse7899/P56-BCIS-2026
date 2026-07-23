package com.p56.safereach.mesh

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants

class GattServer(
    private val context: Context,
    private val btManager: BluetoothManager
) {

    private var server: BluetoothGattServer? = null
    private var currentData: ByteArray = byteArrayOf()

    private val serverCallback = object : BluetoothGattServerCallback() {
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicReadRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid != Constants.BLE_CHARACTERISTIC_UUID) {
                server?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    offset,
                    null
                )
                return
            }
            val value =
                if (offset == 0) currentData else currentData.copyOfRange(offset, currentData.size)
            server?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                value
            )
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun start(data: ByteArray) {
        currentData = data
        server?.close()

        val char = BluetoothGattCharacteristic(
            Constants.BLE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val service = BluetoothGattService(
            Constants.BLE_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ).apply {
            addCharacteristic(char)
        }

        server = btManager.openGattServer(context, serverCallback).apply {
            addService(service)
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun stop() {
        server?.close()
        server = null
    }
}
