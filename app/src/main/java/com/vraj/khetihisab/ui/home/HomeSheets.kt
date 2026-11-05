package com.vraj.khetihisab.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.model.CategoryTotal
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.ui.components.AmountKind
import com.vraj.khetihisab.ui.components.VkAmount
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.theme.VkIcon
import java.time.LocalDate
import java.time.YearMonth

/** Shared sheet frame: theme surface, scrollable body, never a fixed height. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeBottomSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        com.vraj.khetihisab.ui.components.VkSheetSystemBars()
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            content()
        }
    }
}

/** PendingUdhaarSheet (doc 05 §2.1): one row per pending entry with its own "ચૂકતે" button. */
@Composable
internal fun PendingUdhaarSheet(
    rows: List<LedgerRowUi>,
    onSettle: (Long) -> Unit,
    onViewBill: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    HomeBottomSheet(stringResource(R.string.hm_settle_sheet_title), onDismiss) {
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.hm_nothing_pending),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(Modifier.fillMaxWidth().testTag("udhaar_sheet"), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(rows, key = { it.id }) { r ->
                val direction = stringResource(if (r.payStatus == PayStatus.PENDING_RECEIVABLE) R.string.hm_receivable_short else R.string.hm_payable_short)
                Column(Modifier.fillMaxWidth()) {
                    VkListRow(
                        title = r.title,
                        meta = "$direction · ${r.meta}",
                        leadingIcon = r.icon,
                        trailing = {
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                VkAmount(r.amountPaise, AmountKind.PENDING)
                                VkTonalButton(stringResource(R.string.hm_settle_one), { onSettle(r.id) })
                            }
                        },
                    )
                    if (r.billPath != null) {
                        androidx.compose.foundation.layout.Row(Modifier.padding(start = 68.dp, bottom = 8.dp)) { BillChip { onViewBill(r.billPath) } }
                    }
                    VkRowDivider()
                }
            }
        }
    }
}

@Composable
internal fun sectionPartLabel(s: AccountSection): String = stringResource(
    when (s) {
        AccountSection.FARM -> R.string.hm_part_farm
        AccountSection.ANIMAL -> R.string.hm_part_animal
        AccountSection.GENERAL -> R.string.hm_part_general
        AccountSection.FAMILY -> R.string.hm_part_family
        AccountSection.PERSONAL -> R.string.hm_part_personal
    },
)

private fun sectionIcon(s: AccountSection): VkIcon = when (s) {
    AccountSection.FARM -> VkIcon.FARM
    AccountSection.ANIMAL -> VkIcon.ANIMAL
    AccountSection.GENERAL -> VkIcon.UTILITY
    AccountSection.FAMILY -> VkIcon.FAMILY
    AccountSection.PERSONAL -> VkIcon.PERSONAL
}

/** Business P&L breakdown by section (doc 05 §3, `businessBySection`). PERSONAL never listed. */
@Composable
internal fun BusinessBreakdownSheet(business: BusinessUi, onDismiss: () -> Unit) {
    HomeBottomSheet(stringResource(R.string.hm_business_sheet_title), onDismiss) {
        Column(Modifier.fillMaxWidth().testTag("business_sheet")) {
            business.sections.forEach { s ->
                VkListRow(
                    title = sectionPartLabel(s.section),
                    meta = "${stringResource(R.string.scr_income)} ${ScreensFormat.signedRupees(s.incomePaise)} · " +
                        "${stringResource(R.string.scr_expense)} ${ScreensFormat.signedRupees(s.expensePaise)}",
                    leadingIcon = sectionIcon(s.section),
                    trailing = { NetText(s.netPaise) },
                )
                VkRowDivider()
            }
            VkListRow(
                title = stringResource(R.string.hm_total),
                meta = stringResource(R.string.hm_year_chip, com.vraj.khetihisab.ui.farms.SeasonRange.cropYearLabel(business.cropYear)),
                leadingIcon = VkIcon.REPORT,
                trailing = { NetText(business.totals.net) },
            )
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThreeStats(business.totals)
                Text(
                    stringResource(R.string.hm_business_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Animal husbandry this month: totals + spend by category (`animalByCategory`). */
@Composable
internal fun AnimalSheet(today: LocalDate, animal: Totals, categories: List<CategoryTotal>, onDismiss: () -> Unit, onMilk: () -> Unit = {}) {
    HomeBottomSheet(stringResource(R.string.hm_animal_sheet_title, ScreensFormat.monthYearGu(YearMonth.from(today))), onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThreeStats(animal)
            com.vraj.khetihisab.ui.components.VkTonalButton(stringResource(R.string.v3_milk_title), { onDismiss(); onMilk() }, Modifier.fillMaxWidth())
        }
        categories.forEach { c ->
            VkListRow(title = c.name, leadingIcon = VkIcon.ANIMAL, trailing = { VkAmount(c.total, AmountKind.EXPENSE) })
            VkRowDivider()
        }
    }
}
