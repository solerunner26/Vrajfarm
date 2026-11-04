package com.vraj.khetihisab.ui.assets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.media.suppressLockForExternal
import com.vraj.khetihisab.share.WhatsAppLauncher
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkFormat
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import java.io.File

/** AssetsScreen (doc 11, artboard 09): ઉછીની / બધી / ઇતિહાસ + "કોણ લઈ ગયું?" search. */
@Composable
fun AssetsScreen(
    vm: AssetsViewModel,
    onBack: () -> Unit,
    onLend: (assetId: Long?) -> Unit,
) {
    val st by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val returnedMsg = stringResource(R.string.as_saved)
    val blockedMsg = stringResource(R.string.as_delete_blocked)

    LaunchedEffect(vm) {
        vm.eventFlow.collect { e ->
            when (e) {
                is AssetsEvent.OpenWhatsApp -> {
                    // Opens WhatsApp with the text pre-filled; the user taps Send. Never auto-sends.
                    val r = WhatsAppLauncher.open(context, e.phone, e.text)
                    if (r.opened) vm.onReminderOpened(e.loanId)
                }
                is AssetsEvent.Dial -> {
                    suppressLockForExternal(context)
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + e.phone))) }
                }
                AssetsEvent.Returned -> snackbar.showSnackbar(returnedMsg)
                AssetsEvent.DeleteBlocked -> snackbar.showSnackbar(blockedMsg)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            VkTopHeader(title = stringResource(R.string.as_assets_title), onBack = onBack) {
                AsSearchField(st.query, vm::onQuery, stringResource(R.string.as_search_who))
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "tabs") {
                    VkSegmented(
                        options = listOf(
                            AssetsTab.LENT to stringResource(R.string.as_tab_lent, st.lentCount),
                            AssetsTab.ALL to stringResource(R.string.as_tab_all, st.allCount),
                            AssetsTab.HISTORY to stringResource(R.string.as_tab_history),
                        ),
                        selected = st.tab,
                        onSelect = vm::selectTab,
                    )
                }
                when (st.tab) {
                    AssetsTab.LENT -> {
                        if (st.loaded && st.lent.isEmpty()) item(key = "e") { EmptyLine(stringResource(R.string.as_no_loans)) }
                        items(st.lent, key = { "l${it.loan.loan.id}" }) { c ->
                            LoanCardView(c, onRemind = { vm.remind(c.loan) }, onReturned = { vm.askReturn(c.loan) }, onCall = { vm.call(c.loan) }, onPhoto = vm::showPhoto)
                        }
                    }
                    AssetsTab.ALL -> {
                        if (st.loaded && st.inventory.isEmpty()) item(key = "e") { EmptyLine(stringResource(R.string.as_no_assets)) }
                        items(st.inventory.chunked(2), key = { "a${it.first().asset.id}" }) { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { t -> AssetTileView(t, Modifier.weight(1f)) { vm.openDetail(t.asset.id) } }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    AssetsTab.HISTORY -> {
                        if (st.loaded && st.history.isEmpty()) item(key = "e") { EmptyLine(stringResource(R.string.as_no_history)) }
                        items(st.history, key = { "h${it.loan.id}" }) { h -> HistoryRow(h, onPhoto = vm::showPhoto) }
                    }
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SnackbarHost(snackbar)
            VkPrimaryButton(stringResource(R.string.as_lend_fab), onClick = { onLend(null) }, modifier = Modifier.fillMaxWidth(), icon = VkIcon.LEND)
        }
    }

    st.confirmReturn?.let { l -> ConfirmReturnDialog(l, onYes = vm::confirmReturned, onNo = vm::cancelReturn) }
    st.detail?.let { d ->
        AssetDetailSheet(d, onDismiss = vm::closeDetail, onLend = { vm.closeDetail(); onLend(d.asset.id) }, onDelete = { vm.deleteAsset(d.asset.id) }, onPhoto = vm::showPhoto)
    }
    st.fullPhoto?.let { p -> FullPhotoDialog(p, onDismiss = { vm.showPhoto(null) }) }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.padding(8.dp))
}

@Composable
internal fun LoanBadge(status: LoanStatus) {
    val cs = MaterialTheme.colorScheme
    when (status) {
        is LoanStatus.Overdue -> VkBadge(stringResource(R.string.as_days_late, status.days), cs.errorContainer, cs.onErrorContainer)
        LoanStatus.DueToday -> VkBadge(stringResource(R.string.as_due_today), cs.primaryContainer, cs.onPrimaryContainer)
        is LoanStatus.DaysLeft -> VkBadge(stringResource(R.string.as_days_left, status.days), cs.secondaryContainer, cs.onSecondaryContainer)
    }
}

@Composable
private fun LoanCardView(c: LoanCard, onRemind: () -> Unit, onReturned: () -> Unit, onCall: () -> Unit, onPhoto: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val x = LocalVkExtra.current
    val l = c.loan.loan
    val overdueBorder = if (c.status.isOverdue) Modifier.border(1.5.dp, cs.error, RoundedCornerShape(16.dp)) else Modifier
    VkCard(Modifier.fillMaxWidth().then(overdueBorder), contentPadding = 14.dp) {
        Row {
            AsPhoto(
                path = l.borrowerPhotoPath,
                width = 76.dp,
                height = 92.dp,
                radius = 14.dp,
                contentDescription = l.borrowerName,
                onClick = l.borrowerPhotoPath?.let { p -> { onPhoto(p) } },
            ) {
                if (l.borrowerPhotoPath == null) {
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        VkBadge(stringResource(R.string.as_no_photo), cs.errorContainer, cs.onErrorContainer, Modifier.align(Alignment.Center))
                    }
                } else {
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        VkBadge(stringResource(R.string.as_photo), x.gradientEnd, x.onGradient, Modifier.align(Alignment.Center))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VkIconImage(AssetsFormat.icon(c.loan.asset.iconKey), size = 26.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(c.loan.asset.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(l.borrowerName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Meta(stringResource(R.string.as_taken_on, takenLabel(l.handedOverDate, l.handedOverTithi)))
                Meta(stringResource(R.string.as_return_on, AssetsFormat.dmy(l.expectedReturnDate)))
                Spacer(Modifier.size(2.dp))
                LoanBadge(c.status)
                l.lastWhatsAppReminderAt?.let { Meta(stringResource(R.string.as_last_reminded, AssetsFormat.dmy(it))) }
            }
            if (!l.borrowerPhone.isNullOrBlank()) {
                FilledTonalIconButton(
                    onClick = onCall,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer),
                ) { Icon(Icons.Rounded.Call, contentDescription = stringResource(R.string.as_call)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Half-width buttons: no icon (doc 17 #6).
            VkPrimaryButton(stringResource(R.string.as_remind), onRemind, Modifier.weight(1f))
            VkTonalButton(stringResource(R.string.as_mark_returned), onReturned, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Meta(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

private fun takenLabel(date: java.time.LocalDate, tithi: String): String =
    if (tithi.isBlank()) AssetsFormat.dmy(date) else "${AssetsFormat.dmy(date)} · $tithi"

@Composable
private fun AssetTileView(t: AssetTile, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    VkCard(modifier.heightIn(min = 132.dp), onClick = onClick, contentPadding = 14.dp) {
        VkIconImage(AssetsFormat.icon(t.asset.iconKey), size = 40.dp)
        Text(t.asset.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (t.isLent) {
            VkBadge(t.borrowerName?.let { stringResource(R.string.as_lent_to, it) } ?: stringResource(R.string.as_lent_to, ""), cs.secondaryContainer, cs.onSecondaryContainer)
        } else {
            VkBadge(stringResource(R.string.as_at_home), cs.primaryContainer, cs.onPrimaryContainer)
        }
    }
}

@Composable
private fun HistoryRow(h: LoanWithAsset, onPhoto: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val l = h.loan
    VkCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsPhoto(
                path = l.borrowerPhotoPath,
                width = 56.dp,
                height = 64.dp,
                radius = 10.dp,
                contentDescription = l.borrowerName,
                onClick = l.borrowerPhotoPath?.let { p -> { onPhoto(p) } },
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${h.asset.name} · ${l.borrowerName}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Meta(stringResource(R.string.as_taken_on, takenLabel(l.handedOverDate, l.handedOverTithi)))
                val returnedAt = l.returnedAt
                if (returnedAt != null) {
                    Text(
                        stringResource(R.string.as_returned_on, AssetsFormat.dmy(returnedAt)),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = cs.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(stringResource(R.string.as_not_returned), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = cs.error, maxLines = 1)
                }
            }
        }
    }
}

/** Confirm "પરત મળ્યું" — actions stacked full width (doc 01 §8.5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmReturnDialog(l: LoanWithAsset, onYes: () -> Unit, onNo: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onNo) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.as_mark_returned_q, l.asset.name), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.as_mark_returned_body, l.loan.borrowerName), style = MaterialTheme.typography.bodyLarge)
                VkPrimaryButton(stringResource(R.string.as_yes_returned), onYes, Modifier.fillMaxWidth())
                VkTextButton(stringResource(R.string.as_cancel), onNo, Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetDetailSheet(d: AssetDetail, onDismiss: () -> Unit, onLend: () -> Unit, onDelete: () -> Unit, onPhoto: (String) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (d.asset.photoPath != null) {
                    AsPhoto(d.asset.photoPath, 64.dp, 64.dp, 14.dp, d.asset.name, onClick = { onPhoto(d.asset.photoPath) })
                } else {
                    VkIconImage(AssetsFormat.icon(d.asset.iconKey), size = 52.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.asset.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val bits = listOfNotNull(
                        d.asset.pricePaise?.let(VkFormat::rupees),
                        d.asset.purchaseDate?.let { stringResource(R.string.as_bought_on, AssetsFormat.dmy(it)) },
                    )
                    if (bits.isNotEmpty()) Meta(bits.joinToString(" · "))
                }
            }
            if (d.loans.isNotEmpty()) {
                AsLabel(stringResource(R.string.as_asset_detail_loans))
                VkCard(contentPadding = 0.dp) {
                    d.loans.forEachIndexed { i, l ->
                        if (i > 0) VkRowDivider()
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(l.loan.borrowerName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Meta(stringResource(R.string.as_taken_on, takenLabel(l.loan.handedOverDate, l.loan.handedOverTithi)))
                            Meta(l.loan.returnedAt?.let { stringResource(R.string.as_returned_on, AssetsFormat.dmy(it)) } ?: stringResource(R.string.as_not_returned))
                        }
                    }
                }
            }
            if (!d.isLent) VkPrimaryButton(stringResource(R.string.as_lend_fab), onLend, Modifier.fillMaxWidth(), icon = VkIcon.LEND)
            VkTextButton(stringResource(R.string.as_delete), onDelete, Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullPhotoDialog(path: String, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp))
                VkTextButton(stringResource(R.string.as_close), onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}

@Suppress("unused")
private val unusedColor = Color.Unspecified
