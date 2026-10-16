package com.p56.safereach.ui.navigation

import androidx.lifecycle.ViewModel
import com.p56.safereach.service.MeshStatusStore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class SafeReachAppViewModel @Inject constructor(
    meshStatusStore: MeshStatusStore,
) : ViewModel() {

    val bluetoothState = meshStatusStore.bluetoothState
    val bluetoothIncompatibilities = meshStatusStore.bluetoothIncompatibilities
}
