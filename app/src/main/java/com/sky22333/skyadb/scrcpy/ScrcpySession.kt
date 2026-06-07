package com.sky22333.skyadb.scrcpy

import android.content.Context
import android.view.Surface
import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.stream.AdbStream
import java.io.EOFException
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScrcpySession private constructor(
    private val kadb: Kadb,
    private val serverStream: AdbStream,
    private val videoStream: AdbStream,
    private val controlStream: AdbStream,
    val deviceInfo: ScrcpyDeviceInfo,
    val controlClient: ScrcpyControlClient,
    private val decoder: ScrcpyVideoDecoder,
    private val scope: CoroutineScope,
    private val logLines: ArrayDeque<String>,
    private val onError: (Throwable, String) -> Unit,
) {
    fun start() {
        scope.launch { readServerLogs() }
        scope.launch {
            runCatching { decoder.start() }
                .onFailure { error -> onError(error, serverLogTail(maxLines = 20)) }
        }
    }

    fun stop() {
        scope.cancel()
        decoder.stop()
        runCatching { controlStream.close() }
        runCatching { videoStream.close() }
        runCatching { serverStream.close() }
        runCatching { kadb.close() }
    }

    fun serverLogTail(maxLines: Int = 80): String {
        return synchronized(logLines) {
            logLines.takeLast(maxLines.coerceAtLeast(1)).joinToString("\n")
        }
    }

    private suspend fun readServerLogs() = withContext(Dispatchers.IO) {
        while (isActive) {
            val line = try {
                serverStream.source.readUtf8Line() ?: break
            } catch (_: EOFException) {
                break
            } catch (_: Throwable) {
                break
            }
            synchronized(logLines) {
                if (logLines.size >= 120) logLines.removeFirst()
                logLines.addLast(line)
            }
        }
    }

    companion object {
        suspend fun start(
            context: Context,
            kadb: Kadb,
            surface: Surface,
            options: ScrcpyOptions = ScrcpyOptions(),
            onVideoSize: (Int, Int) -> Unit,
            onError: (Throwable, String) -> Unit,
        ): ScrcpySession = withContext(Dispatchers.IO) {
            val serverManager = ScrcpyServerManager(context)
            val logs = ArrayDeque<String>()
            try {
                serverManager.pushServer(kadb)
                val scid = generateScid()
                val socketName = "scrcpy_${scid.toString(16).padStart(8, '0')}"
                val serverStream = kadb.open("shell:${serverManager.buildStartCommand(scid, options)} 2>&1")

                delay(200)
                val videoStream = openLocalAbstractWithRetry(kadb, socketName, expectDummyByte = true)
                val controlStream = openLocalAbstractWithRetry(kadb, socketName, expectDummyByte = false)
                val name = readDeviceName(videoStream)
                val codecId = videoStream.source.readInt()
                val controlClient = ScrcpyControlClient(controlStream)
                val decoder = ScrcpyVideoDecoder(
                    stream = videoStream,
                    codecId = codecId,
                    surface = surface,
                    onVideoSize = { width, height ->
                        controlClient.updateVideoSize(width, height)
                        onVideoSize(width, height)
                    },
                )

                ScrcpySession(
                    kadb = kadb,
                    serverStream = serverStream,
                    videoStream = videoStream,
                    controlStream = controlStream,
                    deviceInfo = ScrcpyDeviceInfo(name = name, codecId = codecId),
                    controlClient = controlClient,
                    decoder = decoder,
                    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                    logLines = logs,
                    onError = onError,
                ).also { it.start() }
            } catch (error: Throwable) {
                runCatching { kadb.close() }
                throw error
            }
        }

        private fun generateScid(): UInt {
            return (Random.nextInt() and 0x7fffffff).toUInt()
        }

        private suspend fun openLocalAbstractWithRetry(
            kadb: Kadb,
            socketName: String,
            expectDummyByte: Boolean,
        ): AdbStream {
            var lastError: Throwable? = null
            repeat(ScrcpyConstants.ConnectRetryCount) {
                try {
                    val stream = kadb.open("localabstract:$socketName")
                    if (expectDummyByte) {
                        val dummy = stream.source.readByte().toInt()
                        if (dummy < 0) throw EOFException("scrcpy dummy byte missing")
                    }
                    return stream
                } catch (error: Throwable) {
                    lastError = error
                    delay(ScrcpyConstants.ConnectRetryDelayMillis)
                }
            }
            throw IllegalStateException("无法连接 scrcpy socket：$socketName", lastError)
        }

        private fun readDeviceName(stream: AdbStream): String {
            val bytes = stream.source.readByteArray(ScrcpyProtocol.DeviceNameLength.toLong())
            val length = bytes.indexOf(0).takeIf { it >= 0 } ?: bytes.size
            return bytes.copyOf(length).toString(Charsets.UTF_8).ifBlank { "Android 设备" }
        }
    }
}
