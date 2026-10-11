package com.p56.safereach.mesh.ble.fsm

sealed interface AggregateBleState {
    data object Off : AggregateBleState
    data object Passive : AggregateBleState
    data class Active(val payload: ByteArray) : AggregateBleState
    data object PartialFail : AggregateBleState
    data object CompleteFail : AggregateBleState
    data object Unknown : AggregateBleState
    data object Disabled : AggregateBleState
}