package com.vraj.khetihisab.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkFormat
import com.vraj.khetihisab.ui.components.VkHeaderButton
import com.vraj.khetihisab.ui.components.VkHeaderChip
import com.vraj.khetihisab.ui.components.VkNumericKeypad
import com.vraj.khetihisab.ui.components.VkOutlinedButton
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.ConfirmDialog
import com.vraj.khetihisab.ui.home.ScreensFormat
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object EntryTestTags {
    const val SAVE = "entry_save"
    const val AMOUNT = "entry_amount"
}

/** Stateful Entry destination (doc 06, artboard 03). [onSaved] receives the new transaction id. */
@Composable
fun EntryScreen(onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val vm = screenViewModel { deps, h -> EntryViewModel(deps, h) }
    val form by vm.form.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val confirm by vm.pendingConfirm.collectAsStateWithLifecycle()
    val savedId by vm.savedId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var viewer by remember { mutableStateOf<String?>(null) }
    val photoFailed = stringResource(R.string.scr_photo_failed)
    val voiceUnavailable = stringResource(R.string.scr_voice_unavailable)
    val voiceTip = stringResource(R.string.scr_voice_tip)
    val kgMsg = stringResource(R.string.scr_kg_normalized)

    LaunchedEffect(savedId) { savedId?.let(onSaved) }
    LaunchedEffect(form.kgNormalized) {
        if (form.kgNormalized) {
            vm.onEvent(EntryEvent.NoticeShown)
            snackbar.showSnackbar(kgMsg)
        }
    }

    val capture = rememberPhotoCapture("bill") { uri, temp ->
        scope.launch {
            val path = BillPhotoHelper.process(context, uri, temp)
            if (path == null) snackbar.showSnackbar(photoFailed) else vm.onEvent(EntryEvent.SetBill(path))
        }
    }
    val voice = rememberGujaratiVoiceInput(
        onText = { vm.onEvent(EntryEvent.SetVoiceNote(it)) },
        onUnavailable = { scope.launch { snackbar.showSnackbar("$voiceUnavailable. $voiceTip") } },
    )

    Box(Modifier.fillMaxSize()) {
        EntryContent(
            form = form,
            lists = lists,
            onEvent = vm::onEvent,
            onBack = onBack,
            onSave = vm::save,
            onTakePhoto = capture.takePhoto,
            onVoice = voice,
            onViewBill = { viewer = it },
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp))
    }

    if (confirm != null) {
        ConfirmDialog(
            title = stringResource(R.string.scr_apmc_replace_title),
            body = stringResource(R.string.scr_apmc_replace_body),
            confirmLabel = stringResource(R.string.scr_ok),
            onConfirm = { vm.confirmApmc(true) },
            onDismiss = { vm.confirmApmc(false) },
        )
    }
    viewer?.let { BillViewer(it) { viewer = null } }
}

/** Stateless 3-tap entry UI. Used by [EntryScreen] and the Compose UI test. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryContent(
    form: EntryForm,
    lists: EntryLists,
    onEvent: (EntryEvent) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onTakePhoto: () -> Unit,
    onVoice: () -> Unit,
    onViewBill: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var showDate by remember { mutableStateOf(false) }
    val todayLabel = if (form.date == ScreensFormat.today()) {
        stringResource(R.string.scr_today_chip, ScreensFormat.dmy(form.date))
    } else {
        ScreensFormat.dmy(form.date)
    }

    Box(Modifier.fillMaxSize().background(cs.surface)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                VkTopHeader(
                    title = stringResource(R.string.scr_entry_title),
                    onBack = onBack,
                    actions = {
                        val micLabel = stringResource(R.string.scr_voice_note)
                        VkHeaderButton(
                            onClick = onVoice,
                            modifier = Modifier.semantics { contentDescription = micLabel },
                        ) { VkIconImage(VkIcon.MIC, size = 30.dp) }
                    },
                ) {
                    VkHeaderChip(todayLabel, Modifier.clickable(role = Role.Button) { showDate = true })
                }
            }
            item {
                VkSegmented(
                    options = listOf(
                        EntryKind.INCOME to stringResource(R.string.scr_kind_income),
                        EntryKind.EXPENSE to stringResource(R.string.scr_kind_expense),
                    ),
                    selected = form.kind,
                    onSelect = { onEvent(EntryEvent.SetKind(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            // ---- Tap 1 ----
            item { VkSectionTitle(stringResource(R.string.scr_tap1), Modifier.padding(horizontal = 16.dp)) }
            item {
                val sections = listOf(
                    Triple(AccountSection.FARM, VkIcon.FARM, R.string.scr_sec_farm_short),
                    Triple(AccountSection.ANIMAL, VkIcon.ANIMAL, R.string.scr_sec_animal_short),
                    Triple(AccountSection.FAMILY, VkIcon.FAMILY, R.string.scr_sec_family_short),
                    Triple(AccountSection.GENERAL, VkIcon.UTILITY, R.string.scr_sec_general_short),
                    Triple(AccountSection.PERSONAL, VkIcon.PERSONAL, R.string.scr_sec_personal_short),
                )
                // 5 sections → two rows of 3 so labels like "પશુપાલન" never squeeze (doc 01 §8).
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (sec, icon, label) ->
                                EntryTile(
                                    label = stringResource(label),
                                    selected = form.section == sec,
                                    onClick = { onEvent(EntryEvent.SetSection(sec)) },
                                    modifier = Modifier.weight(1f),
                                    icon = icon,
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            // ---- Tap 2 ----
            if (form.section != null) {
                item { Tap2(form, lists, onEvent) }
            }
            // ---- Amount ----
            item { AmountArea(form, onEvent) }
            item {
                VkNumericKeypad(
                    onKey = { onEvent(EntryEvent.Key(it)) },
                    onBackspace = { onEvent(EntryEvent.Backspace) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    allowDecimal = true,
                )
            }
            // ---- Tap 3 ----
            item { VkSectionTitle(stringResource(R.string.scr_tap3), Modifier.padding(horizontal = 16.dp)) }
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VkSegmented(
                        options = listOf(false to stringResource(R.string.scr_paid), true to stringResource(R.string.scr_pending)),
                        selected = form.pending,
                        onSelect = { onEvent(EntryEvent.SetPending(it)) },
                        errorSelected = form.pending,
                    )
                    if (form.pending) {
                        OutlinedTextField(
                            value = form.counterparty,
                            onValueChange = { onEvent(EntryEvent.SetCounterparty(it)) },
                            label = { Text(stringResource(R.string.scr_counterparty), maxLines = 1) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VkOutlinedButton(stringResource(R.string.scr_bill_photo), onTakePhoto, Modifier.weight(1f))
                    VkOutlinedButton(stringResource(R.string.scr_voice_note), onVoice, Modifier.weight(1f))
                }
            }
            if (form.billPath != null || form.voiceNote != null) {
                item {
                    FlowRow(
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        form.billPath?.let { path ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = stringResource(R.string.scr_view_bill),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onViewBill(path) },
                                )
                                Spacer(Modifier.width(4.dp))
                                RemoveChip(stringResource(R.string.scr_bill_photo)) { onEvent(EntryEvent.SetBill(null)) }
                            }
                        }
                        form.voiceNote?.let { note ->
                            RemoveChip(note) { onEvent(EntryEvent.SetVoiceNote(null)) }
                            val suggestion = VoiceAmountParser.parseRupees(note)
                            if (suggestion != null && !form.apmcMode && form.amountPaise != suggestion * 100) {
                                VkChip(
                                    text = stringResource(R.string.scr_voice_use_amount, VkFormat.rupees(suggestion * 100)),
                                    selected = false,
                                    onClick = { onEvent(EntryEvent.UseAmount(suggestion)) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky save
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(LocalVkExtra.current.dock)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        ) {
            val amt = form.amountPaise
            VkPrimaryButton(
                text = if (amt > 0) stringResource(R.string.scr_save_amount, VkFormat.rupees(amt)) else stringResource(R.string.scr_save),
                onClick = onSave,
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(EntryTestTags.SAVE),
            )
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = form.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                com.vraj.khetihisab.ui.components.VkTextButton(stringResource(R.string.scr_ok), {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (!d.isAfter(LocalDate.now().plusDays(1))) onEvent(EntryEvent.SetDate(d))
                    }
                    showDate = false
                })
            },
            dismissButton = { com.vraj.khetihisab.ui.components.VkTextButton(stringResource(R.string.scr_cancel), { showDate = false }) },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun RemoveChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = false,
        onClick = onRemove,
        label = { Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp)) },
        trailingIcon = { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.semantics { contentDescription = label },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tap2(form: EntryForm, lists: EntryLists, onEvent: (EntryEvent) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (form.section) {
            AccountSection.FARM -> {
                VkSectionTitle(stringResource(R.string.scr_tap2_farm))
                lists.farms.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { f ->
                            EntryTile(
                                label = f.label,
                                sub = f.sub ?: stringResource(R.string.scr_none_dash),
                                selected = form.farmId == f.id,
                                onClick = { onEvent(EntryEvent.PickFarm(f.id)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (lists.categories.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lists.categories.forEach { c ->
                            VkChip(c.label, form.categoryId == c.id, { onEvent(EntryEvent.PickCategory(c.id, c.isCropSale)) })
                        }
                    }
                }
            }
            AccountSection.FAMILY -> {
                VkSectionTitle(stringResource(R.string.scr_tap2_family))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lists.families.forEach { f ->
                        EntryTile(
                            label = f.label,
                            selected = form.familyId == f.id,
                            onClick = { onEvent(EntryEvent.PickFamily(f.id)) },
                            modifier = Modifier.weight(1f),
                            icon = VkIcon.FAMILY,
                        )
                    }
                }
            }
            AccountSection.ANIMAL, AccountSection.GENERAL, AccountSection.PERSONAL -> {
                VkSectionTitle(stringResource(R.string.scr_tap2_category))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lists.categories.forEach { c ->
                        VkChip(c.label, form.categoryId == c.id, { onEvent(EntryEvent.PickCategory(c.id, c.isCropSale)) })
                    }
                }
            }
            null -> Unit
        }
    }
}

@Composable
private fun AmountArea(form: EntryForm, onEvent: (EntryEvent) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (form.section == AccountSection.FARM) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VkIconImage(VkIcon.CALCULATOR, size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.scr_apmc_switch),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Switch(checked = form.apmcMode, onCheckedChange = { onEvent(EntryEvent.SetApmcSwitch(it)) })
            }
        }
        if (!form.apmcMode) {
            VkCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.scr_amount), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                Text(
                    "₹" + groupTyped(form.amountText.ifEmpty { "0" }),
                    style = MaterialTheme.typography.displaySmall,
                    color = cs.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag(EntryTestTags.AMOUNT),
                )
            }
        } else {
            VkCard(Modifier.fillMaxWidth(), tone = VkTone.Primary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.scr_apmc_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Text(stringResource(R.string.scr_kg_rule), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApmcFieldTile(stringResource(R.string.scr_mann), form.mannText, form.activeField == ApmcField.MANN, Modifier.weight(1f)) {
                        onEvent(EntryEvent.SetField(ApmcField.MANN))
                    }
                    ApmcFieldTile(stringResource(R.string.scr_extra_kg), form.kgText, form.activeField == ApmcField.KG, Modifier.weight(1f)) {
                        onEvent(EntryEvent.SetField(ApmcField.KG))
                    }
                    ApmcFieldTile(stringResource(R.string.scr_rate_mann), form.rateText, form.activeField == ApmcField.RATE, Modifier.weight(1f)) {
                        onEvent(EntryEvent.SetField(ApmcField.RATE))
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.scr_total_mann), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                        Text(
                            stringResource(R.string.scr_mann_value, String.format(java.util.Locale.US, "%.2f", form.totalMann)),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.scr_total_amount), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                        Text(
                            VkFormat.rupees(form.amountPaise),
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                            color = cs.primary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag(EntryTestTags.AMOUNT),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApmcFieldTile(label: String, value: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(cs.surfaceContainerLowest)
            .border(if (active) 2.dp else 1.dp, if (active) cs.primary else cs.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = active }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            value.ifEmpty { "0" },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
            color = cs.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Selectable tile (sections, farms, families): 64dp tall, primary border + container when selected. */
@Composable
fun EntryTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: VkIcon? = null,
    sub: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) cs.primaryContainer else cs.surfaceContainerLowest)
            .border(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        if (icon != null) VkIconImage(icon, size = 28.dp)
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) cs.onPrimaryContainer else cs.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        if (sub != null) {
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Keypad text with Indian grouping on the whole-rupee part, keeping what was typed after "." ("4500" → "4,500"). */
private fun groupTyped(text: String): String {
    val dot = text.indexOf('.')
    val whole = if (dot < 0) text else text.substring(0, dot)
    val grouped = whole.toLongOrNull()?.let(com.vraj.khetihisab.ui.components.VkFormat::groupIndian) ?: whole
    return if (dot < 0) grouped else grouped + text.substring(dot)
}
