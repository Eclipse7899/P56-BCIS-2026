package com.p56.safereach.verification

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64

private const val ED25519 = "Ed25519"

private val bcProvider = BouncyCastleProvider()

fun getSigner(): Signature {
    return Signature.getInstance(ED25519, bcProvider)
}

fun String.toEd25519PublicKey(): PublicKey {
    val bytes = pemBody("PUBLIC KEY")

    return KeyFactory
        .getInstance(ED25519, bcProvider)
        .generatePublic(X509EncodedKeySpec(bytes))
}

private fun String.pemBody(label: String): ByteArray {
    val base64 = this
        .replace("-----BEGIN $label-----", "")
        .replace("-----END $label-----", "")
        .filterNot { it.isWhitespace() }

    return Base64.decode(base64)
}
