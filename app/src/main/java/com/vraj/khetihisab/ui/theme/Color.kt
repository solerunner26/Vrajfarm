package com.vraj.khetihisab.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw colour tokens of one theme, mirroring `design/vk-theme.css` 1:1 (doc 01 §7).
 * CSS variable names are given in the comments. Screens never read this directly; they use
 * `MaterialTheme.colorScheme` and `LocalVkExtra`.
 */
@Immutable
data class VkPalette(
    val g1: Color, // --g1  gradient start
    val g2: Color, // --g2  gradient end
    val onG: Color, // --on-g
    val hdrBtn: Color, // --hdr-btn
    val orn: Color, // --orn (header line art)
    val bg: Color, // --bg   surface
    val card: Color, // --card surfaceContainerLowest
    val bd: Color, // --bd   outlineVariant
    val ol: Color, // --ol   outline
    val low: Color, // --low  surfaceContainerLow
    val cont: Color, // --cont surfaceContainer
    val div: Color, // --div  list dividers
    val on: Color, // --on   onSurface
    val on2: Color, // --on2  onSurfaceVariant
    val p: Color, // --p    primary
    val onP: Color, // --on-p
    val pc: Color, // --pc   primaryContainer
    val onPc: Color, // --on-pc
    val sc: Color, // --sc   secondaryContainer
    val onSc: Color, // --on-sc
    val secondary: Color, // not in CSS: doc 01 §1 "second (lighter) tone"
    val onSecondary: Color,
    val t: Color, // --t    tertiary (Panchang text)
    val tc: Color, // --tc
    val onTc: Color, // --on-tc
    val tcBd: Color, // --tc-bd
    val tOrn: Color, // --t-orn (rangoli dots)
    val err: Color, // --err
    val errc: Color, // --errc
    val onErrc: Color, // --on-errc
    val leaf1: Color, // --leaf1
    val leaf2: Color, // --leaf2
    val sil: Color, // --sil
    val silBg: Color, // --sil-bg
    val scrim: Color, // --scrim
    val shadow: Color, // --shadow
    val dock: Color, // --dock
    val key: Color, // --key
    val isDark: Boolean,
)

private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, (a * 255).toInt())

/** Green · વ્રજ લીલો (the `.vk` defaults). */
val GreenPalette = VkPalette(
    g1 = Color(0xFF2E7D5B), g2 = Color(0xFF174A37), onG = Color(0xFFFFFFFF),
    hdrBtn = rgba(255, 255, 255, .16f), orn = rgba(255, 255, 255, .13f),
    bg = Color(0xFFF5F7F4), card = Color(0xFFFFFFFF), bd = Color(0xFFC4D0C8), ol = Color(0xFF74827A),
    low = Color(0xFFEEF3EF), cont = Color(0xFFE7EEE9), div = Color(0xFFE7EEE9),
    on = Color(0xFF17201B), on2 = Color(0xFF46534C),
    p = Color(0xFF1F5C45), onP = Color(0xFFFFFFFF), pc = Color(0xFFD3EADB), onPc = Color(0xFF0B2E21),
    sc = Color(0xFFE4F2E9), onSc = Color(0xFF123D2D),
    secondary = Color(0xFF4E8C6C), onSecondary = Color(0xFFFFFFFF),
    t = Color(0xFF7A520A), tc = Color(0xFFF5E9CF), onTc = Color(0xFF3D2A05), tcBd = Color(0xFFE2CFA3),
    tOrn = rgba(154, 107, 18, .4f),
    err = Color(0xFFB3261E), errc = Color(0xFFF9DEDC), onErrc = Color(0xFF410E0B),
    leaf1 = Color(0xFF5FA580), leaf2 = Color(0xFF1F5C45), sil = Color(0xFF9EB2A6), silBg = Color(0xFFE7EEE9),
    scrim = rgba(15, 21, 18, .55f), shadow = rgba(23, 74, 55, .28f),
    dock = Color(0xFFE7EEE9), key = Color(0xFFFFFFFF),
    isDark = false,
)

/** Mati · માટી. */
val MatiPalette = GreenPalette.copy(
    g1 = Color(0xFFB5562E), g2 = Color(0xFF7A3218),
    bg = Color(0xFFF8F5F3), bd = Color(0xFFE0D0C8), ol = Color(0xFF85736B),
    low = Color(0xFFF3ECE8), cont = Color(0xFFECE3DE), div = Color(0xFFEFE6E1),
    on = Color(0xFF221A16), on2 = Color(0xFF55463F),
    p = Color(0xFF8F3E1E), pc = Color(0xFFF6DCCF), onPc = Color(0xFF3A1404),
    sc = Color(0xFFFBEAE2), onSc = Color(0xFF4A1D0B),
    secondary = Color(0xFFB5562E),
    leaf1 = Color(0xFFD9805A), leaf2 = Color(0xFF8F3E1E), sil = Color(0xFFC2A99C), silBg = Color(0xFFF1E5DF),
    shadow = rgba(122, 50, 24, .28f), dock = Color(0xFFECE3DE),
)

/** Akash · આકાશ. */
val AkashPalette = GreenPalette.copy(
    g1 = Color(0xFF3F51B5), g2 = Color(0xFF1A237E),
    bg = Color(0xFFF5F6FA), bd = Color(0xFFCDD1E4), ol = Color(0xFF767A8F),
    low = Color(0xFFEEF0F6), cont = Color(0xFFE6E8F1), div = Color(0xFFE8EAF2),
    on = Color(0xFF191B24), on2 = Color(0xFF464A5C),
    p = Color(0xFF2F3E9E), pc = Color(0xFFDDE1FF), onPc = Color(0xFF0A1660),
    sc = Color(0xFFEEF0FF), onSc = Color(0xFF1A237E),
    secondary = Color(0xFF3F51B5),
    leaf1 = Color(0xFF6F7FE0), leaf2 = Color(0xFF2F3E9E), sil = Color(0xFFA3A9C9), silBg = Color(0xFFE6E8F3),
    shadow = rgba(26, 35, 126, .28f), dock = Color(0xFFE6E8F1),
)

/** Kesar · કેસર. Panchang accent is maroon-brown so it does not blend into saffron. */
val KesarPalette = GreenPalette.copy(
    g1 = Color(0xFF9A6408), g2 = Color(0xFF5E3B00),
    bg = Color(0xFFF9F6EF), bd = Color(0xFFE2D6BF), ol = Color(0xFF827562),
    low = Color(0xFFF4EEE2), cont = Color(0xFFEDE5D5), div = Color(0xFFEFE8DA),
    on = Color(0xFF211B10), on2 = Color(0xFF51473A),
    p = Color(0xFF7A5000), pc = Color(0xFFFCE3B0), onPc = Color(0xFF2B1A00),
    sc = Color(0xFFFFF1D6), onSc = Color(0xFF3D2600),
    secondary = Color(0xFF9A6408),
    t = Color(0xFF8A3B12), tc = Color(0xFFF8E1D4), onTc = Color(0xFF3A1405), tcBd = Color(0xFFE9C2AE),
    tOrn = rgba(138, 59, 18, .35f),
    leaf1 = Color(0xFFD99A26), leaf2 = Color(0xFF7A5000), sil = Color(0xFFC4B293), silBg = Color(0xFFF1E8D6),
    shadow = rgba(94, 59, 0, .28f), dock = Color(0xFFEDE5D5),
)

/** Night · રાત્રિ (dark). */
val NightPalette = VkPalette(
    g1 = Color(0xFF245E47), g2 = Color(0xFF0E2A20), onG = Color(0xFFE8F3EC),
    hdrBtn = rgba(255, 255, 255, .10f), orn = rgba(255, 255, 255, .07f),
    bg = Color(0xFF0F1512), card = Color(0xFF18201B), bd = Color(0xFF2C3830), ol = Color(0xFF8A9890),
    low = Color(0xFF1E2722), cont = Color(0xFF222C26), div = Color(0xFF243029),
    on = Color(0xFFE3EAE5), on2 = Color(0xFFB9C6BE),
    p = Color(0xFF8FD3AE), onP = Color(0xFF003824), pc = Color(0xFF1F5C45), onPc = Color(0xFFC8EED8),
    sc = Color(0xFF1B2B23), onSc = Color(0xFFC4E3D1),
    secondary = Color(0xFF4E9A72), onSecondary = Color(0xFF003824),
    t = Color(0xFFE8C27A), tc = Color(0xFF3A2E14), onTc = Color(0xFFF3DFB4), tcBd = Color(0xFF5A4720),
    tOrn = rgba(232, 194, 122, .35f),
    err = Color(0xFFFFB4AB), errc = Color(0xFF5C1A15), onErrc = Color(0xFFFFDAD6),
    leaf1 = Color(0xFF4E9A72), leaf2 = Color(0xFF8FD3AE), sil = Color(0xFF4A5C52), silBg = Color(0xFF222C26),
    scrim = rgba(0, 0, 0, .65f), shadow = rgba(0, 0, 0, .5f),
    dock = Color(0xFF18201B), key = Color(0xFF1E2722),
    isDark = true,
)

/** Settings swatch for the night theme (doc 17 #24): charcoal, not a second green. */
val NightSwatchStart = Color(0xFF2B3631)
val NightSwatchEnd = Color(0xFF07090A)
