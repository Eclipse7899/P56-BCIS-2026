package com.p56.safereach.mesh.ble.scanning

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.util.Log
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanEvent
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanIntent
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState.Failed
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState.Idle
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState.Scanning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private sealed interface Message {
    data class Intent(val intent: BleScanIntent) : Message
    data class Event(val event: BleScanEvent) : Message
}

private const val TAG = "BleScanActor"

@SuppressLint("MissingPermission")
class BleScanActor(
    scope: CoroutineScope,
    manager: BluetoothManager,
    private val onScan: (BleScanResult) -> Unit
) {

    private val _state = MutableStateFlow<BleScanState>(Idle)

    val state = _state.asStateFlow()

    private val messages = Channel<Message>(Channel.UNLIMITED)

    private val executor = BleBeaconScanExecutor(
        btManager = manager,
        onEvent = { event ->
            messages.trySend(Message.Event(event))
        }
    )

    init {
        scope.launch {
            for (msg in messages) {
                Log.d(TAG, "Received message: $msg")
                when (msg) {
                    is Message.Event -> {
                        val newState = reduce(_state.value, msg.event)
                        if (newState != _state.value) {
                            Log.d(TAG, "State transition: ${_state.value} -> $newState via external event ${msg.event}")
                        }
                        _state.value = newState
                    }

                    is Message.Intent -> {
                        val event = execute(msg.intent)
                        val newState = reduce(_state.value, event)
                        if (newState != _state.value) {
                            Log.d(TAG, "State transition: ${_state.value} -> $newState via ${msg.intent} -> $event")
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
            is BleScanEvent.Started -> Scanning(event.callback)
            BleScanEvent.Stopped -> Idle
            BleScanEvent.NoOp -> state
            is BleScanEvent.Scanned -> {
                onScan(event.result)
                state
            }
        }
    }

    private fun execute(intent: BleScanIntent): BleScanEvent {
        return when (intent) {
            BleScanIntent.Start -> {
                when (val state = _state.value) {
                    Idle -> {
                        try {
                            val cb = executor.scan()
                            BleScanEvent.Started(cb)
                        } catch (_: SecurityException) {
                            BleScanEvent.Failed
                        }
                    }

                    else -> {
                        BleScanEvent.NoOp
                    }
                }
            }

            BleScanIntent.Stop -> {
                when (val state = _state.value) {
                    is Scanning -> {
                        try {
                            executor.stop(state.callback)
                            BleScanEvent.Stopped
                        } catch (_: SecurityException) {
                            BleScanEvent.Failed
                        }
                    }

                    else -> BleScanEvent.NoOp
                }
            }
        }
    }


    fun send(intent: BleScanIntent) {
        messages.trySend(Message.Intent(intent))
    }
}