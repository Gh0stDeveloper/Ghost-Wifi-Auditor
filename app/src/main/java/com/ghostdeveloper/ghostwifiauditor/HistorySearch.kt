package com.ghostdeveloper.ghostwifiauditor

/**
 * Search only user-visible metadata, never packet payload, MAC addresses or Wi-Fi secrets.
 * Empty query displays all history entries.
 */
object HistorySearch {
    fun matches(query: String, vararg fields: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return fields.any { it.contains(needle, ignoreCase = true) }
    }

    fun sanitizeLabel(value: String, maxLength: Int = 80): String =
        value.trim().take(maxLength).replace(Regex("[\\r\\n\\t]"), " ")
}
