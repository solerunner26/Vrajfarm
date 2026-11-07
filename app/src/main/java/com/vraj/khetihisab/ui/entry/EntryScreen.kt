package com.vraj.khetihisab.ui.entry

import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.vraj.khetihisab.ui.components.VkActionBar
import com.vraj.khetihisab.ui.components.VkTextField
import com.vraj.khetihisab.ui.components.VkTonalButton
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
    const val NOTE = "entry_note"
    const val VOICE_DIALOG = "entry_voice_dialog"
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
    val snackbar = com.vraj.khetihisab.ui.components.rememberVkSnackbar()
    var viewer by remember { mutableStateOf<String?>(null) }
    val photoFailed = stringResource(R.string.scr_photo_failed)
    val voiceUnavailable = stringResource(R.string.scr_voice_unavailable)
    val voiceTip = stringResource(R.string.scr_voice_tip)
    val kgMsg = stringResource(R.string.scr_kg_normalized)

    LaunchedEffect(savedId) { savedId?.let(onSaved) }
    LaunchedEffect(form.kgNormalized) {
        if (form.kgNormalized) {
            vm.onEvent(EntryEvent.NoticeShown)
            snackbar.show(kgMsg)
        }
    }

    val capture = rememberPhotoCapture("bill") { uri, temp ->
        scope.launch {
            val path = BillPhotoHelper.process(context, uri, temp)
            if (path == null) snackbar.show(photoFailed) else vm.onEvent(EntryEvent.SetBill(path))
        }
    }
    // Speech is shown back first (owner request 2026-11-07): nothing fills the form until "હા, બરાબર છે".
    var heard by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val voice = rememberGujaratiVoiceInput(
        onText = { heard = it.trim().ifEmpty { null } },
        onUnavailable = { snackbar.show("$voiceUnavailable. $voiceTip") },
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
            onAddCategory = vm::addCategory,
        )
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
    heard?.let { text ->
        VoiceConfirmDialog(
            text = text,
            parsed = remember(text, lists) { vm.parseVoice(text) },
            lists = lists,
            onYes = { vm.applyVoice(text); heard = null },
            onAgain = { heard = null; voice() },
            onCancel = { heard = null },
        )
    }
    viewer?.let { BillViewer(it) { viewer = null } }
}

/**
 * "તમે આ બોલ્યા – બરાબર છે?": the heard sentence in full, then what it will fill, with
 * હા, બરાબર છે / ફરી બોલો / રદ કરો. Nothing reaches the form before the first button.
 */
@Composable
fun VoiceConfirmDialog(
    text: String,
    parsed: VoiceEntry,
    lists: EntryLists,
    onYes: () -> Unit,
    onAgain: () -> Unit,
    onCancel: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val rows = buildList {
        parsed.kind?.let { add(R.string.v7_vf_kind to stringResource(if (it == EntryKind.INCOME) R.string.scr_kind_income else R.string.scr_kind_expense)) }
        parsed.section?.let { sec ->
            val l = when (sec) {
                AccountSection.FARM -> R.string.scr_sec_farm_short
                AccountSection.ANIMAL -> R.string.scr_sec_animal_short
                AccountSection.FAMILY -> R.string.scr_sec_family_short
                AccountSection.GENERAL -> R.string.scr_sec_general_short
                AccountSection.PERSONAL -> R.string.scr_sec_personal_short
            }
            add(R.string.v7_vf_section to stringResource(l))
        }
        parsed.farmId?.let { id -> lists.farms.firstOrNull { it.id == id }?.let { add(R.string.v7_vf_farm to it.label) } }
        parsed.familyId?.let { id -> lists.families.firstOrNull { it.id == id }?.let { add(R.string.v7_vf_bhagiya to it.label) } }
        parsed.categoryId?.let { id -> lists.allCategories.firstOrNull { it.id == id }?.let { add(R.string.v7_vf_type to it.name) } }
        parsed.rupees?.let { add(R.string.v7_vf_amount to VkFormat.rupees(it * 100)) }
        if (parsed.pending) add(R.string.v7_vf_credit to (parsed.counterparty ?: stringResource(R.string.v2_cp_someone)))
    }
    com.vraj.khetihisab.ui.components.VkDialog(
        title = stringResource(R.string.v7_voice_check_title),
        onDismiss = onCancel,
        confirmLabel = stringResource(R.string.v7_voice_yes),
        onConfirm = onYes,
        secondaryLabel = stringResource(R.string.v7_voice_again),
        onSecondary = onAgain,
        modifier = Modifier.testTag(EntryTestTags.VOICE_DIALOG),
    ) {
        Text(stringResource(R.string.v7_voice_heard), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        VkCard(Modifier.fillMaxWidth(), tone = VkTone.Primary) {
            Text("“$text”", style = MaterialTheme.typography.titleMedium)
        }
        if (rows.isEmpty()) {
            Text(stringResource(R.string.v7_voice_only_note), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.v7_voice_fills), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, modifier = Modifier.weight(0.42f))
                    Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(0.58f))
                }
            }
        }
    }
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
    onAddCategory: ((String) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    var showDate by remember { mutableStateOf(false) }
    val todayLabel = if (form.date == ScreensFormat.today()) {
        stringResource(R.string.scr_today_chip, ScreensFormat.dmy(form.date))
    } else {
        ScreensFormat.dmy(form.date)
    }

    // The save bar sits BELOW the list (Column), never over it: nothing — the note, a field the
    // keyboard scrolls to, the last card — can slide under the Save button (2026-11-07 audit).
    // imePadding: the screen shrinks above the keyboard, so the note being typed stays in view.
    Column(Modifier.fillMaxSize().background(cs.surface).imePadding()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                VkTopHeader(
                    title = stringResource(if (form.editId != null) R.string.v2_entry_edit_title else R.string.scr_entry_title),
                    onBack = onBack,
                    actions = {
                        val micLabel = stringResource(R.string.scr_voice_note)
                        VkHeaderButton(
                            onClick = onVoice,
                            modifier = Modifier.semantics { contentDescription = micLabel },
                        ) { VkIconImage(VkIcon.MIC, size = 30.dp) }
                    },
                ) {
                    // 60dp touch box around the date pill (audit #13).
                    Box(
                        Modifier.heightIn(min = 60.dp).clip(RoundedCornerShape(30.dp)).clickable(role = Role.Button) { showDate = true },
                        contentAlignment = Alignment.CenterStart,
                    ) { VkHeaderChip(todayLabel) }
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
                item { Tap2(form, lists, onEvent, onAddCategory) }
            }
            // ---- Note: above the amount and keypad so a spoken or typed note is read before
            // the numbers (owner request 2026-11-07); never down by the sticky save bar. ----
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VkTextField(
                        value = form.voiceNote.orEmpty(),
                        onValueChange = { onEvent(EntryEvent.SetNoteText(it)) },
                        label = stringResource(R.string.v3_note),
                        singleLine = false,
                        minLines = 1,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag(EntryTestTags.NOTE),
                    )
                    form.voiceNote?.let { note ->
                        // The whole-entry parser, not a bare number scan: "ખાતર બે હજાર ખેતર ત્રણ" is
                        // ₹2,000 for farm 3, never ₹2,003 (2026-11-07 audit).
                        val suggestion = remember(note, lists) {
                            VoiceEntryParser.parse(note, lists.farms.map { it.id to it.label }, lists.families.map { it.id to it.label }, lists.allCategories).rupees
                        }
                        // Under ₹10 is a count, not money ("બે થેલી" ≠ ₹2).
                        if (suggestion != null && suggestion >= 10 && !form.apmcMode && form.amountPaise != suggestion * 100) {
                            VkTonalButton(
                                stringResource(R.string.scr_voice_use_amount, VkFormat.rupees(suggestion * 100)),
                                { onEvent(EntryEvent.UseAmount(suggestion)) },
                                Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
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
                    // Plain words instead of "ચૂકવેલ / ઉધાર": the labels follow આવક / ખર્ચ (owner feedback).
                    val income = form.txnType == com.vraj.khetihisab.data.entity.TxnType.INCOME
                    VkSegmented(
                        options = listOf(
                            false to stringResource(if (income) R.string.v2_pay_got else R.string.v2_pay_gave),
                            true to stringResource(if (income) R.string.v2_pay_will_get else R.string.v2_pay_will_give),
                        ),
                        selected = form.pending,
                        onSelect = { onEvent(EntryEvent.SetPending(it)) },
                        errorSelected = form.pending,
                    )
                    if (form.pending) {
                        VkTextField(
                            value = form.counterparty,
                            onValueChange = { onEvent(EntryEvent.SetCounterparty(it)) },
                            label = stringResource(if (income) R.string.v2_cp_from else R.string.v2_cp_to),
                            isError = form.needsName,
                            supporting = if (form.needsName) stringResource(R.string.v2_cp_required) else null,
                        )
                        val who = form.counterparty.trim().ifEmpty { stringResource(R.string.v2_cp_someone) }
                        val amt = ScreensFormat.signedRupees(form.amountPaise)
                        VkCard(Modifier.fillMaxWidth(), tone = VkTone.Error) {
                            Text(
                                stringResource(if (income) R.string.v2_udhaar_sentence_get else R.string.v2_udhaar_sentence_give, who, amt),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(stringResource(R.string.v2_udhaar_how_settle), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                // One voice entry point: the header mic (audit #26). The bill photo gets the full width.
                VkTonalButton(
                    stringResource(R.string.scr_bill_photo),
                    onTakePhoto,
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    icon = VkIcon.CAMERA,
                )
            }
            if (form.billPath != null) {
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
                    }
                }
            }
        }

        // Sticky save (audit #23)
        VkActionBar {
            val amt = form.amountPaise
            VkPrimaryButton(
                text = if (amt > 0) stringResource(R.string.scr_save_amount, VkFormat.rupees(amt)) else stringResource(R.string.scr_save),
                onClick = onSave,
                enabled = form.canSave,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag(EntryTestTags.SAVE),
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
        trailingIcon = {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Rounded.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tap2(form: EntryForm, lists: EntryLists, onEvent: (EntryEvent) -> Unit, onAddCategory: ((String) -> Unit)? = null) {
    var addingType by rememberSaveable { mutableStateOf(false) }
    if (addingType && onAddCategory != null) {
        NewTypeDialog(onDismiss = { addingType = false }) { name -> onAddCategory(name); addingType = false }
    }
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
                // Equal tiles in a 2-column grid (owner review: no ragged chip rows).
                com.vraj.khetihisab.ui.components.VkChoiceGrid(
                    options = lists.categories.map { it.id to it.label },
                    selected = form.categoryId,
                    onToggle = { id -> lists.categories.firstOrNull { it.id == id }?.let { c -> onEvent(EntryEvent.PickCategory(c.id, c.isCropSale)) } },
                    addLabel = if (onAddCategory != null) stringResource(R.string.v3_new_type) else null,
                    onAdd = { addingType = true },
                )
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
                com.vraj.khetihisab.ui.components.VkChoiceGrid(
                    options = lists.categories.map { it.id to it.label },
                    selected = form.categoryId,
                    onToggle = { id -> lists.categories.firstOrNull { it.id == id }?.let { c -> onEvent(EntryEvent.PickCategory(c.id, c.isCropSale)) } },
                    addLabel = if (onAddCategory != null) stringResource(R.string.v3_new_type) else null,
                    onAdd = { addingType = true },
                )
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
                com.vraj.khetihisab.ui.components.VkFitText(
                    "₹" + groupTyped(form.amountText.ifEmpty { "0" }),
                    style = MaterialTheme.typography.displaySmall,
                    color = cs.onSurface,
                    minSize = 22,
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
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.scr_total_amount), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                        com.vraj.khetihisab.ui.components.VkFitText(
                            VkFormat.rupees(form.amountPaise),
                            style = MaterialTheme.typography.displaySmall,
                            color = cs.primary,
                            minSize = 20,
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
            style = com.vraj.khetihisab.ui.theme.VkCurrencyRow,
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
                style = MaterialTheme.typography.bodySmall,
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

/** "+ નવો પ્રકાર": name the new type; it is saved for this section and આવક/ખર્ચ and selected. */
@Composable
private fun NewTypeDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    com.vraj.khetihisab.ui.components.VkDialog(
        title = stringResource(R.string.v3_new_type_title),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.scr_save),
        onConfirm = { onSave(name) },
        confirmEnabled = name.isNotBlank(),
    ) {
        VkTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = stringResource(R.string.st_display_name),
            modifier = Modifier.testTag("new_type_name"),
        )
    }
}
