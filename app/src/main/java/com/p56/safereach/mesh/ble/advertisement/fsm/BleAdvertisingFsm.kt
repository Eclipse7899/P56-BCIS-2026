package com.p56.safereach.mesh.ble.advertisement.fsm

import com.p56.safereach.mesh.ble.RetryBackoff
import com.p56.safereach.mesh.ble.advertisement.BleAdvertisingFail
import com.p56.safereach.mesh.ble.advertisement.isRetryable
import kotlin.random.Random

object BleAdvertisingFsm {

    data class EvaluationResult(
        val state: BleAdvertisingState,
        val effect: BleAdvertisingEffect,
    )

    fun evaluate(
        current: BleAdvertisingState,
        desired: BleAdvertisingDesiredState,
        event: BleAdvertisingEvent?,
        nowNanos: Long,
    ): EvaluationResult {
        if (event is BleAdvertisingEvent.Failed) {
            return scheduleRetry(current, event.error, desired, nowNanos)
        }

        val settled: BleAdvertisingState = when (event) {
            is BleAdvertisingEvent.Started ->
                BleAdvertisingState.Advertising(event.set, event.payload)

            BleAdvertisingEvent.Stopped -> BleAdvertisingState.Idle
            null -> current
        }

        return reconcile(settled, desired, nowNanos)
    }

    private fun reconcile(
        state: BleAdvertisingState,
        desired: BleAdvertisingDesiredState,
        nowNanos: Long,
    ): EvaluationResult = when (state) {
        BleAdvertisingState.Idle -> when (desired) {
            is BleAdvertisingDesiredState.Advertising ->
                EvaluationResult(
                    BleAdvertisingState.Starting(attempt = 0),
                    BleAdvertisingEffect.Start(desired.payload),
                )

            BleAdvertisingDesiredState.Idle ->
                EvaluationResult(BleAdvertisingState.Idle, BleAdvertisingEffect.NoOp)
        }

        is BleAdvertisingState.Starting -> when (desired) {
            is BleAdvertisingDesiredState.Advertising ->
                EvaluationResult(state, BleAdvertisingEffect.NoOp)

            BleAdvertisingDesiredState.Idle ->
                EvaluationResult(BleAdvertisingState.Stopping, BleAdvertisingEffect.Stop)
        }

        is BleAdvertisingState.Advertising -> when (desired) {
            is BleAdvertisingDesiredState.Advertising ->
                if (state.payload.contentEquals(desired.payload)) {
                    EvaluationResult(state, BleAdvertisingEffect.NoOp)
                } else {
                    EvaluationResult(
                        state,
                        BleAdvertisingEffect.Update(state.advertisingSet, desired.payload),
                    )
                }

            BleAdvertisingDesiredState.Idle ->
                EvaluationResult(BleAdvertisingState.Stopping, BleAdvertisingEffect.Stop)
        }

        BleAdvertisingState.Stopping -> when (desired) {
            is BleAdvertisingDesiredState.Advertising ->
                EvaluationResult(
                    BleAdvertisingState.Starting(attempt = 0),
                    BleAdvertisingEffect.Start(desired.payload),
                )

            BleAdvertisingDesiredState.Idle ->
                EvaluationResult(BleAdvertisingState.Stopping, BleAdvertisingEffect.NoOp)
        }

        is BleAdvertisingState.Retrying -> when (desired) {
            BleAdvertisingDesiredState.Idle ->
                EvaluationResult(BleAdvertisingState.Idle, BleAdvertisingEffect.NoOp)

            is BleAdvertisingDesiredState.Advertising ->
                if (nowNanos >= state.retryAtNanos) {
                    EvaluationResult(
                        BleAdvertisingState.Starting(attempt = state.attempt),
                        BleAdvertisingEffect.Start(desired.payload),
                    )
                } else {
                    EvaluationResult(state, BleAdvertisingEffect.NoOp)
                }
        }
    }

    private fun scheduleRetry(
        current: BleAdvertisingState,
        error: BleAdvertisingFail,
        desired: BleAdvertisingDesiredState,
        nowNanos: Long,
    ): EvaluationResult {
        if (desired == BleAdvertisingDesiredState.Idle) {
            return EvaluationResult(BleAdvertisingState.Idle, BleAdvertisingEffect.NoOp)
        }

        val attempt = when (current) {
            is BleAdvertisingState.Starting -> current.attempt + 1
            is BleAdvertisingState.Retrying -> current.attempt + 1
            else -> 1
        }
        val delayMillis = RetryBackoff.delayMillis(
            attempt = attempt,
            retryable = error.isRetryable,
            randomValue = Random.nextDouble(),
        )
        val retryAtNanos = nowNanos + delayMillis * NANOS_PER_MILLI

        return EvaluationResult(
            BleAdvertisingState.Retrying(error, attempt, retryAtNanos),
            BleAdvertisingEffect.NoOp,
        )
    }

    private const val NANOS_PER_MILLI = 1_000_000L
}
