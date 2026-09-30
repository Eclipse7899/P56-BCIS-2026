package com.p56.safereach.mesh.ble.advertisement

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.util.Log
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingCommand
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingEvent
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState.Advertising
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState.Disabled
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState.Failed
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState.Idle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private sealed interface Message {
    data class Intent(val intent: BleAdvertisingCommand) : Message
    data class Event(val event: BleAdvertisingEvent) : Message
    data class BluetoothState(val enabled: Boolean) : Message
}

private const val TAG = "BleAdvertisingController"

@SuppressLint("MissingPermission")
class BleAdvertisingController(
    scope: CoroutineScope,
    btManager: BluetoothManager,
) {
    private val adapter = BleBeaconAdvAdapter(
        onEvent = { event ->
            messages.trySend(Message.Event(event))
        },
        btManager
    )
    private val _state = MutableStateFlow<BleAdvertisingState>(Idle)

    private val messages = Channel<Message>(Channel.UNLIMITED)

    val state: StateFlow<BleAdvertisingState> = _state.asStateFlow()

    fun send(intent: BleAdvertisingCommand) {
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

    fun reduce(state: BleAdvertisingState, event: BleAdvertisingEvent): BleAdvertisingState {
        return when (state) {
            is Advertising -> {
                when (event) {
                    is BleAdvertisingEvent.Started -> Advertising(event.set, event.payload)
                    is BleAdvertisingEvent.Failed -> Failed(event.error)
                    is BleAdvertisingEvent.Stopped -> Idle
                    BleAdvertisingEvent.NoOp -> state
                }
            }

            is Idle -> {
                when (event) {
                    is BleAdvertisingEvent.Started -> Advertising(event.set, event.payload)
                    is BleAdvertisingEvent.Failed -> Failed(event.error)
                    is BleAdvertisingEvent.Stopped -> Idle
                    BleAdvertisingEvent.NoOp -> state
                }
            }

            is Failed -> {
                when (event) {
                    is BleAdvertisingEvent.Started -> Advertising(event.set, event.payload)
                    is BleAdvertisingEvent.Failed -> Failed(event.error)
                    is BleAdvertisingEvent.Stopped -> Idle
                    BleAdvertisingEvent.NoOp -> state
                }
            }

            is Disabled -> {
                when (event) {
                    is BleAdvertisingEvent.Started -> Advertising(event.set, event.payload)
                    is BleAdvertisingEvent.Failed -> Failed(event.error)
                    is BleAdvertisingEvent.Stopped -> Idle
                    BleAdvertisingEvent.NoOp -> state
                }
            }
        }
    }

    /**
     * Runs one advertising operation and turns every BLE outcome into an event. A [CancellationException]
     * that is *not* a timeout means the owning scope is shutting down and must propagate, otherwise the
     * message loop below would swallow its own cancellation.
     */
    private suspend fun advertisingEvent(action: suspend () -> BleAdvertisingEvent): BleAdvertisingEvent =
        try {
            action()
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

    private suspend fun execute(intent: BleAdvertisingCommand): BleAdvertisingEvent {
        return when (intent) {
            is BleAdvertisingCommand.Start ->
                when (val state = _state.value) {
                    is Advertising if shouldUpdate(state, intent) -> advertisingEvent {
                        adapter.update(intent.payload, state.advertisingSet)
                        BleAdvertisingEvent.Started(state.advertisingSet, intent.payload)
                    }

                    // Failed is retryable: a transient advertising error must not leave the mesh
                    // unable to advertise for the rest of the process lifetime.
                    is Idle, is Failed -> advertisingEvent { adapter.start(intent.payload) }

                    else -> BleAdvertisingEvent.NoOp
                }

            is BleAdvertisingCommand.Stop -> {
                when (_state.value) {
                    is Advertising -> advertisingEvent {
                        adapter.stop()
                        BleAdvertisingEvent.Stopped
                    }

                    else -> BleAdvertisingEvent.NoOp
                }
            }
        }
    }

    private fun shouldUpdate(
        state: Advertising,
        intent: BleAdvertisingCommand.Start
    ): Boolean = !state.payload.contentEquals(intent.payload)
}