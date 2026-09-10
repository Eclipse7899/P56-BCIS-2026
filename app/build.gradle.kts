import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.FileWriter
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.p56.safereach"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.p56.safereach"
        minSdk = 29
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

fun writePem(label: String, derBytes: ByteArray, file: File) {
    val base64 = Base64.getMimeEncoder(64, "\n".toByteArray())
        .encodeToString(derBytes)

    val pem = "-----BEGIN $label-----\n$base64\n-----END $label-----\n"
    file.writeText(pem)
}

tasks.register("generateSigningKeys") {
    group = "security"
    description = "Generates an Ed25519 signing key pair for debug builds"

    val privateKeyFile = file("src/debug/res/raw/signing_key.pem")
    val publicKeyFile = file("src/main/res/raw/public_key.pem")

    outputs.files(privateKeyFile, publicKeyFile)

    doLast {
        if (privateKeyFile.exists() || publicKeyFile.exists()) {
            throw GradleException(
                """
                Signing keys already exist.

                Private key: ${privateKeyFile.path}
                Public key:  ${publicKeyFile.path}

                Delete them manually if you really want to regenerate them.
                """.trimIndent()
            )
        }

        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        println("Generating Ed25519 key pair...")

        val kpg = KeyPairGenerator.getInstance("Ed25519", "BC")
        val keyPair = kpg.generateKeyPair()

        val privateKey = keyPair.private
        val publicKey = keyPair.public

        FileWriter(privateKeyFile).use { writer ->
            JcaPEMWriter(writer).use { pemWriter ->
                pemWriter.writeObject(privateKey)
            }
        }

        FileWriter(publicKeyFile).use { writer ->
            JcaPEMWriter(writer).use { pemWriter ->
                pemWriter.writeObject(publicKey)
            }
        }

        println()
        println("Ed25519 key pair generated successfully.")
        println()
        println("Private key:")
        println("  ${privateKeyFile.path}")
        println()
        println("Public key:")
        println("  ${publicKeyFile.path}")
        println()
        println("IMPORTANT: Do NOT commit the private key.")
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.mapbox.sdk)
    implementation(libs.mapbox.compose)
    implementation(libs.mapbox.turf)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.play.services.location)
    implementation(libs.hilt)
    implementation(libs.hilt.compose)
    implementation(libs.bouncycastle)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
