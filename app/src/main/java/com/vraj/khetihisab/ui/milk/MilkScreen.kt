package com.vraj.khetihisab.ui.milk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.dao.MilkDao
import com.vraj.khetihisab.data.dao.MilkTotals
import com.vraj.khetihisab.data.entity.MilkEntryEntity
import com.vraj.khetihisab.data.entity.MilkSession
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.ui.components.VkActionBar
import com.vraj.khetihisab.ui.components.VkBottomSheet
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkConfirmDeleteDialog
import com.vraj.khetihisab.ui.components.VkDangerButton
import com.vraj.khetihisab.ui.components.VkDateChip
import com.vraj.khetihisab.ui.components.VkDatePickerDialog
import com.vraj.khetihisab.ui.components.VkEmptyState
import com.vraj.khetihisab.ui.components.VkMonthChip
import com.vraj.khetihisab.ui.components.VkTextField
import com.vraj.khetihisab.ui.components.rememberVkSnackbar
import com.vraj.khetihisab.ui.home.MoneyStat
import com.vraj.khetihisab.ui.theme.VkCurrencyRow
import com.vraj.khetihisab.ui.theme.VkDimens
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkFormat
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkOutlinedButton
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.components.vkAppear
import com.vraj.khetihisab.ui.home.MonthPickerDialog
import com.vraj.khetihisab.ui.home.ScreensFormat
import com.vraj.khetihisab.ui.home.StatTile
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Pure milk-register rules (unit-tested). */
object MilkLogic {
    /** "6.5" / "6,5" → 6.5; blank or invalid → null. */
    fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && !it.isNaN() }

    /** Rupees text per litre → paise ("55.50" → 5550). */
    fun ratePaise(text: String): Long? = number(text)?.let { Math.round(it * 100) }

    /** Dairy paid − register says. Positive = dairy paid more, negative = dairy paid less. */
    fun difference(calculated: Long, paid: Long): Long = paid - calculated
}

data class MilkUi(
    val month: YearMonth,
    val entries: List<MilkEntryEntity> = emptyList(),
    val totals: MilkTotals = MilkTotals(0.0, 0),
    val paid: Long = 0,
    val animals: List<String> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class MilkViewModel(private val dao: MilkDao, today: LocalDate = ScreensFormat.today()) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(today))
    val lastRate = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch { lastRate.value = dao.lastRate() }
    }

    val state: StateFlow<MilkUi> = month.flatMapLatest { ym ->
        val r = DateRange.month(ym)
        combine(dao.entries(r.from, r.to), dao.totals(r.from, r.to), dao.dairyPaid(r.from, r.to), dao.animals()) { e, t, p, a ->
            MilkUi(ym, e, t, p, a)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MilkUi(month.value))

    fun selectMonth(ym: YearMonth) { month.value = ym }

    fun save(existing: MilkEntryEntity?, date: LocalDate, session: String, animal: String, litres: Double, fat: Double?, rate: Long) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            if (existing == null) {
                dao.insert(MilkEntryEntity(date = date, session = session, animal = animal.trim(), litres = litres, fatPercent = fat, ratePaise = rate, createdAt = now, updatedAt = now))
            } else {
                dao.update(existing.copy(date = date, session = session, animal = animal.trim(), litres = litres, fatPercent = fat, ratePaise = rate, updatedAt = now))
            }
            lastRate.value = rate
        }
    }

    fun delete(e: MilkEntryEntity) { viewModelScope.launch { dao.delete(e) } }

    /** Undo for [delete]: the same row (same id) goes back. */
    fun restore(e: MilkEntryEntity) { viewModelScope.launch { dao.insert(e) } }
}

@Composable
fun MilkRoute(onBack: () -> Unit, onDairyPayment: () -> Unit) {
    val vm = screenViewModel { deps, _ -> MilkViewModel(deps.repos.milk) }
    val s by vm.state.collectAsStateWithLifecycle()
    val lastRate by vm.lastRate.collectAsStateWithLifecycle()
    val snackbar = rememberVkSnackbar()
    val saved = stringResource(R.string.v5_saved)
    val deleted = stringResource(R.string.v5_deleted)
    val undo = stringResource(R.string.v5_undo)
    MilkScreen(
        s, lastRate, onBack, vm::selectMonth,
        onSave = { e, d, ses, a, l, f, r ->
            vm.save(e, d, ses, a, l, f, r)
            snackbar.show(saved, success = true)
        },
        onDelete = { e ->
            vm.delete(e)
            snackbar.show(deleted, actionLabel = undo) { vm.restore(e) }
        },
        onDairyPayment = onDairyPayment,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MilkScreen(
    s: MilkUi,
    lastRate: Long?,
    onBack: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onSave: (MilkEntryEntity?, LocalDate, String, String, Double, Double?, Long) -> Unit,
    onDelete: (MilkEntryEntity) -> Unit,
    onDairyPayment: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val pad = Modifier.padding(horizontal = 16.dp)
    var pickMonth by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MilkEntryEntity?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(cs.surface)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("milk_list"),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(VkDimens.listGap),
        ) {
            item(key = "header") {
                VkTopHeader(
                    stringResource(R.string.v3_milk_title),
                    onBack = onBack,
                    actions = { VkMonthChip(ScreensFormat.monthYearGu(s.month), { pickMonth = true }) },
                )
            }
            item(key = "totals") {
                val diff = MilkLogic.difference(s.totals.amountPaise, s.paid)
                VkCard(pad.fillMaxWidth().vkAppear(0, "milk_totals").testTag("milk_totals"), tone = VkTone.Primary) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(stringResource(R.string.v3_milk_litres), stringResource(R.string.v3_milk_litres_value, fmt(s.totals.litres)), Modifier.weight(1f), big = true)
                        MoneyStat(stringResource(R.string.v3_milk_should), s.totals.amountPaise, Modifier.weight(1f), valueColor = cs.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyStat(stringResource(R.string.v3_milk_paid), s.paid, Modifier.weight(1f))
                        MoneyStat(
                            stringResource(R.string.v3_milk_diff),
                            diff,
                            Modifier.weight(1f),
                            valueColor = if (diff < 0) cs.error else cs.primary,
                        )
                    }
                    Text(
                        stringResource(
                            when {
                                s.totals.amountPaise == 0L -> R.string.v3_milk_diff_none
                                diff < 0 -> R.string.v3_milk_diff_less
                                diff > 0 -> R.string.v3_milk_diff_more
                                else -> R.string.v3_milk_diff_equal
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSecondaryContainer,
                    )
                    VkOutlinedButton(stringResource(R.string.v3_milk_record_payment), onDairyPayment, Modifier.fillMaxWidth())
                }
            }
            item(key = "title") { VkSectionTitle(stringResource(R.string.v3_milk_entries), pad.vkAppear(1, "milk_title")) }
            if (s.entries.isEmpty()) {
                item(key = "empty") {
                    VkCard(pad.fillMaxWidth()) {
                        VkEmptyState(VkIcon.ANIMAL, stringResource(R.string.v3_milk_empty), actionLabel = stringResource(R.string.v3_milk_add), onAction = { adding = true })
                    }
                }
            } else {
                item(key = "rows") {
                    VkCard(pad.fillMaxWidth().vkAppear(2, "milk_rows"), contentPadding = 0.dp) {
                        s.entries.forEachIndexed { i, e ->
                            if (i > 0) VkRowDivider()
                            VkListRow(
                                title = "${e.animal} · " + stringResource(if (e.session == MilkSession.MORNING) R.string.v3_milk_am else R.string.v3_milk_pm),
                                meta = ScreensFormat.dmy(e.date) + " · " + stringResource(R.string.v3_milk_litres_value, fmt(e.litres)) +
                                    (e.fatPercent?.let { " · " + stringResource(R.string.v3_milk_fat_value, fmt(it)) } ?: ""),
                                leadingIcon = VkIcon.ANIMAL,
                                onClick = { editing = e },
                                trailing = { Text(VkFormat.rupees(e.amountPaise), style = VkCurrencyRow, maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                }
            }
        }
        // Add sits in the bottom action bar, always reachable (audit #23).
        VkActionBar {
            VkPrimaryButton(stringResource(R.string.v3_milk_add), { adding = true }, Modifier.weight(1f).testTag("milk_add"), sign = "+")
        }
    }

    if (pickMonth) MonthPickerDialog(s.month, onPick = { onSelectMonth(it); pickMonth = false }, onDismiss = { pickMonth = false })
    if (adding || editing != null) {
        MilkSheet(
            existing = editing,
            animals = s.animals,
            lastRate = lastRate,
            onDismiss = { adding = false; editing = null },
            onSave = { d, ses, a, l, f, r -> onSave(editing, d, ses, a, l, f, r); adding = false; editing = null },
            onDelete = editing?.let { e -> { onDelete(e); editing = null } },
        )
    }
}

private fun fmt(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else "%.2f".format(v).trimEnd('0').trimEnd('.')

/** Add / edit one milk entry in a bottom sheet that rises with the keyboard (audit #9). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MilkSheet(
    existing: MilkEntryEntity?,
    animals: List<String>,
    lastRate: Long?,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, String, Double, Double?, Long) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val today = ScreensFormat.today()
    var date by remember { mutableStateOf(existing?.date ?: today) }
    var pickDate by remember { mutableStateOf(false) }
    var session by rememberSaveable { mutableStateOf(existing?.session ?: if (java.time.LocalTime.now().hour < 14) MilkSession.MORNING else MilkSession.EVENING) }
    var animal by rememberSaveable { mutableStateOf(existing?.animal ?: animals.firstOrNull().orEmpty()) }
    var litres by rememberSaveable { mutableStateOf(existing?.litres?.let(::fmt).orEmpty()) }
    var fat by rememberSaveable { mutableStateOf(existing?.fatPercent?.let(::fmt).orEmpty()) }
    var rate by rememberSaveable { mutableStateOf((existing?.ratePaise ?: lastRate)?.let { fmt(it / 100.0) }.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val l = MilkLogic.number(litres)
    val r = MilkLogic.ratePaise(rate)
    val ok = animal.isNotBlank() && l != null && l > 0 && r != null && r > 0
    val decimal = { t: String -> t.filter { c -> c.isDigit() || c == '.' } }
    VkBottomSheet(stringResource(if (existing == null) R.string.v3_milk_add else R.string.v2_edit), onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VkDateChip(
                if (date == today) stringResource(R.string.scr_today_chip, ScreensFormat.dmy(date)) else ScreensFormat.dmy(date),
                { pickDate = true },
            )
            VkSegmented(
                options = listOf(MilkSession.MORNING to stringResource(R.string.v3_milk_am), MilkSession.EVENING to stringResource(R.string.v3_milk_pm)),
                selected = session,
                onSelect = { session = it },
            )
            if (animals.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    animals.take(8).forEach { a -> VkChip(a, animal == a, { animal = a }) }
                }
            }
            VkTextField(animal, { animal = it.take(40) }, stringResource(R.string.v3_milk_animal))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VkTextField(litres, { litres = decimal(it) }, stringResource(R.string.v3_milk_litres), Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                VkTextField(fat, { fat = decimal(it) }, stringResource(R.string.v3_milk_fat), Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            }
            VkTextField(rate, { rate = decimal(it) }, stringResource(R.string.v3_milk_rate), keyboardType = KeyboardType.Decimal)
            if (ok) Text(stringResource(R.string.v3_milk_amount_line, VkFormat.rupees(Math.round(l!! * r!!))), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            VkPrimaryButton(
                stringResource(R.string.scr_save),
                { if (ok) onSave(date, session, animal, l!!, MilkLogic.number(fat), r!!) },
                Modifier.fillMaxWidth().testTag("milk_save"),
                enabled = ok,
            )
            if (onDelete != null) {
                VkDangerButton(stringResource(R.string.scr_delete), { confirmDelete = true }, Modifier.fillMaxWidth())
            }
        }
    }
    if (pickDate) VkDatePickerDialog(date, { date = it }, { pickDate = false }, maxDate = today)
    if (confirmDelete && onDelete != null) {
        VkConfirmDeleteDialog(
            what = existing?.let { "${it.animal} · ${ScreensFormat.dmy(it.date)} · ${VkFormat.rupees(it.amountPaise)}" }.orEmpty(),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
