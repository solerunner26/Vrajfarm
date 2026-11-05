package com.vraj.khetihisab.ui.farms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkHeaderChip
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.entry.BillViewer
import com.vraj.khetihisab.ui.home.DockBottomPadding
import com.vraj.khetihisab.ui.home.HomeLogic
import com.vraj.khetihisab.ui.home.ScreensDepsProvider
import com.vraj.khetihisab.ui.home.ScreensFormat
import com.vraj.khetihisab.ui.home.ThreeStats
import com.vraj.khetihisab.ui.components.vkAppear
import com.vraj.khetihisab.ui.home.TxnListRow
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage

@Composable
fun FarmDetailRoute(farmId: Long, choice: PeriodChoice, onBack: () -> Unit, onAddEntry: (farmId: Long) -> Unit, onEditEntry: (Long) -> Unit = {}) {
    val ctx = LocalContext.current
    val vm: FarmDetailViewModel = viewModel(key = "farm_$farmId-${choice.period}-${choice.cropYear}") {
        FarmDetailViewModel(ScreensDepsProvider.get(ctx).repos, farmId, choice)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    FarmDetailScreen(state, onBack, { onAddEntry(farmId) }, vm::changeCrop, onEditEntry)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmDetailScreen(
    state: FarmDetailUiState,
    onBack: () -> Unit,
    onAddEntry: () -> Unit,
    onChangeCrop: (Long) -> Unit,
    onEditEntry: (Long) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val pad = Modifier.padding(horizontal = 16.dp)
    var bill by remember { mutableStateOf<String?>(null) }
    var actionsFor by remember { mutableStateOf<com.vraj.khetihisab.ui.home.LedgerRowUi?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(cs.surface)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(com.vraj.khetihisab.ui.theme.VkDimens.listGap),
        ) {
            item(key = "header") {
                VkTopHeader(state.name, onBack = onBack, subtitle = periodLabel(state.choice)) {
                    state.cycle?.let { VkHeaderChip(it.crop.name) }
                }
            }
            item(key = "stats") { VkCard(pad.fillMaxWidth().vkAppear(0, "fd_stats")) { ThreeStats(state.totals) } }
            item(key = "cycle") {
                VkCard(pad.fillMaxWidth().vkAppear(1, "fd_cycle")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VkIconImage(VkIcon.FARM, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.scr_active_crop), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                            val c = state.cycle
                            Text(
                                c?.crop?.name ?: stringResource(R.string.scr_no_crop),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (c != null) {
                                val season = SeasonRange.fromCycleSeason(c.cycle.season)
                                val seasonText = season?.let { periodLabel(PeriodChoice.ofSeason(it, c.cycle.year)) } ?: c.cycle.season
                                Text(
                                    stringResource(R.string.scr_crop_since, seasonText, ScreensFormat.dmy(c.cycle.startDate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    VkTonalButton(stringResource(R.string.scr_change_crop), { picking = true }, Modifier.fillMaxWidth())
                }
            }
            item(key = "entries_title") { VkSectionTitle(stringResource(R.string.scr_all_entries), pad) }
            item(key = "entries") {
                VkCard(pad.fillMaxWidth().vkAppear(2, "fd_entries"), contentPadding = 0.dp) {
                    if (state.entries.isEmpty()) {
                        com.vraj.khetihisab.ui.components.VkEmptyState(
                            VkIcon.FARM,
                            stringResource(R.string.scr_no_entries_period),
                            actionLabel = stringResource(R.string.v5_no_entries_action),
                            onAction = onAddEntry,
                        )
                    }
                    state.entries.forEachIndexed { i, t ->
                        if (i > 0) VkRowDivider()
                        TxnListRow(
                            title = t.categoryId?.let(state.categories::get) ?: t.note ?: state.name,
                            meta = HomeLogic.meta(t),
                            icon = HomeLogic.iconFor(t),
                            amountPaise = t.amountPaise,
                            kind = ScreensFormat.amountKind(t),
                            billPath = t.billPhotoPath,
                            onViewBill = { bill = it },
                            pendingBadge = t.payStatus != com.vraj.khetihisab.data.entity.PayStatus.PAID,
                            onClick = {
                                actionsFor = HomeLogic.row(t, com.vraj.khetihisab.ui.home.LedgerNames(categories = state.categories)) { state.name }
                            },
                        )
                    }
                }
            }
        }
        com.vraj.khetihisab.ui.components.VkActionBar {
            VkPrimaryButton(stringResource(R.string.scr_entry_for_farm), onAddEntry, Modifier.weight(1f))
        }
    }

    if (picking) {
        com.vraj.khetihisab.ui.components.VkDialog(
            title = stringResource(R.string.hm_change_crop_title),
            onDismiss = { picking = false },
        ) {
            if (state.crops.isEmpty()) {
                Text(stringResource(R.string.hm_no_crops), style = MaterialTheme.typography.bodyLarge)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.crops.forEach { crop ->
                        VkChip(crop.name, selected = state.cycle?.crop?.id == crop.id, onClick = {
                            picking = false
                            if (state.cycle?.crop?.id != crop.id) onChangeCrop(crop.id)
                        })
                    }
                }
            }
        }
    }
    actionsFor?.let { r ->
        com.vraj.khetihisab.ui.home.EntryActionsSheet(r, onEdit = { onEditEntry(r.id) }, onDismiss = { actionsFor = null })
    }
    bill?.let { BillViewer(it) { bill = null } }
}
