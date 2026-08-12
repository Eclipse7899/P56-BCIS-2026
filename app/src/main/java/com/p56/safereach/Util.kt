@file:OptIn(ExperimentalSerializationApi::class)

package com.p56.safereach

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf


inline fun <reified T> serialize(message: T): ByteArray {
    return ProtoBuf.encodeToByteArray(message)
}

inline fun <reified T> deserialize(data: ByteArray): T {
    return ProtoBuf.decodeFromByteArray<T>(data)
}

inline fun <reified T> prepareForTransmission(data: T): ByteArray {
    return serialize(data)
}

inline fun <reified T> parseReceivedData(data: ByteArray): T {
    return deserialize<T>(data)
}

const val MS_IN_SECOND = 1000L

fun secondsToMillis(seconds: Long): Long = seconds * MS_IN_SECOND

fun hasPermissions(permissions: Iterable<String>, context: Context): Boolean {
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

sealed interface Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>
    data class Right<out R>(val value: R) : Either<Nothing, R>
}
