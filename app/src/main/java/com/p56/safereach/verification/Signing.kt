package com.p56.safereach.verification

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


fun getSigner(): Signature {
    return Signature.getInstance(ED25519, BC)
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
