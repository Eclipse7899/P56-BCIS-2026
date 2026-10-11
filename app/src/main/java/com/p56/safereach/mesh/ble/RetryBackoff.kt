package com.p56.safereach.mesh.ble

import kotlin.math.pow
import kotlin.random.Random

class RetryBackoff(
    private val initialDelayMillis: Long = DEFAULT_INITIAL_DELAY_MILLIS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS,
    private val multiplier: Double = DEFAULT_MULTIPLIER,
    private val jitterRatio: Double = DEFAULT_JITTER_RATIO,
    private val random: () -> Double = { Random.nextDouble() },
) {
    var attempt: Int = 0
        private set

    fun nextDelayMillis(retryable: Boolean = true): Long {
        attempt += 1
        return delayMillis(
            attempt = attempt,
            retryable = retryable,
            initialDelayMillis = initialDelayMillis,
            maxDelayMillis = maxDelayMillis,
            multiplier = multiplier,
            jitterRatio = jitterRatio,
            randomValue = random(),
        )
    }

    fun reset() {
        attempt = 0
    }

    companion object {
        const val DEFAULT_INITIAL_DELAY_MILLIS: Long = 1_000L
        const val DEFAULT_MAX_DELAY_MILLIS: Long = 60_000L
        const val DEFAULT_MULTIPLIER: Double = 2.0
        const val DEFAULT_JITTER_RATIO: Double = 0.2

        fun delayMillis(
            attempt: Int,
            retryable: Boolean = true,
            initialDelayMillis: Long = DEFAULT_INITIAL_DELAY_MILLIS,
            maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS,
            multiplier: Double = DEFAULT_MULTIPLIER,
            jitterRatio: Double = DEFAULT_JITTER_RATIO,
            randomValue: Double = 0.5,
        ): Long {
            if (!retryable) return maxDelayMillis

            val exponent = (attempt - 1).coerceAtLeast(0)
            val base = (initialDelayMillis.toDouble() * multiplier.pow(exponent.toDouble()))
                .coerceAtMost(maxDelayMillis.toDouble())
            val jitterFactor = 1.0 + jitterRatio * (2.0 * randomValue - 1.0)
            return (base * jitterFactor)
                .coerceIn(0.0, maxDelayMillis.toDouble())
                .toLong()
        }
    }
}
