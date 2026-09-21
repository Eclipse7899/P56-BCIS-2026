package com.p56.safereach.verification

import com.google.protobuf.ByteString
import com.p56.safereach.proto.SignedMessage
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64

private const val BC = "BC"
private const val ED25519 = "Ed25519"

fun setupCrypto() {
    Security.removeProvider(BC)
    Security.insertProviderAt(BouncyCastleProvider(), 1)
}

fun verify(
    signed: SignedMessage,
    publicKey: PublicKey
): Boolean {
    val verifier = Signature.getInstance(ED25519, BC)
    verifier.initVerify(publicKey)
    verifier.update(signed.data.toByteArray())

    return verifier.verify(signed.signature.toByteArray())
}

fun sign(data: ByteArray, privateKey: PrivateKey): SignedMessage {
    val signer = Signature.getInstance(ED25519, BC)
    signer.initSign(privateKey)
    signer.update(data)

    return SignedMessage.newBuilder()
        .setData(ByteString.copyFrom(data))
        .setSignature(ByteString.copyFrom(signer.sign()))
        .build()
}

fun String.toEd25519PublicKey(): PublicKey {
    val base64 = this
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\s".toRegex(), "")

    val bytes = Base64.decode(base64)

    return KeyFactory
        .getInstance(ED25519, BC)
        .generatePublic(X509EncodedKeySpec(bytes))
}

fun String.toEd25519PrivateKey(): PrivateKey {
    val base64 = this
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\\s".toRegex(), "")

    val keyBytes = Base64.decode(base64)

    val keySpec = PKCS8EncodedKeySpec(keyBytes)

    return KeyFactory
        .getInstance(ED25519, BC)
        .generatePrivate(keySpec)
}