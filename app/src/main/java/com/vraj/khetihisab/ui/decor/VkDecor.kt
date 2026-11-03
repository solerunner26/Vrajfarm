package com.vraj.khetihisab.ui.decor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import kotlin.math.abs

/*
 * Decorative ornaments (doc 01 §4). All are ImageVectors, contentDescription = null.
 * Two-tone only: header line-art is white at 7–13 % (theme `ornament`), rangoli dots are gold at
 * 25–40 % (theme `panchangOrnament`), leaves use the theme leaf tones.
 */
object VkDecor {

    /** Header motif: three gentle field furrows and a rising-sun arc, 360×170 viewport. */
    fun fieldMotif(color: Color): ImageVector =
        ImageVector.Builder("vk_field_motif", 360.dp, 170.dp, 360f, 170f).apply {
            val s = SolidColor(color)
            // Rising sun: arc + short rays, top-right.
            path(stroke = s, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(250f, 92f); arcTo(42f, 42f, 0f, false, true, 334f, 92f)
            }
            path(stroke = s, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(292f, 38f); lineTo(292f, 26f)
                moveTo(256f, 54f); lineTo(247f, 46f)
                moveTo(328f, 54f); lineTo(337f, 46f)
                moveTo(240f, 84f); lineTo(229f, 81f)
                moveTo(344f, 84f); lineTo(355f, 81f)
            }
            // Furrows.
            path(stroke = s, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(-10f, 118f); quadTo(170f, 88f, 370f, 112f)
                moveTo(-10f, 136f); quadTo(170f, 104f, 370f, 130f)
                moveTo(-10f, 154f); quadTo(170f, 120f, 370f, 148f)
            }
        }.build()

    /** Toran scallop bottom edge (12 arcs across the width), filled with the page surface. */
    fun toranEdge(fill: Color, arcs: Int = 12): ImageVector {
        val w = 360f
        val h = 12f
        val step = w / arcs
        return ImageVector.Builder("vk_toran_edge", 360.dp, 12.dp, w, h).apply {
            path(fill = SolidColor(fill)) {
                moveTo(0f, 0f)
                for (i in 0 until arcs) {
                    val x = i * step
                    quadTo(x + step / 2f, h * 2f - 2f, x + step, 0f)
                }
                lineTo(w, h + 1f); lineTo(0f, h + 1f); close()
            }
        }.build()
    }

    /** Two-leaf sprig used by VkSectionTitle, 24×16 viewport. */
    fun leafDivider(light: Color, dark: Color): ImageVector =
        ImageVector.Builder("vk_leaf", 24.dp, 16.dp, 24f, 16f).apply {
            path(fill = SolidColor(light)) {
                moveTo(12f, 14f); curveTo(5f, 14f, 1f, 10f, 1f, 3f); curveTo(8f, 3f, 12f, 7f, 12f, 14f); close()
            }
            path(fill = SolidColor(dark)) {
                moveTo(12f, 14f); curveTo(12f, 7f, 16f, 2f, 23f, 2f); curveTo(23f, 9f, 19f, 14f, 12f, 14f); close()
            }
        }.build()

    /** Three faint dots in a triangle (Panchang card / festival dialog corners), 24×24 viewport. */
    fun rangoliDots(color: Color): ImageVector =
        ImageVector.Builder("vk_rangoli", 24.dp, 24.dp, 24f, 24f).apply {
            val s = SolidColor(color)
            fun dot(cx: Float, cy: Float, r: Float) = path(fill = s) {
                moveTo(cx - r, cy); arcTo(r, r, 0f, true, true, cx + r, cy); arcTo(r, r, 0f, true, true, cx - r, cy); close()
            }
            dot(12f, 5f, 3f); dot(5f, 18f, 3f); dot(19f, 18f, 3f)
        }.build()

    /**
     * Moon phase disc (doc 01 §4.3). [illumination] 0 = new, 1 = full. Waxing is lit on the right
     * (as seen from India). 48×48 viewport.
     */
    fun moonPhase(illumination: Float, waxing: Boolean, disc: Color, lit: Color, ring: Color): ImageVector {
        val k = illumination.coerceIn(0f, 1f)
        val c = 24f
        val r = 20f
        val rx = r * abs(1f - 2f * k)
        return ImageVector.Builder("vk_moon", 48.dp, 48.dp, 48f, 48f).apply {
            path(fill = SolidColor(disc)) {
                moveTo(c - r, c); arcTo(r, r, 0f, true, true, c + r, c); arcTo(r, r, 0f, true, true, c - r, c); close()
            }
            if (k > 0.005f) {
                path(fill = SolidColor(lit)) {
                    moveTo(c, c - r)
                    // Lit limb: right half when waxing, left half when waning.
                    arcTo(r, r, 0f, false, waxing, c, c + r)
                    // Terminator back to the top: crescent bulges toward the lit side, gibbous away.
                    val gibbous = k > 0.5f
                    val sweep = if (waxing) gibbous else !gibbous
                    if (rx < 0.05f) lineTo(c, c - r) else arcTo(rx, r, 0f, false, sweep, c, c - r)
                    close()
                }
            }
            path(stroke = SolidColor(ring), strokeLineWidth = 1.5f) {
                moveTo(c - r, c); arcTo(r, r, 0f, true, true, c + r, c); arcTo(r, r, 0f, true, true, c - r, c); close()
            }
        }.build()
    }
}

@Composable
fun VkFieldMotif(modifier: Modifier = Modifier, color: Color = LocalVkExtra.current.ornament) {
    val v = remember(color) { VkDecor.fieldMotif(color) }
    Image(v, contentDescription = null, modifier = modifier, contentScale = ContentScale.FillBounds)
}

@Composable
fun VkToranEdge(modifier: Modifier = Modifier, fill: Color = MaterialTheme.colorScheme.surface) {
    val v = remember(fill) { VkDecor.toranEdge(fill) }
    Image(v, contentDescription = null, modifier = modifier.fillMaxWidth().height(12.dp), contentScale = ContentScale.FillBounds)
}

@Composable
fun VkLeafDivider(modifier: Modifier = Modifier) {
    val x = LocalVkExtra.current
    val v = remember(x.leaf1, x.leaf2) { VkDecor.leafDivider(x.leaf1, x.leaf2) }
    Image(v, contentDescription = null, modifier = modifier.size(24.dp, 16.dp))
}

@Composable
fun VkRangoliDots(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalVkExtra.current.panchangOrnament
    val v = remember(color) { VkDecor.rangoliDots(color) }
    Image(v, contentDescription = null, modifier = modifier.size(size))
}

@Composable
fun VkMoonPhase(illumination: Float, waxing: Boolean, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val cs = MaterialTheme.colorScheme
    val v = remember(illumination, waxing, cs.surfaceContainer, cs.primary, cs.tertiary) {
        VkDecor.moonPhase(illumination, waxing, cs.surfaceContainer, cs.primary, cs.tertiary)
    }
    Image(v, contentDescription = null, modifier = modifier.size(size))
}
