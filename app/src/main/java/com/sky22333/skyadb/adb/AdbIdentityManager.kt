package com.sky22333.skyadb.adb

import android.content.Context
import android.util.Log
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.KadbCertPolicy
import com.flyfishxu.kadb.cert.OkioFilePrivateKeyStore
import com.sky22333.skyadb.diagnostics.DiagnosticLogger
import com.sky22333.skyadb.diagnostics.DiagnosticModule
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
            DiagnosticLogger.record(
                module = DiagnosticModule.App,
                operation = "修复 ADB 身份",
                message = "ADB 身份密钥修复失败",
                suggestion = "请重启应用后重试连接；如果仍失败，可以重新安装应用后再次授权目标设备。",
                cause = error,
            )
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
            DiagnosticLogger.record(
                module = DiagnosticModule.App,
                operation = "初始化加密 Provider",
                message = "BouncyCastle Provider 注册失败",
                suggestion = "旧版 Android 设备可能无法完成 ADB 身份认证，请更新系统或反馈完整诊断日志。",
                cause = error,
            )
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
            DiagnosticLogger.record(
                module = DiagnosticModule.App,
                operation = "初始化 ADB 身份",
                message = "ADB 身份初始化失败",
                suggestion = "请重启应用后重试连接；如果目标设备弹出授权窗口，请重新允许调试授权。",
                cause = error,
            )
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
            DiagnosticLogger.record(
                module = DiagnosticModule.App,
                operation = "检查 RSA Provider",
                message = "RSA Provider 自检失败",
                suggestion = "旧版 Android 设备可能无法生成可用的 ADB 身份密钥，请反馈完整诊断日志。",
                cause = error,
            )
        }
    }
}
