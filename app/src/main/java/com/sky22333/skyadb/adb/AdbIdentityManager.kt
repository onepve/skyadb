package com.sky22333.skyadb.adb

import android.content.Context
import android.util.Log
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.KadbCertPolicy
import com.flyfishxu.kadb.cert.OkioFilePrivateKeyStore
import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import okio.Path.Companion.toPath
import org.bouncycastle.jce.provider.BouncyCastleProvider

object AdbIdentityManager {
    private const val Tag = "SkyadbIdentity"
    private const val CrtError = "Only RSA private keys with CRT parameters are supported"

    fun initialize(context: Context) {
        installBouncyCastleProvider()
        configureKadbIdentity(context)
        ensureIdentityReady()
    }

    fun repairIdentity(): Boolean {
        return runCatching {
            KadbCert.clear()
            KadbCert.rotate()
            true
        }.getOrElse { error ->
            Log.w(Tag, "ADB identity repair failed", error)
            false
        }
    }

    fun isRsaCrtError(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current.message?.contains(CrtError, ignoreCase = true) == true) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun installBouncyCastleProvider() {
        val position = runCatching {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }.getOrElse { error ->
            Log.w(Tag, "BouncyCastle provider install failed", error)
            -1
        }
        logRsaProviderHealth(position)
    }

    private fun configureKadbIdentity(context: Context) {
        val keyFile = File(context.filesDir, "adb_identity/adbkey.pem")
        KadbCert.configure(
            store = OkioFilePrivateKeyStore(keyFile.absolutePath.toPath()),
            policy = KadbCertPolicy(autoHealInvalidPrivateKey = true),
        )
    }

    private fun ensureIdentityReady() {
        runCatching {
            KadbCert.ensureReady()
        }.recoverCatching { error ->
            if (isRsaCrtError(error)) {
                KadbCert.clear()
                KadbCert.rotate()
            } else {
                throw error
            }
        }.onFailure { error ->
            Log.w(Tag, "ADB identity initialization failed", error)
        }
    }

    private fun logRsaProviderHealth(insertedAt: Int) {
        runCatching {
            val keyPair = KeyPairGenerator.getInstance("RSA").apply {
                initialize(512, SecureRandom.getInstance("SHA1PRNG"))
            }.generateKeyPair()
            val factory = KeyFactory.getInstance("RSA")
            val parsed = factory.generatePrivate(PKCS8EncodedKeySpec(keyPair.private.encoded))
            Log.i(Tag, "BC insertedAt=$insertedAt, RSA provider=${factory.provider.name}, isCrt=${parsed is RSAPrivateCrtKey}")
        }.onFailure { error ->
            Log.w(Tag, "RSA provider health check failed", error)
        }
    }
}
