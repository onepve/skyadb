package com.sky22333.skyadb.adb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.rv882.fastbootjava.FastbootCommand
import com.rv882.fastbootjava.FastbootDeviceContext
import com.rv882.fastbootjava.FastbootResponse
import com.rv882.fastbootjava.transport.UsbTransport
import com.sky22333.skyadb.R
import com.sky22333.skyadb.i18n.appString
import com.sky22333.skyadb.model.AdbOperationResult
import com.sky22333.skyadb.usb.AndroidUsbInterface
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FastbootOtgManager {
    private val mutex = Mutex()
    private var deviceContext: FastbootDeviceContext? = null
    private var activeDeviceName: String? = null

    suspend fun connect(
        usbManager: UsbManager,
        device: UsbDevice,
    ): AdbOperationResult<String> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                disconnectLocked()
                val usbInterface =
                    AndroidUsbInterface.findFastbootInterface(device)
                        ?: return@withContext AdbOperationResult.Failure(
                            message = appString(R.string.fastboot_interface_not_found),
                            suggestion = appString(R.string.fastboot_interface_suggestion),
                        )
                val connection =
                    usbManager.openDevice(device)
                        ?: return@withContext AdbOperationResult.Failure(
                            message = appString(R.string.usb_cannot_open_device),
                            suggestion = appString(R.string.usb_replug_allow_access_suggestion),
                        )
                runCatching {
                    val transport = UsbTransport(usbInterface, connection)
                    val context = FastbootDeviceContext(transport)
                    context.sendCommand(FastbootCommand.getVar(ProbeVariable), ProbeTimeoutMs, true)
                    val status = FastbootResponse.getStatus()
                    if (!isAliveResponse(status)) {
                        context.close()
                        throw IOException(
                            appString(R.string.fastboot_invalid_response_error, status)
                        )
                    }
                    deviceContext = context
                    activeDeviceName = device.deviceName
                    AdbOperationResult.Success("fastboot:${device.deviceName}")
                }
                    .getOrElse { error ->
                        runCatching { connection.close() }
                        AdbOperationResult.Failure(
                            message = appString(R.string.fastboot_connect_failed),
                            suggestion = appString(R.string.fastboot_connect_failed_suggestion),
                            cause = error,
                        )
                    }
            }
        }

    suspend fun sendCommand(command: String): AdbOperationResult<String> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val context =
                    deviceContext
                        ?: return@withContext AdbOperationResult.Failure(
                            message = appString(R.string.fastboot_not_connected),
                            suggestion =
                                appString(R.string.fastboot_connect_via_usb_otg_suggestion),
                        )
                val normalized = command.trim()
                if (normalized.isEmpty()) {
                    return@withContext AdbOperationResult.Failure(
                        message = appString(R.string.fastboot_command_empty),
                        suggestion = appString(R.string.fastboot_command_empty_suggestion),
                    )
                }
                runCatching {
                    context.sendCommand(
                        normalized.toByteArray(Charsets.UTF_8),
                        CommandTimeoutMs,
                        true,
                    )
                    val status = FastbootResponse.getStatus()
                    if (!isAliveResponse(status)) {
                        throw IOException(
                            appString(R.string.fastboot_invalid_response_error, status)
                        )
                    }
                    val data = FastbootResponse.getData().trim { it <= ' ' || it == '\u0000' }
                    if (!isSuccessfulResponse(status)) {
                        throw IOException("$status: $data")
                    }
                    AdbOperationResult.Success("$status: $data")
                }
                    .getOrElse { error ->
                        AdbOperationResult.Failure(
                            message = appString(R.string.fastboot_command_failed),
                            suggestion =
                                error.message
                                    ?: appString(R.string.fastboot_command_failed_suggestion),
                            cause = error,
                        )
                    }
            }
        }

    suspend fun disconnect() = withContext(Dispatchers.IO) { mutex.withLock { disconnectLocked() } }

    private fun disconnectLocked() {
        runCatching { deviceContext?.close() }
        deviceContext = null
        activeDeviceName = null
    }

    fun currentDeviceName(): String? = activeDeviceName

    companion object {
        private const val ProbeVariable = "product"
        private const val ProbeTimeoutMs = 3_000
        private const val CommandTimeoutMs = 5_000

        fun isSuccessfulResponse(status: FastbootResponse.ResponseStatus): Boolean =
            status == FastbootResponse.ResponseStatus.OKAY

        fun isAliveResponse(status: FastbootResponse.ResponseStatus): Boolean {
            return status == FastbootResponse.ResponseStatus.OKAY ||
                status == FastbootResponse.ResponseStatus.FAIL ||
                status == FastbootResponse.ResponseStatus.INFO ||
                status == FastbootResponse.ResponseStatus.DATA
        }
    }
}
