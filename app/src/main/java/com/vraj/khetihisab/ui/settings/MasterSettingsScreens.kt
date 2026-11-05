package com.vraj.khetihisab.ui.settings

import kotlinx.coroutines.launch

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.CategoryMasterEntity
import com.vraj.khetihisab.data.entity.CropMasterEntity
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.Season
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon

@Composable
private fun masterViewModel(): MasterSettingsViewModel =
    screenViewModel { deps, _ -> MasterSettingsViewModel(deps.repos.master) }

/** Shows "સાચવ્યું" / duplicate errors raised after a write. */
@Composable
private fun MasterEvents(vm: MasterSettingsViewModel) {
    val saved = stringResource(R.string.st_saved)
    val dup = stringResource(R.string.st_err_duplicate)
    val snackbar = com.vraj.khetihisab.ui.components.rememberVkSnackbar()
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                MasterEvent.Saved -> snackbar.show(saved, success = true)
                is MasterEvent.Error -> snackbar.show(dup)
            }
        }
    }
}

@Composable
internal fun sectionLabel(s: AccountSection): String = stringResource(
    when (s) {
        AccountSection.FARM -> R.string.st_sec_farm
        AccountSection.ANIMAL -> R.string.st_sec_animal
        AccountSection.FAMILY -> R.string.st_sec_family
        AccountSection.GENERAL -> R.string.st_sec_general
        AccountSection.PERSONAL -> R.string.st_sec_personal
    },
)

@Composable
private fun typeLabel(t: TxnType): String = stringResource(
    when (t) {
        TxnType.INCOME -> R.string.st_txn_income
        TxnType.EXPENSE -> R.string.st_txn_expense
        TxnType.ADVANCE -> R.string.st_txn_advance
    },
)

@Composable
private fun seasonLabel(s: String): String = stringResource(
    when (s) {
        Season.RABI -> R.string.st_season_rabi
        Season.SUMMER -> R.string.st_season_summer
        else -> R.string.st_season_kharif
    },
)

@Composable
private fun managerLabel(r: ProfileRole?): String = stringResource(
    when (r) {
        ProfileRole.MANAGER_1 -> R.string.st_manager_1
        ProfileRole.MANAGER_2 -> R.string.st_manager_2
        else -> R.string.st_manager_none
    },
)

@Composable
private fun errorText(e: MasterError): String = stringResource(
    when (e) {
        MasterError.NAME -> R.string.st_err_name
        MasterError.VIGHA -> R.string.st_vigha_invalid
        MasterError.PHONE -> R.string.st_err_phone
        MasterError.DUPLICATE -> R.string.st_err_duplicate
        MasterError.IN_USE -> R.string.v2_err_in_use
    },
)

// ------------------------------------------------------------------ farms

@Composable
fun FarmsSettingsRoute(onBack: () -> Unit) {
    val vm = masterViewModel()
    MasterEvents(vm)
    val farms by vm.farms.collectAsStateWithLifecycle()
    val cycles by vm.activeCycles.collectAsStateWithLifecycle()
    val crops by vm.crops.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FarmEntity?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var deletingFarm by remember { mutableStateOf<Pair<FarmEntity, Int>?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_set_farms), onBack = onBack)
        StBodyColumn {
            VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column {
                    farms.forEachIndexed { i, f ->
                        val crop = cycles.firstOrNull { it.cycle.farmId == f.id }?.crop?.name
                        val vigha = if (f.areaInVigha > 0) stringResource(R.string.st_vigha_value, SettingsFormat.vighaText(f.areaInVigha)) else stringResource(R.string.st_vigha_not_set)
                        VkListRow(
                            title = f.name,
                            meta = listOfNotNull(vigha, if (f.jointManagers) stringResource(R.string.v3_both_bhagiya) else f.managerRole?.let { managerLabel(it) }, crop).joinToString(" · "),
                            leadingIcon = VkIcon.FARM,
                            showChevron = true,
                            onClick = { editing = f },
                        )
                        if (i < farms.lastIndex) VkRowDivider()
                    }
                }
            }
            VkTonalButton(stringResource(R.string.st_add_farm), { adding = true }, Modifier.fillMaxWidth())
        }
    }

    val target = editing
    if (target != null || adding) {
        val cycle = target?.let { f -> cycles.firstOrNull { it.cycle.farmId == f.id }?.cycle }
        FarmEditDialog(
            farm = target,
            crops = crops.filter { it.isActive },
            initialCropId = cycle?.cropId,
            initialSeason = cycle?.season ?: Season.KHARIF,
            initialYear = cycle?.year ?: vm.currentYear(),
            onDismiss = { editing = null; adding = false },
            onDelete = target?.let { f ->
                {
                    editing = null
                    scope.launch { deletingFarm = f to vm.farmUsage(f) }
                }
            },
            onSave = { edit -> vm.saveFarm(edit).also { if (it == null) { editing = null; adding = false } } },
        )
    }
    deletingFarm?.let { (f, used) ->
        DeleteConfirm(
            f.name,
            if (used > 0) stringResource(R.string.v2_delete_farm_used, used) else stringResource(R.string.v2_delete_cat_unused),
            { deletingFarm = null },
        ) { vm.deleteFarm(f); deletingFarm = null }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FarmEditDialog(
    farm: FarmEntity?,
    crops: List<CropMasterEntity>,
    initialCropId: Long?,
    initialSeason: String,
    initialYear: Int,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (FarmEdit) -> MasterError?,
) {
    var name by rememberSaveable(farm?.id) { mutableStateOf(farm?.name.orEmpty()) }
    var vigha by rememberSaveable(farm?.id) { mutableStateOf(farm?.areaInVigha?.takeIf { it > 0 }?.let(SettingsFormat::vighaText).orEmpty()) }
    // "none" | "m1" | "m2" | "both" — a farm can be run by one or BOTH ભાગિયા (2026-11-05).
    var manager by rememberSaveable(farm?.id) {
        mutableStateOf(
            when {
                farm?.jointManagers == true -> "both"
                farm?.managerRole == ProfileRole.MANAGER_1 -> "m1"
                farm?.managerRole == ProfileRole.MANAGER_2 -> "m2"
                else -> "none"
            },
        )
    }
    var cropId by rememberSaveable(farm?.id) { mutableStateOf(initialCropId) }
    var season by rememberSaveable(farm?.id) { mutableStateOf(initialSeason) }
    var year by rememberSaveable(farm?.id) { mutableStateOf(initialYear.toString()) }
    var error by remember { mutableStateOf<MasterError?>(null) }

    StDialog(
        title = farm?.name ?: stringResource(R.string.st_add_farm),
        confirmLabel = stringResource(R.string.st_save),
        onConfirm = {
            val role = when (manager) {
                "m1" -> ProfileRole.MANAGER_1
                "m2" -> ProfileRole.MANAGER_2
                "both" -> ProfileRole.MANAGER_1
                else -> null
            }
            error = onSave(FarmEdit(farm?.id, name, vigha, role, joint = manager == "both", cropId = cropId, season = season, year = year.toIntOrNull() ?: initialYear))
        },
        dismissLabel = stringResource(R.string.st_cancel),
        onDismiss = onDismiss,
        deleteLabel = if (onDelete != null) stringResource(R.string.scr_delete) else null,
        onDelete = onDelete,
    ) {
        StTextField(name, { name = it; error = null }, stringResource(R.string.st_farm_name), isError = error == MasterError.NAME)
        StTextField(
            vigha, { v -> vigha = v.filter { it.isDigit() || it == '.' }; error = null },
            stringResource(R.string.st_vigha),
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = error == MasterError.VIGHA,
            supporting = stringResource(R.string.st_vigha_invalid),
        )
        com.vraj.khetihisab.ui.components.VkDropdownField(
            label = stringResource(R.string.st_manager),
            options = listOf(
                "none" to stringResource(R.string.st_manager_none),
                "m1" to stringResource(R.string.st_manager_1),
                "m2" to stringResource(R.string.st_manager_2),
                "both" to stringResource(R.string.v3_both_bhagiya),
            ),
            selected = manager,
            onSelect = { manager = it },
            testTag = "farm_manager",
        )
        com.vraj.khetihisab.ui.components.VkDropdownField(
            label = stringResource(R.string.st_active_crop),
            options = crops.map { it.id to it.name },
            selected = cropId,
            onSelect = { cropId = it },
            testTag = "farm_crop",
        )
        VkSegmented(
            options = listOf(Season.KHARIF, Season.RABI, Season.SUMMER).map { it to seasonLabel(it) },
            selected = season,
            onSelect = { season = it },
        )
        StTextField(year, { v -> year = v.filter(Char::isDigit).take(4) }, stringResource(R.string.st_year), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
        error?.let { StError(errorText(it)) }
    }
}

// ------------------------------------------------------------------ crops

@Composable
fun CropsSettingsRoute(onBack: () -> Unit) {
    val vm = masterViewModel()
    MasterEvents(vm)
    val crops by vm.crops.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf<CropMasterEntity?>(null) }
    var deletingCrop by remember { mutableStateOf<CropMasterEntity?>(null) }
    var cropBlocked by remember { mutableStateOf<CropMasterEntity?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var adding by rememberSaveable { mutableStateOf(false) }
    val on = stringResource(R.string.st_on)
    val off = stringResource(R.string.st_off)

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_set_crops), onBack = onBack)
        StBodyColumn {
            VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column {
                    crops.forEachIndexed { i, c ->
                        VkListRow(
                            title = c.name,
                            meta = if (c.isActive) on else off,
                            onClick = { renaming = c },
                            trailing = { Switch(checked = c.isActive, onCheckedChange = { vm.setCropActive(c, it) }) },
                        )
                        if (i < crops.lastIndex) VkRowDivider()
                    }
                }
            }
            StNote(stringResource(R.string.st_rename_hint))
            VkTonalButton(stringResource(R.string.st_new_crop), { adding = true }, Modifier.fillMaxWidth())
        }
    }
    if (adding) {
        NameDialog(stringResource(R.string.st_new_crop), "", onDismiss = { adding = false }) { n -> vm.addCrop(n).also { if (it == null) adding = false } }
    }
    renaming?.let { c ->
        NameDialog(
            stringResource(R.string.st_rename), c.name, onDismiss = { renaming = null },
            onDelete = {
                renaming = null
                scope.launch {
                    val used = vm.cropUsage(c)
                    if (used > 0) cropBlocked = c else deletingCrop = c
                }
            },
        ) { n -> vm.renameCrop(c, n).also { if (it == null) renaming = null } }
    }
    deletingCrop?.let { c ->
        DeleteConfirm(c.name, stringResource(R.string.v2_delete_crop_body), { deletingCrop = null }) { vm.deleteCrop(c); deletingCrop = null }
    }
    cropBlocked?.let { c ->
        StDialog(
            title = c.name,
            confirmLabel = stringResource(R.string.v2_switch_off),
            onConfirm = { vm.setCropActive(c, false); cropBlocked = null },
            dismissLabel = stringResource(R.string.st_cancel),
            onDismiss = { cropBlocked = null },
        ) { Text(stringResource(R.string.v2_crop_in_use), style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (String) -> MasterError?) {
    var name by rememberSaveable { mutableStateOf(initial) }
    var error by remember { mutableStateOf<MasterError?>(null) }
    StDialog(
        title = title,
        confirmLabel = stringResource(R.string.st_save),
        onConfirm = { error = onSave(name) },
        dismissLabel = stringResource(R.string.st_cancel),
        onDismiss = onDismiss,
        deleteLabel = if (onDelete != null) stringResource(R.string.scr_delete) else null,
        onDelete = onDelete,
    ) {
        StTextField(name, { name = it; error = null }, stringResource(R.string.st_display_name), isError = error != null)
        error?.let { StError(errorText(it)) }
    }
}

/** "કાઢી નાખશો?" with what happens to old entries. */
@Composable
private fun DeleteConfirm(name: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    StDialog(
        title = stringResource(R.string.v2_delete_q, name),
        confirmLabel = stringResource(R.string.scr_delete),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.st_cancel),
        onDismiss = onDismiss,
    ) { Text(body, style = MaterialTheme.typography.bodyLarge) }
}

// ------------------------------------------------------------------ categories

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesSettingsRoute(onBack: () -> Unit) {
    val vm = masterViewModel()
    MasterEvents(vm)
    val all by vm.categories.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(AccountSection.FARM) }
    var renaming by remember { mutableStateOf<CategoryMasterEntity?>(null) }
    var deletingCat by remember { mutableStateOf<Pair<CategoryMasterEntity, Int>?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var addingType by rememberSaveable { mutableStateOf<TxnType?>(null) }
    val on = stringResource(R.string.st_on)
    val off = stringResource(R.string.st_off)
    val grouped = remember(all, section) { MasterSettingsViewModel.groupCategories(all, section) }

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_set_categories), onBack = onBack)
        StBodyColumn {
            com.vraj.khetihisab.ui.components.VkDropdownField(
                label = stringResource(R.string.v6_section),
                options = MasterSettingsViewModel.SECTIONS.map { it to sectionLabel(it) },
                selected = section,
                onSelect = { section = it },
                testTag = "cat_section",
            )
            grouped.forEach { (type, list) ->
                VkSectionTitle("${sectionLabel(section)} · ${typeLabel(type)}")
                if (list.isNotEmpty()) {
                    VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                        Column {
                            list.forEachIndexed { i, c ->
                                VkListRow(
                                    title = c.name,
                                    meta = if (c.isActive) on else off,
                                    onClick = { renaming = c },
                                    trailing = { Switch(checked = c.isActive, onCheckedChange = { vm.setCategoryActive(c, it) }) },
                                )
                                if (i < list.lastIndex) VkRowDivider()
                            }
                        }
                    }
                }
                VkTonalButton(stringResource(R.string.st_new_category), { addingType = type }, Modifier.fillMaxWidth())
            }
            StNote(stringResource(R.string.st_category_note))
        }
    }
    addingType?.let { t ->
        NameDialog("${stringResource(R.string.st_new_category)} · ${typeLabel(t)}", "", onDismiss = { addingType = null }) { n ->
            vm.addCategory(section, t, n).also { if (it == null) addingType = null }
        }
    }
    renaming?.let { c ->
        NameDialog(
            stringResource(R.string.st_rename), c.name, onDismiss = { renaming = null },
            onDelete = {
                renaming = null
                scope.launch { deletingCat = c to vm.categoryUsage(c) }
            },
        ) { n -> vm.renameCategory(c, n).also { if (it == null) renaming = null } }
    }
    deletingCat?.let { (c, used) ->
        DeleteConfirm(
            c.name,
            if (used > 0) stringResource(R.string.v2_delete_cat_used, used) else stringResource(R.string.v2_delete_cat_unused),
            { deletingCat = null },
        ) { vm.deleteCategory(c); deletingCat = null }
    }
}
