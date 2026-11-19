package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import android.os.Build
import org.json.JSONArray

data class CompatibilityEntry(
    val id: String,
    val name: String,
    val modelPatterns: List<String>,
    val support: String,
    val requirements: String,
    val sourceUrl: String,
    val evidenceDate: String,
    val level: String,
    val usbMayBeNeeded: Boolean
)

/** Local, versioned evidence; NEVER infer 'supported' from a substring match alone. */
object DeviceCatalog {
    fun load(context: Context): List<CompatibilityEntry> = runCatching {
        val raw = context.assets.open("device_compatibility.v1.json")
            .bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val patterns = item.getJSONArray("modelPatterns")
            CompatibilityEntry(
                id = item.getString("id"),
                name = item.getString("name"),
                modelPatterns = List(patterns.length()) { patterns.getString(it) },
                support = item.getString("support"),
                requirements = item.getString("requirements"),
                sourceUrl = item.optString("sourceUrl"),
                evidenceDate = item.optString("evidenceDate", "Sin fecha"),
                level = item.getString("level"),
                usbMayBeNeeded = item.getBoolean("usbMayBeNeeded")
            )
        }
    }.getOrDefault(emptyList())

    fun matching(entries: List<CompatibilityEntry>, manufacturer: String = Build.MANUFACTURER, model: String = Build.MODEL): List<CompatibilityEntry> {
        val text = "$manufacturer $model".lowercase()
        return entries.filter { it.modelPatterns.any { term ->
            val key = term.lowercase().trim()
            key.isNotEmpty() && (text == key || text.contains(key))
        } }
    }
}
