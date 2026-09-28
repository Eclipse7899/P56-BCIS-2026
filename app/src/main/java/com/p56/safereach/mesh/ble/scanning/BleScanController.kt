package com.p56.safereach.mesh.ble.scanning

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanCommand
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanEvent
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState.Disabled
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState.Failed
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState.Idle
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState.Scanning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private sealed interface Message {
    data class Intent(val intent: BleScanCommand) : Message
    data class Event(val event: BleScanEvent) : Message
    data class BluetoothState(val enabled: Boolean) : Message
}

private const val TAG = "BleScanController"

@SuppressLint("MissingPermission")
class BleScanController(
    scope: CoroutineScope,
    manager: BluetoothManager,
    context: Context,
) {

    private val _state = MutableStateFlow<BleScanState>(Idle)

    val state = _state.asStateFlow()

    private val messages = Channel<Message>(Channel.UNLIMITED)

    private val adapter = BleBeaconScanAdapter(
        btManager = manager,
        context = context
    )

    init {
        scope.launch {
            for (msg in messages) {
                Log.d(TAG, "Received message: $msg")
                when (msg) {
                    is Message.BluetoothState -> {
                        val newState = if (msg.enabled) {
                            if (_state.value is Disabled) {
                                Idle
                            } else {
                                _state.value
                            }
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
                }
            }
        }
    }

    fun reduce(state: BleScanState, event: BleScanEvent): BleScanState {
        return when (event) {
            BleScanEvent.Failed -> Failed
            is BleScanEvent.Started -> Scanning
            BleScanEvent.Stopped -> Idle
            BleScanEvent.NoOp -> state
            is BleScanEvent.Scanned -> state
        }
    }

    private fun execute(intent: BleScanCommand): BleScanEvent {
        return when (intent) {
            BleScanCommand.Start -> {
                when (_state.value) {
                    Idle, is Failed -> {
                        try {
                            adapter.scan()
                            BleScanEvent.Started
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start scan", e)
                            BleScanEvent.Failed
                        }
                    }

                    else -> {
                        BleScanEvent.NoOp
                    }
                }
            }

            BleScanCommand.Stop -> {
                when (_state.value) {
                    is Scanning -> {
                        try {
                            adapter.stop()
                            BleScanEvent.Stopped
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to stop scan", e)
                            BleScanEvent.Failed
                        }
                    }

                    else -> BleScanEvent.NoOp
                }
            }
        }
    }


    fun send(intent: BleScanCommand) {
        messages.trySend(Message.Intent(intent))
    }

    fun onBluetoothStateChanged(enabled: Boolean) {
        messages.trySend(Message.BluetoothState(enabled))
    }
}