@file:OptIn(ExperimentalSerializationApi::class)

package com.p56.safereach

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf


data class CompressedData(
    val data: ByteArray,
    val length: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompressedData

        if (length != other.length) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = length
        result = 31 * result + data.contentHashCode()
        return result
    }
}

@Serializable
data class Signed<T>(
    val message: T,
    val signature: String,
    val algorithm: String
)

private fun <T> sign(message: T): Signed<T> {
    return Signed(
        message = message,
        signature = "dummy_signature",
        algorithm = "dummy_algorithm"
    )
}

private fun <T> verify(signed: Signed<T>): Boolean {
    return true
}

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



