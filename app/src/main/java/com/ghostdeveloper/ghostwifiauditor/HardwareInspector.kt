package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Observational diagnostics only. No kernel patching, interface mutation, injection or firmware commands. */
object HardwareInspector {
    private fun shell(command: String, needRoot: Boolean = false): String {
        val argv = if (needRoot) arrayOf("su", "-c", command)
                   else arrayOf("/system/bin/sh", "-c", command)
        return try {
            val p = ProcessBuilder(*argv).redirectErrorStream(true).start()
            if (!p.waitFor(4, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                return "Tiempo de espera agotado"
            }
            p.inputStream.bufferedReader().use { it.readText().take(20000) }.trim()
        } catch (e: Exception) { "No disponible: ${e.javaClass.simpleName}" }
    }

    suspend fun rootState(): String = withContext(Dispatchers.IO) {
        val text = shell("id", true)
        when {
            text.contains("uid=0(root)") -> "Root confirmado (sesión actual)"
            text.startsWith("Tiempo") -> "Root: sin respuesta"
            else -> "Root no concedido: ${text.take(150)}"
        }
    }

    suspend fun monitorState(): String = withContext(Dispatchers.IO) {
        val root = shell("id", true).contains("uid=0(root)")
        val raw = if (root) shell("/system/bin/iw list", true) else shell("/system/bin/iw list")
        if (raw.startsWith("No disponible") || raw.contains("not found") || raw.startsWith("Tiempo")) {
            "No verificable: 'iw' no disponible"
        } else {
            // Only read Supported interface modes, not capabilities in other sections.
            val modes = raw.substringAfter("Supported interface modes:", "")
                .substringBefore("Band 1:").substringBefore("software interface modes")
            if (Regex("(?m)^\\s*\\* monitor\\s*$").containsMatchIn(modes))
                "Modo monitor anunciado (requiere prueba funcional)"
            else "Modo monitor no anunciado por el driver"
        }
    }

    fun usbDevices(context: Context): List<String> {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return manager.deviceList.values.sortedBy { it.deviceName }.map { usb ->
            val vendor = "%04X".format(usb.vendorId)
            val product = "%04X".format(usb.productId)
            "USB $vendor:$product · ${usb.deviceName.substringAfterLast('/')}"
        }
    }
}
