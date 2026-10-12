package com.p56.safereach.mesh.ble.scanning

import android.util.Log
import com.p56.safereach.mesh.ble.fsm.BluetoothState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanDesiredState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanEffect
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanEvent
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanFsm
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanIntent
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.service.MeshStatusStore
import com.p56.safereach.util.ApplicationScope
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.nanoseconds

private const val TAG = "BleScanController"

@Singleton
class BleScanController @Inject constructor(
    private val meshStatusStore: MeshStatusStore,
    @param:ApplicationScope private val scope: CoroutineScope,
    private val adapter: BleBeaconScanAdapter,
) {
    private val _state = MutableStateFlow<BleScanState>(BleScanState.Idle)
    val state: StateFlow<BleScanState> = _state.asStateFlow()

    private val intentFlow = MutableStateFlow<BleScanIntent>(BleScanIntent.Stop)
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)


    init {
        scope.launch {
            combine(
                intentFlow,
                meshStatusStore.bluetoothState,
                retryTrigger.onStart { emit(Unit) }
            ) { intent, btState, _ ->
                getDesiredState(intent, btState)
            }.collect { desiredState ->
                settle(event = null, desiredState = desiredState)
            }
        }

        scope.launch {
            observeAdapterFailures()
        }
    }

    fun getDesiredState(
        intent: BleScanIntent,
        bluetoothState: BluetoothState
    ): BleScanDesiredState {
        return if (intent == BleScanIntent.Start && bluetoothState == BluetoothState.Enabled) {
            BleScanDesiredState.Scanning
        } else {
            BleScanDesiredState.Idle
        }
    }

    fun send(intent: BleScanIntent) {
        intentFlow.value = intent
    }

    private suspend fun observeAdapterFailures() {
        for (error in BleScanErrorQueue.channel) {
            val desired = getDesiredState(intentFlow.value, meshStatusStore.bluetoothState.value)
            settle(event = BleScanEvent.Failed(error), desiredState = desired)
        }
    }

    fun settle(event: BleScanEvent?, desiredState: BleScanDesiredState) {
        var pending = event
        while (true) {
            val result = BleScanFsm.evaluate(
                current = _state.value,
                desired = desiredState,
                event = pending,
                nowNanos = System.nanoTime(),
            )
            publish(result.state)

            if (result.state is BleScanState.Retrying && pending is BleScanEvent.Failed) {
                scheduleRetry(result.state.retryAtNanos)
            }

            pending = when (result.effect) {
                BleScanEffect.NoOp -> return
                BleScanEffect.Start -> runAdapter {
                    adapter.scan()
                    BleScanEvent.ScanStarted
                }

                BleScanEffect.Stop -> runAdapter {
                    adapter.stop()
                    BleScanEvent.ScanStopped
                }
            }
        }
    }

    private var retryJob: Job? = null

    private fun scheduleRetry(retryAtNanos: Long) {
        retryJob?.cancel()
        retryJob = scope.launch {
            val delayNanos = (retryAtNanos - System.nanoTime()).coerceAtLeast(0L)
            delay(delayNanos.nanoseconds)
            retryTrigger.tryEmit(Unit)
        }
    }

    private inline fun runAdapter(block: () -> BleScanEvent): BleScanEvent {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "BLE scan operation failed", e)
            BleScanEvent.Failed(BleScanFail.InternalError)
        }
    }

    private fun publish(newState: BleScanState) {
        if (newState == _state.value) return
        Log.d(TAG, "State transition: ${_state.value} -> $newState")
        _state.value = newState
        meshStatusStore.updateScannerState(newState)
    }
}