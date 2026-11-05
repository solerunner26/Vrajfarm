package com.vraj.khetihisab.ui.shopping

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Edit
import com.vraj.khetihisab.ui.components.vkAppear
import com.vraj.khetihisab.ui.components.vkPressScale
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.media.suppressLockForExternal
import com.vraj.khetihisab.ui.assets.AsDatePickerDialog
import com.vraj.khetihisab.ui.assets.AsLabel
import com.vraj.khetihisab.ui.assets.AsTextField
import com.vraj.khetihisab.ui.assets.AssetsFormat
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkFormat
import com.vraj.khetihisab.ui.components.VkHeaderIconButton
import com.vraj.khetihisab.ui.components.VkOutlinedButton
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage

/** ShoppingScreen (doc 11, artboard 11). */
@Composable
fun ShoppingScreen(vm: ShoppingViewModel, onBack: () -> Unit, onOpenAssets: () -> Unit) {
    var editing by remember { mutableStateOf<ShopRow?>(null) }
    val st by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = com.vraj.khetihisab.ui.components.rememberVkSnackbar()
    var pickDraftDate by rememberSaveable { mutableStateOf(false) }

    val boughtMsg = stringResource(R.string.as_bought_snack)
    val undoLabel = stringResource(R.string.as_undo)
    val addedMsg = stringResource(R.string.as_asset_added)
    val deletedMsg = stringResource(R.string.v5_deleted)
    val savedMsg = stringResource(R.string.v5_saved)
    val restoreLabel = stringResource(R.string.v5_undo)
    LaunchedEffect(vm) {
        vm.eventFlow.collect { e ->
            when (e) {
                is ShoppingEvent.Bought -> snackbar.show(boughtMsg.format(e.name), actionLabel = undoLabel) { vm.undo(e.id) }
                is ShoppingEvent.AssetCreated -> snackbar.show(addedMsg, success = true)
            }
        }
    }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(vm::onVoiceText)
        }
    }
    fun startVoice() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN")
        suppressLockForExternal(context)
        runCatching { voice.launch(i) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            VkTopHeader(
                title = stringResource(R.string.as_shop_title),
                onBack = onBack,
                actions = { VkHeaderIconButton(VkIcon.ASSET, stringResource(R.string.as_open_assets), onOpenAssets) },
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(com.vraj.khetihisab.ui.theme.VkDimens.listGap),
            ) {
                item(key = "seg") {
                    VkSegmented(
                        options = listOf(
                            ShopTab.FARM to stringResource(R.string.as_tab_farm),
                            ShopTab.PERSONAL to stringResource(R.string.as_tab_personal),
                        ),
                        selected = st.tab,
                        onSelect = vm::selectTab,
                    )
                }
                item(key = "add") {
                    AddRow(st, vm, onVoice = ::startVoice, onPickDate = { pickDraftDate = true })
                }
                if (st.loaded && st.groups.isEmpty()) {
                    item(key = "empty") {
                        VkCard { com.vraj.khetihisab.ui.components.VkEmptyState(VkIcon.SHOPPING, stringResource(R.string.as_shop_empty)) }
                    }
                }
                st.groups.forEachIndexed { gi, g ->
                    item(key = "g${g.date}") {
                        Column(Modifier.vkAppear(gi + 2, "shop_g${g.date}"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val d = AssetsFormat.dmy(g.date)
                            AsLabel(if (g.isToday) stringResource(R.string.as_today_prefix, d) else d)
                            VkCard(contentPadding = 0.dp) {
                                g.rows.forEachIndexed { i, row ->
                                    if (i > 0) VkRowDivider()
                                    ShopItemRow(row, st.tab, onEdit = { editing = row }) { vm.toggle(row) }
                                }
                            }
                        }
                    }
                }
                item(key = "weekly") {
                    VkCard(tone = VkTone.Primary) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VkIconImage(VkIcon.REMINDER, size = 28.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.as_weekly_note), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    if (pickDraftDate) {
        AsDatePickerDialog(st.draft.date, onDismiss = { pickDraftDate = false }, onPicked = vm::onDraftDate)
    }
    st.convert?.let { ConvertSheet(it, vm) }
    editing?.let { r ->
        ShopEditDialog(
            row = r,
            onDismiss = { editing = null },
            onSave = { n, p -> vm.saveEdit(r.item, n, p); editing = null; snackbar.show(savedMsg, success = true) },
            onDelete = {
                vm.delete(r.id)
                editing = null
                snackbar.show(deletedMsg, actionLabel = restoreLabel) { vm.restore(r.item) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddRow(st: ShoppingUiState, vm: ShoppingViewModel, onVoice: () -> Unit, onPickDate: () -> Unit) {
    val d = st.draft
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AsTextField(
                value = d.name,
                onValueChange = vm::onDraftName,
                label = stringResource(R.string.as_add_item_hint),
                keyboard = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
            val voiceLabel = stringResource(R.string.as_voice_add)
            FilledTonalIconButton(
                onClick = onVoice,
                modifier = Modifier.size(60.dp).semantics { contentDescription = voiceLabel },
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = cs.primaryContainer),
            ) { VkIconImage(VkIcon.MIC, size = 32.dp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VkTonalButton(stringResource(if (d.expanded) R.string.as_less else R.string.as_more), vm::toggleMore, Modifier.weight(1f))
            VkPrimaryButton(stringResource(R.string.as_add), vm::add, Modifier.weight(1f), sign = "+", enabled = d.name.isNotBlank())
        }
        if (d.expanded) {
            VkCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.vraj.khetihisab.ui.components.VkPickerField(stringResource(R.string.as_for_date), AssetsFormat.dmy(d.date), onPickDate)
                    if (st.tab == ShopTab.FARM && st.farms.isNotEmpty()) {
                        com.vraj.khetihisab.ui.components.VkDropdownField(
                            label = stringResource(R.string.as_farm),
                            options = listOf<Pair<Long?, String>>(null to stringResource(R.string.as_no_farm)) + st.farms.map { it.id to it.name },
                            selected = d.farmId,
                            onSelect = { vm.onDraftFarm(it) },
                        )
                    }
                    AsTextField(
                        value = d.priceText,
                        onValueChange = vm::onDraftPrice,
                        label = stringResource(R.string.as_est_price),
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    CheckRow(stringResource(R.string.as_is_tool), d.isTool, vm::onDraftTool)
                }
            }
        }
    }
}

@Composable
internal fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * ≥ 64dp row with an animated check-off (A7). Ticked = strike-through in onSurfaceVariant.
 * The price lives on the meta line and "સુધારો" is a 60dp icon button, so long names fit (audit #14).
 */
@Composable
private fun ShopItemRow(row: ShopRow, tab: ShopTab, onEdit: () -> Unit, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val where = when {
        row.farmName != null -> row.farmName
        row.isTool -> stringResource(R.string.as_new_tool)
        tab == ShopTab.PERSONAL -> stringResource(R.string.as_home)
        else -> null
    }
    val meta = listOfNotNull(where, row.pricePaise?.let { VkFormat.rupees(it) }).joinToString(" · ").ifEmpty { null }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .vkPressScale(interaction, 0.98f)
            .toggleable(value = row.struck, interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.vraj.khetihisab.ui.components.VkCheckMark(row.struck, size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.name,
                style = MaterialTheme.typography.titleMedium.copy(textDecoration = if (row.struck) TextDecoration.LineThrough else null),
                color = if (row.struck) cs.onSurfaceVariant else cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        com.vraj.khetihisab.ui.components.VkIconAction(
            androidx.compose.material.icons.Icons.Rounded.Edit,
            stringResource(R.string.v2_edit),
            onEdit,
        )
    }
}

/** સુધારો / કાઢી નાખો for one shopping item. */
@Composable
private fun ShopEditDialog(row: ShopRow, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onDelete: () -> Unit) {
    var name by remember(row.id) { mutableStateOf(row.name) }
    var price by remember(row.id) { mutableStateOf(row.pricePaise?.let(MoneyInput::fromPaise).orEmpty()) }
    var confirm by remember { mutableStateOf(false) }
    com.vraj.khetihisab.ui.components.VkDialog(
        title = stringResource(R.string.v2_edit),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.scr_save),
        onConfirm = { onSave(name, price) },
        confirmEnabled = name.isNotBlank(),
        deleteLabel = stringResource(R.string.scr_delete),
        onDelete = { confirm = true },
    ) {
        com.vraj.khetihisab.ui.components.VkTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.as_add_item_hint))
        com.vraj.khetihisab.ui.components.VkTextField(
            value = price,
            onValueChange = { v -> price = v.filter { it.isDigit() || it == '.' } },
            label = stringResource(R.string.as_est_price),
            keyboardType = KeyboardType.Decimal,
        )
    }
    if (confirm) {
        com.vraj.khetihisab.ui.components.VkConfirmDeleteDialog(
            what = row.name,
            onConfirm = { confirm = false; onDelete() },
            onDismiss = { confirm = false },
        )
    }
}

/** "મિલકતમાં ઉમેરવું છે?" sheet (canvas 11). */
@Composable
private fun ConvertSheet(c: ConvertSheet, vm: ShoppingViewModel) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    com.vraj.khetihisab.ui.components.VkBottomSheet(stringResource(R.string.as_convert_q), vm::dismissConvert) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VkIconImage(VkIcon.ASSET, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.as_convert_sub, c.name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(stringResource(R.string.as_convert_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AsTextField(c.name, vm::onConvertName, stringResource(R.string.as_name), isError = c.name.isBlank())
            AsTextField(c.priceText, vm::onConvertPrice, stringResource(R.string.as_price), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            com.vraj.khetihisab.ui.components.VkPickerField(stringResource(R.string.as_purchase_date), AssetsFormat.dmy(c.purchaseDate), { pickDate = true })
            VkPrimaryButton(stringResource(R.string.as_yes_add), vm::confirmConvert, Modifier.fillMaxWidth(), enabled = c.name.isNotBlank())
            VkTextButton(stringResource(R.string.as_no), vm::dismissConvert, Modifier.fillMaxWidth())
        }
    }
    if (pickDate) AsDatePickerDialog(c.purchaseDate, onDismiss = { pickDate = false }, onPicked = vm::onConvertDate)
}
