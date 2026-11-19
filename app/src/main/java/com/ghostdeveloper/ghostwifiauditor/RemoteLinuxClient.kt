package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.xfer.FileSystemFile
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * SSHJ uses host-key pinning through an OpenSSH known_hosts entry supplied independently
 * by the operator. Never use PromiscuousVerifier or blindly trust ssh-keyscan.
 * Secrets are passed for a single operation only and are not saved to SharedPreferences.
 */
data class RemoteLinuxConfig(
    val host: String,
    val port: Int,
    val username: String,
    val knownHostLine: String,
    val password: String = "",
    val keyUri: Uri? = null,
    val keyPassphrase: String = ""
)

data class RemoteLinuxStatus(
    val hostname: String,
    val kernel: String,
    val interfaces: List<RemoteInterface>,
    val captureAvailable: Boolean,
    val agentVersion: String
)
data class RemoteInterface(val name: String, val mode: String) {
    val monitor: Boolean get() = mode == "monitor"
}

object RemoteLinuxClient {
    private const val AGENT = "/usr/local/bin/ghost-wifi-agent"
    private val ifacePattern = Regex("^[a-zA-Z0-9_.:-]{1,15}$")
    private val userPattern = Regex("^[a-z_][a-z0-9_.-]{0,31}$")
    private val hostPattern = Regex("^[A-Za-z0-9._:-]{1,253}$")
    private val idPattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    internal fun verifyConfig(cfg: RemoteLinuxConfig) {
        require(hostPattern.matches(cfg.host) && !cfg.host.startsWith("-")) { "Host/IP no válido" }
        require(cfg.port in 1..65535) { "Puerto fuera de rango" }
        require(userPattern.matches(cfg.username)) { "Usuario Linux no válido" }
        val fields = cfg.knownHostLine.trim().split(Regex("\\s+"))
        val expected = if (cfg.port == 22) cfg.host else "[${cfg.host}]:${cfg.port}"
        require(fields.size in 3..4 && fields[0] == expected &&
            fields[1] in setOf("ssh-ed25519", "ecdsa-sha2-nistp256", "rsa-sha2-512", "rsa-sha2-256", "ssh-rsa") &&
            fields[2].matches(Regex("^[A-Za-z0-9+/=]{40,4096}$"))) {
            "Pega la línea known_hosts del host y puerto exactos, verificada por otra vía"
        }
        require(cfg.password.isNotEmpty() || cfg.keyUri != null) {
            "Introduce contraseña o selecciona una clave SSH"
        }
    }

    private fun open(context: Context, cfg: RemoteLinuxConfig): SSHClient {
        verifyConfig(cfg)
        val ssh = SSHClient()
        ssh.connectTimeout = 12000
        ssh.timeout = 20000
        val known = File.createTempFile("trusted-host-", ".txt", context.cacheDir)
        var key: File? = null
        try {
            known.writeText(cfg.knownHostLine.trim() + "\n")
            ssh.loadKnownHosts(known)
            ssh.connect(cfg.host, cfg.port)
            if (cfg.keyUri != null) {
                key = File.createTempFile("ssh-auth-", ".key", context.cacheDir)
                val source = context.contentResolver.openInputStream(cfg.keyUri)
                    ?: throw IOException("No se pudo abrir la clave privada")
                source.use { input ->
                    key.outputStream().use { output ->
                        val copied = input.copyTo(output, 4096)
                        if (copied > 128 * 1024) throw IOException("Clave SSH demasiado grande")
                    }
                }
                ssh.authPublickey(cfg.username,
                    ssh.loadKeys(key.absolutePath, cfg.keyPassphrase.toCharArray()))
            } else {
                val secret = cfg.password.toCharArray()
                try { ssh.authPassword(cfg.username, secret) }
                finally { secret.fill('\u0000') }
            }
            return ssh
        } catch (e: Exception) {
            runCatching { ssh.disconnect() }
            throw e
        } finally {
            known.delete()
            key?.delete()
        }
    }

    private fun execute(ssh: SSHClient, command: String, seconds: Long = 20): JSONObject {
        ssh.startSession().use { session ->
            val process = session.exec(command)
            process.join(seconds, TimeUnit.SECONDS)
            if (process.exitStatus == null) {
                runCatching { process.close() }
                throw IOException("Tiempo de respuesta del agente agotado")
            }
            // Agent emits bounded, one-line JSON. Do not expose arbitrary remote shell.
            val raw = process.inputStream.bufferedReader().use { it.readText().take(32768) }
            if (process.exitStatus != 0) {
                throw IOException("Error remoto: ${raw.take(300)}")
            }
            val result = JSONObject(raw)
            if (!result.optBoolean("ok", false)) {
                throw IOException(result.optString("error", "Agente Linux no disponible").take(220))
            }
            return result
        }
    }

    suspend fun diagnose(context: Context, cfg: RemoteLinuxConfig): RemoteLinuxStatus =
        withContext(Dispatchers.IO) {
            open(context, cfg).use { ssh ->
                val data = execute(ssh, "$AGENT list", 15)
                val arr = data.getJSONArray("interfaces")
                val interfaces = (0 until arr.length()).map { idx ->
                    val item = arr.getJSONObject(idx)
                    RemoteInterface(item.getString("name"), item.getString("type"))
                }
                RemoteLinuxStatus(
                    hostname = data.optString("hostname").take(80),
                    kernel = data.optString("kernel").take(120),
                    interfaces = interfaces,
                    captureAvailable = data.optBoolean("tcpdump", false),
                    agentVersion = data.optString("version", "desconocido")
                )
            }
        }

    /**
     * Only monitor-mode interfaces and a bounded capture duration are allowed.
     * The remote agent revalidates both values and launches tcpdump with argv, no shell.
     * Successful captures are transferred over authenticated SFTP and parsed locally.
     */
    suspend fun capture(
        context: Context, cfg: RemoteLinuxConfig, iface: String, seconds: Int
    ): CaptureEntry = withContext(Dispatchers.IO) {
        require(ifacePattern.matches(iface)) { "Nombre de interfaz no permitido" }
        require(seconds in 5..30) { "Duración debe estar entre 5 y 30 segundos" }
        open(context, cfg).use { ssh ->
            val data = execute(ssh, "$AGENT capture --interface $iface --seconds $seconds", 45)
            val id = data.getString("id")
            val remote = data.getString("path")
            require(idPattern.matches(id)) { "Identificador de captura remoto inválido" }
            require(remote.contains("/.local/share/ghost-wifi-auditor/captures/") &&
                remote.endsWith("/$id.pcap") &&
                !remote.contains("..") && remote.length < 512) {
                "Ruta de descarga no autorizada"
            }
            val file = File.createTempFile("ghost-remote-", ".pcap", context.cacheDir)
            try {
                ssh.newSFTPClient().use { sftp ->
                    val attrs = sftp.stat(remote)
                    if (attrs.size > 32L * 1024 * 1024) {
                        throw IOException("El archivo remoto supera 32 MiB")
                    }
                    sftp.get(remote, FileSystemFile(file))
                }
                if (file.length() > 32L * 1024 * 1024) {
                    throw IOException("La descarga supera 32 MiB")
                }
                val imported = CaptureRepository.importCapture(context, Uri.fromFile(file), "linux_ssh")
                runCatching { execute(ssh, "$AGENT cleanup --id $id", 10) }
                imported
            } finally {
                file.delete()
            }
        }
    }
}
