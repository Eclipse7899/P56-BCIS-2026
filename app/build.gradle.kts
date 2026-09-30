import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.FileWriter
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.protobuf)
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
    sourceSets {
        named("main") {
            proto {
                srcDir("../protobuf")
            }
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobufCompiler.get()}"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                id("kotlin") {
                    option("lite")
                }
                id("java") {
                    option("lite")
                }
            }
        }
    }
}

fun writePem(label: String, derBytes: ByteArray, file: File) {
    val base64 = Base64.getMimeEncoder(64, "\n".toByteArray())
        .encodeToString(derBytes)

    val pem = "-----BEGIN $label-----\n$base64\n-----END $label-----\n"
    file.writeText(pem)
}

tasks.register("generateSigningKeysDebug") {
    group = "security"
    description = "Generates an Ed25519 signing key pair for debug builds"

    val privateKeyFile = file("src/debug/res/raw/signing_key.pem")
    val publicKeyFile = file("src/debug/res/raw/public_key.pem")

    outputs.files(privateKeyFile, publicKeyFile)

    onlyIf {
        !privateKeyFile.exists() || !publicKeyFile.exists()
    }

    doLast {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }

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
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.mapbox.sdk)
    implementation(libs.mapbox.compose)
    implementation(libs.mapbox.turf)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)
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
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
