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
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.p56.safereach.Constants
import com.p56.safereach.Constants.MAX_DATA_PAYLOAD
import com.p56.safereach.Constants.SERVICE_UUIDS
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
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
    private val onEvent: (BleAdvertisingEvent) -> Unit,
    private val btManager: BluetoothManager
) {

    private val params = AdvertisingSetParameters.Builder()
        .setScannable(false)
        .setConnectable(false)
        .setLegacyMode(false)
        .setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
        .setSecondaryPhy(BluetoothDevice.PHY_LE_CODED)
        .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
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
        if (fragments.size > SERVICE_UUIDS.size) {
            Log.e(
                TAG,
                "Payload of ${payload.size} bytes needs ${fragments.size} fragments, " +
                        "but only ${SERVICE_UUIDS.size} service-data slots are available"
            )
            throw BleAdvertisingException(BleAdvertisingFail.DataTooLarge)
        }
        for (i in fragments.indices) {
            builder.addServiceData(ParcelUuid(SERVICE_UUIDS[i]), fragments[i])
        }
        return builder.build()
    }

    private var operationStatus: OperationStatus = OperationStatus.Idle

    private val mutex = Mutex()

    @Volatile
    private var currentCallback: AdvertisingSetCallback? = null

    private fun createCallback(): AdvertisingSetCallback = object : AdvertisingSetCallback() {
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
                                    BleAdvertisingException(
                                        failure
                                    )
                                )
                            )
                        }

                        else -> {
                            Log.w(TAG, "Reporting failure to controller: $failure")
                            onEvent(BleAdvertisingEvent.Failed(failure))
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
                    onEvent(BleAdvertisingEvent.Stopped)
                }
            }
            if (currentCallback === this) {
                currentCallback = null
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
                                    BleAdvertisingException(
                                        failure
                                    )
                                )
                            )
                        }

                        else -> {
                            Log.w(TAG, "Reporting data update failure to controller: $failure")
                            onEvent(BleAdvertisingEvent.Failed(failure))
                        }
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    suspend fun start(payload: ByteArray): BleAdvertisingEvent.Started {
        Log.d(TAG, "start: payload size=${payload.size}")
        val advertiser = advertiser()

        stopCurrentSet(advertiser)

        val callback = createCallback()
        currentCallback = callback
        val set = try {
            withAdvertisingTimeout {
                awaitOperation<AdvertisingSet>(
                    advertiser,
                    callback,
                    operation = {
                        OperationStatus.Starting(it)
                    }
                ) {
                    Log.d(TAG, "Executing startAdvertisingSet")
                    advertiser.startAdvertisingSet(
                        params,
                        buildAdvertiseData(payload),
                        null,
                        null,
                        null,
                        callback
                    )
                }
            }
        } catch (e: Throwable) {
            releaseCallback(advertiser, callback)
            throw e
        }
        Log.d(TAG, "start: completed")
        return BleAdvertisingEvent.Started(set, payload)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    suspend fun stop() {
        Log.d(TAG, "stop")
        val advertiser = advertiser()
        val callback = currentCallback
        if (callback == null) {
            Log.d(TAG, "stop: no active advertising set")
            return
        }
        return withAdvertisingTimeout {
            awaitOperation<Unit>(
                advertiser,
                callback,
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
    suspend fun update(payload: ByteArray, set: AdvertisingSet) {
        Log.d(TAG, "update: data size=${payload.size}")
        val advertiser = advertiser()
        val callback = currentCallback
        return withAdvertisingTimeout {
            awaitOperation<Unit>(
                advertiser,
                callback,
                operation = {
                    OperationStatus.Updating(it)
                }
            ) {
                Log.d(TAG, "Executing setAdvertisingData")
                set.setAdvertisingData(buildAdvertiseData(payload))
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private suspend fun <T> awaitOperation(
        advertiser: BluetoothLeAdvertiser,
        callback: AdvertisingSetCallback?,
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
                    callback?.let { advertiser.stopAdvertisingSet(it) }
                    if (callback != null && currentCallback === callback) {
                        currentCallback = null
                    }
                }
            }
        } finally {
            operationStatus = OperationStatus.Idle
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private suspend fun stopCurrentSet(advertiser: BluetoothLeAdvertiser) {
        val callback = currentCallback ?: return
        Log.d(TAG, "stopCurrentSet: releasing previous advertising set")
        try {
            withAdvertisingTimeout {
                awaitOperation<Unit>(
                    advertiser,
                    callback,
                    operation = { OperationStatus.Stopping(it) }
                ) {
                    Log.d(TAG, "Executing stopAdvertisingSet (before start)")
                    advertiser.stopAdvertisingSet(callback)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "stopCurrentSet: stop failed or timed out, releasing callback", e)
            releaseCallback(advertiser, callback)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun releaseCallback(
        advertiser: BluetoothLeAdvertiser,
        callback: AdvertisingSetCallback
    ) {
        try {
            advertiser.stopAdvertisingSet(callback)
        } catch (e: Throwable) {
            Log.w(TAG, "releaseCallback: failed to release callback", e)
        }
        if (currentCallback === callback) {
            currentCallback = null
        }
    }

    private fun advertiser(): BluetoothLeAdvertiser =
        btManager.adapter.bluetoothLeAdvertiser
            ?: throw BleAdvertisingException(BleAdvertisingFail.InternalError).also {
                Log.e(TAG, "BluetoothLeAdvertiser is null")
            }

    private suspend fun <T> withAdvertisingTimeout(block: suspend () -> T): T =
        try {
            withTimeout(Constants.BLE_ADVERTISING_TIMEOUT.milliseconds) { block() }
        } catch (_: TimeoutCancellationException) {
            Log.e(
                TAG,
                "Advertising operation timed out after ${Constants.BLE_ADVERTISING_TIMEOUT}ms"
            )
            throw BleAdvertisingException(BleAdvertisingFail.Timeout)
        }

    fun mapStatus(status: Int): BleAdvertisingFail {
        return when (status) {
            ADVERTISE_FAILED_ALREADY_STARTED -> BleAdvertisingFail.AlreadyStarted
            ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> BleAdvertisingFail.FeatureUnsupported
            ADVERTISE_FAILED_INTERNAL_ERROR -> BleAdvertisingFail.InternalError
            ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> BleAdvertisingFail.TooManyAdvertisements
            ADVERTISE_FAILED_DATA_TOO_LARGE -> BleAdvertisingFail.DataTooLarge
            else -> BleAdvertisingFail.Unknown(status)
        }
    }
}
