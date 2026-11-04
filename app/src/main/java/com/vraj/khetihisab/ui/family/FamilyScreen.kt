package com.vraj.khetihisab.ui.family

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.share.PhoneNormalizer
import com.vraj.khetihisab.share.WhatsAppTemplateStore
import com.vraj.khetihisab.ui.components.AmountKind
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.ConfirmDialog
import com.vraj.khetihisab.ui.home.MonthPickerDialog
import com.vraj.khetihisab.ui.home.PersonAvatar
import com.vraj.khetihisab.ui.home.PlainBottomPadding
import com.vraj.khetihisab.ui.home.ScreensFormat
import com.vraj.khetihisab.ui.home.ScreensShare
import com.vraj.khetihisab.ui.home.StatTile
import com.vraj.khetihisab.ui.home.TxnListRow
import com.vraj.khetihisab.ui.home.rememberScreensDeps
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon
import java.time.YearMonth

@Composable
fun FamilyRoute(onBack: () -> Unit, onAddAdvance: (familyId: Long?) -> Unit, onEditEntry: (Long) -> Unit = {}) {
    val ctx = LocalContext.current
    val fallback = stringResource(R.string.scr_advance)
    val vm = screenViewModel { deps, handle ->
        FamilyViewModel(RepoFamilySource(deps.repos, WhatsAppTemplateStore(ctx)), handle, fallbackTitle = fallback)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val deps = rememberScreensDeps()
    FamilyScreen(
        state = state,
        onBack = onBack,
        onSelectFamily = vm::selectFamily,
        onSelectMonth = vm::selectMonth,
        onDelete = vm::delete,
        onSavePhone = vm::savePhone,
        onSend = { phone, text -> ScreensShare.whatsApp(ctx, deps, phone, text) },
        onAddAdvance = { onAddAdvance(state.family?.id) },
        onEditEntry = onEditEntry,
    )
}

/** Family A/B advance ledger + WhatsApp receipt (doc 08, artboard 05). */
@Composable
fun FamilyScreen(
    state: FamilyUiState,
    onBack: () -> Unit,
    onSelectFamily: (String) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onDelete: (Long) -> Unit,
    onSavePhone: (String) -> Unit,
    onSend: (phone: String?, text: String) -> Unit,
    onAddAdvance: () -> Unit,
    onEditEntry: (Long) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val pad = Modifier.padding(horizontal = 16.dp)
    var pickMonth by rememberSaveable { mutableStateOf(false) }
    var askPhone by rememberSaveable { mutableStateOf(false) }
    var sendAfterPhone by rememberSaveable { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsFor by remember { mutableStateOf<com.vraj.khetihisab.ui.home.LedgerRowUi?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().background(cs.surface).testTag("family_list"),
        contentPadding = PaddingValues(bottom = PlainBottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") { VkTopHeader(stringResource(R.string.scr_family_title), onBack = onBack) }
        item(key = "seg") {
            VkSegmented(
                options = listOf(
                    FamilyLogic.CODE_A to (state.names[FamilyLogic.CODE_A]?.takeIf { it.isNotBlank() } ?: stringResource(R.string.scr_family_a)),
                    FamilyLogic.CODE_B to (state.names[FamilyLogic.CODE_B]?.takeIf { it.isNotBlank() } ?: stringResource(R.string.scr_family_b)),
                ),
                selected = state.code,
                onSelect = onSelectFamily,
                modifier = pad,
            )
        }
        item(key = "hero") {
            VkCard(pad.fillMaxWidth().testTag("family_hero"), tone = VkTone.Primary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PersonAvatar(photoPath = null, initial = if (state.code == FamilyLogic.CODE_B) "૨" else "૧", size = 56.dp, onClick = { askPhone = true })
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.displayName.ifEmpty { stringResource(if (state.code == FamilyLogic.CODE_B) R.string.scr_family_b else R.string.scr_family_a) },
                            style = MaterialTheme.typography.titleLarge,
                            color = cs.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val phone = state.phone
                        if (phone != null) {
                            Text(PhoneNormalizer.display(phone), style = MaterialTheme.typography.bodyMedium, color = cs.onSecondaryContainer, maxLines = 1)
                        } else {
                            VkTextButton(stringResource(R.string.scr_add_number), { askPhone = true })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        stringResource(R.string.scr_month_advance, ScreensFormat.monthGu(state.month)),
                        ScreensFormat.signedRupees(state.monthTotal),
                        Modifier.weight(1f).testTag("family_month_total"),
                        cs.onSecondaryContainer,
                        big = true,
                    )
                    StatTile(
                        stringResource(R.string.scr_year_total, state.month.year),
                        ScreensFormat.signedRupees(state.yearTotal),
                        Modifier.weight(1f).testTag("family_year_total"),
                        cs.onSecondaryContainer,
                        big = true,
                    )
                }
                VkChip(ScreensFormat.monthYearGu(state.month) + " ▾", selected = false, onClick = { pickMonth = true })
            }
        }
        item(key = "list_title") { VkSectionTitle(stringResource(R.string.scr_this_month_list), pad) }
        item(key = "list") {
            VkCard(pad.fillMaxWidth(), contentPadding = 0.dp) {
                if (state.rows.isEmpty()) {
                    Text(
                        stringResource(R.string.scr_no_advance_month),
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                state.rows.forEachIndexed { i, r ->
                    if (i > 0) VkRowDivider()
                    TxnListRow(
                        title = r.title,
                        meta = r.meta,
                        icon = VkIcon.FAMILY,
                        amountPaise = r.amountPaise,
                        kind = AmountKind.NEUTRAL,
                        billPath = null,
                        onViewBill = {},
                        onClick = {
                            actionsFor = com.vraj.khetihisab.ui.home.LedgerRowUi(
                                id = r.id, title = r.title, meta = r.meta, icon = VkIcon.FAMILY, amountPaise = r.amountPaise,
                                kind = AmountKind.NEUTRAL, billPath = null, pending = false,
                                payStatus = com.vraj.khetihisab.data.entity.PayStatus.PAID,
                            )
                        },
                    )
                }
            }
        }
        item(key = "preview") {
            VkCard(pad.fillMaxWidth()) {
                Text(stringResource(R.string.scr_wa_preview), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp))
                        .background(cs.secondaryContainer)
                        .padding(14.dp),
                ) {
                    Text(state.receipt, style = MaterialTheme.typography.bodyLarge, color = cs.onSecondaryContainer, modifier = Modifier.testTag("family_receipt"))
                }
            }
        }
        item(key = "send") {
            VkPrimaryButton(
                stringResource(R.string.scr_wa_send_receipt),
                {
                    if (state.phone == null) {
                        sendAfterPhone = true
                        askPhone = true
                    } else {
                        onSend(state.phone, state.receipt)
                    }
                },
                pad.fillMaxWidth(),
                icon = VkIcon.MESSAGE,
                enabled = state.family != null,
            )
        }
        item(key = "add") { VkTonalButton(stringResource(R.string.scr_add_advance), onAddAdvance, pad.fillMaxWidth()) }
    }

    if (pickMonth) {
        MonthPickerDialog(state.month, onPick = { onSelectMonth(it); pickMonth = false }, onDismiss = { pickMonth = false })
    }
    if (askPhone) {
        PhoneDialog(
            initial = state.phone.orEmpty(),
            sendAfter = sendAfterPhone,
            onSave = { phone ->
                onSavePhone(phone)
                askPhone = false
                if (sendAfterPhone) onSend(phone, state.receipt)
                sendAfterPhone = false
            },
            onDismiss = {
                askPhone = false
                sendAfterPhone = false
            },
        )
    }
    actionsFor?.let { r ->
        com.vraj.khetihisab.ui.home.EntryActionsSheet(r, onEdit = { onEditEntry(r.id) }, onDismiss = { actionsFor = null })
    }
    deleteId?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.scr_delete_entry_title),
            body = null,
            confirmLabel = stringResource(R.string.scr_delete),
            onConfirm = {
                onDelete(id)
                deleteId = null
            },
            onDismiss = { deleteId = null },
        )
    }
}

@Composable
private fun PhoneDialog(initial: String, sendAfter: Boolean, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text(stringResource(R.string.scr_wa_add_number_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = false
                },
                label = { Text(stringResource(R.string.scr_phone)) },
                singleLine = true,
                isError = error,
                supportingText = if (error) {
                    { Text(stringResource(R.string.hm_phone_invalid)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().testTag("family_phone_input"),
            )
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VkPrimaryButton(
                    stringResource(if (sendAfter) R.string.hm_save_send else R.string.scr_save),
                    { if (FamilyLogic.isValidPhone(text)) onSave(text.trim()) else error = true },
                    Modifier.fillMaxWidth(),
                )
                VkTextButton(stringResource(R.string.scr_cancel), onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}
