package com.p56.safereach

import com.p56.safereach.Constants.EXPIRY_TICK_MARGIN_MS
import com.p56.safereach.proto.Alert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration.Companion.milliseconds

fun Alert.isExpired() = this.expiresAt > 0L && this.expiresAt < System.currentTimeMillis()

fun Alert.isSuperseded(other: Alert): Boolean {
    return this.version < other.version
}

fun nextExpiryAt(alerts: Iterable<Alert>, now: Long = System.currentTimeMillis()): Long? =
    alerts.asSequence()
        .map { it.expiresAt }
        .filter { it > now }
        .minOrNull()


@OptIn(ExperimentalCoroutinesApi::class)
fun expiryTicks(alerts: Flow<Iterable<Alert>>): Flow<Unit> = alerts.transformLatest { snapshot ->
    while (true) {
        val now = System.currentTimeMillis()
        val nextExpiry = nextExpiryAt(snapshot, now) ?: return@transformLatest
        delay((nextExpiry - now + EXPIRY_TICK_MARGIN_MS).milliseconds)
        emit(Unit)
    }
}
