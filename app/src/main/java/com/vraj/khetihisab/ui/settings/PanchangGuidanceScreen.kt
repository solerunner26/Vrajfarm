package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.panchang.PanchangNames
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon

@Composable
fun PanchangGuidanceRoute(onBack: () -> Unit) {
    val vm: PanchangGuidanceViewModel = screenViewModel { deps, _ -> PanchangGuidanceViewModel.create(deps) }
    PanchangGuidanceScreen(vm, onBack)
}

/** doc 10 "સલાહ બદલો" editor + "મારો તહેવાર ઉમેરો". Guidance is always framed "માન્યતા પ્રમાણે". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanchangGuidanceScreen(vm: PanchangGuidanceViewModel, onBack: () -> Unit) {
    val d by vm.draft.collectAsStateWithLifecycle()
    val festivals by vm.festivals.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_pa_edit_guidance), onBack = onBack)
        StBodyColumn {
            com.vraj.khetihisab.ui.components.VkDropdownField(
                label = stringResource(R.string.v6_alert_type),
                options = PanchangTypeRows.map { it.type to stringResource(it.title) },
                selected = d.type,
                onSelect = { vm.select(it) },
            )
            VkCard(Modifier.fillMaxWidth()) {
                StTextField(
                    value = d.doText,
                    onValueChange = vm::onDoText,
                    label = stringResource(R.string.st_pa_do_label),
                    singleLine = false,
                    minLines = 3,
                )
                StTextField(
                    value = d.avoidText,
                    onValueChange = vm::onAvoidText,
                    label = stringResource(R.string.st_pa_avoid_label),
                    singleLine = false,
                    minLines = 3,
                )
                StNote(stringResource(R.string.st_pa_one_per_line))
                StNote(stringResource(R.string.st_pa_belief))
                VkPrimaryButton(stringResource(R.string.st_save), vm::save, Modifier.fillMaxWidth(), enabled = d.loaded)
                VkTonalButton(stringResource(R.string.st_pa_reset), vm::resetToDefault, Modifier.fillMaxWidth(), enabled = d.loaded && !d.isDefault)
                if (d.saved) StNote(stringResource(R.string.st_saved))
            }

            VkSectionTitle(stringResource(R.string.st_pa_my_festivals))
            if (festivals.isNotEmpty()) {
                VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                    Column {
                        festivals.forEachIndexed { i, f ->
                            VkListRow(
                                title = f.nameGu,
                                meta = festivalRule(f),
                                leadingIcon = VkIcon.DIYA,
                                trailing = { VkTextButton(stringResource(R.string.st_delete), { confirmDelete = f.id }) },
                            )
                            if (i < festivals.lastIndex) VkRowDivider()
                        }
                    }
                }
            } else {
                StNote(stringResource(R.string.st_pa_no_festivals))
            }
            VkTonalButton(stringResource(R.string.st_pa_add_festival), { adding = true }, Modifier.fillMaxWidth())
        }
    }

    if (adding) AddFestivalDialog(onDismiss = { adding = false }, onAdd = { input -> vm.addFestival(input).also { if (it == null) adding = false } })

    confirmDelete?.let { id ->
        StDialog(
            title = stringResource(R.string.st_pa_delete_festival_q),
            confirmLabel = stringResource(R.string.st_delete),
            onConfirm = {
                vm.deleteFestival(id)
                confirmDelete = null
            },
            dismissLabel = stringResource(R.string.st_cancel),
            onDismiss = { confirmDelete = null },
        ) {}
    }
}

@Composable
private fun festivalRule(f: CustomFestivalEntity): String {
    f.fixedMonthDay?.let { return stringResource(R.string.st_pa_rule_date, it) }
    val month = f.monthIndex?.let { PanchangNames.months.getOrNull(it) }.orEmpty()
    val paksha = if (f.paksha == "VAD") PanchangNames.PAKSHA_VAD else PanchangNames.PAKSHA_SUD
    return "$month $paksha ${f.tithiInPaksha ?: ""}".trim()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFestivalDialog(onDismiss: () -> Unit, onAdd: (FestivalInput) -> FestivalError?) {
    var name by rememberSaveable { mutableStateOf("") }
    var byTithi by rememberSaveable { mutableStateOf(true) }
    var month by rememberSaveable { mutableStateOf(7) }
    var vad by rememberSaveable { mutableStateOf(false) }
    var tithi by rememberSaveable { mutableStateOf(1) }
    var date by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<FestivalError?>(null) }

    StDialog(
        title = stringResource(R.string.st_pa_add_festival),
        confirmLabel = stringResource(R.string.st_save),
        onConfirm = { error = onAdd(FestivalInput(name, byTithi, month, vad, tithi, date)) },
        dismissLabel = stringResource(R.string.st_cancel),
        onDismiss = onDismiss,
    ) {
        StTextField(name, { name = it; error = null }, stringResource(R.string.st_pa_festival_name), isError = error == FestivalError.NAME)
        VkSegmented(
            options = listOf(true to stringResource(R.string.st_pa_by_tithi), false to stringResource(R.string.st_pa_by_date)),
            selected = byTithi,
            onSelect = { byTithi = it; error = null },
        )
        if (byTithi) {
            com.vraj.khetihisab.ui.components.VkDropdownField(
                label = stringResource(R.string.st_pa_month),
                options = PanchangNames.months.mapIndexed { i, m -> i to m },
                selected = month,
                onSelect = { month = it },
            )
            VkSegmented(
                options = listOf(false to PanchangNames.PAKSHA_SUD, true to PanchangNames.PAKSHA_VAD),
                selected = vad,
                onSelect = { vad = it },
            )
            com.vraj.khetihisab.ui.components.VkDropdownField(
                label = stringResource(R.string.st_pa_tithi_number),
                options = (1..15).map { it to it.toString() },
                selected = tithi,
                onSelect = { tithi = it },
            )
        } else {
            StTextField(
                date,
                { date = it; error = null },
                stringResource(R.string.st_pa_fixed_date),
                isError = error == FestivalError.DATE,
                supporting = if (error == FestivalError.DATE) stringResource(R.string.st_pa_invalid_date) else null,
            )
        }
        if (error == FestivalError.NAME) StError(stringResource(R.string.st_err_name))
    }
}
