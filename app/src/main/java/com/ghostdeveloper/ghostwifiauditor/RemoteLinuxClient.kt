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
    internal fun verifyConfig(cfg: RemoteLinuxConfig) {
        RemoteCommandPolicy.validateConnection(
            cfg.host, cfg.port, cfg.username, cfg.knownHostLine,
            cfg.password.isNotEmpty() || cfg.keyUri != null
        )
    }

    private fun open(context: Context, cfg: RemoteLinuxConfig): SSHClient {
        verifyConfig(cfg)
        if (AppPreferences.load(context).offlineOnly) {
            throw IOException("El modo sin conexión está activado en Ajustes")
        }
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
                        val bytes=ByteArray(4096)
                        var copied=0L
                        while (true) {
                            val read=input.read(bytes)
                            if(read == -1) break
                            copied+=read
                            if(copied > 128L * 1024L) throw IOException("Clave SSH demasiado grande")
                            output.write(bytes,0,read)
                        }
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
            val raw = process.inputStream.bufferedReader().use { input ->
                val result=StringBuilder()
                val buffer=CharArray(4096)
                while (true) {
                    val count=input.read(buffer)
                    if(count < 0) break
                    if(result.length + count > 32768) {
                        throw IOException("Respuesta del agente demasiado grande")
                    }
                    result.append(buffer,0,count)
                }
                result.toString()
            }
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
                val data = execute(ssh, "${RemoteCommandPolicy.AGENT_PATH} list", 15)
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
        val command=RemoteCommandPolicy.captureCommand(iface,seconds)
        open(context, cfg).use { ssh ->
            val data = execute(ssh, command, 45)
            val id = data.getString("id")
            val remote = data.getString("path")
            require(RemoteCommandPolicy.isAcceptedCapturePath(id,remote)) {
                "Ruta de descarga no autorizada"
            }
            val file = File.createTempFile("ghost-remote-", ".pcap", context.cacheDir)
            try {
                ssh.newSFTPClient().use { sftp ->
                    val attrs = sftp.stat(remote)
                    if (!RemoteCommandPolicy.validateCaptureSize(attrs.size)) {
                        throw IOException("El archivo remoto supera 32 MiB")
                    }
                    sftp.get(remote, FileSystemFile(file))
                }
                if (!RemoteCommandPolicy.validateCaptureSize(file.length())) {
                    throw IOException("La descarga supera 32 MiB")
                }
                val imported = CaptureRepository.importCapture(context, Uri.fromFile(file), "linux_ssh")
                imported
            } finally {
                file.delete()
                // Delete remote file whether import succeeded or failed.
                runCatching { execute(ssh, RemoteCommandPolicy.cleanupCommand(id), 10) }
            }
        }
    }
}
