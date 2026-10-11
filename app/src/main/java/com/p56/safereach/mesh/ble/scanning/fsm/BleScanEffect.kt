package com.p56.safereach.mesh.ble.scanning.fsm


sealed interface BleScanEffect {

    data object Start : BleScanEffect

    data object Stop : BleScanEffect

    data object NoOp : BleScanEffect
}
