package com.vraj.khetihisab.ui.panchang

import com.vraj.khetihisab.data.model.DayTotal
import com.vraj.khetihisab.ui.components.VkFormat
import com.vraj.khetihisab.ui.home.EntryActionsSheet
import com.vraj.khetihisab.ui.home.HomeRoutes
import com.vraj.khetihisab.ui.home.LedgerRowUi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.panchang.PanchangNames
import com.vraj.khetihisab.panchang.SpecialDay
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkHeaderButton
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.decor.VkLeafDivider
import com.vraj.khetihisab.ui.decor.VkMoonPhase
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.settings.StBodyColumn
import com.vraj.khetihisab.ui.settings.StError
import com.vraj.khetihisab.ui.settings.StNote
import com.vraj.khetihisab.ui.settings.StScrollBody
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import java.time.LocalDate

@Composable
fun PanchangCalendarRoute(initialEpochDay: Long?, onBack: () -> Unit, onOpenReminders: (() -> Unit)? = null, onOpenEntry: ((String) -> Unit)? = null) {
    val vm: PanchangCalendarViewModel = screenViewModel(key = "panchang_calendar_$initialEpochDay") { deps, _ ->
        PanchangCalendarViewModel.create(deps, initialEpochDay)
    }
    PanchangCalendarScreen(vm, onBack, onOpenReminders, onOpenEntry)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanchangCalendarScreen(
    vm: PanchangCalendarViewModel,
    onBack: () -> Unit,
    onOpenReminders: (() -> Unit)? = null,
    onOpenEntry: ((String) -> Unit)? = null,
) {
    val ym by vm.month.collectAsStateWithLifecycle()
    val money by vm.money.collectAsStateWithLifecycle()
    val cal by vm.calendar.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val x = LocalVkExtra.current
    val cs = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }
    val created = stringResource(R.string.st_reminder_created)
    val exists = stringResource(R.string.st_reminder_exists)
    val failed = stringResource(R.string.st_reminder_failed)
    val view = stringResource(R.string.st_view)
    val weekdays = stringArrayResource(R.array.st_weekdays_short)

    LaunchedEffect(vm) {
        vm.events.collect { o ->
            val msg = when (o) {
                ReminderOutcome.CREATED -> created
                ReminderOutcome.EXISTS -> exists
                ReminderOutcome.FAILED -> failed
            }
            val r = snackbar.showSnackbar(msg, actionLabel = if (onOpenReminders != null && o != ReminderOutcome.FAILED) view else null)
            if (r == SnackbarResult.ActionPerformed) onOpenReminders?.invoke()
        }
    }

    Box(Modifier.fillMaxSize()) {
        StScrollBody {
            VkTopHeader(title = stringResource(R.string.st_pa_calendar), onBack = onBack) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    VkHeaderButton(onClick = vm::previousMonth) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.st_cal_prev), tint = x.onGradient)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${PanchangNames.gregorianMonths[ym.monthValue - 1]} ${ym.year}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = x.onGradient,
                            maxLines = 1,
                        )
                        Text(
                            cal?.takeIf { it.month == ym }?.lunarSubtitle.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = x.onGradient.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    VkHeaderButton(onClick = vm::nextMonth) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.st_cal_next), tint = x.onGradient)
                    }
                }
            }
            StBodyColumn {
                val month = cal?.takeIf { it.month == ym }
                if (month == null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                } else {
                    VkCard(Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                        Row(Modifier.fillMaxWidth()) {
                            weekdays.forEach { w ->
                                Text(
                                    w,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cs.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            month.weeks.forEach { week ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    week.forEach { cell ->
                                        if (cell == null) {
                                            Spacer(Modifier.weight(1f))
                                        } else {
                                            DayCell(cell, money[cell.date], isSelected = cell.date == selected, onClick = { vm.select(cell.date) }, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Legend (shape, not colour) + money colours.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                        MoneyLegend(stringResource(R.string.v2_cal_income_day), LocalVkExtra.current.income)
                        MoneyLegend(stringResource(R.string.v2_cal_expense_day), cs.error)
                        DayMarker.entries.forEach { m ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MarkerGlyph(m, size = 16.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(markerLabel(m), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }

                    VkSectionTitle(stringResource(R.string.st_pa_special_this_month))
                    if (month.specialDays.isEmpty()) {
                        StNote(stringResource(R.string.st_cal_no_special))
                    } else {
                        VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                            Column {
                                val typeLabels = com.vraj.khetihisab.panchang.PanchangAlertType.entries.associateWith { panchangTypeLabel(it) }
                                month.specialDays.forEachIndexed { i, s ->
                                    val dd = s.date.dayOfMonth.toString().padStart(2, '0')
                                    val wd = weekdays[PanchangCalendarModel.column(s.date)]
                                    VkListRow(
                                        title = "$dd $wd · ${PanchangCalendarModel.listTitle(s) { typeLabels.getValue(it) }}",
                                        meta = s.day.tithiGu,
                                        onClick = { vm.select(s.date) },
                                        trailing = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                PanchangCalendarModel.markersOf(s).forEach { MarkerGlyph(it, size = 16.dp) }
                                            }
                                        },
                                    )
                                    if (i < month.specialDays.lastIndex) VkRowDivider()
                                }
                            }
                        }
                    }
                    StNote(stringResource(R.string.st_pa_belief))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }

    selected?.let { date ->
        val cell = cal?.cells?.firstOrNull { it.date == date }
        DaySheet(date, cell, vm, onDismiss = { vm.select(null) }, onOpenEntry = onOpenEntry)
    }
}

@Composable
private fun DayCell(cell: CalendarCell, money: DayTotal?, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val incomeColor = LocalVkExtra.current.income
    val inc = money?.income ?: 0L
    val out = money?.expense ?: 0L
    val hasIn = inc > 0L
    val hasOut = out > 0L
    val shape = RoundedCornerShape(12.dp)
    val tithi = shortTithiLabel(cell.shortTithi)
    val name = cell.special?.titleGu
    val desc = listOfNotNull(
        "${cell.date.dayOfMonth} ${PanchangNames.gregorianMonths[cell.date.monthValue - 1]}",
        cell.day?.tithiGu,
        name,
    ).joinToString(", ")
    val bg = when {
        isSelected -> cs.primaryContainer
        cell.isSpecial -> cs.secondaryContainer
        else -> cs.surfaceContainerLowest
    }
    Box(
        modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(bg)
            .then(if (cell.isToday) Modifier.border(2.dp, cs.primary, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = desc },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = when {
                    hasIn && (!hasOut || inc >= out) -> incomeColor
                    hasOut -> cs.error
                    isSelected -> cs.onPrimaryContainer
                    else -> cs.onSurface
                },
                maxLines = 1,
            )
            Text(
                tithi,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = if (isSelected) cs.onPrimaryContainer else cs.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        if (hasIn || hasOut) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (hasIn) Box(Modifier.size(width = 12.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(incomeColor))
                if (hasOut) Box(Modifier.size(width = 12.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(cs.error))
            }
        }
        if (cell.markers.isNotEmpty()) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // At most two glyphs fit in a 46dp cell; the list and sheet show the rest.
                cell.markers.take(2).forEach { MarkerGlyph(it, size = 11.dp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DaySheet(date: LocalDate, cell: CalendarCell?, vm: PanchangCalendarViewModel, onDismiss: () -> Unit, onOpenEntry: ((String) -> Unit)?) {
    val cs = MaterialTheme.colorScheme
    val entries by vm.selectedEntries.collectAsStateWithLifecycle()
    var actionsFor by remember { mutableStateOf<LedgerRowUi?>(null) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val day = cell?.day ?: remember(date) { runCatching { PanchangSource.day(date) }.getOrNull() }
    val special: SpecialDay? = cell?.special
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        com.vraj.khetihisab.ui.components.VkSheetSystemBars()
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (day == null) {
                StError(stringResource(R.string.st_cal_out_of_range))
                VkTextButton(stringResource(R.string.st_close), onDismiss, Modifier.fillMaxWidth())
                return@Column
            }
            VkCard(Modifier.fillMaxWidth(), tone = VkTone.Panchang) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VkMoonPhase(day.moonIllumination, day.waxing, size = 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${date.dayOfMonth.toString().padStart(2, '0')} ${PanchangNames.gregorianMonths[date.monthValue - 1]} ${date.year} · ${day.weekdayGu}",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onTertiaryContainer,
                        )
                        Text(day.tithiGu, style = MaterialTheme.typography.headlineSmall, color = cs.onTertiaryContainer)
                        Text(day.vsLabel, style = MaterialTheme.typography.bodySmall, color = cs.onTertiaryContainer)
                    }
                }
            }
            Text(
                stringResource(
                    R.string.st_pa_tithi_ends,
                    "${PanchangCalendarModel.dm(day.tithiEndsAt)} ${dayPeriodLabel(PanchangCalendarModel.periodOfDay(day.tithiEndsAt.hour))} ${PanchangCalendarModel.hm12(day.tithiEndsAt)}",
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.st_pa_sun, PanchangCalendarModel.hm(day.sunrise), PanchangCalendarModel.hm(day.sunset)),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (day.nearBoundary) StNote(stringResource(R.string.st_pa_near_boundary))

            // ---- this day's money (all sections) ----
            DayMoney(entries, onRow = { actionsFor = it })
            if (onOpenEntry != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VkTonalButton(stringResource(R.string.v2_cal_add_income), { onOpenEntry(HomeRoutes.entry(HomeRoutes.KIND_INCOME, date = date)) }, Modifier.weight(1f))
                    VkTonalButton(stringResource(R.string.v2_cal_add_expense), { onOpenEntry(HomeRoutes.entry(HomeRoutes.KIND_EXPENSE, date = date)) }, Modifier.weight(1f))
                }
            }

            if (special == null) {
                StNote(stringResource(R.string.st_pa_normal_day))
            } else {
                Text(special.titleGu, style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    special.types.forEach { t -> VkBadge(panchangTypeLabel(t), cs.surfaceContainer, cs.onSurface) }
                    special.festivals.drop(1).forEach { f -> VkBadge(f, cs.tertiaryContainer, cs.onTertiaryContainer) }
                }
                val alert = vm.alertFor(special)
                if (alert == null) {
                    StNote(stringResource(R.string.st_pa_types_off))
                } else if (alert.showGuidance) {
                    Guidance(stringResource(R.string.st_pa_do), alert.doLines.take(4), avoid = false)
                    if (alert.avoidLines.isNotEmpty()) Guidance(stringResource(R.string.st_pa_avoid), alert.avoidLines.take(4), avoid = true)
                    StNote(stringResource(R.string.st_pa_belief))
                }
                VkTonalButton(stringResource(R.string.st_pa_make_reminder), { vm.makeReminder(special) }, Modifier.fillMaxWidth())
            }
            VkTextButton(stringResource(R.string.st_close), onDismiss, Modifier.fillMaxWidth())
        }
    }
    actionsFor?.let { row ->
        EntryActionsSheet(
            row = row,
            onEdit = { onOpenEntry?.invoke(HomeRoutes.editEntry(row.id)) },
            onDismiss = { actionsFor = null },
        )
    }
}

@Composable
private fun MoneyLegend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 14.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** "આ દિવસની એન્ટ્રી": totals in green / red, then every entry (tap → સુધારો / કાઢો). */
@Composable
private fun DayMoney(rows: List<LedgerRowUi>, onRow: (LedgerRowUi) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val incomeColor = LocalVkExtra.current.income
    Text(stringResource(R.string.v2_cal_day_entries), style = MaterialTheme.typography.titleLarge)
    if (rows.isEmpty()) {
        StNote(stringResource(R.string.v2_cal_no_entries))
        return
    }
    val inc = rows.filter { it.isIncome }.sumOf { it.amountPaise }
    val out = rows.filter { !it.isIncome }.sumOf { it.amountPaise }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VkCard(Modifier.weight(1f), contentPadding = 12.dp) {
            Text(stringResource(R.string.scr_income), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1)
            Text("+" + VkFormat.rupees(inc), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = incomeColor, maxLines = 1, softWrap = false)
        }
        VkCard(Modifier.weight(1f), contentPadding = 12.dp) {
            Text(stringResource(R.string.scr_expense), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1)
            Text("\u2212" + VkFormat.rupees(out), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = cs.error, maxLines = 1, softWrap = false)
        }
    }
    VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
        rows.forEachIndexed { i, r ->
            if (i > 0) VkRowDivider()
            VkListRow(
                title = r.title,
                meta = r.meta,
                leadingIcon = r.icon,
                onClick = { onRow(r) },
                trailing = {
                    Text(
                        (if (r.isIncome) "+" else "\u2212") + VkFormat.rupees(r.amountPaise),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (r.isIncome) incomeColor else cs.error,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
            )
        }
    }
}

@Composable
private fun Guidance(title: String, lines: List<String>, avoid: Boolean) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (avoid) cs.error else cs.primary)
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avoid) {
                    Box(Modifier.size(14.dp).border(1.5.dp, cs.error, androidx.compose.foundation.shape.CircleShape))
                } else {
                    VkLeafDivider()
                }
                Spacer(Modifier.width(8.dp))
                Text(line, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
