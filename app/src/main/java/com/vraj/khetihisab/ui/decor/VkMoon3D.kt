package com.vraj.khetihisab.ui.decor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import kotlin.math.abs

/*
 * A realistic, embossed moon (owner request 2026-11-04: "should look like a real moon").
 * Drawn, not a bitmap, so it is sharp at 8dp calendar markers and at 56dp in the Panchang card:
 *  - sphere shading on the lit side (light from the sun's side, limb darkening at the edge),
 *  - earthshine: the dark side is a faint blue-grey disc, never invisible,
 *  - maria (the dark "seas") and a few craters with lit rims, placed like the real near side,
 *  - a soft terminator and a halo + drop shadow so it sits ON the card.
 * The moon's own greys are physical colours (like a photo), not theme roles; the halo uses the theme.
 */

/** Maria as (cx, cy, rx, ry, alpha) in unit-disc coordinates (−1..1), roughly the real near side. */
private val MARIA = listOf(
    floatArrayOf(-0.32f, -0.38f, 0.34f, 0.26f, 0.30f), // Imbrium
    floatArrayOf(0.12f, -0.30f, 0.20f, 0.18f, 0.26f), // Serenitatis
    floatArrayOf(0.30f, -0.02f, 0.24f, 0.20f, 0.28f), // Tranquillitatis
    floatArrayOf(0.66f, -0.18f, 0.13f, 0.11f, 0.30f), // Crisium
    floatArrayOf(-0.52f, 0.06f, 0.30f, 0.40f, 0.24f), // Procellarum
    floatArrayOf(-0.12f, 0.34f, 0.20f, 0.15f, 0.22f), // Nubium
    floatArrayOf(0.40f, 0.30f, 0.16f, 0.14f, 0.20f), // Fecunditatis
)

/** Craters (cx, cy, r) — Tycho, Copernicus, Kepler, Plato and two small ones. */
private val CRATERS = listOf(
    floatArrayOf(-0.14f, 0.66f, 0.07f),
    floatArrayOf(-0.30f, -0.06f, 0.06f),
    floatArrayOf(-0.56f, -0.10f, 0.04f),
    floatArrayOf(-0.20f, -0.70f, 0.05f),
    floatArrayOf(0.54f, 0.52f, 0.04f),
    floatArrayOf(0.20f, 0.62f, 0.035f),
)

private val LIT_HI = Color(0xFFFFFBF0)
private val LIT_MID = Color(0xFFE8E2D2)
private val LIT_LO = Color(0xFFB8B09E)
private val MARE = Color(0xFF6E6858)
private val DARK_HI = Color(0xFF3A3F4C)
private val DARK_LO = Color(0xFF181B22)

/**
 * [illumination] 0 = new (અમાસ), 1 = full (પૂનમ). Waxing (સુદ) is lit on the right, as seen from India.
 * [detailed] = maria, craters and halo (off for tiny calendar markers).
 */
@Composable
fun VkMoon3D(illumination: Float, waxing: Boolean, modifier: Modifier = Modifier, size: Dp = 48.dp, detailed: Boolean = size >= 24.dp) {
    val halo = LocalVkExtra.current.panchangOrnament
    val k = illumination.coerceIn(0f, 1f)
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f * if (detailed) 0.84f else 0.96f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        if (detailed) {
            // Halo, stronger the fuller the moon.
            drawCircle(
                Brush.radialGradient(listOf(halo.copy(alpha = 0.10f + 0.30f * k), Color.Transparent), center = c, radius = r * 1.22f),
                radius = r * 1.22f, center = c,
            )
            // Drop shadow under the sphere (embossed, sits on the card).
            drawCircle(Color.Black.copy(alpha = 0.22f), radius = r, center = c + Offset(r * 0.06f, r * 0.10f))
        }
        moon(c, r, k, waxing, detailed)
    }
}

private fun DrawScope.moon(c: Offset, r: Float, k: Float, waxing: Boolean, detailed: Boolean) {
    val disc = Path().apply { addOval(Rect(c, r)) }
    val litSide = if (waxing) 1f else -1f

    // 1. Earthshine — the whole disc, dim blue-grey sphere.
    drawCircle(
        Brush.radialGradient(listOf(DARK_HI, DARK_LO), center = c + Offset(-litSide * r * 0.25f, -r * 0.3f), radius = r * 1.3f),
        radius = r, center = c,
    )
    if (detailed) clipPath(disc) { maria(c, r, alphaScale = 0.35f, color = Color.Black) }

    // 2. Lit part (limb semicircle + terminator half-ellipse).
    if (k > 0.004f) {
        val lit = litPath(c, r, k, waxing)
        clipPath(lit) {
            // Sphere shading: brightest toward the sun, darker at the limb and toward the terminator.
            drawCircle(
                Brush.radialGradient(
                    listOf(LIT_HI, LIT_MID, LIT_LO),
                    center = c + Offset(litSide * r * 0.35f, -r * 0.30f),
                    radius = r * 1.45f,
                ),
                radius = r, center = c,
            )
            if (detailed) {
                maria(c, r, alphaScale = 1.7f, color = MARE)
                craters(c, r, litSide)
            }
            // Soft terminator: darken a band along the shadow edge.
            val tx = c.x + litSide * r * (1f - 2f * k)
            drawRect(
                Brush.horizontalGradient(
                    if (waxing) listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent) else listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                    startX = if (waxing) tx - r * 0.05f else tx - r * 0.35f,
                    endX = if (waxing) tx + r * 0.35f else tx + r * 0.05f,
                ),
                topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r),
            )
        }
    }

    // 3. Limb darkening over everything + a fine rim light: the 3D "ball" feel.
    drawCircle(
        Brush.radialGradient(
            0.0f to Color.Transparent, 0.72f to Color.Transparent, 1.0f to Color.Black.copy(alpha = 0.35f),
            center = c, radius = r,
        ),
        radius = r, center = c,
    )
    drawCircle(Color.White.copy(alpha = if (detailed) 0.22f else 0.30f), radius = r - 0.5f, center = c, style = Stroke(width = (r * 0.05f).coerceAtLeast(0.8f)))
}

/** The sunlit region for illumination [k]: crescent bulges to the lit side, gibbous to the dark side. */
private fun litPath(c: Offset, r: Float, k: Float, waxing: Boolean): Path {
    if (k >= 0.996f) return Path().apply { addOval(Rect(c, r)) }
    val rx = r * abs(1f - 2f * k)
    val path = Path()
    // Limb: the lit half.
    path.moveTo(c.x, c.y - r)
    path.arcTo(Rect(c, r), -90f, if (waxing) 180f else -180f, false)
    // Terminator back up: half of an ellipse with x-radius rx.
    val gibbous = k > 0.5f
    val ell = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)
    val towardDark = gibbous // gibbous: terminator lies on the dark side
    // From bottom (90°) back to top (−90°); choose the half that bulges correctly.
    val sweep = if (waxing == towardDark) 180f else -180f
    path.arcTo(ell, 90f, sweep, false)
    path.close()
    return path
}

private fun DrawScope.maria(c: Offset, r: Float, alphaScale: Float, color: Color) {
    for (m in MARIA) {
        val center = Offset(c.x + m[0] * r, c.y + m[1] * r)
        val rx = m[2] * r
        val ry = m[3] * r
        drawOval(
            Brush.radialGradient(listOf(color.copy(alpha = m[4] * alphaScale), color.copy(alpha = m[4] * alphaScale * 0.4f), Color.Transparent), center = center, radius = maxOf(rx, ry)),
            topLeft = Offset(center.x - rx, center.y - ry), size = Size(2 * rx, 2 * ry),
        )
    }
}

private fun DrawScope.craters(c: Offset, r: Float, litSide: Float) {
    for (cr in CRATERS) {
        val center = Offset(c.x + cr[0] * r, c.y + cr[1] * r)
        val rr = cr[2] * r
        // Floor (shadowed away from the sun) + bright rim toward the sun.
        drawCircle(MARE.copy(alpha = 0.35f), radius = rr, center = center + Offset(-litSide * rr * 0.25f, 0f))
        drawCircle(Color.White.copy(alpha = 0.45f), radius = rr, center = center + Offset(litSide * rr * 0.2f, -rr * 0.15f), style = Stroke(width = rr * 0.35f))
    }
}
