package com.vraj.khetihisab.ui.personal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkBottomDock
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkHeaderChip
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.decor.VkLeafDivider
import com.vraj.khetihisab.ui.entry.BillViewer
import com.vraj.khetihisab.ui.home.DockBottomPadding
import com.vraj.khetihisab.ui.home.MonthPickerDialog
import com.vraj.khetihisab.ui.home.ScreensFormat
import com.vraj.khetihisab.ui.home.StatTile
import com.vraj.khetihisab.ui.home.TxnListRow
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import java.time.YearMonth

@Composable
fun PersonalRoute(onBack: () -> Unit, onExpense: () -> Unit, onIncome: () -> Unit) {
    val others = stringResource(R.string.scr_others)
    val section = stringResource(R.string.scr_sec_personal_short)
    val vm = screenViewModel { deps, handle -> PersonalViewModel(deps.repos, handle, others, section) }
    val state by vm.state.collectAsStateWithLifecycle()
    PersonalScreen(state, onBack, vm::selectMonth, onExpense, onIncome)
}

/** Isolated personal / home account (doc 09, artboard 06). Only `section = PERSONAL` numbers. */
@Composable
fun PersonalScreen(
    state: PersonalUiState,
    onBack: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val pad = Modifier.padding(horizontal = 16.dp)
    var pickMonth by rememberSaveable { mutableStateOf(false) }
    var bill by remember { mutableStateOf<String?>(null) }
    val monthGu = ScreensFormat.monthGu(state.month)

    Box(Modifier.fillMaxSize().background(cs.surface)) {
        LazyColumn(
            Modifier.fillMaxSize().testTag("personal_list"),
            contentPadding = PaddingValues(bottom = DockBottomPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                VkTopHeader(
                    stringResource(R.string.scr_personal_title),
                    onBack = onBack,
                    actions = {
                        Box(
                            Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button) { pickMonth = true },
                            contentAlignment = Alignment.Center,
                        ) { VkHeaderChip(ScreensFormat.monthYearGu(state.month) + " ▾") }
                    },
                )
            }
            item(key = "info") {
                VkCard(pad.fillMaxWidth(), tone = VkTone.Primary) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VkIconImage(VkIcon.LOCK, size = 28.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.scr_personal_isolated), style = MaterialTheme.typography.bodyLarge, color = cs.onSecondaryContainer)
                    }
                }
            }
            item(key = "stats") {
                Column(pad.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VkCard(Modifier.weight(1f).fillMaxHeight()) {
                            StatTile(stringResource(R.string.scr_income_month, monthGu), ScreensFormat.signedRupees(state.totals.income), valueColor = cs.primary, big = true)
                        }
                        VkCard(Modifier.weight(1f).fillMaxHeight()) {
                            StatTile(stringResource(R.string.scr_expense_month, monthGu), ScreensFormat.signedRupees(state.totals.expense), big = true)
                        }
                    }
                    Text(
                        stringResource(R.string.scr_savings, ScreensFormat.signedRupees(state.savings)),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (state.savings < 0) cs.error else cs.primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.testTag("personal_savings"),
                    )
                }
            }
            if (state.showUdhaar) {
                item(key = "udhaar") {
                    VkCard(pad.fillMaxWidth(), tone = VkTone.Error) {
                        Text(stringResource(R.string.scr_personal_udhaar), style = MaterialTheme.typography.titleMedium, color = cs.onErrorContainer, maxLines = 1)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(stringResource(R.string.hm_personal_payable), ScreensFormat.signedRupees(state.udhaar.payable), Modifier.weight(1f), cs.onErrorContainer, big = true)
                            StatTile(stringResource(R.string.hm_personal_receivable), ScreensFormat.signedRupees(state.udhaar.receivable), Modifier.weight(1f), cs.onErrorContainer, big = true)
                        }
                    }
                }
            }
            if (state.isEmpty) {
                item(key = "empty") {
                    Column(pad.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VkLeafDivider()
                        Text(stringResource(R.string.scr_empty_month), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                if (state.bars.isNotEmpty()) {
                    item(key = "bars") {
                        VkCard(pad.fillMaxWidth().testTag("personal_bars")) {
                            Text(stringResource(R.string.scr_where_spent), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            state.bars.forEach { b -> CategoryBarRow(b) }
                        }
                    }
                }
                item(key = "recent_title") { VkSectionTitle(stringResource(R.string.scr_recent), pad) }
                item(key = "recent") {
                    VkCard(pad.fillMaxWidth(), contentPadding = 0.dp) {
                        state.rows.forEachIndexed { i, r ->
                            if (i > 0) VkRowDivider()
                            TxnListRow(
                                title = r.title,
                                meta = r.meta,
                                icon = r.icon,
                                amountPaise = r.amountPaise,
                                kind = r.kind,
                                billPath = r.billPath,
                                onViewBill = { bill = it },
                                pendingBadge = r.pending,
                            )
                        }
                    }
                }
            }
        }
        VkBottomDock(onExpense = onExpense, onIncome = onIncome, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (pickMonth) {
        MonthPickerDialog(state.month, onPick = { onSelectMonth(it); pickMonth = false }, onDismiss = { pickMonth = false })
    }
    bill?.let { BillViewer(it) { bill = null } }
}

@Composable
private fun CategoryBarRow(b: CategoryBar) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(b.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(ScreensFormat.signedRupees(b.totalPaise), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1, softWrap = false)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(cs.surfaceContainer)) {
            if (b.fraction > 0f) Box(Modifier.fillMaxWidth(b.fraction).height(10.dp).clip(RoundedCornerShape(5.dp)).background(cs.primary))
        }
    }
}
