package com.vraj.khetihisab.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkTextButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Shared so the Settings "એપ અપડેટ" tile can trigger the same dialog. */
object UpdateState {
    val available = MutableStateFlow<UpdateInfo?>(null)
    /**
     * Last GitHub check (ms). Android keeps the process alive for days, so "once per process" missed
     * updates on real phones (owner, Pixel 10 Pro, 2026-11-05): now every time the app comes to the
     * front, at most once per [MIN_GAP_MS].
     */
    @Volatile var lastCheckAt = 0L
    const val MIN_GAP_MS = 10 * 60 * 1000L

    /** One check; safe to call from anywhere. Returns the newer release or null. */
    suspend fun refresh(force: Boolean = false): UpdateInfo? {
        val now = System.currentTimeMillis()
        if (!force && now - lastCheckAt < MIN_GAP_MS) return available.value
        lastCheckAt = now
        val u = AppUpdater.check()
        available.value = u
        return u
    }
}

/**
 * Drawn over the unlocked app. On first show it asks GitHub for a newer release; if there is one, a
 * single big button updates the app (owner: "user will only click one button").
 */
@Composable
fun UpdateHost() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val info by UpdateState.available.collectAsStateWithLifecycle()
    var dismissed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(-1f) }
    var failed by remember { mutableStateOf(false) }
    var waitingForPermission by remember { mutableStateOf(false) }

    // Every time the app is opened / brought back: ask GitHub again, and show the dialog again
    // (a "પછી" only hides it until the next time the app is opened).
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            dismissed = false
            UpdateState.refresh()
        }
    }

    fun start(u: UpdateInfo) {
        if (!AppUpdater.canInstall(ctx)) {
            // Android's one-time switch; we come back and continue automatically.
            waitingForPermission = true
            ctx.startActivity(AppUpdater.allowInstallsIntent(ctx))
            return
        }
        failed = false
        progress = 0f
        scope.launch {
            val apk = AppUpdater.download(ctx, u) { p -> progress = p }
            if (apk == null) {
                failed = true
                progress = -1f
            } else {
                AppUpdater.install(ctx, apk)
            }
        }
    }

    // Back from "Allow from this source": carry on without a second tap.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(waitingForPermission) {
        if (!waitingForPermission) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (waitingForPermission && AppUpdater.canInstall(ctx)) {
                waitingForPermission = false
                info?.let(::start)
            }
        }
    }

    val u = info ?: return
    if (dismissed && progress < 0f) return
    val busy = progress >= 0f
    AlertDialog(
        onDismissRequest = { if (!busy) dismissed = true },
        properties = DialogProperties(dismissOnClickOutside = !busy, dismissOnBackPress = !busy),
        title = { Text(stringResource(R.string.v2_up_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.fillMaxWidth().testTag("update_dialog"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.v2_up_body, u.versionName), style = MaterialTheme.typography.bodyLarge)
                if (u.notes.isNotBlank()) Text(u.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (busy) {
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.v2_up_downloading, (progress * 100).toInt().coerceIn(0, 100)), style = MaterialTheme.typography.bodyMedium)
                }
                if (failed) Text(stringResource(R.string.v2_up_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                if (waitingForPermission) Text(stringResource(R.string.v2_up_allow), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            VkPrimaryButton(
                stringResource(if (failed) R.string.v2_up_retry else R.string.v2_up_now),
                { start(u) },
                Modifier.fillMaxWidth().testTag("update_now"),
                enabled = !busy,
            )
        },
        dismissButton = if (busy) null else {
            { VkTextButton(stringResource(R.string.v2_up_later), { dismissed = true }) }
        },
    )
}
