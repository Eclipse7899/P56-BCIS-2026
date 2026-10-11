package com.p56.safereach.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.p56.safereach.model.SignedMessage

@Entity(tableName = "signed_messages")
data class SignedMessageEntity(
    @PrimaryKey
    val id: Int = 0,
    val timestamp: Long,
    val data: ByteArray,
    val signature: ByteArray,
    val receivedFrom: String,
    val receivedAt: Long,
    val cleared: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedMessageEntity) return false
        return timestamp == other.timestamp &&
            data.contentEquals(other.data) &&
            signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

fun SignedMessageEntity.toDomain(): SignedMessage = SignedMessage(
    payload = data,
    signature = signature,
    receivedFrom = receivedFrom,
    receivedAt = receivedAt,
    cleared = cleared,
)

fun SignedMessage.toEntity(
    timestamp: Long,
): SignedMessageEntity = SignedMessageEntity(
    timestamp = timestamp,
    data = payload,
    signature = signature,
    receivedFrom = receivedFrom,
    receivedAt = receivedAt,
    cleared = cleared,
)
