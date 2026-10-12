package com.p56.safereach.mesh.ble.advertisement

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.util.Log
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingDesiredState
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingEffect
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingEvent
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingFsm
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingIntent
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState
import com.p56.safereach.mesh.ble.fsm.BluetoothState
import com.p56.safereach.service.MeshStatusStore
import com.p56.safereach.util.ApplicationScope
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
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


private const val TAG = "BleAdvertisingController"

@SuppressLint("MissingPermission")
@Singleton
class BleAdvertisingController @Inject constructor(
    private val meshStatusStore: MeshStatusStore,
    @param:ApplicationScope private val scope: CoroutineScope,
    btManager: BluetoothManager,
) {
    private val adapter = BleBeaconAdvAdapter(
        onEvent = { event ->
            adapterEvents.trySend(event)
        },
        btManager
    )
    private val _state = MutableStateFlow<BleAdvertisingState>(BleAdvertisingState.Idle)
    val state: StateFlow<BleAdvertisingState> = _state.asStateFlow()

    private val intentFlow = MutableStateFlow<BleAdvertisingIntent>(BleAdvertisingIntent.Stop)
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val adapterEvents = Channel<BleAdvertisingEvent>(Channel.UNLIMITED)


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
            observeAdapterEvents()
        }
    }

    fun getDesiredState(
        intent: BleAdvertisingIntent,
        bluetoothState: BluetoothState
    ): BleAdvertisingDesiredState {
        return if (intent is BleAdvertisingIntent.Start && bluetoothState == BluetoothState.Enabled) {
            BleAdvertisingDesiredState.Advertising(intent.payload)
        } else {
            BleAdvertisingDesiredState.Idle
        }
    }

    fun send(intent: BleAdvertisingIntent) {
        intentFlow.value = intent
    }

    private suspend fun observeAdapterEvents() {
        for (event in adapterEvents) {
            val desired = getDesiredState(intentFlow.value, meshStatusStore.bluetoothState.value)
            settle(event = event, desiredState = desired)
        }
    }

    suspend fun settle(event: BleAdvertisingEvent?, desiredState: BleAdvertisingDesiredState) {
        var pending = event
        while (true) {
            val result = BleAdvertisingFsm.evaluate(
                current = _state.value,
                desired = desiredState,
                event = pending,
                nowNanos = System.nanoTime(),
            )
            publish(result.state)

            if (result.state is BleAdvertisingState.Retrying && pending is BleAdvertisingEvent.Failed) {
                scheduleRetry(result.state.retryAtNanos)
            }

            pending = when (val effect = result.effect) {
                BleAdvertisingEffect.NoOp -> return

                is BleAdvertisingEffect.Start -> runAdapter {
                    adapter.start(effect.payload)
                }

                is BleAdvertisingEffect.Update -> runAdapter {
                    adapter.stop()
                    adapter.start(effect.payload)
                }

                BleAdvertisingEffect.Stop -> runAdapter {
                    adapter.stop()
                    BleAdvertisingEvent.Stopped
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

    private suspend inline fun runAdapter(
        block: suspend () -> BleAdvertisingEvent,
    ): BleAdvertisingEvent = try {
        block()
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Advertising operation timed out")
        BleAdvertisingEvent.Failed(BleAdvertisingFail.Timeout)
    } catch (e: CancellationException) {
        throw e
    } catch (e: BleAdvertisingException) {
        BleAdvertisingEvent.Failed(e.failure)
    } catch (e: Exception) {
        Log.e(TAG, "Advertising operation failed", e)
        BleAdvertisingEvent.Failed(BleAdvertisingFail.InternalError)
    }

    private fun publish(newState: BleAdvertisingState) {
        if (newState == _state.value) return
        Log.d(TAG, "State transition: ${_state.value} -> $newState")
        _state.value = newState
        meshStatusStore.updateAdvertiserState(newState)
    }
}
