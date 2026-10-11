package com.p56.safereach.ui.debug

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.components.BuildConfig
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState
import com.p56.safereach.mesh.ble.fsm.AggregateBleState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.model.Alert
import com.p56.safereach.model.AlertBatch
import com.p56.safereach.model.ReceivedMessage
import com.p56.safereach.model.SignedMessage
import com.p56.safereach.model.toDto
import com.p56.safereach.service.AlertRepository
import com.p56.safereach.service.MeshStatusStore
import com.p56.safereach.verification.SigningManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebugUiState(
    val receivedMessage: ReceivedMessage? = null,
    val isMeshRunning: Boolean = false,
    val orchestratorState: AggregateBleState = AggregateBleState.Off,
    val advertiserState: BleAdvertisingState = BleAdvertisingState.Idle,
    val scannerState: BleScanState = BleScanState.Idle,
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val signingManager: SigningManager,
    private val alertRepository: AlertRepository,
    meshStatusStore: MeshStatusStore,
) : ViewModel() {
    val uiState: StateFlow<DebugUiState> = combine(
        alertRepository.receivedMessage,
        meshStatusStore.isServiceRunning,
        meshStatusStore.bleOrchestratorState,
        meshStatusStore.advertiserState,
        meshStatusStore.scannerState,
    ) { receivedMessage, isMeshRunning, orchestratorState, advertiserState, scannerState ->
        DebugUiState(
            receivedMessage = receivedMessage,
            isMeshRunning = isMeshRunning,
            orchestratorState = orchestratorState,
            advertiserState = advertiserState,
            scannerState = scannerState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebugUiState(),
    )

    fun addAlert(alert: Alert) {
        viewModelScope.launch {
            val data = AlertBatch(
                alerts = listOf(alert),
                validUntil = alert.expiresAt,
                timestamp = alert.timestamp,
            )
            val message = SignedMessage(
                payload = data.toDto().toByteArray(),
                signature = ByteArray(0),
                receivedAt = System.currentTimeMillis(),
                receivedFrom = "Debug"
            )
            alertRepository.addSigned(message)
        }
    }

    fun deleteAlert() {
        viewModelScope.launch {
            Log.d("DebugViewModel", "Deleting message")
            alertRepository.removeSigned()
        }
    }
}
