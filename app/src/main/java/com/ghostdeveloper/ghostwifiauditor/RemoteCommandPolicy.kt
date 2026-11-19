package com.ghostdeveloper.ghostwifiauditor

/**
 * Strict, testable policy for the only commands allowed by the Linux agent.
 * This is NOT a generic SSH command executor.
 */
object RemoteCommandPolicy {
    const val AGENT_PATH = "/usr/local/bin/ghost-wifi-agent"
    private val interfaceName = Regex("^[A-Za-z][A-Za-z0-9_.:-]{0,14}$")
    private val username = Regex("^[a-z_][a-z0-9_.-]{0,31}$")
    private val hostname = Regex("^[A-Za-z0-9._:-]{1,253}$")
    private val uuid = Regex("^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$")
    private val keyTypes = setOf(
        "ssh-ed25519", "ecdsa-sha2-nistp256", "rsa-sha2-512", "rsa-sha2-256", "ssh-rsa"
    )

    fun validateConnection(
        host: String, port: Int, user: String, knownHostLine: String,
        hasCredentials: Boolean
    ) {
        require(hostname.matches(host) && !host.startsWith("-") && !host.endsWith(".")) {
            "Nombre de host no permitido"
        }
        require(port in 1..65535) { "Puerto SSH fuera de rango" }
        require(username.matches(user) && !user.startsWith("-")) { "Usuario Linux no permitido" }
        val fields = knownHostLine.trim().split(Regex("\\s+"))
        val expected = if (port == 22) host else "[$host]:$port"
        require(fields.size in 3..4 && fields[0] == expected &&
            fields[1] in keyTypes &&
            fields[2].matches(Regex("^[A-Za-z0-9+/=]{40,4096}$"))) {
            "Se requiere la clave pública verificada del servidor exacto"
        }
        require(hasCredentials) { "Se necesita una contraseña o clave privada SSH" }
    }

    fun validatedInterface(input: String): String {
        require(interfaceName.matches(input)) { "Nombre de interfaz inválido" }
        return input
    }

    fun captureCommand(iface: String, durationSeconds: Int): String {
        validatedInterface(iface)
        require(durationSeconds in 5..30) { "La captura debe durar entre 5 y 30 segundos" }
        return "$AGENT_PATH capture --interface $iface --seconds $durationSeconds"
    }

    fun cleanupCommand(id: String): String {
        require(uuid.matches(id)) { "ID de captura inválido" }
        return "$AGENT_PATH cleanup --id $id"
    }

    fun isAcceptedCapturePath(id: String, path: String): Boolean {
        if (!uuid.matches(id) || path.length > 512 || !path.startsWith("/") ||
            path.contains("..") || path.contains("//") ||
            path.any { it == '\n' || it == '\r' || it == '\u0000' }) return false
        val base = "/.local/share/ghost-wifi-auditor/captures/"
        val pos = path.indexOf(base)
        return pos > 0 && pos == path.lastIndexOf(base) &&
            path.substring(pos) == "$base$id.pcap"
    }

    fun validateCaptureSize(bytes: Long): Boolean =
        bytes in 25..(32L * 1024L * 1024L)
}
