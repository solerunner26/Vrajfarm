package com.vraj.khetihisab.ui.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vraj.khetihisab.data.entity.Priority
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.media.suppressLockForExternal
import com.vraj.khetihisab.notify.Deadline
import com.vraj.khetihisab.notify.ReminderTiming
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage

/** Bottom content padding: the floating "+ નવું રિમાઇન્ડર" button (≈ 64dp) + margins + 16dp. */
private val FabBottomPadding = 112.dp

/** RemindersScreen (doc 13, canvas 09 Reminders). */
@Composable
fun RemindersScreen(vm: RemindersViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = com.vraj.khetihisab.ui.components.rememberVkSnackbar()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshExactAllowed() }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val doneMsg = stringResource(R.string.rem_done_snack)
    val undoLabel = stringResource(R.string.rem_undo)
    val savedMsg = stringResource(R.string.rem_saved)
    val deletedMsg = stringResource(R.string.rem_deleted)
    val reopenedMsg = stringResource(R.string.rem_reopened)
    LaunchedEffect(vm) {
        vm.eventFlow.collect { e ->
            when (e) {
                is RemindersEvent.Done -> snackbar.show(doneMsg.format(e.title), actionLabel = undoLabel) { vm.undoDone(e.id) }
                RemindersEvent.Saved -> snackbar.show(savedMsg, success = true)
                RemindersEvent.Deleted -> snackbar.show(deletedMsg)
                RemindersEvent.Reopened -> snackbar.show(reopenedMsg)
                RemindersEvent.AskNotificationPermission ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suppressLockForExternal(context)
                        runCatching { notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            VkTopHeader(title = stringResource(R.string.rem_title), onBack = onBack)
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(com.vraj.khetihisab.ui.theme.VkDimens.listGap),
            ) {
                item(key = "seg") {
                    VkSegmented(
                        options = listOf(
                            ReminderTab.ACTIVE to stringResource(R.string.rem_active, st.active.size),
                            ReminderTab.ARCHIVE to stringResource(R.string.rem_archive, st.archived.size),
                        ),
                        selected = st.tab,
                        onSelect = vm::selectTab,
                    )
                }
                if (!st.exactAllowed && st.tab == ReminderTab.ACTIVE && st.active.isNotEmpty()) {
                    item(key = "exact") {
                        ExactAlarmBanner(onAllow = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                suppressLockForExternal(context)
                                val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(i) }
                            }
                        })
                    }
                }
                if (st.loaded && st.visible.isEmpty()) {
                    item(key = "empty") {
                        VkCard(Modifier.fillMaxWidth()) {
                            com.vraj.khetihisab.ui.components.VkEmptyState(
                                VkIcon.REMINDER,
                                stringResource(if (st.tab == ReminderTab.ACTIVE) R.string.rem_empty_active else R.string.rem_empty_archive),
                            )
                        }
                    }
                }
                items(st.visible, key = { it.id }) { card ->
                    ReminderCardView(
                        card,
                        modifier = Modifier.animateItem(),
                        onDone = { vm.markDone(card.id, card.title) },
                        onReopen = { vm.reopen(card.id) },
                        onEdit = { vm.openEdit(card.id) },
                    )
                }
            }

            // Add sits in the bottom action bar (audit #23).
            com.vraj.khetihisab.ui.components.VkActionBar {
                VkPrimaryButton(
                    text = stringResource(R.string.rem_sheet_new),
                    onClick = vm::openNew,
                    modifier = Modifier.weight(1f),
                    sign = "+",
                )
            }
        }
    }

    st.editor?.let { ReminderEditorSheet(it, vm) }
}

@Composable
private fun ExactAlarmBanner(onAllow: () -> Unit) {
    VkCard(Modifier.fillMaxWidth(), tone = VkTone.Primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VkIconImage(VkIcon.REMINDER, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.rem_exact_banner), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.padding(top = 8.dp))
        VkTonalButton(stringResource(R.string.rem_exact_allow), onAllow, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ReminderCardView(card: ReminderCard, onDone: () -> Unit, onReopen: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    // A7: the tick draws in first, then the card moves to the archive.
    var ticking by remember(card.id) { mutableStateOf(false) }
    LaunchedEffect(ticking) {
        if (ticking) {
            kotlinx.coroutines.delay(450)
            onDone()
        }
    }
    VkCard(modifier.fillMaxWidth(), onClick = if (card.archived) null else onEdit) {
        Row(verticalAlignment = Alignment.Top) {
            if (card.imagePath != null) {
                ReminderThumb(card.imagePath, size = 64.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TagBadge(card.tag)
                    if (card.origin == ReminderOrigin.LOAN) {
                        VkBadge(stringResource(R.string.rem_origin_loan), cs.secondaryContainer, cs.onSecondaryContainer)
                    }
                    val whenText = if (card.archived && card.doneAt != null) {
                        stringResource(R.string.rem_done_on, ReminderTiming.dateText(card.doneAt))
                    } else {
                        deadlineLabel(card.deadline)
                    }
                    Text(
                        whenText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!card.archived && card.deadline is Deadline.Overdue) cs.error else cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Text(
                    card.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        textDecoration = if (card.archived) TextDecoration.LineThrough else null,
                    ),
                    color = if (card.archived) cs.onSurfaceVariant else cs.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                card.note?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (card.archived) {
            VkTextButton(stringResource(R.string.rem_reopen), onReopen, Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (ticking) com.vraj.khetihisab.ui.components.VkCheckMark(true, size = 36.dp)
                VkTonalButton(stringResource(R.string.rem_done), { ticking = true }, Modifier.weight(1f), enabled = !ticking)
            }
        }
    }
}

@Composable
private fun TagBadge(tag: ReminderTag) {
    val cs = MaterialTheme.colorScheme
    when (tag) {
        ReminderTag.HIGH -> com.vraj.khetihisab.ui.components.VkPriorityBadge(Priority.HIGH)
        ReminderTag.NORMAL -> com.vraj.khetihisab.ui.components.VkPriorityBadge(Priority.NORMAL)
        ReminderTag.PANCHANG -> VkBadge(stringResource(R.string.rem_origin_panchang), cs.tertiaryContainer, cs.onTertiaryContainer)
    }
}

@Composable
private fun deadlineLabel(d: Deadline): String {
    val ctx = LocalContext.current
    return d.label { res, arg -> ctx.getString(res, arg) }
}
