package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import org.json.JSONArray

/**
 * Hardware references are only identification candidates.
 * No USB ID by itself proves an external Wi-Fi interface or monitor mode works.
 */
data class UsbAdapterProfile(
    val id: String,
    val vendorId: Int,
    val productId: Int,
    val candidateChipset: String,
    val linuxDriver: String,
    val firmware: String,
    val bands: String,
    val modelExamples: String,
    val caveat: String,
    val sourceUrl: String
)

object UsbProfiles {
    fun match(profiles: List<UsbAdapterProfile>, vendorId: Int, productId: Int): UsbAdapterProfile? =
        profiles.firstOrNull { it.vendorId == vendorId && it.productId == productId }

    fun formatId(vendorId: Int, productId: Int): String =
        "%04X:%04X".format(vendorId and 0xffff, productId and 0xffff)

    fun driverEvidence(profile: UsbAdapterProfile?, loadedModuleNames: Set<String>?): String {
        if (profile == null) return "Chipset por identificar; no hay controlador verificado"
        if (loadedModuleNames == null) return "Módulos del kernel no consultables"
        val driver = profile.linuxDriver
        // A loaded driver is a clue only. It may serve a different USB device.
        return if (loadedModuleNames.contains(driver)) {
            "Módulo $driver cargado (no demuestra asociación ni modo monitor)"
        } else {
            "No se encontró $driver entre los módulos cargados; podría estar integrado en el kernel"
        }
    }

    fun load(context: Context): List<UsbAdapterProfile> = runCatching {
        val json = context.assets.open("usb_adapters.v1.json")
            .bufferedReader().use { it.readText() }
        val data = JSONArray(json)
        List(data.length()) { index ->
            val item = data.getJSONObject(index)
            UsbAdapterProfile(
                id = item.getString("id"),
                vendorId = item.getString("vid").toInt(16),
                productId = item.getString("pid").toInt(16),
                candidateChipset = item.getString("chipset"),
                linuxDriver = item.getString("driver"),
                firmware = item.optString("firmware", "Consultar fabricante"),
                bands = item.optString("bands", "Por verificar"),
                modelExamples = item.getString("examples"),
                caveat = item.getString("caveat"),
                sourceUrl = item.getString("source")
            )
        }
    }.getOrDefault(emptyList())
}
