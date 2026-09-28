package com.p56.safereach.verification

import android.annotation.SuppressLint
import android.content.Context
import com.p56.safereach.BuildConfig
import com.p56.safereach.R
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.security.PrivateKey
import java.security.PublicKey



@Singleton
class SigningManager {
    private val context: Context

    @Inject
    constructor(@ApplicationContext context: Context) {
        this.context = context
        setupCrypto()
    }

    private val publicKey: PublicKey by lazy {
        context.resources
            .openRawResource(R.raw.public_key)
            .bufferedReader()
            .use { it.readText() }
            .toEd25519PublicKey()
    }

    private val privateKey: PrivateKey? by lazy {
        if (!BuildConfig.DEBUG) {
            null
        } else {
            @SuppressLint("DiscouragedApi")
            val resourceId = context.resources.getIdentifier(
                "signing_key",
                "raw",
                context.packageName
            )

            require(resourceId != 0) {
                "Debug signing key not found"
            }

            context.resources
                .openRawResource(resourceId)
                .bufferedReader()
                .use { it.readText() }
                .toEd25519PrivateKey()
        }
    }

    fun verify(
        data: ByteArray,
        signature: ByteArray
    ): Boolean {
        val verifier = getSigner()
        verifier.initVerify(publicKey)
        verifier.update(data)
        return verifier.verify(signature)
    }

    fun sign(
        data: ByteArray
    ): ByteArray {
        if (privateKey == null) {
            throw IllegalStateException("Signing is not available in release builds")
        }
        val signer = getSigner()
        signer.initSign(privateKey)
        signer.update(data)
        return signer.sign()
    }
}