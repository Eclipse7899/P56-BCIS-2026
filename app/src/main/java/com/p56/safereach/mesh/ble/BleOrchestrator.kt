package com.p56.safereach.mesh.ble

import android.util.Log
import com.p56.safereach.mesh.ble.advertisement.BleAdvertisingController
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingIntent
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState
import com.p56.safereach.mesh.ble.fsm.AggregateBleState
import com.p56.safereach.mesh.ble.fsm.AggregateBleState.Active
import com.p56.safereach.mesh.ble.fsm.AggregateBleState.CompleteFail
import com.p56.safereach.mesh.ble.fsm.AggregateBleState.Off
import com.p56.safereach.mesh.ble.fsm.AggregateBleState.PartialFail
import com.p56.safereach.mesh.ble.fsm.AggregateBleState.Passive
import com.p56.safereach.mesh.ble.fsm.AggregateBleState.Unknown
import com.p56.safereach.mesh.ble.fsm.BleIntent
import com.p56.safereach.mesh.ble.scanning.BleScanController
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanIntent
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.util.ApplicationScope
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "BleOrchestrator"


private data class Snapshot(
    val desired: BleIntent,
    val scan: BleScanState,
    val adv: BleAdvertisingState,
)

@Singleton
class BleOrchestrator @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val advertiser: BleAdvertisingController,
    private val scanner: BleScanController,
) {
    private val _state: MutableStateFlow<AggregateBleState> =
        MutableStateFlow(
            Off
        )

    val state: StateFlow<AggregateBleState> = _state.asStateFlow()

    private val desired: MutableStateFlow<BleIntent> = MutableStateFlow(
        BleIntent.Off
    )

    init {
        scope.launch {
            combine(
                desired,
                scanner.state,
                advertiser.state
            ) { intent, scannerState, advertiserState ->
                Snapshot(intent, scannerState, advertiserState)
            }.collect { snapshot ->
                val newState = deriveState(snapshot.scan, snapshot.adv)
                if (newState != _state.value) {
                    Log.d(TAG, "State transition: ${_state.value} -> $newState")
                }
                _state.value = newState
                Log.d(TAG, "Reconciling snapshot: $snapshot")
                reconcile(snapshot.desired)
            }
        }
    }


    fun send(intent: BleIntent) {
        Log.d(TAG, "Setting desired state: $intent")
        desired.value = intent
    }

    private fun deriveState(
        scanState: BleScanState,
        advState: BleAdvertisingState,
    ): AggregateBleState {
        return when (scanState) {
            BleScanState.Idle, BleScanState.Stopping -> when (advState) {
                is BleAdvertisingState.Advertising -> Unknown
                is BleAdvertisingState.Starting, BleAdvertisingState.Stopping -> Unknown
                is BleAdvertisingState.Retrying -> PartialFail
                BleAdvertisingState.Idle -> Off
            }

            is BleScanState.Starting, BleScanState.Scanning -> when (advState) {
                is BleAdvertisingState.Advertising -> Active(advState.payload)
                is BleAdvertisingState.Starting, BleAdvertisingState.Stopping -> Unknown
                is BleAdvertisingState.Retrying -> PartialFail
                BleAdvertisingState.Idle -> Passive
            }

            is BleScanState.Retrying -> when (advState) {
                is BleAdvertisingState.Advertising -> PartialFail
                is BleAdvertisingState.Starting, BleAdvertisingState.Stopping -> PartialFail
                is BleAdvertisingState.Retrying -> CompleteFail
                BleAdvertisingState.Idle -> PartialFail
            }
        }
    }

    private fun reconcile(
        intent: BleIntent,
    ) {
        when (intent) {
            BleIntent.Off -> {
                scanner.send(BleScanIntent.Stop)
                advertiser.send(BleAdvertisingIntent.Stop)
            }

            BleIntent.Passive -> {
                scanner.send(BleScanIntent.Start)
                advertiser.send(BleAdvertisingIntent.Stop)
            }

            is BleIntent.Active -> {
                scanner.send(BleScanIntent.Start)
                advertiser.send(BleAdvertisingIntent.Start(intent.payload))
            }
        }
    }
}