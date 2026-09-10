package com.p56.safereach.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p56.safereach.Alert
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.MeshMeta
import com.p56.safereach.SignedMessage
import com.p56.safereach.mesh.MeshStatusRepository
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.verification.SigningManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

data class DebugUiState(
    val msg: MeshMeta<SignedMessage>? = null,
    val alert: Alert? = null,
    val isMeshRunning: Boolean = false,
    val orchestratorState: BleState = BleState.Off,
    val advertiserState: BleAdvState = BleAdvState.Idle,
    val scannerState: BleScanState = BleScanState.Idle,
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val signingManager: SigningManager
) : ViewModel() {

    val uiState: StateFlow<DebugUiState> = combine(
        AlertStateRepository.message,
        AlertStateRepository.alert,
        MeshStatusRepository.isServiceRunning,
        MeshStatusRepository.orchestratorState,
        MeshStatusRepository.advertiserState,
        MeshStatusRepository.scannerState,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DebugUiState(
            msg = values[0] as MeshMeta<SignedMessage>?,
            alert = values[1] as Alert?,
            isMeshRunning = values[2] as Boolean,
            orchestratorState = values[3] as BleState,
            advertiserState = values[4] as BleAdvState,
            scannerState = values[5] as BleScanState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebugUiState(),
    )

    @OptIn(ExperimentalSerializationApi::class)
    fun updateAlert(alert: Alert) {
        AlertStateRepository.updateAlert(alert)
        val signed = signingManager.sign(ProtoBuf.encodeToByteArray(alert))
        AlertStateRepository.updateMessage(
            MeshMeta(
                receivedAt = System.currentTimeMillis(),
                receivedFrom = null,
                hopCount = 0,
                data = signed
            )
        )
    }

    fun clearAlert() {
        AlertStateRepository.clearAlert()
        AlertStateRepository.clearMessage()
    }
}
