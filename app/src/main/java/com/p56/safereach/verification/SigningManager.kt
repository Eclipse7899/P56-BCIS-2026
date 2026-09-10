package com.p56.safereach.verification

import android.content.Context
import com.p56.safereach.R
import com.p56.safereach.SignedMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.security.PrivateKey
import java.security.PublicKey


@Singleton
class SigningManager @Inject constructor(
    @ApplicationContext context: Context
){

    private val publicKey: PublicKey by lazy {
        context.resources
            .openRawResource(R.raw.public_key)
            .bufferedReader()
            .use { it.readText() }
            .toEd25519PublicKey()
    }

    private val privateKey: PrivateKey by lazy {
        context.resources
            .openRawResource(R.raw.signing_key)
            .bufferedReader()
            .use { it.readText() }
            .toEd25519PrivateKey()
    }

    fun verify(
        signed: SignedMessage,
    ): Boolean {
        return verify(signed, publicKey)
    }

    fun sign(
        data: ByteArray
    ): SignedMessage = sign(
        data = data,
        privateKey = privateKey
    )
}