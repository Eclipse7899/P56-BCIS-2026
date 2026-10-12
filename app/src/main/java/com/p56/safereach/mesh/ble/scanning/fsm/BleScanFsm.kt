package com.p56.safereach.mesh.ble.scanning.fsm

import com.p56.safereach.mesh.ble.RetryBackoff
import com.p56.safereach.mesh.ble.scanning.BleScanFail
import com.p56.safereach.mesh.ble.scanning.isRetryable
import kotlin.random.Random

object BleScanFsm {

    data class EvaluationResult(
        val state: BleScanState,
        val effect: BleScanEffect,
    )

    fun evaluate(
        current: BleScanState,
        desired: BleScanDesiredState,
        event: BleScanEvent?,
        nowNanos: Long,
    ): EvaluationResult {
        if (event is BleScanEvent.Failed) {
            return scheduleRetry(current, event.error, desired, nowNanos)
        }

        val settled: BleScanState = when (event) {
            BleScanEvent.ScanStarted -> BleScanState.Scanning
            BleScanEvent.ScanStopped -> BleScanState.Idle
            null -> current
        }

        return reconcile(settled, desired, nowNanos)
    }

    private fun reconcile(
        state: BleScanState,
        desired: BleScanDesiredState,
        nowNanos: Long,
    ): EvaluationResult = when (state) {
        BleScanState.Idle -> when (desired) {
            BleScanDesiredState.Scanning ->
                EvaluationResult(BleScanState.Starting(attempt = 0), BleScanEffect.Start)

            BleScanDesiredState.Idle ->
                EvaluationResult(BleScanState.Idle, BleScanEffect.NoOp)
        }

        is BleScanState.Starting -> when (desired) {
            BleScanDesiredState.Scanning ->
                EvaluationResult(state, BleScanEffect.NoOp)

            BleScanDesiredState.Idle ->
                EvaluationResult(BleScanState.Stopping, BleScanEffect.Stop)
        }

        BleScanState.Scanning -> when (desired) {
            BleScanDesiredState.Scanning ->
                EvaluationResult(BleScanState.Scanning, BleScanEffect.NoOp)

            BleScanDesiredState.Idle ->
                EvaluationResult(BleScanState.Stopping, BleScanEffect.Stop)
        }

        BleScanState.Stopping -> when (desired) {
            BleScanDesiredState.Scanning ->
                EvaluationResult(BleScanState.Starting(attempt = 0), BleScanEffect.Start)

            BleScanDesiredState.Idle ->
                EvaluationResult(BleScanState.Stopping, BleScanEffect.NoOp)
        }

        is BleScanState.Retrying -> when {
            desired == BleScanDesiredState.Idle ->
                EvaluationResult(BleScanState.Idle, BleScanEffect.NoOp)

            nowNanos >= state.retryAtNanos ->
                EvaluationResult(
                    BleScanState.Starting(attempt = state.attempt),
                    BleScanEffect.Start
                )

            else ->
                EvaluationResult(state, BleScanEffect.NoOp)
        }
    }

    private fun scheduleRetry(
        current: BleScanState,
        error: BleScanFail,
        desired: BleScanDesiredState,
        nowNanos: Long,
    ): EvaluationResult {
        if (desired == BleScanDesiredState.Idle) {
            return EvaluationResult(BleScanState.Idle, BleScanEffect.NoOp)
        }

        val attempt = when (current) {
            is BleScanState.Starting -> current.attempt + 1
            is BleScanState.Retrying -> current.attempt + 1
            else -> 1
        }
        val delayMillis = RetryBackoff.delayMillis(
            attempt = attempt,
            retryable = error.isRetryable,
            randomValue = Random.nextDouble(),
        )
        val retryAtNanos = nowNanos + delayMillis * NANOS_PER_MILLI

        return EvaluationResult(
            BleScanState.Retrying(error, attempt, retryAtNanos),
            BleScanEffect.NoOp,
        )
    }

    private const val NANOS_PER_MILLI = 1_000_000L
}
