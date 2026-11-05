package com.vraj.khetihisab.ui.farms

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.vraj.khetihisab.ui.components.vkAppear
import com.vraj.khetihisab.ui.home.MoneyStat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkHeaderIconButton
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.PlainBottomPadding
import com.vraj.khetihisab.ui.home.ScreensFormat
import com.vraj.khetihisab.ui.home.StatTile
import com.vraj.khetihisab.ui.home.screenViewModel

/** "ખરીફ 2026", "રવી 2026", "ઉનાળુ 2027", "આખું વર્ષ". */
@Composable
fun periodLabel(c: PeriodChoice): String = when (c.period) {
    FarmPeriod.KHARIF -> stringResource(R.string.scr_season_kharif, c.seasonYear)
    FarmPeriod.RABI -> stringResource(R.string.scr_season_rabi, c.seasonYear)
    FarmPeriod.SUMMER -> stringResource(R.string.scr_season_summer, c.seasonYear)
    FarmPeriod.FULL_YEAR -> stringResource(R.string.scr_full_year)
}

@Composable
fun FarmsRoute(onBack: () -> Unit, onOpenFarm: (farmId: Long, choice: PeriodChoice) -> Unit, onReport: (byVigha: Boolean) -> Unit = {}) {
    val vm = screenViewModel { deps, handle -> FarmsViewModel(RepoFarmsSource(deps.repos), handle) }
    val state by vm.state.collectAsStateWithLifecycle()
    FarmsScreen(
        state = state,
        onBack = onBack,
        onPeriod = vm::selectPeriod,
        onShiftYear = vm::shiftYear,
        onByVigha = vm::setByVigha,
        onOpenFarm = { onOpenFarm(it, state.choice) },
        onReport = { onReport(state.byVigha) },
    )
}

/** Farms P&L + Vigha scorecard (doc 07, artboard 04). */
@Composable
fun FarmsScreen(
    state: FarmsUiState,
    onBack: () -> Unit,
    onPeriod: (FarmPeriod) -> Unit,
    onShiftYear: (Int) -> Unit,
    onByVigha: (Boolean) -> Unit,
    onOpenFarm: (Long) -> Unit,
    onReport: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val pad = Modifier.padding(horizontal = 16.dp)
    LazyColumn(
        Modifier.fillMaxSize().background(cs.surface).testTag("farms_list"),
        contentPadding = PaddingValues(bottom = PlainBottomPadding),
        verticalArrangement = Arrangement.spacedBy(com.vraj.khetihisab.ui.theme.VkDimens.listGap),
    ) {
        item(key = "header") {
            VkTopHeader(
                stringResource(R.string.scr_farms_title),
                onBack = onBack,
                actions = { VkHeaderIconButton(VkIcon.REPORT, stringResource(R.string.scr_pdf), onReport) },
            )
        }
        item(key = "chips") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.chips.forEach { c -> VkChip(periodLabel(c), selected = c == state.choice, onClick = { onPeriod(c.period) }) }
            }
        }
        item(key = "year") {
            Row(pad.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                com.vraj.khetihisab.ui.components.VkIconAction(
                    androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    stringResource(R.string.hm_prev_year),
                    { onShiftYear(-1) },
                )
                Text(
                    stringResource(R.string.hm_crop_year, SeasonRange.cropYearLabel(state.choice.cropYear)),
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                com.vraj.khetihisab.ui.components.VkIconAction(
                    androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    stringResource(R.string.hm_next_year),
                    { onShiftYear(1) },
                )
            }
        }
        item(key = "summary") { SummaryCard(state, pad.vkAppear(0, "farms_summary")) }
        item(key = "score_title") { VkSectionTitle(stringResource(R.string.scr_scorecard), pad) }
        item(key = "seg") {
            VkSegmented(
                options = listOf(true to stringResource(R.string.scr_per_vigha), false to stringResource(R.string.scr_total_profit)),
                selected = state.byVigha,
                onSelect = onByVigha,
                modifier = pad,
            )
        }
        if (state.rows.isEmpty()) {
            item(key = "empty") { VkCard(pad.fillMaxWidth()) { com.vraj.khetihisab.ui.components.VkEmptyState(VkIcon.FARM, stringResource(R.string.v5_no_farms)) } }
        }
        itemsIndexed(state.rows, key = { _, r -> r.score.farmId }) { i, r ->
            ScoreRow(r, state.byVigha, { onOpenFarm(r.score.farmId) }, pad.vkAppear((i + 1).coerceAtMost(6), "farm_${r.score.farmId}"))
        }
    }
}

@Composable
private fun SummaryCard(state: FarmsUiState, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val t = state.totals
    VkCard(modifier.fillMaxWidth().testTag("farms_summary"), tone = VkTone.Primary) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyStat(stringResource(R.string.scr_farm_income), t.income, Modifier.weight(1f), cs.onSecondaryContainer)
            MoneyStat(stringResource(R.string.scr_farm_expense), t.expense, Modifier.weight(1f), cs.onSecondaryContainer)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyStat(
                stringResource(R.string.scr_net_profit),
                t.net,
                Modifier.weight(1f),
                if (t.net < 0) cs.error else cs.primary,
            )
            StatTile(
                stringResource(R.string.scr_total_land),
                stringResource(R.string.scr_vigha_value, FarmScoring.vighaText(state.totalVigha)),
                Modifier.weight(1f),
                cs.onSecondaryContainer,
                big = true,
            )
        }
        Text(stringResource(R.string.scr_personal_excluded), style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer)
    }
}

@Composable
private fun ScoreRow(r: ScoreRowUi, byVigha: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val s = r.score
    VkCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val top = r.rank <= 3
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(if (top) cs.primaryContainer else cs.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    r.rank.toString(),
                    color = if (top) cs.primary else cs.onSurface,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val vigha = if (s.hasVigha) stringResource(R.string.scr_vigha_value, FarmScoring.vighaText(s.vigha)) else stringResource(R.string.scr_vigha_not_set)
                Text(
                    if (s.crop != null) stringResource(R.string.hm_crop_vigha, s.crop, vigha) else vigha,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (s.hasVigha) cs.onSurfaceVariant else cs.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                val v = r.valuePaise
                Text(
                    if (v == null) stringResource(R.string.scr_none_dash) else ScreensFormat.signedRupees(v),
                    style = com.vraj.khetihisab.ui.theme.VkCurrencyRow,
                    color = if (v != null && v < 0) cs.error else cs.primary,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    stringResource(if (byVigha) R.string.scr_per_vigha_caption else R.string.scr_total_profit),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        ScoreBar(r.barFraction)
    }
}

@Composable
private fun ScoreBar(fraction: Float) {
    // A5: the bar grows in from 0.
    com.vraj.khetihisab.ui.components.VkProgressBar(fraction, height = 8.dp)
}
