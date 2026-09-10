package com.p56.safereach.verification

import android.content.Context
import com.p56.safereach.BuildConfig
import com.p56.safereach.R
import com.p56.safereach.SignedMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security


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
        signed: SignedMessage,
    ): Boolean {
        return verify(signed, publicKey)
    }

    fun sign(
        data: ByteArray
    ): SignedMessage {
        if (privateKey == null){
            throw IllegalStateException("Signing is not available in release builds")
        }
        return sign(
            data = data,
            privateKey = privateKey!!
        )
    }
}