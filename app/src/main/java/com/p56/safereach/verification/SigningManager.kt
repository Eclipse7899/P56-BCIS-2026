package com.p56.safereach.verification

import android.content.Context
import com.p56.safereach.R
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
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

    fun verify(
        data: ByteArray,
        signature: ByteArray
    ): Boolean {
        val verifier = getSigner()
        verifier.initVerify(publicKey)
        verifier.update(data)
        return verifier.verify(signature)
    }
}