package com.sky22333.skyadb.scrcpy

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import com.sky22333.skyadb.adb.KadbManager
import com.sky22333.skyadb.diagnostics.DiagnosticLogger
import com.sky22333.skyadb.diagnostics.DiagnosticModule
import com.sky22333.skyadb.model.AdbOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScrcpyRepository(
    private val context: Context,
    private val kadbManager: KadbManager,
) {
    private var session: ScrcpySession? = null

    suspend fun start(
        surface: Surface,
        qualityPreset: MirrorQualityPreset = MirrorQualityPreset.Balanced,
        onVideoSize: (Int, Int) -> Unit,
        onStreamError: (Throwable) -> Unit = {},
    ): AdbOperationResult<ScrcpyDeviceInfo> = withContext(Dispatchers.IO) {
        stop()
        val options = qualityPreset.options
        val optionsText = options.diagnosticText()
        val kadb = kadbManager.createStreamingClient()
            ?: return@withContext AdbOperationResult.Failure(
                message = "未连接设备",
                suggestion = "请先连接设备，再启动屏幕镜像。",
            )

        runCatching {
            ScrcpySession.start(
                context = context,
                kadb = kadb,
                surface = surface,
                options = options,
                onVideoSize = onVideoSize,
                onError = { error, serverLog ->
                    DiagnosticLogger.record(
                        module = DiagnosticModule.Mirror,
                        operation = "视频流",
                        target = kadbManager.currentEndpoint(),
                        message = "屏幕镜像视频流异常",
                        suggestion = mirrorDiagnosticSuggestion(qualityPreset, optionsText, serverLog),
                        cause = error,
                    )
                    stop()
                    onStreamError(error)
                },
            ).also { session = it }
        }.fold(
            onSuccess = { AdbOperationResult.Success(it.deviceInfo) },
            onFailure = { error ->
                DiagnosticLogger.record(
                    module = DiagnosticModule.Mirror,
                    operation = "启动镜像",
                    target = kadbManager.currentEndpoint(),
                    message = "屏幕镜像启动失败",
                    suggestion = mirrorDiagnosticSuggestion(qualityPreset, optionsText),
                    cause = error,
                )
                AdbOperationResult.Failure(
                    message = "屏幕镜像启动失败",
                    suggestion = error.message ?: "请查看设置里的诊断日志。",
                    cause = error,
                )
            },
        )
    }

    fun sendTouch(event: MotionEvent, surfaceWidth: Int, surfaceHeight: Int) {
        runCatching {
            session?.controlClient?.sendTouch(event, surfaceWidth, surfaceHeight)
        }.onFailure { error ->
            DiagnosticLogger.record(
                module = DiagnosticModule.Mirror,
                operation = "发送触摸",
                message = "远程触摸发送失败",
                suggestion = "镜像连接可能已断开，请重新进入屏幕镜像。",
                cause = error,
            )
        }
    }

    fun sendKey(keyCode: Int) {
        runCatching {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                session?.controlClient?.sendBackOrScreenOn()
            } else {
                session?.controlClient?.sendKey(keyCode)
            }
        }.onFailure { error ->
            DiagnosticLogger.record(
                module = DiagnosticModule.Mirror,
                operation = "发送按键",
                message = "远程按键发送失败",
                suggestion = "镜像连接可能已断开，请重新进入屏幕镜像。",
                cause = error,
            )
        }
    }

    fun sendText(text: String) {
        runCatching { session?.controlClient?.sendText(text) }
            .onFailure { error ->
                DiagnosticLogger.record(
                    module = DiagnosticModule.Mirror,
                    operation = "发送文本",
                    message = "远程文本发送失败",
                    suggestion = "镜像连接可能已断开，请重新进入屏幕镜像。",
                    cause = error,
                )
            }
    }

    fun stop() {
        runCatching { session?.stop() }
            .onFailure { error ->
                DiagnosticLogger.record(
                    module = DiagnosticModule.Mirror,
                    operation = "停止镜像",
                    message = "释放屏幕镜像资源失败",
                    suggestion = "如果再次启动异常，请重新连接设备。",
                    cause = error,
                )
            }
        session = null
    }

    private fun mirrorDiagnosticSuggestion(
        qualityPreset: MirrorQualityPreset,
        optionsText: String,
        serverLog: String = "",
    ): String {
        val base = "当前画质：${qualityPreset.label}。启动参数：$optionsText。请重新进入屏幕镜像；如果持续失败，请切换到流畅画质。"
        return if (serverLog.isBlank()) {
            base
        } else {
            "$base\nscrcpy server 日志：\n${serverLog.take(ServerLogDiagnosticMaxChars)}"
        }
    }

    private companion object {
        const val ServerLogDiagnosticMaxChars = 300
    }
}
