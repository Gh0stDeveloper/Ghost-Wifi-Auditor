package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** File-backed history for imports. Files live only in app-private storage. */
data class CaptureEntry(
    val id: String,
    val displayName: String,
    val importedAt: Long,
    val bytes: Long,
    val summary: JSONObject
) {
    val format: String get() = summary.optString("format", "desconocido")
    val packets: Long get() = summary.optLong("packets", 0L)
    fun reportJson(): String = JSONObject().apply {
        put("application", "Ghost WiFi Auditor")
        put("reportVersion", 1)
        put("id", id)
        put("fileName", displayName)
        put("importedAtEpochMs", importedAt)
        put("fileSizeBytes", bytes)
        put("analysis", summary)
        put("notice", "Offline metadata analysis; does not validate network security or reveal credentials.")
    }.toString(2)
}

object CaptureRepository {
    private const val MAX_CAPTURE_BYTES = 128L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 256L * 1024 * 1024
    private const val MAX_ENTRIES = 20
    private val legalId = Regex("^[a-f0-9\\-]{36}$")
    private fun dir(context: Context): File =
        File(context.filesDir, "captures").also { if (!it.exists() && !it.mkdirs()) throw IOException("No se pudo crear almacenamiento privado") }
    private fun capture(context: Context, id: String): File {
        require(legalId.matches(id))
        return File(dir(context), "$id.cap")
    }
    private fun meta(context: Context, id: String): File {
        require(legalId.matches(id))
        return File(dir(context), "$id.json")
    }

    fun list(context: Context): List<CaptureEntry> = runCatching {
        dir(context).listFiles().orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .mapNotNull { file ->
                runCatching {
                    val obj = JSONObject(file.readText())
                    val id = obj.getString("id")
                    if (!legalId.matches(id) || file.name != "$id.json" || !capture(context,id).isFile) null
                    else CaptureEntry(
                        id = id,
                        displayName = obj.getString("displayName"),
                        importedAt = obj.getLong("importedAt"),
                        bytes = obj.getLong("bytes"),
                        summary = obj.getJSONObject("summary")
                    )
                }.getOrNull()
            }.sortedByDescending { it.importedAt }
    }.getOrDefault(emptyList())

    private fun displayName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null,null,null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull().orEmpty().take(110).ifBlank { "captura_importada" }
        .replace(Regex("[\\r\\n\\t]"), "_")

    suspend fun importCapture(context: Context, uri: Uri): CaptureEntry = withContext(Dispatchers.IO) {
        val previous = list(context)
        if (previous.size >= MAX_ENTRIES) throw IOException("Límite de 20 capturas; elimina una antes de importar")
        if (previous.sumOf { it.bytes } >= MAX_TOTAL_BYTES) throw IOException("Almacenamiento de capturas lleno")
        val id = UUID.randomUUID().toString()
        val target = capture(context,id)
        val tmp = File(dir(context), "$id.partial")
        val metaFile = meta(context,id)
        try {
            var total=0L
            val input=context.contentResolver.openInputStream(uri)
                ?: throw IOException("No se puede abrir el archivo seleccionado")
            input.use { source ->
                tmp.outputStream().buffered().use { output ->
                    val buffer=ByteArray(64*1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read=source.read(buffer)
                        if (read<0) break
                        if (read==0) continue
                        total+=read
                        if (total>MAX_CAPTURE_BYTES || previous.sumOf { it.bytes }+total>MAX_TOTAL_BYTES)
                            throw IOException("Captura demasiado grande (máximo 128 MiB por archivo, 256 MiB total)")
                        output.write(buffer,0,read)
                    }
                    output.flush()
                }
            }
            if (!tmp.renameTo(target)) throw IOException("No se pudo finalizar la importación")
            val parsed=runCatching { JSONObject(NativePcap.analyzeFile(target.absolutePath)) }
                .getOrElse { throw IOException("No se pudo interpretar el análisis nativo") }
            if (!parsed.optBoolean("ok",false)) {
                throw IOException("PCAP inválido: ${parsed.optString("error","desconocido")}")
            }
            val entry = CaptureEntry(id,displayName(context,uri),System.currentTimeMillis(),total,parsed)
            val saved = JSONObject().apply {
                put("id",entry.id);put("displayName",entry.displayName)
                put("importedAt",entry.importedAt);put("bytes",entry.bytes)
                put("summary",entry.summary)
            }
            metaFile.writeText(saved.toString())
            entry
        } catch(e:Exception) {
            tmp.delete(); target.delete(); metaFile.delete()
            throw e
        }
    }

    suspend fun exportReport(context: Context, entry: CaptureEntry, target: Uri) =
        withContext(Dispatchers.IO) {
            val output=context.contentResolver.openOutputStream(target,"w")
                ?: throw IOException("No se pudo abrir destino JSON")
            output.use { it.write(entry.reportJson().toByteArray(Charsets.UTF_8)) }
        }

    suspend fun exportCapture(context: Context, entry: CaptureEntry, target: Uri) =
        withContext(Dispatchers.IO) {
            val file=capture(context,entry.id)
            if (!file.isFile) throw IOException("Archivo original no encontrado")
            val output=context.contentResolver.openOutputStream(target,"w")
                ?: throw IOException("No se pudo abrir destino de captura")
            file.inputStream().use { source -> output.use { sink -> source.copyTo(sink,64*1024) } }
        }

    fun delete(context: Context, id: String): Boolean {
        if (!legalId.matches(id)) return false
        val a=capture(context,id)
        val b=meta(context,id)
        return (!a.exists() || a.delete()) && (!b.exists() || b.delete())
    }
}
