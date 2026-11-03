package com.vraj.khetihisab.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** The five user-selectable themes (doc 01 §7). Stored by [storageKey] in DataStore (`vk_theme`). */
enum class VkThemeId(val storageKey: String) {
    GREEN("green"), MATI("mati"), AKASH("akash"), KESAR("kesar"), NIGHT("night");

    companion object {
        fun fromKey(key: String?): VkThemeId = entries.firstOrNull { it.storageKey == key } ?: GREEN
    }
}

/** Non-M3 colours every Vk* component needs. */
@Immutable
data class VkExtraColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val onGradient: Color,
    val headerButton: Color,
    val ornament: Color,
    val silhouette: Color,
    val silhouetteBg: Color,
    val panchangBorder: Color,
    val panchangOrnament: Color,
    val leaf1: Color,
    val leaf2: Color,
    val divider: Color,
    val dock: Color,
    val key: Color,
    val scrim: Color,
    val shadow: Color,
    val isDark: Boolean,
) {
    /** Brand gradient, 135° (top-left → bottom-right). */
    val brandBrush: Brush
        get() = Brush.linearGradient(listOf(gradientStart, gradientEnd), start = Offset.Zero, end = Offset.Infinite)
}

val LocalVkExtra = staticCompositionLocalOf<VkExtraColors> { error("VkTheme missing") }

object VkThemes {

    fun palette(id: VkThemeId): VkPalette = when (id) {
        VkThemeId.GREEN -> GreenPalette
        VkThemeId.MATI -> MatiPalette
        VkThemeId.AKASH -> AkashPalette
        VkThemeId.KESAR -> KesarPalette
        VkThemeId.NIGHT -> NightPalette
    }

    fun of(id: VkThemeId): Pair<ColorScheme, VkExtraColors> {
        val p = palette(id)
        return colorScheme(p) to extra(p)
    }

    fun colorScheme(p: VkPalette): ColorScheme {
        // Dialogs and sheets are `card` in the design (css .vk-dialog / .vk-sheet), so the M3
        // high containers map to card; menus and the dock use surfaceContainer.
        return if (p.isDark) {
            darkColorScheme(
                primary = p.p, onPrimary = p.onP, primaryContainer = p.pc, onPrimaryContainer = p.onPc,
                inversePrimary = GreenPalette.p,
                secondary = p.secondary, onSecondary = p.onSecondary,
                secondaryContainer = p.sc, onSecondaryContainer = p.onSc,
                tertiary = p.t, onTertiary = p.tc, tertiaryContainer = p.tc, onTertiaryContainer = p.onTc,
                background = p.bg, onBackground = p.on, surface = p.bg, onSurface = p.on,
                surfaceVariant = p.cont, onSurfaceVariant = p.on2, surfaceTint = p.p,
                inverseSurface = p.on, inverseOnSurface = p.bg,
                error = p.err, onError = p.errc, errorContainer = p.errc, onErrorContainer = p.onErrc,
                outline = p.ol, outlineVariant = p.bd, scrim = Color.Black,
                surfaceBright = p.low, surfaceDim = p.bg,
                surfaceContainerLowest = p.card, surfaceContainerLow = p.low, surfaceContainer = p.cont,
                surfaceContainerHigh = p.card, surfaceContainerHighest = p.cont,
            )
        } else {
            lightColorScheme(
                primary = p.p, onPrimary = p.onP, primaryContainer = p.pc, onPrimaryContainer = p.onPc,
                inversePrimary = p.pc,
                secondary = p.secondary, onSecondary = p.onSecondary,
                secondaryContainer = p.sc, onSecondaryContainer = p.onSc,
                tertiary = p.t, onTertiary = Color.White, tertiaryContainer = p.tc, onTertiaryContainer = p.onTc,
                background = p.bg, onBackground = p.on, surface = p.bg, onSurface = p.on,
                surfaceVariant = p.cont, onSurfaceVariant = p.on2, surfaceTint = p.p,
                inverseSurface = p.on, inverseOnSurface = p.bg,
                error = p.err, onError = Color.White, errorContainer = p.errc, onErrorContainer = p.onErrc,
                outline = p.ol, outlineVariant = p.bd, scrim = Color.Black,
                surfaceBright = p.card, surfaceDim = p.cont,
                surfaceContainerLowest = p.card, surfaceContainerLow = p.low, surfaceContainer = p.cont,
                surfaceContainerHigh = p.card, surfaceContainerHighest = p.cont,
            )
        }
    }

    fun extra(p: VkPalette) = VkExtraColors(
        gradientStart = p.g1, gradientEnd = p.g2, onGradient = p.onG,
        headerButton = p.hdrBtn, ornament = p.orn,
        silhouette = p.sil, silhouetteBg = p.silBg,
        panchangBorder = p.tcBd, panchangOrnament = p.tOrn,
        leaf1 = p.leaf1, leaf2 = p.leaf2, divider = p.div, dock = p.dock, key = p.key,
        scrim = p.scrim, shadow = p.shadow, isDark = p.isDark,
    )
}
