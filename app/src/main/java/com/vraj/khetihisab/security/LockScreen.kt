package com.vraj.khetihisab.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkNumericKeypad
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.decor.VkFieldMotif
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkLogoImage
import com.vraj.khetihisab.ui.theme.VkWordmarkStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LockMode { ENTER, SET_NEW, CONFIRM }

/**
 * PIN lock (doc 04, artboard 01). First run (no PIN) runs the two-step set-PIN flow.
 * Nothing else composes underneath while this is shown (VkRoot), so content never flashes.
 *
 * @param biometricAvailable device has an enrolled biometric (hide the button otherwise, no error).
 * @param onBiometric launches BiometricPrompt; on success the caller unlocks.
 */
@Composable
fun LockScreen(
    pinStore: PinStore,
    onUnlocked: () -> Unit,
    biometricAvailable: Boolean,
    onBiometric: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(if (pinStore.hasPin()) LockMode.ENTER else LockMode.SET_NEW) }
    var entered by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var lockoutMs by remember { mutableLongStateOf(pinStore.lockoutRemainingMillis()) }
    var showForgot by remember { mutableStateOf(false) }
    var askBiometric by remember { mutableStateOf(false) }
    // #53: a wrong PIN shakes the dots and gives a short "reject" buzz.
    var shake by remember { mutableStateOf(0) }

    val wrongText = stringResource(R.string.lock_wrong)
    val mismatchText = stringResource(R.string.lock_mismatch)
    val bioEnabled = biometricAvailable && pinStore.biometricEnabled && mode == LockMode.ENTER

    // Auto-prompt biometric when the lock screen opens (doc 04 §4).
    LaunchedEffect(Unit) { if (bioEnabled && lockoutMs == 0L) onBiometric() }

    // Lockout countdown.
    LaunchedEffect(lockoutMs > 0) {
        while (lockoutMs > 0) {
            delay(1_000)
            lockoutMs = pinStore.lockoutRemainingMillis()
        }
    }

    fun submit(pin: String) {
        when (mode) {
            LockMode.SET_NEW -> {
                firstPin = pin; entered = ""; message = null; mode = LockMode.CONFIRM
            }
            LockMode.CONFIRM -> {
                if (pin == firstPin) {
                    busy = true
                    scope.launch {
                        withContext(Dispatchers.Default) { pinStore.setPin(pin) }
                        busy = false
                        entered = ""
                        if (biometricAvailable) askBiometric = true else onUnlocked()
                    }
                } else {
                    message = mismatchText; entered = ""; firstPin = ""; mode = LockMode.SET_NEW; shake++
                }
            }
            LockMode.ENTER -> {
                busy = true
                scope.launch {
                    val r = withContext(Dispatchers.Default) { pinStore.verify(pin) }
                    busy = false
                    entered = ""
                    when (r) {
                        PinStore.VerifyResult.Success -> onUnlocked()
                        is PinStore.VerifyResult.Wrong -> { message = wrongText; shake++ }
                        is PinStore.VerifyResult.LockedOut -> { message = wrongText; lockoutMs = r.remainingMillis; shake++ }
                        PinStore.VerifyResult.NoPin -> mode = LockMode.SET_NEW
                    }
                }
            }
        }
    }

    val inputBlocked = busy || lockoutMs > 0

    Box(Modifier.fillMaxSize().background(cs.surface)) {
        // Field motif at 6 % primary behind the top of the screen.
        VkFieldMotif(Modifier.fillMaxWidth().height(220.dp), color = cs.primary.copy(alpha = 0.06f))
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            VkLogoImage(size = 96.dp)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.app_wordmark), style = VkWordmarkStyle, color = cs.primary, maxLines = 1)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(
                    when (mode) {
                        LockMode.ENTER -> R.string.lock_enter_pin
                        LockMode.SET_NEW -> R.string.lock_set_new
                        LockMode.CONFIRM -> R.string.lock_confirm
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            PinDots(count = entered.length, shake = shake)
            // Reserved message slot: fixed height so nothing jumps (doc 17 #10).
            Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                val text = when {
                    lockoutMs > 0 -> stringResource(R.string.lock_retry_in, formatMmSs(lockoutMs))
                    else -> message
                }
                if (text != null) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.error,
                        maxLines = 1,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = text
                            liveRegion = LiveRegionMode.Polite
                        },
                    )
                }
            }
            VkNumericKeypad(
                onKey = { c ->
                    if (!inputBlocked && entered.length < PinStore.PIN_LENGTH) {
                        message = null
                        entered += c
                        if (entered.length == PinStore.PIN_LENGTH) submit(entered)
                    }
                },
                onBackspace = { if (!inputBlocked && entered.isNotEmpty()) entered = entered.dropLast(1) },
                allowDecimal = false,
                keySize = 76.dp,
                keyShape = CircleShape,
                fillWidth = false,
            )
            Spacer(Modifier.height(16.dp))
            // Reserved 60dp slot for the biometric button (always reserved: no layout jump).
            Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                if (bioEnabled) {
                    VkTonalButton(
                        stringResource(R.string.lock_bio),
                        onClick = { if (lockoutMs == 0L) onBiometric() },
                        icon = VkIcon.LOCK,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = lockoutMs == 0L,
                    )
                }
            }
            if (mode == LockMode.ENTER) {
                VkTextButton(stringResource(R.string.lock_forgot), onClick = { showForgot = true })
            }
        }
    }

    if (showForgot) {
        com.vraj.khetihisab.ui.components.VkDialog(
            title = stringResource(R.string.lock_forgot_title),
            onDismiss = { showForgot = false },
            confirmLabel = stringResource(R.string.lock_forgot_ok),
            onConfirm = { showForgot = false },
            dismissLabel = null,
        ) { Text(stringResource(R.string.lock_forgot_body), style = MaterialTheme.typography.bodyLarge) }
    }

    if (askBiometric) {
        com.vraj.khetihisab.ui.components.VkDialog(
            title = stringResource(R.string.lock_bio_enable_title),
            onDismiss = { pinStore.biometricEnabled = false; askBiometric = false; onUnlocked() },
            confirmLabel = stringResource(R.string.lock_bio_enable_yes),
            onConfirm = { pinStore.biometricEnabled = true; askBiometric = false; onUnlocked() },
            dismissLabel = stringResource(R.string.lock_bio_enable_no),
        ) { Text(stringResource(R.string.lock_bio_enable_body), style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun PinDots(count: Int, shake: Int = 0) {
    val cs = MaterialTheme.colorScheme
    val desc = stringResource(R.string.lock_digits_entered, count)
    val view = androidx.compose.ui.platform.LocalView.current
    val motion = com.vraj.khetihisab.ui.components.LocalVkMotion.current.enabled
    val dx = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(shake) {
        if (shake <= 0) return@LaunchedEffect
        view.performHapticFeedback(
            if (android.os.Build.VERSION.SDK_INT >= 30) android.view.HapticFeedbackConstants.REJECT else android.view.HapticFeedbackConstants.LONG_PRESS,
        )
        if (motion) {
            for (x in listOf(-14f, 12f, -9f, 6f, -3f, 0f)) dx.animateTo(x, androidx.compose.animation.core.tween(55))
        }
    }
    Row(
        Modifier.graphicsLayer { translationX = dx.value * density }.clearAndSetSemantics {
            // TalkBack: "4 માંથી 2 અંક દાખલ થયા". Digits are never spoken.
            contentDescription = desc
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        repeat(PinStore.PIN_LENGTH) { i ->
            val filled = i < count
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (filled) cs.primary else cs.surface)
                    .border(2.dp, if (filled) cs.primary else cs.outline, CircleShape),
            )
        }
    }
}

internal fun formatMmSs(ms: Long): String {
    val total = (ms + 999) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
