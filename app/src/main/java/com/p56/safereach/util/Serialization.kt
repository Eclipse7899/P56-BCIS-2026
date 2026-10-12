package com.p56.safereach.util


import com.google.protobuf.InvalidProtocolBufferException
import com.p56.safereach.model.SignedMessage
import com.p56.safereach.model.toDomain
import com.p56.safereach.proto.SignedMessageDto
import okio.ByteString.Companion.decodeBase64

fun msgFromFirebase(base64: String): Result<SignedMessage> {
    val bytes = base64.decodeBase64()
        ?: return Result.failure(IllegalArgumentException("Invalid base64 string"))
    return msgFromBytes(bytes.toByteArray(), "Firebase", System.currentTimeMillis())
}

fun msgFromMesh(bytes: ByteArray, fromAddress: String, at: Long): Result<SignedMessage> {
    return msgFromBytes(bytes, fromAddress, at)
}

private fun msgFromBytes(bytes: ByteArray, from: String, at: Long): Result<SignedMessage> {
    try {
        val dto = SignedMessageDto.parseFrom(bytes)
        val model = dto.toDomain(from, at)
        return Result.success(model)
    } catch (e: InvalidProtocolBufferException) {
        return Result.failure(e)
    }
}