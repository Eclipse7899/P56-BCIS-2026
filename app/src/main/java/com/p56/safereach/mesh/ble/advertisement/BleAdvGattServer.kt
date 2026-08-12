package com.p56.safereach.mesh.ble.advertisement

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants

/**
 * A GATT server that provides a read-only characteristic for BLE devices to retrieve data.
 *
 * @property context The application context.
 * @property btManager The Bluetooth manager used to open the GATT server.
 */
class BleAdvGattServer(
    private val context: Context,
    private val btManager: BluetoothManager
) {

    private var server: BluetoothGattServer? = null
    private var currentData: ByteArray = byteArrayOf()

    private val serverCallback = object : BluetoothGattServerCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
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

    /**
     * Starts the GATT server with the provided data.
     *
     * @param data The byte array to be served.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

    /**
     * Stops the GATT server and releases resources.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun stop() {
        server?.close()
        server = null
    }
}