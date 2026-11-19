package com.ghostdeveloper.ghostwifiauditor

/**
 * An observed status is not a promise about future boots or other firmware builds.
 * UNKNOWN means insufficient evidence; ERROR means a probe failed unexpectedly.
 */
enum class CapabilityStatus { SUPPORTED, UNSUPPORTED, UNKNOWN, ERROR }

data class DiagnosticFinding(
    val status: CapabilityStatus,
    val summary: String,
    val evidence: String,
    val observedAtEpochMs: Long = System.currentTimeMillis()
)

data class DeviceFingerprint(
    val manufacturer: String,
    val model: String,
    val device: String,
    val android: String,
    val sdk: Int,
    val build: String,
    val kernel: String,
    val wlanDriver: String,
    val wlanFirmware: String
) {
    // Never include serial, IMEI, MAC address, Wi-Fi passwords or complete fingerprint.
    fun publicSummary(): String = listOf(
        "Model: $manufacturer $model", "Android: $android (SDK $sdk)",
        "Kernel: $kernel", "WLAN: $wlanDriver",
        "WLAN firmware: $wlanFirmware"
    ).joinToString("\n")
}

object DiagnosticParsers {
    private val modeLine = Regex("^\\s*\\*\\s*([^\\s]+)\\s*$")
    private val supportedHeader = Regex("^\\s*Supported interface modes:\\s*$")

    /** Only the Supported interface modes section counts; RX/TX or software modes do not. */
    fun monitorModes(output: String): Set<String>? {
        val lines = output.lineSequence().toList()
        val start = lines.indexOfFirst { supportedHeader.matches(it) }
        if (start < 0) return null
        val modes = linkedSetOf<String>()
        for (line in lines.drop(start + 1)) {
            val item = modeLine.matchEntire(line)
            if (item != null) {
                modes.add(item.groupValues[1])
            } else if (line.isNotBlank()) {
                // Stop at the first following section. Never scan unrelated sections.
                break
            }
        }
        return modes.ifEmpty { null }
    }

    fun monitor(output: String, exitCode: Int, timedOut: Boolean): DiagnosticFinding {
        if (timedOut) return DiagnosticFinding(CapabilityStatus.ERROR, "Consulta Wi-Fi agotó el tiempo", "iw list: timeout")
        if (exitCode != 0) return DiagnosticFinding(CapabilityStatus.UNKNOWN, "No se pudo consultar el controlador", "iw list: exit=$exitCode; ${output.take(180)}")
        val modes = monitorModes(output) ?: return DiagnosticFinding(
            CapabilityStatus.UNKNOWN, "No hay una lista de modos interpretable",
            "No apareció la sección Supported interface modes"
        )
        val advertised = "monitor" in modes
        return DiagnosticFinding(
            if (advertised) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            if (advertised) "Monitor anunciado (no verificado funcionalmente)" else "Monitor no anunciado por el controlador",
            "iw list: " + modes.joinToString(", ")
        )
    }

    fun root(output: String, exitCode: Int, timedOut: Boolean): DiagnosticFinding {
        if (timedOut) return DiagnosticFinding(CapabilityStatus.ERROR, "La solicitud de root agotó el tiempo", "su -c id: timeout")
        if (exitCode == 0 && Regex("\\buid=0\\(root\\)").containsMatchIn(output)) {
            return DiagnosticFinding(CapabilityStatus.SUPPORTED, "Root confirmado en esta sesión", "su -c id: uid=0")
        }
        return DiagnosticFinding(
            CapabilityStatus.UNKNOWN,
            "Root no concedido o no disponible",
            "su -c id: exit=$exitCode; ${output.take(120)}"
        )
    }
}
