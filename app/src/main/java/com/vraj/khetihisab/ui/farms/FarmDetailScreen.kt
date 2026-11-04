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
import com.vraj.khetihisab.ui.home.TxnListRow
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage

@Composable
fun FarmDetailRoute(farmId: Long, choice: PeriodChoice, onBack: () -> Unit, onAddEntry: (farmId: Long) -> Unit) {
    val ctx = LocalContext.current
    val vm: FarmDetailViewModel = viewModel(key = "farm_$farmId-${choice.period}-${choice.cropYear}") {
        FarmDetailViewModel(ScreensDepsProvider.get(ctx).repos, farmId, choice)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    FarmDetailScreen(state, onBack, { onAddEntry(farmId) }, vm::changeCrop)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmDetailScreen(
    state: FarmDetailUiState,
    onBack: () -> Unit,
    onAddEntry: () -> Unit,
    onChangeCrop: (Long) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val pad = Modifier.padding(horizontal = 16.dp)
    var bill by remember { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(cs.surface)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = DockBottomPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                VkTopHeader(state.name, onBack = onBack, subtitle = periodLabel(state.choice)) {
                    state.cycle?.let { VkHeaderChip(it.crop.name) }
                }
            }
            item(key = "stats") { VkCard(pad.fillMaxWidth()) { ThreeStats(state.totals) } }
            item(key = "cycle") {
                VkCard(pad.fillMaxWidth()) {
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
                VkCard(pad.fillMaxWidth(), contentPadding = 0.dp) {
                    if (state.entries.isEmpty()) {
                        Text(
                            stringResource(R.string.scr_no_entries_period),
                            style = MaterialTheme.typography.bodyLarge,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
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
                        )
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(cs.surfaceContainer).navigationBarsPadding().padding(16.dp)) {
            VkPrimaryButton(stringResource(R.string.scr_entry_for_farm), onAddEntry, Modifier.fillMaxWidth())
        }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = cs.surfaceContainerLowest,
            title = { Text(stringResource(R.string.hm_change_crop_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
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
            },
            confirmButton = { VkTextButton(stringResource(R.string.scr_cancel), { picking = false }) },
        )
    }
    bill?.let { BillViewer(it) { bill = null } }
}
