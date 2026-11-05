package com.vraj.khetihisab.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Tiles (owner request 2026-11-04): ONE tile language for Home and Settings.
 * Neutral card + one-hue ornament in a corner, icon on a primaryContainer disc, single-line labels,
 * a staggered rise-in on first show and a springy press. Colours only from the theme (doc 01 §7).
 */

/** Which corner ornament a tile draws. All use the theme's single hue. */
enum class VkTileOrnament { LEAF, RANGOLI, MOON, FIELD, WAVE, SUN }

/**
 * Rise + fade in, staggered by [index] (capped at 6, so nothing waits long); plays once per screen
 * visit (not on every scroll). Skipped when the phone's animations are off ([LocalVkMotion]).
 */
fun Modifier.vkAppear(index: Int, key: String = "appear"): Modifier = composed {
    var played by rememberSaveable(key) { mutableStateOf(false) }
    val motion = LocalVkMotion.current.enabled
    val a = remember { Animatable(if (played || !motion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played && motion) {
            delay(40L * index.coerceIn(0, 6))
            a.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
            played = true
        }
    }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 28.dp.toPx()
    }
}

/** Springy 0.96 press scale for any clickable surface sharing [interaction]. */
fun Modifier.vkPressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalVkMotion.current.enabled
    val s by animateFloatAsState(if (pressed && motion) pressedScale else 1f, spring(dampingRatio = 0.55f, stiffness = 600f), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/**
 * The standard tile. [value] (optional) is the big line under the title — pass a composable so Home
 * can mask money. Minimum height 128dp keeps a 2-column grid even; the whole tile is the touch target.
 */
@Composable
fun VkTile(
    icon: VkIcon,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    ornament: VkTileOrnament = VkTileOrnament.LEAF,
    index: Int = 0,
    testTag: String? = null,
    value: (@Composable () -> Unit)? = null,
    /** Red flag (M3 error) — e.g. backup not taken today. */
    alert: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val x = LocalVkExtra.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .vkAppear(index, "tile_$title")
            .vkPressScale(interaction)
            .heightIn(min = 128.dp)
            .clip(shape)
            .background(cs.surfaceContainerLowest)
            .border(1.dp, cs.outlineVariant, shape)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) {
        TileOrnament(ornament, x.ornament, cs.primaryContainer, Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                IconDisc(icon, breathe = alert)
                if (alert) VkAlertFlag(Modifier.align(Alignment.TopEnd))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (value != null) value()
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (alert) FontWeight.Bold else FontWeight.Normal),
                    color = if (alert) cs.error else cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Icon on a soft disc — the tile's focal point. The slow "breathing" ring plays only when
 * [breathe] (alert tiles), so calm tiles stay still.
 */
@Composable
fun IconDisc(icon: VkIcon, size: Dp = 52.dp, onGradient: Boolean = false, breathe: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val x = LocalVkExtra.current
    val ring = if (breathe && LocalVkMotion.current.enabled) {
        val t = rememberInfiniteTransition(label = "disc")
        t.animateFloat(0.0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "ring").value
    } else {
        0.5f
    }
    val disc = if (onGradient) x.headerButton else cs.primaryContainer
    val ringColor = if (onGradient) x.onGradient else cs.primary
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(disc)
            drawCircle(ringColor.copy(alpha = 0.10f + 0.18f * ring), radius = this.size.minDimension / 2f - 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
        }
        VkIconImage(icon, size = size * 0.62f)
    }
}

@Composable
private fun TileOrnament(kind: VkTileOrnament, line: Color, blob: Color, modifier: Modifier) {
    Canvas(modifier) {
        // Soft corner blob (top-right) — same for every tile, so the grid reads as one family.
        drawCircle(blob.copy(alpha = 0.55f), radius = size.height * 0.48f, center = Offset(size.width * 1.02f, -size.height * 0.08f))
        // Top-right, inside the blob: never under the title or subtitle text.
        val c = Offset(size.width - 34.dp.toPx(), 34.dp.toPx())
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        val col = line.copy(alpha = 0.55f)
        when (kind) {
            VkTileOrnament.LEAF -> leaf(c, 18.dp.toPx(), col, stroke)
            VkTileOrnament.RANGOLI -> rangoli(c, 14.dp.toPx(), col)
            VkTileOrnament.MOON -> {
                drawCircle(col, radius = 13.dp.toPx(), center = c, style = stroke)
                drawCircle(blob, radius = 11.dp.toPx(), center = c + Offset(6.dp.toPx(), -4.dp.toPx()))
            }
            VkTileOrnament.FIELD -> for (i in 0..3) {
                val y = c.y - 18.dp.toPx() + i * 8.dp.toPx()
                drawLine(col, Offset(c.x - 22.dp.toPx() + i * 4.dp.toPx(), y), Offset(c.x + 14.dp.toPx(), y - 6.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            VkTileOrnament.WAVE -> {
                val p = Path()
                val w = 44.dp.toPx()
                for (row in 0..1) {
                    val y0 = c.y - 6.dp.toPx() + row * 9.dp.toPx()
                    p.moveTo(c.x - w / 2, y0)
                    p.cubicTo(c.x - w / 4, y0 - 7.dp.toPx(), c.x + w / 4, y0 + 7.dp.toPx(), c.x + w / 2, y0)
                }
                drawPath(p, col, style = stroke)
            }
            VkTileOrnament.SUN -> {
                drawCircle(col, radius = 7.dp.toPx(), center = c, style = stroke)
                for (i in 0 until 8) {
                    val a = i * PI / 4
                    drawLine(
                        col,
                        c + Offset((11.dp.toPx() * cos(a)).toFloat(), (11.dp.toPx() * sin(a)).toFloat()),
                        c + Offset((16.dp.toPx() * cos(a)).toFloat(), (16.dp.toPx() * sin(a)).toFloat()),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

private fun DrawScope.leaf(c: Offset, r: Float, col: Color, stroke: Stroke) {
    val p = Path().apply {
        moveTo(c.x - r, c.y + r * 0.6f)
        quadraticTo(c.x - r * 0.2f, c.y - r, c.x + r, c.y - r * 0.6f)
        quadraticTo(c.x + r * 0.2f, c.y + r, c.x - r, c.y + r * 0.6f)
    }
    drawPath(p, col, style = stroke)
    drawLine(col, Offset(c.x - r, c.y + r * 0.6f), Offset(c.x + r * 0.55f, c.y - r * 0.35f), strokeWidth = stroke.width * 0.8f)
}

private fun DrawScope.rangoli(c: Offset, r: Float, col: Color) {
    for (i in 0 until 8) {
        val a = i * PI / 4
        drawCircle(col, radius = 2.2f * density, center = c + Offset((r * cos(a)).toFloat(), (r * sin(a)).toFloat()))
    }
    drawCircle(col, radius = 3f * density, center = c)
}

/** Two tiles per row with equal height. */
@Composable
fun VkTileRow(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

// ------------------------------------------------------------------ hero tile (Home "હિસાબ")

/**
 * Brand-gradient hero tile with a slow light sweep. Holds sub-tiles ([VkSubTile]) and an optional
 * trailing action (the eye toggle).
 */
@Composable
fun VkHeroTile(
    icon: VkIcon,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val x = LocalVkExtra.current
    val shape = RoundedCornerShape(16.dp)
    // Light sweep plays once per visit (audit #52), not forever.
    val motion = LocalVkMotion.current.enabled
    val sweepA = remember { Animatable(if (motion) -0.4f else 1.4f) }
    LaunchedEffect(Unit) { if (motion) sweepA.animateTo(1.4f, tween(1800, easing = FastOutSlowInEasing)) }
    val sweep = sweepA.value
    Box(modifier.vkAppear(0, "hero_$title").clip(shape).background(x.brandBrush)) {
        Canvas(Modifier.matchParentSize()) {
            // Light sweep — a soft diagonal band moving across the gradient.
            val w = size.width
            val cx = w * sweep
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, x.onGradient.copy(alpha = 0.10f), Color.Transparent),
                    start = Offset(cx - w * 0.25f, 0f),
                    end = Offset(cx + w * 0.25f, size.height),
                ),
                size = Size(w, size.height),
            )
            // Field furrows in the corner.
            for (i in 0..4) {
                val y = size.height - 14.dp.toPx() - i * 9.dp.toPx()
                drawLine(x.onGradient.copy(alpha = 0.10f), Offset(w * 0.55f + i * 10.dp.toPx(), y), Offset(w, y - 18.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDisc(icon, size = 48.dp, onGradient = true)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = x.onGradient, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = x.onGradient.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (trailing != null) trailing()
            }
            content()
        }
    }
}

/** A white sub-tile inside [VkHeroTile]. */
@Composable
fun VkSubTile(
    icon: VkIcon,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    value: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .vkPressScale(interaction)
            .heightIn(min = 104.dp)
            .clip(shape)
            .background(cs.surfaceContainerLowest)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Icon above the title: long Gujarati titles ("કુલ નફો-નુકસાન") get the full width.
        VkIconImage(icon, size = 30.dp)
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        value()
    }
}

// ------------------------------------------------------------------ masked money

/** Text shown instead of a hidden amount (owner wording "XXXXX.XX"). */
const val VK_MASK = "₹XXXXX.XX"

/**
 * Money that can be hidden. Revealed amounts count up from 0 (600 ms) — a small delight that also
 * shows the number is live. Single line; long amounts autosize down to 20sp (audit #4), never clip
 * with an ellipsis.
 */
@Composable
fun VkMaskedRupees(
    paise: Long,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    color: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String? = null,
) {
    AnimatedContent(
        targetState = revealed,
        transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(160)) },
        label = "mask",
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) { show ->
        if (!show) {
            FitText(VK_MASK, style, color.copy(alpha = 0.75f))
        } else {
            val motion = LocalVkMotion.current.enabled
            val a = remember { Animatable(if (motion) 0f else 1f) }
            LaunchedEffect(paise) {
                if (motion) {
                    a.snapTo(0f)
                    a.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                }
            }
            val shown = (paise * a.value).toLong()
            val text = (if (paise < 0) "−" else "") + VkFormat.rupees(kotlin.math.abs(shown))
            FitText(text, style, color)
        }
    }
}

@Composable
private fun FitText(text: String, style: TextStyle, color: Color) =
    VkFitText(text, style, color, minSize = 20)

/** Eye toggle (like a password field). 60dp touch target, 44dp visible disc. */
@Composable
fun VkEyeToggle(revealed: Boolean, onToggle: () -> Unit, showLabel: String, hideLabel: String, onGradient: Boolean = true) {
    val x = LocalVkExtra.current
    val cs = MaterialTheme.colorScheme
    val disc = if (onGradient) x.headerButton else cs.primaryContainer
    val ink = if (onGradient) x.onGradient else cs.onPrimaryContainer
    val interaction = remember { MutableInteractionSource() }
    val label = if (revealed) hideLabel else showLabel
    Box(
        Modifier
            .size(60.dp)
            .vkPressScale(interaction)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = label }
            .testTag("eye_toggle"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(44.dp)) {
            drawCircle(disc)
            val w = size.width * 0.62f
            val h = size.height * 0.36f
            val c = center
            val eye = Path().apply {
                moveTo(c.x - w / 2, c.y)
                quadraticTo(c.x, c.y - h, c.x + w / 2, c.y)
                quadraticTo(c.x, c.y + h, c.x - w / 2, c.y)
                close()
            }
            val st = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawPath(eye, ink, style = st)
            drawCircle(ink, radius = h * 0.32f, center = c)
            if (!revealed) {
                drawLine(ink, Offset(c.x - w / 2, c.y + h * 0.9f), Offset(c.x + w / 2, c.y - h * 0.9f), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}


/** Small red "!" flag with a pulsing ring (error role only — red never means anything else). */
@Composable
fun VkAlertFlag(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "flag")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "pulse")
    Box(modifier.size(size).testTag("alert_flag"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(cs.error.copy(alpha = 0.35f * (1f - pulse)), radius = this.size.minDimension / 2f * (1f + 0.5f * pulse))
            drawCircle(cs.error, radius = this.size.minDimension / 2f)
        }
        Text("!", color = cs.onError, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
    }
}
