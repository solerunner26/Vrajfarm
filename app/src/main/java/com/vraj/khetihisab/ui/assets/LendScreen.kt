package com.vraj.khetihisab.ui.assets

import androidx.compose.ui.platform.testTag

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.runtime.remember

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.media.PersonPhotoCapture
import com.vraj.khetihisab.media.openAppSettings
import com.vraj.khetihisab.media.rememberPersonPhotoCapture
import com.vraj.khetihisab.share.WhatsAppLauncher
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkOutlinedButton
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.theme.VkIcon

/** Lend Asset form (doc 11, artboard 10): photo → borrower → asset → dates → WhatsApp preview. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LendScreen(vm: LendViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val f = st.form
    var pickHandover by rememberSaveable { mutableStateOf(false) }
    var pickReturn by rememberSaveable { mutableStateOf(false) }

    val camera = rememberPersonPhotoCapture(folder = PersonPhotoCapture.FOLDER_BORROWERS) { path -> vm.onPhoto(path) }

    LaunchedEffect(vm) {
        vm.eventFlow.collect { e ->
            when (e) {
                is LendEvent.Saved -> {
                    if (e.sendText != null) {
                        // Pre-filled only; the user taps Send in WhatsApp (handover/12).
                        val r = WhatsAppLauncher.open(context, e.phone, e.sendText)
                        if (r.opened) vm.onReminderOpened(e.loanId)
                    }
                    onDone()
                }
            }
        }
    }

    Scaffold(
        topBar = { VkTopHeader(title = stringResource(R.string.as_lend_title), onBack = onBack) },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VkTonalButton(stringResource(R.string.as_save), onClick = { vm.save(send = false) }, modifier = Modifier.weight(1f), enabled = !st.saving)
                VkPrimaryButton(stringResource(R.string.as_save_send), onClick = { vm.save(send = true) }, modifier = Modifier.weight(1f), enabled = !st.saving)
            }
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1 · photo
            AsLabel(stringResource(R.string.as_step_photo))
            VkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsPhoto(f.photoPath, 96.dp, 120.dp, 16.dp, stringResource(R.string.as_photo), onClick = camera::launch)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.as_take_photo_hint), style = MaterialTheme.typography.bodyMedium)
                        VkOutlinedButton(
                            stringResource(if (f.photoPath == null) R.string.as_take_photo else R.string.as_retake),
                            onClick = camera::launch,
                            enabled = !camera.busy,
                        )
                    }
                }
                if (camera.permissionDenied) {
                    Text(stringResource(R.string.as_camera_permission), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    VkTextButton(stringResource(R.string.as_open_settings), onClick = { openAppSettings(context) })
                }
            }

            // 2 · who
            AsLabel(stringResource(R.string.as_step_who))
            AsTextField(
                f.name, vm::onName, stringResource(R.string.as_borrower_name),
                isError = LendError.NAME in st.errors,
                supporting = if (LendError.NAME in st.errors) stringResource(R.string.as_err_name) else null,
            )
            if (st.suggestions.isNotEmpty() && f.name.isBlank()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    st.suggestions.take(6).forEach { s -> VkChip(s.borrowerName, selected = false, onClick = { vm.onSuggestion(s) }) }
                }
            }
            AsTextField(
                f.phone, vm::onPhone, stringResource(R.string.as_borrower_phone),
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )

            // 3 · asset — pick from the list of existing assets, or add a new one (owner 2026-11-05).
            AsLabel(stringResource(R.string.as_step_asset))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssetDropdown(
                    names = st.assets.map { it.id to it.name },
                    selectedId = f.assetId,
                    isError = LendError.ASSET in st.errors,
                    onPick = vm::onAsset,
                    modifier = Modifier.weight(1f),
                )
                VkTonalButton(stringResource(R.string.v4_asset_new_short), vm::showNewAsset, sign = "+")
            }
            if (LendError.ASSET in st.errors) Text(stringResource(R.string.as_err_asset), color = MaterialTheme.colorScheme.error)

            // 4 · dates
            AsLabel(stringResource(R.string.as_step_dates))
            VkCard {
                Text(stringResource(R.string.as_handover_date), style = MaterialTheme.typography.labelLarge)
                VkTextButton("${AssetsFormat.dmy(f.handover)} · ${st.handoverTithi}", onClick = { pickHandover = true })
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.as_return_after), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 7, 15, 30).forEach { d -> VkChip(stringResource(R.string.v4_days_n, d), selected = f.returnDays == d, onClick = { vm.onReturnDays(d) }) }
                    VkChip(stringResource(R.string.as_pick_date), selected = f.customReturn != null, onClick = { pickReturn = true })
                }
                Text(
                    stringResource(R.string.as_return_date, AssetsFormat.dmy(f.expectedReturn)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (LendError.RETURN_BEFORE_HANDOVER in st.errors) {
                    Text(stringResource(R.string.as_err_return_before), color = MaterialTheme.colorScheme.error)
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.as_notify_on_due), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = f.notify, onCheckedChange = vm::onNotify)
                }
            }

            // WhatsApp preview
            if (st.preview.isNotBlank()) {
                VkCard(tone = com.vraj.khetihisab.ui.components.VkTone.Primary) {
                    Text(stringResource(R.string.as_preview), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(st.preview, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (pickHandover) AsDatePickerDialog(f.handover, onDismiss = { pickHandover = false }, onPicked = vm::onHandover)
    if (pickReturn) AsDatePickerDialog(f.expectedReturn, onDismiss = { pickReturn = false }, onPicked = vm::onCustomReturn)

    when (st.dialog) {
        // Same dialog shape and button order everywhere: primary action right, text button left.
        LendDialog.NO_PHOTO -> AlertDialog(
            onDismissRequest = vm::dismissDialog,
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.as_no_photo_confirm), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(stringResource(R.string.as_no_photo_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { VkPrimaryButton(stringResource(R.string.as_take_photo), onClick = { vm.dismissDialog(); camera.launch() }) },
            dismissButton = { VkTextButton(stringResource(R.string.as_save_anyway), onClick = vm::confirmDialog) },
        )
        LendDialog.ALREADY_LENT -> AlertDialog(
            onDismissRequest = vm::dismissDialog,
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.as_already_lent), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(stringResource(R.string.as_already_lent_body, st.selectedAsset?.name.orEmpty()), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { VkPrimaryButton(stringResource(R.string.as_continue), onClick = vm::confirmDialog) },
            dismissButton = { VkTextButton(stringResource(R.string.as_cancel), onClick = vm::dismissDialog) },
        )
        LendDialog.NEW_ASSET -> AssetFormDialog(
            title = stringResource(R.string.v4_asset_add_title),
            initialName = "", initialPrice = "", initialNote = "",
            onDismiss = vm::dismissDialog,
            onSave = { n, _, _ -> vm.addAsset(n) },
        )
        null -> Unit
    }
}

/** Dropdown of existing assets (standard M3 exposed menu). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AssetDropdown(
    names: List<Pair<Long, String>>,
    selectedId: Long?,
    isError: Boolean,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val selected = names.firstOrNull { it.first == selectedId }?.second.orEmpty()
    androidx.compose.material3.ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it && names.isNotEmpty() }, modifier = modifier) {
        androidx.compose.material3.OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(stringResource(if (names.isEmpty()) R.string.v4_asset_none_yet else R.string.v4_asset_pick), maxLines = 1) },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .testTag("asset_dropdown"),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            names.forEach { (id, name) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1) },
                    onClick = { onPick(id); open = false },
                    modifier = Modifier.heightIn(min = 56.dp),
                )
            }
        }
    }
}
