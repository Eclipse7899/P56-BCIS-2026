package com.p56.safereach.verification

import com.p56.safereach.SignedMessage
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64

fun verify(
    signed: SignedMessage,
    publicKey: PublicKey
): Boolean {
    val verifier = Signature.getInstance("Ed25519")
    verifier.initVerify(publicKey)
    verifier.update(signed.data)

    return verifier.verify(signed.signature)
}

fun sign(data: ByteArray, privateKey: PrivateKey): SignedMessage {
    val signer = Signature.getInstance("Ed25519")
    signer.initSign(privateKey)
    signer.update(data)

    return SignedMessage(
        data = data,
        signature = signer.sign()
    )
}

fun String.toEd25519PublicKey(): PublicKey {
    val base64 = this
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\s".toRegex(), "")

    val bytes = Base64.decode(base64)

    return KeyFactory
        .getInstance("Ed25519")
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
        .getInstance("Ed25519")
        .generatePrivate(keySpec)
}