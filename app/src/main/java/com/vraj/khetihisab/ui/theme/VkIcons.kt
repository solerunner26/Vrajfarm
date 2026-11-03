package com.vraj.khetihisab.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

/*
 * Theme-aware ic_vk_* icons (doc 01 §5, §7).
 *
 * Path data lives in the generated VkIconSet.kt (source: design/icons, one SVG per icon). The three brushes
 * (tile, glyph, accent) plus the highlight colour come from LocalVkIcons, whose per-theme values
 * are copied from design/gen-themes.js. There are no 5×24 drawables; res/drawable/ic_vk_*.xml
 * (green only) exist solely for Glance widgets / notifications that cannot use ImageVector.
 *
 * To regenerate VkIconSet.kt and the drawables after editing an SVG, run the svg2vk.js converter
 * (`node tools/svg2vk.js .` from the project root).
 */

/** Per-theme icon palette (keys T1/T2/A1/A2/B1/B2/H of design/gen-themes.js). */
@Immutable
data class VkIconPalette(
    val tile1: Color, val tile2: Color,
    val glyph1: Color, val glyph2: Color,
    val accent1: Color, val accent2: Color,
    val highlight: Color,
    internal val key: String,
)

object VkIcons {
    val Green = VkIconPalette(Color(0xFFE6F3EB), Color(0xFFCBE6D6), Color(0xFF2E7D5B), Color(0xFF174A37), Color(0xFF9ED3B3), Color(0xFF5FA580), Color(0xFFEEF7F1), "green")
    val Mati = VkIconPalette(Color(0xFFFBEAE2), Color(0xFFF2D0C1), Color(0xFFB5562E), Color(0xFF7A3218), Color(0xFFF0B79A), Color(0xFFD9805A), Color(0xFFFDF3EE), "mati")
    val Akash = VkIconPalette(Color(0xFFEEF0FF), Color(0xFFD6DBFA), Color(0xFF3F51B5), Color(0xFF1A237E), Color(0xFFA9B4FF), Color(0xFF6F7FE0), Color(0xFFF4F5FF), "akash")
    val Kesar = VkIconPalette(Color(0xFFFFF3DC), Color(0xFFF8DFAE), Color(0xFFA86F0A), Color(0xFF5E3B00), Color(0xFFF5C76A), Color(0xFFD99A26), Color(0xFFFFF8EA), "kesar")
    val Night = VkIconPalette(Color(0xFF22302A), Color(0xFF18231D), Color(0xFF8FD3AE), Color(0xFF4E9A72), Color(0xFF5FA580), Color(0xFF2F6B50), Color(0xFF1C2822), "night")

    fun of(id: VkThemeId): VkIconPalette = when (id) {
        VkThemeId.GREEN -> Green
        VkThemeId.MATI -> Mati
        VkThemeId.AKASH -> Akash
        VkThemeId.KESAR -> Kesar
        VkThemeId.NIGHT -> Night
    }

    private val cache = ConcurrentHashMap<String, ImageVector>()

    /** Builds (and caches) the vector of [icon] in [palette]. */
    fun vector(icon: VkIcon, palette: VkIconPalette): ImageVector =
        cache.getOrPut("${palette.key}/${icon.name}") { build(icon, palette) }

    private fun brush(ink: VkInk, p: VkIconPalette): Brush {
        fun lin(a: Color, b: Color) = Brush.linearGradient(listOf(a, b), start = Offset(0f, 0f), end = Offset(96f, 96f))
        return when (ink) {
            VkInk.T -> lin(p.tile1, p.tile2)
            VkInk.A -> lin(p.glyph1, p.glyph2)
            VkInk.B -> lin(p.accent1, p.accent2)
            VkInk.H -> SolidColor(p.highlight)
        }
    }

    private fun build(icon: VkIcon, p: VkIconPalette): ImageVector {
        val b = ImageVector.Builder(
            name = "${icon.resName}_${p.key}",
            defaultWidth = 40.dp, defaultHeight = 40.dp,
            viewportWidth = 96f, viewportHeight = 96f,
        )
        for (part in icon.parts) {
            val nodes = addPathNodes(part.d)
            val add: ImageVector.Builder.() -> Unit = {
                addPath(
                    pathData = nodes,
                    fill = part.fill?.let { brush(it, p) },
                    fillAlpha = part.alpha,
                    stroke = part.stroke?.let { brush(it, p) },
                    strokeAlpha = part.alpha,
                    strokeLineWidth = part.strokeWidth,
                    strokeLineCap = if (part.roundCap) StrokeCap.Round else StrokeCap.Butt,
                    strokeLineJoin = if (part.roundJoin) StrokeJoin.Round else StrokeJoin.Miter,
                )
            }
            if (part.rotate != 0f) {
                b.addGroup(rotate = part.rotate, pivotX = 48f, pivotY = 48f)
                b.add()
                b.clearGroup()
            } else {
                b.add()
            }
        }
        return b.build()
    }
}

val LocalVkIcons = staticCompositionLocalOf { VkIcons.Green }

/** The logo tile (design/logo/logo-mark.svg, 192×192) as an ImageVector, recoloured per theme. */
object VkLogo {
    private val cache = ConcurrentHashMap<String, ImageVector>()

    fun vector(palette: VkIconPalette, extra: VkExtraColors): ImageVector = cache.getOrPut(palette.key) {
        val night = extra.isDark
        val bgStart = if (night) Color(0xFF245E47) else palette.glyph1
        val bgEnd = when (palette.key) { // gen-themes.js "L"
            "mati" -> Color(0xFF5E2410); "akash" -> Color(0xFF141B61); "kesar" -> Color(0xFF4A2E00)
            "night" -> Color(0xFF0E2A20); else -> Color(0xFF123D2D)
        }
        // On night the leaves stay light (gen-themes.js keeps the green leaf colours).
        val t1 = if (night) VkIcons.Green.tile1 else palette.tile1
        val t2 = if (night) VkIcons.Green.tile2 else palette.tile2
        val b1 = if (night) VkIcons.Green.accent1 else palette.accent1
        val b2 = if (night) VkIcons.Green.accent2 else palette.accent2
        ImageVector.Builder("vk_logo_${palette.key}", 96.dp, 96.dp, 192f, 192f).apply {
            path(fill = Brush.linearGradient(listOf(bgStart, bgEnd), Offset(0f, 0f), Offset(192f, 192f))) {
                roundRect(0f, 0f, 192f, 192f, 54f)
            }
            path(fill = Brush.radialGradient(listOf(b1.copy(alpha = .35f), b1.copy(alpha = 0f)), Offset(96f, 60f), 96f)) {
                roundRect(0f, 0f, 192f, 192f, 54f)
            }
            val ln = Brush.linearGradient(listOf(t1, t2), Offset(52f, 0f), Offset(140f, 0f))
            path(stroke = ln, strokeLineWidth = 9f, strokeLineCap = StrokeCap.Round) { moveTo(96f, 148f); lineTo(96f, 92f) }
            addPath(addPathNodes("M96 108C60 108 38 84 38 52c34-2 58 18 58 56z"), fill = Brush.linearGradient(listOf(t1, b1), Offset(40f, 40f), Offset(96f, 104f)))
            addPath(addPathNodes("M96 98c0-34 22-56 58-58 0 36-22 58-58 58z"), fill = Brush.linearGradient(listOf(t2, b2), Offset(152f, 36f), Offset(96f, 96f)), fillAlpha = .96f)
            path(fill = ln) { roundRect(50f, 146f, 92f, 9f, 4.5f) }
            path(fill = ln, fillAlpha = .7f) { roundRect(66f, 162f, 60f, 7f, 3.5f) }
        }.build()
    }

    private fun androidx.compose.ui.graphics.vector.PathBuilder.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        moveTo(x + r, y); lineTo(x + w - r, y)
        arcTo(r, r, 0f, false, true, x + w, y + r); lineTo(x + w, y + h - r)
        arcTo(r, r, 0f, false, true, x + w - r, y + h); lineTo(x + r, y + h)
        arcTo(r, r, 0f, false, true, x, y + h - r); lineTo(x, y + r)
        arcTo(r, r, 0f, false, true, x + r, y); close()
    }
}

/** Draws an ic_vk icon in the current theme's palette. Decorative by default; never tinted. */
@Composable
fun VkIconImage(icon: VkIcon, modifier: Modifier = Modifier, size: Dp = 40.dp, contentDescription: String? = null) {
    val palette = LocalVkIcons.current
    val vector = remember(icon, palette) { VkIcons.vector(icon, palette) }
    Image(imageVector = vector, contentDescription = contentDescription, modifier = modifier.size(size))
}

@Composable
fun VkLogoImage(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val palette = LocalVkIcons.current
    val extra = LocalVkExtra.current
    val vector = remember(palette, extra) { VkLogo.vector(palette, extra) }
    Image(imageVector = vector, contentDescription = null, modifier = modifier.size(size))
}
