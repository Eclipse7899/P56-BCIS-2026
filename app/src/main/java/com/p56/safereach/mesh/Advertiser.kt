package com.p56.safereach.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants
import kotlinx.coroutines.sync.Mutex


class Advertiser(
    private val adapter: BluetoothAdapter,
) {
    private val advertisingMutex = Mutex()

    private var currentCallback: AdvertisingSetCallback? = null

    @Throws(IllegalStateException::class)
    private fun <T> advertisingLock(block: () -> T): T {
        val acquired = advertisingMutex.tryLock()

        if (!acquired) {
            Log.e(TAG, "advertisingLock: Failed to acquire lock - advertising already in progress")
            throw IllegalStateException("Advertising already in progress")
        }

        try {
            return block()
        } catch (t: Throwable) {
            throw t
        } finally {
            advertisingMutex.unlock()
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startAdvertising(endedCallback: () -> Unit) = advertisingLock {
        val advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.i(TAG, "BLE advertiser unavailable")
            return@advertisingLock
        }

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setConnectable(true)
            .setScannable(true)
            .build()

        val advertisingData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(Constants.BLE_SERVICE_UUID))
            .build()

        val callback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                Log.i(TAG, "Advertising stopped")
                currentCallback = null
                endedCallback()
            }

            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet?,
                txPower: Int,
                status: Int
            ) {
                if (status != ADVERTISE_SUCCESS) {
                    Log.i(
                        TAG,
                        "Advertising failed to start with status $status and txPower $txPower"
                    )
                    currentCallback = null
                    endedCallback()
                } else {
                    Log.i(TAG, "Advertising set started successfully with txPower $txPower")
                }
            }
        }

        currentCallback = callback
        advertiser.startAdvertisingSet(
            parameters,
            advertisingData,
            null,
            null,
            null,
            callback
        )
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        val callback = currentCallback ?: return
        Log.i(TAG, "Stop advertising requested")
        adapter.getBluetoothLeAdvertiser()?.stopAdvertisingSet(callback)
    }

    companion object {
        private const val TAG = "Advertiser"
    }
}
