package com.vraj.khetihisab.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.ui.theme.VkCurrencyStyle
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/*
 * Feedback + motion building blocks (design audit, final release). Colours come only from
 * MaterialTheme.colorScheme, so all five themes (night included) work unchanged.
 */

// ------------------------------------------------------------------ motion

/** Whether decorative motion plays. Off when the phone's "animator duration scale" is 0. */
@Stable
data class VkMotion(val enabled: Boolean = true)

/** Provided by VkRoot from [vkMotionFrom]; defaults to on (previews, UI tests). */
val LocalVkMotion = staticCompositionLocalOf { VkMotion() }

/** Reads Settings.Global.ANIMATOR_DURATION_SCALE — 0 means the user turned animations off. */
fun vkMotionFrom(context: Context): VkMotion {
    val scale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    return VkMotion(enabled = scale != 0f)
}

// ------------------------------------------------------------------ fitting text / money

/**
 * One line that autosizes down to [minSize] sp instead of clipping or wrapping — big lakh amounts
 * stay whole (audit #3, #4).
 */
@Composable
fun VkFitText(
    text: String,
    style: TextStyle,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    minSize: Int = 18,
    textAlign: TextAlign? = null,
) {
    val max = if (style.fontSize.isSp && style.fontSize.value > minSize) style.fontSize else TextUnit(minSize.toFloat(), TextUnitType.Sp)
    BasicText(
        text = text,
        style = style.copy(color = color, textAlign = textAlign ?: style.textAlign),
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
        autoSize = TextAutoSize.StepBased(minFontSize = TextUnit(minSize.toFloat(), TextUnitType.Sp), maxFontSize = max),
    )
}

/**
 * Money that fits its box (autosize, min 18sp) and counts up from the previous value when it
 * changes (A2). Sign: "−" for negatives; [plusSign] adds "+" for positives.
 */
@Composable
fun VkFitMoney(
    paise: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = VkCurrencyStyle,
    color: Color = MaterialTheme.colorScheme.onSurface,
    plusSign: Boolean = false,
    minSize: Int = 18,
    testTag: String? = null,
) {
    val motion = LocalVkMotion.current.enabled
    val anim = remember { Animatable(paise.toFloat()) }
    LaunchedEffect(paise) {
        if (motion && anim.value != paise.toFloat()) {
            anim.animateTo(paise.toFloat(), tween(650, easing = FastOutSlowInEasing))
        } else {
            anim.snapTo(paise.toFloat())
        }
    }
    // While animating show whole rupees; the exact value (with paise) once settled.
    val shown = if (anim.isRunning) (anim.value.toLong() / 100) * 100 else paise
    val sign = when {
        shown < 0 -> "−"
        plusSign && shown > 0 -> "+"
        else -> ""
    }
    VkFitText(
        sign + VkFormat.rupees(abs(shown)),
        style,
        color,
        modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        minSize = minSize,
    )
}

// ------------------------------------------------------------------ snackbar + success burst

/**
 * App-level snackbar (one host in VkRoot, above every dock). Screens call [show]; deletes pass
 * an undo action ("પાછું લાવો"). [success] also plays the [VkSuccessBurst] tick (A1).
 * [scope] lives as long as the activity, so work started from a closing sheet still finishes.
 */
@Stable
class VkSnackbarController(val hostState: SnackbarHostState, val scope: CoroutineScope) {
    var burstKey by mutableIntStateOf(0)
        private set

    fun show(message: String, actionLabel: String? = null, success: Boolean = false, onAction: (() -> Unit)? = null) {
        if (success) burstKey++
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val r = hostState.showSnackbar(
                message,
                actionLabel = actionLabel,
                withDismissAction = false,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (r == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    /** Plays the success tick without a message. */
    fun celebrate() {
        burstKey++
    }
}

/** Null outside VkRoot (UI tests); use [rememberVkSnackbar] which always returns a controller. */
val LocalVkSnackbar = staticCompositionLocalOf<VkSnackbarController?> { null }

/** The app controller, or a local throw-away one (messages then simply are not shown). */
@Composable
fun rememberVkSnackbar(): VkSnackbarController {
    val app = LocalVkSnackbar.current
    if (app != null) return app
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return remember { VkSnackbarController(SnackbarHostState(), scope) }
}

/** Host + burst overlay; place once at the root, inside a Box that fills the screen. */
@Composable
fun BoxScope.VkSnackbarHost(controller: VkSnackbarController, bottomPadding: Dp = 104.dp) {
    VkSuccessBurst(controller.burstKey, Modifier.align(Alignment.Center))
    SnackbarHost(
        controller.hostState,
        Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding)
            .testTag("vk_snackbar"),
    ) { data ->
        val cs = MaterialTheme.colorScheme
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(16.dp),
            containerColor = cs.inverseSurface,
            contentColor = cs.inverseOnSurface,
            actionColor = cs.inversePrimary,
        )
    }
}

/**
 * Animated check (A1): a disc pops in, the tick draws itself, then everything fades (~1.1 s).
 * Plays each time [trigger] changes to a value > 0. Decorative only.
 */
@Composable
fun VkSuccessBurst(trigger: Int, modifier: Modifier = Modifier, size: Dp = 112.dp) {
    if (trigger <= 0) return
    val motion = LocalVkMotion.current.enabled
    val p = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { p.animateTo(1f, tween(if (motion) 1100 else 600)) }
    val t = p.value
    if (t >= 1f) return
    val cs = MaterialTheme.colorScheme
    val pop = if (!motion) 1f else when {
        t < 0.22f -> 0.6f + 0.5f * (t / 0.22f) // 0.6 → 1.1
        t < 0.35f -> 1.1f - 0.1f * ((t - 0.22f) / 0.13f)
        else -> 1f
    }
    val alpha = if (t > 0.75f) 1f - (t - 0.75f) / 0.25f else 1f
    val tick = if (!motion) 1f else ((t - 0.18f) / 0.32f).coerceIn(0f, 1f)
    Canvas(
        modifier
            .size(size)
            .graphicsLayer { scaleX = pop; scaleY = pop; this.alpha = alpha }
            .testTag("vk_success_burst"),
    ) {
        val r = this.size.minDimension / 2f
        drawCircle(cs.primary.copy(alpha = 0.16f), radius = r * (1f + 0.25f * t))
        drawCircle(cs.primary, radius = r * 0.78f)
        val path = Path().apply {
            moveTo(center.x - r * 0.36f, center.y + r * 0.02f)
            lineTo(center.x - r * 0.08f, center.y + r * 0.30f)
            lineTo(center.x + r * 0.40f, center.y - r * 0.26f)
        }
        val m = PathMeasure().apply { setPath(path, false) }
        val part = Path()
        m.getSegment(0f, m.length * tick, part, true)
        drawPath(part, cs.onPrimary, style = Stroke(width = r * 0.14f, cap = StrokeCap.Round))
    }
}

/**
 * Small animated check-off (A7): a ring fills and a tick draws in when [checked] turns true.
 * Used for shopping "bought", reminder "done" and udhaar "ચૂકતે".
 */
@Composable
fun VkCheckMark(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val motion = LocalVkMotion.current.enabled
    val p by animateFloatAsState(if (checked) 1f else 0f, if (motion) tween(420, easing = FastOutSlowInEasing) else tween(0), label = "check")
    val cs = MaterialTheme.colorScheme
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(cs.outline, radius = r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        if (p > 0f) {
            drawCircle(cs.primary, radius = (r - 1.dp.toPx()) * p)
            val path = Path().apply {
                moveTo(center.x - r * 0.42f, center.y + r * 0.02f)
                lineTo(center.x - r * 0.10f, center.y + r * 0.34f)
                lineTo(center.x + r * 0.46f, center.y - r * 0.30f)
            }
            val m = PathMeasure().apply { setPath(path, false) }
            val part = Path()
            m.getSegment(0f, m.length * ((p - 0.3f) / 0.7f).coerceIn(0f, 1f), part, true)
            drawPath(part, cs.onPrimary, style = Stroke(width = r * 0.2f, cap = StrokeCap.Round))
        }
    }
}

// ------------------------------------------------------------------ progress, loading, empty

/** Rounded progress bar that grows from 0 to [fraction] on first show (A5). */
@Composable
fun VkProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceContainer,
    height: Dp = 10.dp,
) {
    val motion = LocalVkMotion.current.enabled
    val target = fraction.coerceIn(0f, 1f)
    val a = remember { Animatable(if (motion) 0f else target) }
    LaunchedEffect(target) {
        if (motion) a.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 120f)) else a.snapTo(target)
    }
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(a.value.coerceIn(0f, 1f)).height(height).clip(CircleShape).background(color))
    }
}

/** Centered spinner with an optional line of text. */
@Composable
fun VkLoading(modifier: Modifier = Modifier, text: String? = null) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        if (text != null) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Empty list placeholder (audit #16, #21): the section's ic_vk icon, one friendly line and an
 * optional tonal action. Use inside a card or directly in a list.
 */
@Composable
fun VkEmptyState(
    icon: VkIcon,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp).testTag("vk_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconDisc(icon, size = 72.dp)
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            VkTonalButton(actionLabel, onAction)
        }
    }
}

/** Pulse ring used by the backup-done celebration (A3): one soft ring that expands and fades. */
@Composable
fun VkPulseRing(trigger: Any?, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val motion = LocalVkMotion.current.enabled
    val p = remember(trigger) { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(trigger) { if (motion) p.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    if (p.value >= 1f) return
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(color.copy(alpha = 0.45f * (1f - p.value)), radius = r * (0.6f + 0.6f * p.value), style = Stroke(3.dp.toPx()))
    }
}
