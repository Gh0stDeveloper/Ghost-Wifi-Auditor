package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Independent of operating system locale; persisted locally, no cloud sync. */
data class UiPreferences(
    val language: String = "es",
    val theme: String = "amoled",
    val textScale: Float = 1f,
    val offlineOnly: Boolean = false,
    val includeIdentifiers: Boolean = false
)

object AppPreferences {
    private const val NAME = "ghost_wifi_settings"

    fun load(context: Context): UiPreferences {
        val p = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return UiPreferences(
            language = p.getString("app_language", "es").let { if (it == "en") "en" else "es" },
            theme = p.getString("app_theme", "amoled").let {
                if (it in setOf("amoled", "graphite", "light")) it!! else "amoled"
            },
            textScale = p.getFloat("app_text_scale", 1f).coerceIn(0.9f, 1.3f),
            offlineOnly = p.getBoolean("app_offline_only", false),
            includeIdentifiers = p.getBoolean("app_include_identifiers", false)
        )
    }

    fun save(context: Context, value: UiPreferences) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("app_language", value.language)
            .putString("app_theme", value.theme)
            .putFloat("app_text_scale", value.textScale.coerceIn(0.9f,1.3f))
            .putBoolean("app_offline_only", value.offlineOnly)
            .putBoolean("app_include_identifiers", value.includeIdentifiers)
            .apply()
    }
}

data class DesignColors(
    val bg: Color, val card: Color, val panel: Color,
    val accent: Color, val blue: Color, val secondary: Color,
    val warning: Color, val foreground: Color
)
fun designColors(mode: String): DesignColors = when (mode) {
    "light" -> DesignColors(
        Color(0xFFF4F6F8), Color(0xFFFFFFFF), Color(0xFFE9EDF1),
        Color(0xFF127244), Color(0xFF0B638C), Color(0xFF536172),
        Color(0xFFA05A0B), Color(0xFF101820)
    )
    "graphite" -> DesignColors(
        Color(0xFF15191F), Color(0xFF2D3138), Color(0xFF242A32),
        Color(0xFF65D69A), Color(0xFF75C5F5), Color(0xFFB5BCC5),
        Color(0xFFFFC87D), Color.White
    )
    else -> DesignColors(
        Color(0xFF000000), Color(0xFF14171B), Color(0xFF0A0B0D),
        Color(0xFF54D98A), Color(0xFF66B9F1), Color(0xFFA9ACB3),
        Color(0xFFFFC87D), Color.White
    )
}

val LocalUiPreferences = compositionLocalOf { UiPreferences() }
val LocalDesignColors = compositionLocalOf { designColors("amoled") }

@Composable
fun tr(es: String, en: String): String =
    if (LocalUiPreferences.current.language == "en") en else es
