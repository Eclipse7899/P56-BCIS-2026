package com.p56.safereach.model

data class SignedMessage(
    val payload: ByteArray,
    val signature: ByteArray,
    val receivedFrom: String,
    val receivedAt: Long,
    val cleared: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedMessage) return false
        return payload.contentEquals(other.payload) && signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var result = payload.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}
