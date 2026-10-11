package com.p56.safereach.ui.navigation

import androidx.lifecycle.ViewModel
import com.p56.safereach.mesh.ble.fsm.AggregateBleState
import com.p56.safereach.service.MeshStatusStore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SafeReachAppViewModel @Inject constructor(
    meshStatusStore: MeshStatusStore,
) : ViewModel() {

    val bluetoothState = meshStatusStore.bluetoothState
}
