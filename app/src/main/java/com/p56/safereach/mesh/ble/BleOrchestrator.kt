package com.p56.safereach.mesh.ble

import android.util.Log
import com.p56.safereach.mesh.ble.advertisement.BleAdvController
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvIntent
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleIntent
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.fsm.BleState.Active
import com.p56.safereach.mesh.ble.fsm.BleState.Disabled
import com.p56.safereach.mesh.ble.fsm.BleState.Failed
import com.p56.safereach.mesh.ble.fsm.BleState.Off
import com.p56.safereach.mesh.ble.fsm.BleState.Passive
import com.p56.safereach.mesh.ble.scanning.BleScanController
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanIntent
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
    val desired: BleIntent,
    val scan: BleScanState,
    val adv: BleAdvState,
)

class BleOrchestrator(
    scope: CoroutineScope,
    private val advertiser: BleAdvController,
    private val scanner: BleScanController,
) {
    private val _state: MutableStateFlow<BleState> =
        MutableStateFlow(
            Off
        )

    val state: StateFlow<BleState> = _state.asStateFlow()

    private val desiredState: MutableStateFlow<BleIntent> = MutableStateFlow(
        BleIntent.Off
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


    fun send(intent: BleIntent) {
        Log.d(TAG, "Setting desired state: $intent")
        desiredState.value = intent
    }

    private fun derive(
        scanState: BleScanState,
        advState: BleAdvState,
    ): BleState {
        return when {
            scanState is BleScanState.Disabled || advState is BleAdvState.Disabled ->
                Disabled

            scanState is BleScanState.Idle && advState is BleAdvState.Idle ->
                Off

            scanState is BleScanState.Failed || advState is BleAdvState.Failed ->
                Failed

            advState is BleAdvState.Advertising && scanState is BleScanState.Scanning ->
                Active(
                    payload = advState.data,
                )

            scanState is BleScanState.Scanning && advState is BleAdvState.Idle ->
                Passive

            else -> Failed
        }
    }

    private fun reconcile(
        intent: BleIntent,
        scanState: BleScanState,
        advState: BleAdvState,
    ) {
        when (intent) {
            BleIntent.Off -> {
                if (scanState is BleScanState.Scanning) {
                    Log.d(TAG, "reconcile(Off): Stopping scanner")
                    scanner.send(BleScanIntent.Stop)
                }

                if (advState is BleAdvState.Advertising) {
                    Log.d(TAG, "reconcile(Off): Stopping advertiser")
                    advertiser.send(BleAdvIntent.Stop)
                }
            }

            BleIntent.Passive -> {
                if (scanState is BleScanState.Idle) {
                    Log.d(TAG, "reconcile(Passive): Starting scanner")
                    scanner.send(BleScanIntent.Start)
                }

                if (advState is BleAdvState.Advertising) {
                    Log.d(TAG, "reconcile(Passive): Stopping advertiser")
                    advertiser.send(BleAdvIntent.Stop)
                }
            }

            is BleIntent.Active -> {
                if (scanState is BleScanState.Idle) {
                    Log.d(TAG, "reconcile(Active): Starting scanner")
                    scanner.send(BleScanIntent.Start)
                }

                when (advState) {
                    BleAdvState.Idle -> {
                        Log.d(TAG, "reconcile(Active): Starting advertiser")
                        advertiser.send(BleAdvIntent.Start(intent.payload))
                    }

                    is BleAdvState.Advertising -> {
                        Log.d(TAG, "reconcile(Active): Updating advertising intent ")
                        advertiser.send(
                            BleAdvIntent.Start(intent.payload)
                        )
                    }

                    BleAdvState.Disabled -> {
                        Log.d(TAG, "reconcile(Active): Advertiser is in Disabled state")
                    }

                    is BleAdvState.Failed -> {
                        Log.w(TAG, "reconcile(Active): Advertiser is in Failed state")
                    }
                }
            }
        }
    }
}