package com.vraj.khetihisab.ui.reports

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.report.ReportShare
import com.vraj.khetihisab.report.ReportType
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.vkAppear
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.rememberScreensDeps
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.settings.StBodyColumn
import com.vraj.khetihisab.ui.settings.StError
import com.vraj.khetihisab.ui.settings.StNote
import com.vraj.khetihisab.ui.settings.StScrollBody
import com.vraj.khetihisab.ui.theme.VkIcon
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

internal fun rangeLabel(r: DateRange): String =
    if (r.from == r.to) r.from.format(DMY) else "${r.from.format(DMY)} – ${r.to.format(DMY)}"

@Composable
fun ReportsRoute(initialType: ReportType?, onBack: () -> Unit) {
    val vm: ReportsViewModel = screenViewModel(key = "reports_${initialType?.slug}") { deps, _ -> ReportsViewModel.create(deps, initialType) }
    ReportsScreen(vm, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(vm: ReportsViewModel, onBack: () -> Unit) {
    val u by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val deps = rememberScreensDeps()
    val noViewer = stringResource(R.string.st_rep_no_viewer)
    val shareTitle = stringResource(R.string.st_rep_share)
    val snackbar = com.vraj.khetihisab.ui.components.rememberVkSnackbar()

    fun open(path: String) {
        deps.lockManager.suppressNextLock = true
        val f = File(path)
        if (!ReportShare.open(context, f)) {
            snackbar.show(noViewer)
            runCatching { ReportShare.share(context, f, shareTitle) }
        }
    }

    fun share(path: String) {
        deps.lockManager.suppressNextLock = true
        runCatching { ReportShare.share(context, File(path), shareTitle) }
    }

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_reports), subtitle = stringResource(R.string.st_rep_sub), onBack = onBack)
        StBodyColumn {
            val last = u.lastFile
            if (last != null && u.lastType != null) {
                VkCard(Modifier.fillMaxWidth().vkAppear(0, "rep_ready"), tone = VkTone.Primary) {
                    Text(stringResource(R.string.st_rep_ready, stringResource(u.lastType!!.titleRes)), style = MaterialTheme.typography.titleMedium)
                    Text(File(last).name, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VkPrimaryButton(stringResource(R.string.st_rep_open), { open(last) }, Modifier.weight(1f))
                        VkTonalButton(stringResource(R.string.st_rep_share), { share(last) }, Modifier.weight(1f))
                    }
                }
            }
            VkSectionTitle(stringResource(R.string.st_rep_pick))
            VkCard(Modifier.fillMaxWidth().vkAppear(1, "rep_list"), contentPadding = 0.dp) {
                Column {
                    ReportType.entries.forEachIndexed { i, t ->
                        VkListRow(
                            title = stringResource(t.titleRes),
                            meta = stringResource(if (t.usesRange) R.string.st_rep_meta_range else R.string.st_rep_meta_now),
                            leadingIcon = VkIcon.REPORT,
                            showChevron = true,
                            onClick = { vm.select(t) },
                        )
                        if (i < ReportType.entries.lastIndex) VkRowDivider()
                    }
                }
            }
            StNote(stringResource(R.string.st_rep_note))
        }
    }

    val sel = u.selected
    if (sel != null && !u.generating) {
        com.vraj.khetihisab.ui.components.VkBottomSheet(stringResource(sel.titleRes), vm::close, scrollable = false) {
            ReportOptions(sel, u.range, u.failed, vm)
        }
    }

    if (u.generating) {
        com.vraj.khetihisab.ui.components.VkDialog(
            title = stringResource(R.string.st_rep_generating),
            onDismiss = {},
            dismissLabel = null,
            dismissible = false,
        ) { com.vraj.khetihisab.ui.components.VkLoading() }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ReportOptions(type: ReportType, range: DateRange?, failed: Boolean, vm: ReportsViewModel) {
    var picking by rememberSaveable { mutableStateOf<String?>(null) } // "day" | "from" | "to"
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (type.usesRange && range != null) {
            val presets = ReportsViewModel.presetsFor(type)
            if (presets.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { p ->
                        val pr = ReportsViewModel.presetRange(p, LocalDate.now(ReportsViewModel.IST))
                        VkChip(stringResource(presetLabel(p)), selected = pr == range, onClick = { vm.setPreset(p) })
                    }
                }
            }
            Text(rangeLabel(range), style = MaterialTheme.typography.titleMedium)
            if (type == ReportType.DAILY_LEDGER) {
                VkTonalButton(stringResource(R.string.st_rep_pick_day), { picking = "day" }, Modifier.fillMaxWidth())
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VkTonalButton(stringResource(R.string.st_rep_from), { picking = "from" }, Modifier.weight(1f))
                    VkTonalButton(stringResource(R.string.st_rep_to), { picking = "to" }, Modifier.weight(1f))
                }
            }
        } else {
            StNote(stringResource(R.string.st_rep_meta_now))
        }
        if (failed) StError(stringResource(R.string.st_rep_failed))
        VkPrimaryButton(stringResource(R.string.st_rep_generate), vm::generate, Modifier.fillMaxWidth())
        VkTextButton(stringResource(R.string.st_cancel), vm::close, Modifier.fillMaxWidth().align(Alignment.CenterHorizontally))
    }

    picking?.let { which ->
        val initial = when (which) {
            "to" -> range?.to
            else -> range?.from
        } ?: LocalDate.now(ReportsViewModel.IST)
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                VkTextButton(stringResource(R.string.st_ok), {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        when (which) {
                            "day" -> vm.setDay(d)
                            "from" -> vm.setFrom(d)
                            else -> vm.setTo(d)
                        }
                    }
                    picking = null
                })
            },
            dismissButton = { VkTextButton(stringResource(R.string.st_cancel), { picking = null }) },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun presetLabel(p: RangePreset): Int = when (p) {
    RangePreset.TODAY -> R.string.st_rep_today
    RangePreset.YESTERDAY -> R.string.st_rep_yesterday
    RangePreset.THIS_MONTH -> R.string.st_rep_this_month
    RangePreset.LAST_MONTH -> R.string.st_rep_last_month
    RangePreset.FINANCIAL_YEAR -> R.string.st_rep_fy
    RangePreset.THIS_YEAR -> R.string.st_rep_this_year
    RangePreset.LAST_YEAR -> R.string.st_rep_last_year
}
