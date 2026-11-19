package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.DateFormat
import java.util.Date

/** User-initiated PDF export, using Android's built-in PdfDocument (no external SDK). */
object PdfReportExporter {
    suspend fun export(
        context: Context,
        entry: CaptureEntry,
        destination: Uri,
        includeIdentifiers: Boolean = false,
        language: String = "es"
    ) = withContext(Dispatchers.IO) {
        val en = language == "en"
        fun label(spanish: String, english: String) = if (en) english else spanish
        val lines = buildList {
            add(label("Resumen de captura", "Capture summary") to "")
            add(label("ID de sesión", "Session ID") to entry.id)
            add(label("Importada", "Imported") to DateFormat.getDateTimeInstance().format(Date(entry.importedAt)))
            add(label("Origen", "Source") to if(entry.source == "linux_ssh") "Linux SSH" else label("Archivo local","Local file"))
            add(label("Formato", "Format") to entry.format)
            add(label("Bytes", "Bytes") to entry.bytes.toString())
            add(label("Paquetes", "Packets") to entry.packets.toString())
            add(label("Tramas de gestión", "Management frames") to entry.summary.optLong("management").toString())
            add(label("Tramas de control", "Control frames") to entry.summary.optLong("control").toString())
            add(label("Tramas de datos", "Data frames") to entry.summary.optLong("data").toString())
            add(label("Tramas Ethernet", "Ethernet frames") to entry.summary.optLong("ethernet").toString())
            add(label("Otras tramas", "Other frames") to entry.summary.optLong("other").toString())
            add(label("Tipos de enlace", "Link types") to entry.summary.optJSONArray("linkTypes").let { a ->
                if(a == null) label("No disponible","Unavailable")
                else (0 until a.length()).joinToString(", ") { a.optInt(it).toString() }
            })
            add(label("Cifrado anunciado", "Advertised encryption") to label("NO EVALUADO","NOT MEASURED"))
            add("WPS" to label("NO EVALUADO","NOT MEASURED"))
            add(label("Canal", "Channel") to label("NO EVALUADO","NOT MEASURED"))
            add("RSSI" to label("NO EVALUADO","NOT MEASURED"))
            if (includeIdentifiers) {
                add(label("Archivo original", "Original filename") to entry.displayName)
                add(label("Etiqueta", "Label") to entry.label)
                add(label("Nota", "Note") to entry.note)
                add(label("Dispositivo", "Device") to entry.deviceProfile)
                add(label("Adaptador seleccionado", "Selected adapter") to entry.adapterProfile)
            }
            add(label("Privacidad", "Privacy") to if(includeIdentifiers) "INCLUDE_IDENTIFIERS" else "REDACTED")
        }
        val pdf = PdfDocument()
        val width = 595
        val height = 842
        val left = 42f
        val right = 553f
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = android.graphics.Color.rgb(35,43,50)
            typeface = Typeface.create("sans-serif",Typeface.NORMAL)
        }
        val heading = Paint(regular).apply {
            textSize = 18f
            typeface = Typeface.create("sans-serif",Typeface.BOLD)
            color = android.graphics.Color.rgb(15,115,77)
        }
        val secondary = Paint(regular).apply {
            textSize=9f
            color=android.graphics.Color.rgb(84,101,113)
        }
        var pageNumber=0
        var page: PdfDocument.Page? = null
        var y=0f
        fun nextPage() {
            page?.let { pdf.finishPage(it) }
            pageNumber += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(width,height,pageNumber).create())
            y = 45f
            val canvas = page!!.canvas
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawText("Ghost WiFi Auditor",left,y,heading)
            y += 24f
            canvas.drawText(label("Informe local de auditoría", "Local audit report"),
                left,y,secondary)
            y += 20f
        }
        fun line(text: String, paint: Paint = regular) {
            var remaining=text.ifBlank { "—" }.replace(Regex("[\\r\\n\\t]")," ")
            // Maximum three lines per field to keep reports bounded and legible.
            repeat(3) { idx ->
                if (remaining.isEmpty()) return
                if (y > height - 68) nextPage()
                val max=(right-left).toInt()
                val count=paint.breakText(remaining,true,max.toFloat(),null).coerceAtLeast(1)
                val chunk=remaining.take(count)
                page!!.canvas.drawText(chunk,left,y,paint)
                y += 15f
                remaining=remaining.drop(count)
                if (remaining.isEmpty()) return
                if (idx==2) {
                    if (y > height-68) nextPage()
                    page!!.canvas.drawText("…",left,y,paint)
                    y += 15f
                }
            }
        }
        try {
            nextPage()
            for ((key,value) in lines) {
                line(key, secondary)
                line(value)
                y += 5f
            }
            if (y > height-120) nextPage()
            y += 12f
            line(label("Estos datos describen una captura, no verifican vulnerabilidades.",
                "These results describe a capture and do not verify vulnerabilities."),secondary)
            line(label("No se incluyen paquetes, contraseñas ni claves privadas en este PDF.",
                "No raw packets, passwords or private keys are included in this PDF."),secondary)
            page?.let { pdf.finishPage(it);page=null }
            val output=context.contentResolver.openOutputStream(destination,"w")
                ?: throw IOException("Could not open PDF destination")
            output.use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }
}
