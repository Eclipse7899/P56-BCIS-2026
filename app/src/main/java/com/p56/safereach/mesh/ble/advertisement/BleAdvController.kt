package com.p56.safereach.mesh.ble.advertisement

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.util.Log
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvEvent
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvIntent
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState.Advertising
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState.Disabled
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState.Failed
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState.Idle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private sealed interface Message {
    data class Intent(val intent: BleAdvIntent) : Message
    data class Event(val event: BleAdvEvent) : Message
    data class BluetoothState(val enabled: Boolean) : Message
}

private const val TAG = "BleAdvController"

@SuppressLint("MissingPermission")
class BleAdvController(
    scope: CoroutineScope,
    btManager: BluetoothManager,
) {
    private val adapter = BleBeaconAdvAdapter(
        onEvent = { event ->
            messages.trySend(Message.Event(event))
        },
        btManager
    )
    private val _state = MutableStateFlow<BleAdvState>(Idle)

    private val messages = Channel<Message>(Channel.UNLIMITED)

    val state: StateFlow<BleAdvState> = _state.asStateFlow()

    fun send(intent: BleAdvIntent) {
        messages.trySend(Message.Intent(intent))
    }

    fun onBluetoothStateChanged(enabled: Boolean) {
        messages.trySend(Message.BluetoothState(enabled))
    }

    init {
        scope.launch {
            for (msg in messages) {
                Log.d(TAG, "Received message: $msg")
                when (msg) {
                    is Message.BluetoothState -> {
                        val newState = if (msg.enabled) {
                            if (_state.value is Disabled) Idle else _state.value
                        } else {
                            Disabled
                        }
                        if (newState != _state.value) {
                            Log.d(
                                TAG,
                                "State transition: ${_state.value} -> $newState via bluetooth enabled=${msg.enabled}"
                            )
                        }
                        _state.value = newState
                    }

                    is Message.Intent -> {
                        val event = execute(msg.intent)
                        val newState = reduce(_state.value, event)
                        if (newState != _state.value) {
                            Log.d(
                                TAG,
                                "State transition: ${_state.value} -> $newState via ${msg.intent} -> $event"
                            )
                        }
                        _state.value = newState
                    }

                    is Message.Event -> {
                        val newState = reduce(_state.value, msg.event)
                        if (newState != _state.value) {
                            Log.d(
                                TAG,
                                "State transition: ${_state.value} -> $newState via external event ${msg.event}"
                            )
                        }
                        _state.value = newState
                    }
                }
            }
        }
    }

    fun reduce(state: BleAdvState, event: BleAdvEvent): BleAdvState {
        return when (state) {
            is Advertising -> {
                when (event) {
                    is BleAdvEvent.Started -> Advertising(event.set, event.data)
                    is BleAdvEvent.Failed -> Failed(event.error)
                    is BleAdvEvent.Stopped -> Idle
                    BleAdvEvent.NoOp -> state
                }
            }

            is Idle -> {
                when (event) {
                    is BleAdvEvent.Started -> Advertising(event.set, event.data)
                    is BleAdvEvent.Failed -> Failed(event.error)
                    is BleAdvEvent.Stopped -> Idle
                    BleAdvEvent.NoOp -> state
                }
            }

            is Failed -> {
                when (event) {
                    is BleAdvEvent.Started -> Advertising(event.set, event.data)
                    is BleAdvEvent.Failed -> Failed(event.error)
                    is BleAdvEvent.Stopped -> Idle
                    BleAdvEvent.NoOp -> state
                }
            }

            is Disabled -> {
                when (event) {
                    is BleAdvEvent.Started -> Advertising(event.set, event.data)
                    is BleAdvEvent.Failed -> Failed(event.error)
                    is BleAdvEvent.Stopped -> Idle
                    BleAdvEvent.NoOp -> state
                }
            }
        }
    }

    /**
     * Runs one advertising operation and turns every BLE outcome into an event. A [CancellationException]
     * that is *not* a timeout means the owning scope is shutting down and must propagate, otherwise the
     * message loop below would swallow its own cancellation.
     */
    private suspend fun advertisingEvent(action: suspend () -> BleAdvEvent): BleAdvEvent =
        try {
            action()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Advertising operation timed out")
            BleAdvEvent.Failed(BleAdvFail.Timeout)
        } catch (e: CancellationException) {
            throw e
        } catch (e: BleAdvException) {
            BleAdvEvent.Failed(e.failure)
        } catch (e: Exception) {
            Log.e(TAG, "Advertising operation failed", e)
            BleAdvEvent.Failed(BleAdvFail.InternalError)
        }

    private suspend fun execute(intent: BleAdvIntent): BleAdvEvent {
        return when (intent) {
            is BleAdvIntent.Start ->
                when (val state = _state.value) {
                    is Advertising if shouldUpdate(state, intent) -> advertisingEvent {
                        adapter.update(intent.payload, state.advertisingSet)
                        BleAdvEvent.Started(state.advertisingSet, intent.payload)
                    }

                    // Failed is retryable: a transient advertising error must not leave the mesh
                    // unable to advertise for the rest of the process lifetime.
                    is Idle, is Failed -> advertisingEvent { adapter.start(intent.payload) }

                    else -> BleAdvEvent.NoOp
                }

            is BleAdvIntent.Stop -> {
                when (_state.value) {
                    is Advertising -> advertisingEvent {
                        adapter.stop()
                        BleAdvEvent.Stopped
                    }

                    else -> BleAdvEvent.NoOp
                }
            }
        }
    }

    private fun shouldUpdate(
        state: Advertising,
        intent: BleAdvIntent.Start
    ): Boolean = !state.data.contentEquals(intent.payload)
}