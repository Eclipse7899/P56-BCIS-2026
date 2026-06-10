package com.p56.safereach

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