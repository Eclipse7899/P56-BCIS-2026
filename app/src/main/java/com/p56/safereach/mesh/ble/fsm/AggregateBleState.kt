package com.p56.safereach.mesh.ble.fsm

/** Aggregate view of the scan and advertise state machines, derived by `BleOrchestrator.deriveState`. */
sealed interface AggregateBleState {
    /** Both subsystems are stopped. */
    data object Off : AggregateBleState

    /** Scanning only: there is no alert to advertise. */
    data object Passive : AggregateBleState

    /** Scanning and advertising the given payload. */
    data class Active(val payload: ByteArray) : AggregateBleState

    /** One subsystem is retrying or inconsistent with the desired state. */
    data object PartialFail : AggregateBleState

    /** Both subsystems failed to reach the desired state. */
    data object CompleteFail : AggregateBleState

    /** Transitional or inconsistent child states (for example starting or stopping). */
    data object Unknown : AggregateBleState
}