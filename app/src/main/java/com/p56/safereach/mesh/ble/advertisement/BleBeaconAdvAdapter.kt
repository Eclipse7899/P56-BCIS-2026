package com.p56.safereach.mesh.ble.advertisement

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED
import android.bluetooth.le.AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE
import android.bluetooth.le.AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
import android.bluetooth.le.AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR
import android.bluetooth.le.AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.AdvertisingSetParameters.INTERVAL_HIGH
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants
import com.p56.safereach.Constants.MAX_DATA_PAYLOAD
import com.p56.safereach.Constants.SERVICE_UUID_MAP
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.Continuation
import kotlin.time.Duration.Companion.milliseconds


private sealed interface OperationStatus {
    data class Starting(val continuation: Continuation<AdvertisingSet>) : OperationStatus
    data class Stopping(val continuation: Continuation<Unit>) : OperationStatus
    data class Updating(val continuation: Continuation<Unit>) : OperationStatus
    data object Idle : OperationStatus
}

private const val TAG = "BleBeaconAdvAdapter"

class BleBeaconAdvAdapter(
    private val onEvent: (BleAdvEvent) -> Unit,
    private val btManager: BluetoothManager
) {

    private val params = AdvertisingSetParameters.Builder()
        .setScannable(false)
        .setConnectable(false)
        .setLegacyMode(false)
        .setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
        .setSecondaryPhy(BluetoothDevice.PHY_LE_CODED)
        .setInterval(INTERVAL_HIGH)
        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
        .build()

    fun fragmentData(payload: ByteArray): List<ByteArray> {
        val fragments = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < payload.size) {
            val end = (offset + MAX_DATA_PAYLOAD).coerceAtMost(payload.size)
            val fragment = payload.copyOfRange(offset, end)
            fragments.add(fragment)
            offset += MAX_DATA_PAYLOAD
        }
        Log.d(TAG, "fragmentData: created ${fragments.size} fragments")
        return fragments
    }

    fun buildAdvertiseData(payload: ByteArray): AdvertiseData {
        val fragments = fragmentData(payload)
        val builder = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(Constants.BLE_SERVICE_UUID_START))
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
        for (i in fragments.indices) {
            if (SERVICE_UUID_MAP.size > i) {
                builder.addServiceData(ParcelUuid(SERVICE_UUID_MAP[i]), fragments[i])
            }
        }
        return builder.build()
    }

    private var operationStatus: OperationStatus = OperationStatus.Idle

    private val mutex = Mutex()

    private val callback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet?,
            txPower: Int,
            status: Int
        ) {
            Log.d(TAG, "onAdvertisingSetStarted: status=$status")
            when (status) {
                ADVERTISE_SUCCESS -> {
                    Log.d(TAG, "Advertising set started successfully")
                    when (val opStatus = operationStatus) {
                        is OperationStatus.Starting -> {
                            opStatus.continuation.resumeWith(Result.success(advertisingSet!!))
                        }

                        else -> {
                            Log.w(
                                TAG,
                                "Received started callback but not in Starting state: $opStatus"
                            )
                        }
                    }
                }

                else -> {
                    val failure = mapStatus(status)
                    Log.e(TAG, "Advertising set failed to start: $failure")
                    when (val operation = operationStatus) {
                        is OperationStatus.Starting -> {
                            operation.continuation.resumeWith(
                                Result.failure(
                                    BleAdvException(
                                        failure
                                    )
                                )
                            )
                        }

                        else -> {
                            Log.w(TAG, "Reporting failure to controller: $failure")
                            onEvent(BleAdvEvent.Failed(failure))
                        }
                    }
                }
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            Log.d(TAG, "onAdvertisingSetStopped")
            when (val operation = operationStatus) {
                is OperationStatus.Stopping -> {
                    Log.d(TAG, "Advertising set stopped successfully (requested)")
                    operation.continuation.resumeWith(Result.success(Unit))
                }

                else -> {
                    Log.d(TAG, "Advertising set stopped (external or redundant)")
                    onEvent(BleAdvEvent.Stopped)
                }
            }
        }

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet?, status: Int) {
            Log.d(TAG, "onAdvertisingDataSet: status=$status")
            when (status) {
                ADVERTISE_SUCCESS -> {
                    Log.d(TAG, "Advertising data set successfully")
                    when (val operation = operationStatus) {
                        is OperationStatus.Updating -> {
                            operation.continuation.resumeWith(Result.success(Unit))
                        }

                        else -> {
                            Log.w(
                                TAG,
                                "Received data set callback but not in Updating state: $operation"
                            )
                        }
                    }
                }

                else -> {
                    val failure = mapStatus(status)
                    Log.e(TAG, "Advertising data set failed: $failure")
                    when (val status = operationStatus) {
                        is OperationStatus.Updating -> {
                            status.continuation.resumeWith(
                                Result.failure(
                                    BleAdvException(
                                        failure
                                    )
                                )
                            )
                        }

                        else -> {
                            Log.w(TAG, "Reporting data update failure to controller: $failure")
                            onEvent(BleAdvEvent.Failed(failure))
                        }
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    suspend fun start(data: ByteArray): BleAdvEvent.Started {
        Log.d(TAG, "start: data size=${data.size}")
        val maxPayloadBytes: Int = btManager.adapter.getLeMaximumAdvertisingDataLength()
        Log.d(TAG, "start: max payload bytes=$maxPayloadBytes")
        Log.d(TAG, "start: payload size=${data.size}")
        val advertiser = btManager.adapter.bluetoothLeAdvertiser
            ?: throw BleAdvException(BleAdvFail.InternalError).also {
                Log.e(
                    TAG,
                    "BluetoothLeAdvertiser is null"
                )
            }
        val set = withTimeout(Constants.BLE_ADVERTISING_TIMEOUT.milliseconds) {
            awaitOperation(
                advertiser,
                operation = {
                    OperationStatus.Starting(it)
                }
            ) {
                Log.d(TAG, "Executing startAdvertisingSet")
                advertiser.startAdvertisingSet(
                    params,
                    buildAdvertiseData(data),
                    null,
                    null,
                    null,
                    callback
                )
            }
        }
        Log.d(TAG, "start: completed")
        return BleAdvEvent.Started(set, data)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    suspend fun stop() {
        Log.d(TAG, "stop")
        val advertiser = btManager.adapter.bluetoothLeAdvertiser
            ?: throw BleAdvException(BleAdvFail.InternalError).also {
                Log.e(
                    TAG,
                    "BluetoothLeAdvertiser is null"
                )
            }
        return withTimeout(Constants.BLE_ADVERTISING_TIMEOUT.milliseconds) {
            awaitOperation(
                advertiser,
                operation = {
                    OperationStatus.Stopping(it)
                }
            ) {
                Log.d(TAG, "Executing stopAdvertisingSet")
                advertiser.stopAdvertisingSet(callback)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    suspend fun update(data: ByteArray, set: AdvertisingSet) {
        Log.d(TAG, "update: data size=${data.size}")
        val advertiser = btManager.adapter.bluetoothLeAdvertiser
            ?: throw BleAdvException(BleAdvFail.InternalError).also {
                Log.e(
                    TAG,
                    "BluetoothLeAdvertiser is null"
                )
            }
        return withTimeout(Constants.BLE_ADVERTISING_TIMEOUT.milliseconds) {
            awaitOperation(
                advertiser,
                operation = {
                    OperationStatus.Updating(it)
                }
            ) {
                Log.d(TAG, "Executing setAdvertisingData")
                set.setAdvertisingData(buildAdvertiseData(data))
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private suspend fun <T> awaitOperation(
        advertiser: BluetoothLeAdvertiser,
        operation: (Continuation<T>) -> OperationStatus,
        start: () -> Unit
    ): T = mutex.withLock {
        try {
            suspendCancellableCoroutine { continuation ->
                operationStatus = operation(continuation)
                try {
                    start()
                } catch (e: Throwable) {
                    continuation.resumeWith(Result.failure(e))
                }

                continuation.invokeOnCancellation {
                    advertiser.stopAdvertisingSet(callback)
                }
            }
        } finally {
            operationStatus = OperationStatus.Idle
        }
    }

    fun mapStatus(status: Int): BleAdvFail {
        return when (status) {
            ADVERTISE_FAILED_ALREADY_STARTED -> BleAdvFail.AlreadyStarted
            ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> BleAdvFail.FeatureUnsupported
            ADVERTISE_FAILED_INTERNAL_ERROR -> BleAdvFail.InternalError
            ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> BleAdvFail.TooManyAdvertisements
            ADVERTISE_FAILED_DATA_TOO_LARGE -> BleAdvFail.DataTooLarge
            else -> BleAdvFail.Unknown(status)
        }
    }
}