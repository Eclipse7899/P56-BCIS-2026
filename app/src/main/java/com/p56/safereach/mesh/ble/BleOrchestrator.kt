package com.p56.safereach.mesh.ble

import android.util.Log
import com.p56.safereach.mesh.ble.advertisement.BleAdvertisingController
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingCommand
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState
import com.p56.safereach.mesh.ble.fsm.BleCommand
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.fsm.BleState.Active
import com.p56.safereach.mesh.ble.fsm.BleState.Disabled
import com.p56.safereach.mesh.ble.fsm.BleState.Failed
import com.p56.safereach.mesh.ble.fsm.BleState.Off
import com.p56.safereach.mesh.ble.fsm.BleState.Passive
import com.p56.safereach.mesh.ble.scanning.BleScanController
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanCommand
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "BleOrchestrator"


private data class Snapshot(
    val desired: BleCommand,
    val scan: BleScanState,
    val adv: BleAdvertisingState,
)

class BleOrchestrator(
    scope: CoroutineScope,
    private val advertiser: BleAdvertisingController,
    private val scanner: BleScanController,
) {
    private val _state: MutableStateFlow<BleState> =
        MutableStateFlow(
            Off
        )

    val state: StateFlow<BleState> = _state.asStateFlow()

    private val desiredState: MutableStateFlow<BleCommand> = MutableStateFlow(
        BleCommand.Off
    )

    private val reconcileRequests = Channel<Snapshot>(Channel.CONFLATED)

    init {
        scope.launch {
            combine(
                desiredState,
                scanner.state,
                advertiser.state
            ) { intent, scannerState, advertiserState ->
                Snapshot(intent, scannerState, advertiserState)
            }.collect { snapshot ->
                val newState = derive(snapshot.scan, snapshot.adv)
                if (newState != _state.value) {
                    Log.d(TAG, "State transition: ${_state.value} -> $newState")
                }
                _state.value = newState
                reconcileRequests.send(snapshot)
            }
        }

        scope.launch {
            for (snapshot in reconcileRequests) {
                Log.d(TAG, "Reconciling snapshot: $snapshot")
                reconcile(snapshot.desired, snapshot.scan, snapshot.adv)
            }
        }
    }


    fun send(intent: BleCommand) {
        Log.d(TAG, "Setting desired state: $intent")
        desiredState.value = intent
    }

    private fun derive(
        scanState: BleScanState,
        advState: BleAdvertisingState,
    ): BleState {
        return when {
            scanState is BleScanState.Disabled || advState is BleAdvertisingState.Disabled ->
                Disabled

            scanState is BleScanState.Idle && advState is BleAdvertisingState.Idle ->
                Off

            scanState is BleScanState.Failed || advState is BleAdvertisingState.Failed ->
                Failed

            advState is BleAdvertisingState.Advertising && scanState is BleScanState.Scanning ->
                Active(
                    payload = advState.payload,
                )

            scanState is BleScanState.Scanning && advState is BleAdvertisingState.Idle ->
                Passive

            else -> Failed
        }
    }

    private fun reconcile(
        intent: BleCommand,
        scanState: BleScanState,
        advState: BleAdvertisingState,
    ) {
        when (intent) {
            BleCommand.Off -> {
                if (scanState is BleScanState.Scanning) {
                    Log.d(TAG, "reconcile(Off): Stopping scanner")
                    scanner.send(BleScanCommand.Stop)
                }

                if (advState is BleAdvertisingState.Advertising) {
                    Log.d(TAG, "reconcile(Off): Stopping advertiser")
                    advertiser.send(BleAdvertisingCommand.Stop)
                }
            }

            BleCommand.Passive -> {
                if (scanState is BleScanState.Idle || scanState is BleScanState.Failed) {
                    Log.d(TAG, "reconcile(Passive): Starting scanner (state=$scanState)")
                    scanner.send(BleScanCommand.Start)
                }

                if (advState is BleAdvertisingState.Advertising) {
                    Log.d(TAG, "reconcile(Passive): Stopping advertiser")
                    advertiser.send(BleAdvertisingCommand.Stop)
                }
            }

            is BleCommand.Active -> {
                if (scanState is BleScanState.Idle || scanState is BleScanState.Failed) {
                    Log.d(TAG, "reconcile(Active): Starting scanner (state=$scanState)")
                    scanner.send(BleScanCommand.Start)
                }

                when (advState) {
                    // Failed is retryable: a transient advertising error must not leave the mesh
                    // unable to advertise for the rest of the process lifetime.
                    BleAdvertisingState.Idle, is BleAdvertisingState.Failed -> {
                        Log.d(TAG, "reconcile(Active): Starting advertiser (state=$advState)")
                        advertiser.send(BleAdvertisingCommand.Start(intent.payload))
                    }

                    is BleAdvertisingState.Advertising -> {
                        Log.d(TAG, "reconcile(Active): Updating advertising intent ")
                        advertiser.send(
                            BleAdvertisingCommand.Start(intent.payload)
                        )
                    }

                    BleAdvertisingState.Disabled -> {
                        Log.d(TAG, "reconcile(Active): Advertiser is in Disabled state")
                    }
                }
            }
        }
    }
}