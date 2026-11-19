package com.ghostdeveloper.ghostwifiauditor

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import java.io.File

const val ACTION_GHOST_USB_PERMISSION = "com.ghostdeveloper.ghostwifiauditor.USB_PERMISSION"
const val ACTION_GHOST_USB_CHANGED = "com.ghostdeveloper.ghostwifiauditor.USB_CHANGED"

data class UsbAdapterSnapshot(
    val deviceName: String,
    val usbId: String,
    val description: String,
    val deviceClass: Int,
    val interfaces: Int,
    val permissionGranted: Boolean,
    val profile: UsbAdapterProfile?,
    val driverEvidence: String
)

/** Android's USB Host API supplies enumeration and optional permission; not a Wi-Fi driver. */
object UsbAdapterInspector {
    fun enumerate(context: Context, profiles: List<UsbAdapterProfile>): List<UsbAdapterSnapshot> {
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        val loaded = loadedModules()
        return manager.deviceList.values.sortedBy { it.deviceName }.map { usb ->
            val profile = UsbProfiles.match(profiles, usb.vendorId, usb.productId)
            val manufacturer = runCatching { usb.manufacturerName.orEmpty() }.getOrDefault("")
            val product = runCatching { usb.productName.orEmpty() }.getOrDefault("")
            val label = listOf(manufacturer, product).filter { it.isNotBlank() }
                .joinToString(" ").take(100).ifBlank { "Dispositivo USB" }
            UsbAdapterSnapshot(
                deviceName = usb.deviceName,
                usbId = UsbProfiles.formatId(usb.vendorId, usb.productId),
                description = label,
                deviceClass = usb.deviceClass,
                interfaces = usb.interfaceCount,
                permissionGranted = manager.hasPermission(usb),
                profile = profile,
                driverEvidence = UsbProfiles.driverEvidence(profile, loaded)
            )
        }
    }

    private fun loadedModules(): Set<String>? = runCatching {
        File("/proc/modules").useLines { lines ->
            lines.mapNotNull { it.split(' ').firstOrNull()?.takeIf(String::isNotBlank) }
                .toSet()
        }
    }.getOrNull()

    fun requestAccess(context: Context, deviceName: String): String {
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return "USB Host no disponible"
        val usb: UsbDevice = manager.deviceList[deviceName] ?: return "Dispositivo desconectado"
        if (manager.hasPermission(usb)) return "Permiso USB ya concedido"
        val intent = Intent(context, UsbPermissionReceiver::class.java).apply {
            action = ACTION_GHOST_USB_PERMISSION
        }
        // Android injects USB_DEVICE + PERMISSION_GRANTED into this PendingIntent.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return runCatching {
            val request = PendingIntent.getBroadcast(context, 0, intent, flags)
            manager.requestPermission(usb, request)
            "Solicitud enviada al sistema Android"
        }.getOrElse { "No fue posible solicitar permiso USB: ${it.javaClass.simpleName}" }
    }
}

/**
 * Explicit, non-exported receiver: Android may fill the permission result.
 * We trust only UsbManager.hasPermission() when the UI refreshes.
 */
class UsbPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GHOST_USB_PERMISSION) return
        context.sendBroadcast(
            Intent(ACTION_GHOST_USB_CHANGED).setPackage(context.packageName)
        )
    }
}
