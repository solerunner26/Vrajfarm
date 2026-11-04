package com.vraj.khetihisab.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.vraj.khetihisab.settings.ThemeRepository
import com.vraj.khetihisab.settings.vkPrefsDataStore
import com.vraj.khetihisab.ui.theme.VkPalette
import com.vraj.khetihisab.ui.theme.VkThemeId
import com.vraj.khetihisab.ui.theme.VkThemes
import kotlinx.coroutines.flow.first

/**
 * Widget colours come from the same theme tokens as the app (doc 01 §7, doc 13 "GlanceTheme with
 * colours from doc 01"): the `vk_theme` DataStore key, plus "follow system dark" → night.
 * Solid fills only; Glance cannot draw the brand gradient reliably on every launcher.
 */
object WidgetTheme {

    suspend fun themeId(context: Context): VkThemeId {
        val prefs = runCatching { context.applicationContext.vkPrefsDataStore.data.first() }.getOrNull()
        val chosen = VkThemeId.fromKey(prefs?.get(ThemeRepository.KEY_THEME))
        val follow = prefs?.get(ThemeRepository.KEY_FOLLOW_SYSTEM) ?: false
        val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (follow && systemDark) VkThemeId.NIGHT else chosen
    }

    suspend fun colors(context: Context): WidgetColors = WidgetColors.of(themeId(context))
}

/** The handful of roles a widget needs, as Glance [ColorProvider]s. */
data class WidgetColors(
    val id: VkThemeId,
    val surface: ColorProvider,
    val onSurface: ColorProvider,
    val onSurfaceVariant: ColorProvider,
    val primary: ColorProvider,
    val onPrimary: ColorProvider,
    val primaryContainer: ColorProvider,
    val onPrimaryContainer: ColorProvider,
    val panchang: ColorProvider,
    val error: ColorProvider,
    val divider: ColorProvider,
) {
    companion object {
        fun of(id: VkThemeId): WidgetColors = from(id, VkThemes.palette(id))

        fun from(id: VkThemeId, p: VkPalette) = WidgetColors(
            id = id,
            surface = cp(p.card),
            onSurface = cp(p.on),
            onSurfaceVariant = cp(p.on2),
            primary = cp(p.p),
            onPrimary = cp(p.onP),
            primaryContainer = cp(p.pc),
            onPrimaryContainer = cp(p.onPc),
            panchang = cp(p.t),
            error = cp(p.err),
            divider = cp(p.div),
        )

        private fun cp(c: Color) = ColorProvider(c)
    }
}
