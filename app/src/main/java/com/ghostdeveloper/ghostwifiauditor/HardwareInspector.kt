package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Build
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Runtime-only, opt-in and read-only diagnostics. No firmware commands, interface mutation,
 * arbitrary shell parameters, root installation, Wi-Fi packet injection, or persistent root cache.
 */
object HardwareInspector {
    private data class ShellResult(val exit: Int, val text: String, val timedOut: Boolean)

    private fun execute(context: Context, argv: List<String>, timeoutSeconds: Long = 12): ShellResult {
        // Output is redirected to a private file, avoiding pipe deadlocks on large iw dumps.
        val output = File.createTempFile("probe_", ".txt", context.cacheDir)
        return try {
            val process = ProcessBuilder(argv).redirectErrorStream(true)
                .redirectOutput(output).start()
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                process.waitFor(1, TimeUnit.SECONDS)
            }
            ShellResult(
                if (completed) process.exitValue() else -1,
                output.inputStream().bufferedReader().use { it.readText().take(20000) },
                !completed
            )
        } catch (e: Exception) {
            ShellResult(-1, "Excepción: ${e.javaClass.simpleName}", false)
        } finally {
            output.delete()
        }
    }

    suspend fun rootDiagnostic(context: Context): DiagnosticFinding = withContext(Dispatchers.IO) {
        val result = execute(context, listOf("su", "-c", "id"), 15)
        DiagnosticParsers.root(result.text, result.exit, result.timedOut)
    }

    suspend fun monitorDiagnostic(context: Context, rootAllowed: Boolean): DiagnosticFinding =
        withContext(Dispatchers.IO) {
            // Do not execute any command that creates, deletes or alters a network interface.
            val locations = listOf("/system/bin/iw", "/vendor/bin/iw", "/system/xbin/iw")
            val binary = locations.firstOrNull { File(it).canExecute() }
            val result = when {
                binary != null -> execute(context, listOf(binary, "list"), 8)
                rootAllowed -> execute(context, listOf("su", "-c", "iw list"), 10)
                else -> return@withContext DiagnosticFinding(
                    CapabilityStatus.UNKNOWN, "No se encuentra iw",
                    "Buscado en rutas de sistema; root no autorizado para búsqueda alternativa"
                )
            }
            // Permission errors are not evidence of absent monitor support.
            if (result.exit != 0 && rootAllowed && binary != null) {
                val retry = execute(context, listOf("su", "-c", "$binary list"), 10)
                return@withContext DiagnosticParsers.monitor(retry.text, retry.exit, retry.timedOut)
            }
            DiagnosticParsers.monitor(result.text, result.exit, result.timedOut)
        }

    private fun property(context: Context, name: String): String {
        val result = execute(context, listOf("/system/bin/getprop", name), 3)
        return if (result.exit == 0) result.text.trim().take(100).ifBlank { "No disponible" }
        else "No disponible"
    }

    suspend fun deviceInfo(context: Context): DeviceFingerprint = withContext(Dispatchers.IO) {
        val release = runCatching { Os.uname().release }.getOrDefault("No disponible")
        val driver = runCatching {
            File("/proc/modules").useLines { lines ->
                lines.firstOrNull { it.startsWith("wlan_") }?.substringBefore(' ')
            }
        }.getOrNull().orEmpty().ifBlank { property(context, "ro.vendor.wlan.gen") }
        DeviceFingerprint(
            manufacturer = Build.MANUFACTURER.orEmpty().take(40),
            model = Build.MODEL.orEmpty().take(60),
            device = Build.DEVICE.orEmpty().take(60),
            android = Build.VERSION.RELEASE.orEmpty(),
            sdk = Build.VERSION.SDK_INT,
            build = Build.DISPLAY.orEmpty().take(100),
            kernel = release.take(110),
            wlanDriver = driver.take(100),
            wlanFirmware = property(context, "vendor.wlan.firmware.version")
        )
    }

    // Compatibility wrappers for older UI clients. They do not cache results.
    suspend fun rootState(context: Context): String = rootDiagnostic(context).summary
    suspend fun monitorState(context: Context, rootAllowed: Boolean): String =
        monitorDiagnostic(context, rootAllowed).summary

    fun usbDevices(context: Context): List<String> {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return manager.deviceList.values.sortedBy { it.deviceName }.map { usb ->
            val vendor = "%04X".format(usb.vendorId)
            val product = "%04X".format(usb.productId)
            "USB $vendor:$product · ${usb.deviceName.substringAfterLast('/')}"
        }
    }
}
