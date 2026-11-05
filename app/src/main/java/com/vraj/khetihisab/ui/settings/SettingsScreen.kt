package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.BuildConfig
import com.vraj.khetihisab.R
import com.vraj.khetihisab.backup.BackupState
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.security.BiometricHelper
import com.vraj.khetihisab.ui.components.VkTile
import com.vraj.khetihisab.ui.components.VkTileOrnament
import com.vraj.khetihisab.ui.components.VkTileRow
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.components.themeFullNameRes
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.update.AppUpdater
import com.vraj.khetihisab.update.UpdateState
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Navigation targets of the main Settings list (wired by [settingsGraph]). */
data class SettingsActions(
    val onBack: () -> Unit,
    val onOpen: (String) -> Unit,
)

@Composable
fun SettingsRoute(actions: SettingsActions) {
    val vm: SettingsViewModel = screenViewModel { deps, _ -> SettingsViewModel.create(deps) }
    SettingsScreen(vm, actions)
}

/**
 * Settings as tiles (owner request 2026-11-04): થીમ, સુરક્ષા, બેકઅપ, પંચાંગ સૂચના, સામાન્ય સેટિંગ,
 * રિપોર્ટ, મદદ, એપ અપડેટ. Each opens its own page, so this screen stays short.
 */
@Composable
fun SettingsScreen(vm: SettingsViewModel, actions: SettingsActions) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val bioAvailable = remember { BiometricHelper.isAvailable(ctx) }
    var checking by remember { mutableStateOf(false) }
    var upToDate by remember { mutableStateOf(false) }

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_title), onBack = actions.onBack)
        StBodyColumn {
            val alertsOn = PanchangAlertType.entries.count { s.enabled(it) }
            val backupState = s.backup.stateAt(System.currentTimeMillis())
            val backupSub = when {
                backupState == BackupState.FAILED -> stringResource(R.string.bk_pill_failed)
                s.backup.lastSuccessAt != null -> SettingsFormat.backupWhen(
                    s.backup.lastSuccessAt!!, LocalDate.now(SettingsFormat.IST), stringResource(R.string.st_today), stringResource(R.string.st_tomorrow),
                )
                else -> stringResource(R.string.st_backup_never)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VkTileRow {
                    VkTile(
                        VkIcon.SETTINGS, stringResource(R.string.v2_tile_theme), { actions.onOpen(SettingsRoutes.THEME) },
                        Modifier.weight(1f).fillMaxHeight(), subtitle = stringResource(themeFullNameRes(s.theme.themeId)),
                        ornament = VkTileOrnament.WAVE, index = 0, testTag = "st_tile_theme",
                    )
                    VkTile(
                        VkIcon.LOCK, stringResource(R.string.v2_tile_security), { actions.onOpen(SettingsRoutes.SECURITY) },
                        Modifier.weight(1f).fillMaxHeight(),
                        subtitle = stringResource(if (s.biometricEnabled && bioAvailable) R.string.v2_sec_pin_bio else R.string.v2_sec_pin),
                        ornament = VkTileOrnament.LEAF, index = 1, testTag = "st_tile_security",
                    )
                }
                VkTileRow {
                    VkTile(
                        VkIcon.BACKUP, stringResource(R.string.st_backup_title), { actions.onOpen(SettingsRoutes.BACKUP) },
                        Modifier.weight(1f).fillMaxHeight(), subtitle = backupSub,
                        ornament = VkTileOrnament.SUN, index = 2, testTag = "st_tile_backup",
                        alert = !s.backup.takenRecently(System.currentTimeMillis()),
                    )
                    VkTile(
                        VkIcon.PANCHANG, stringResource(R.string.st_panchang_alerts), { actions.onOpen(SettingsRoutes.PANCHANG_ALERTS) },
                        Modifier.weight(1f).fillMaxHeight(), subtitle = stringResource(R.string.v2_alerts_on, alertsOn),
                        ornament = VkTileOrnament.MOON, index = 3, testTag = "st_tile_panchang",
                    )
                }
                VkTileRow {
                    VkTile(
                        VkIcon.FARM, stringResource(R.string.v2_tile_general), { actions.onOpen(SettingsRoutes.GENERAL) },
                        Modifier.weight(1f).fillMaxHeight(), subtitle = stringResource(R.string.v2_general_sub),
                        ornament = VkTileOrnament.FIELD, index = 4, testTag = "st_tile_general",
                    )
                    VkTile(
                        VkIcon.REPORT, stringResource(R.string.st_reports), { actions.onOpen(SettingsRoutes.reports()) },
                        Modifier.weight(1f).fillMaxHeight(), subtitle = stringResource(R.string.v2_reports_sub),
                        ornament = VkTileOrnament.RANGOLI, index = 5, testTag = "st_tile_reports",
                    )
                }
                VkTileRow {
                    VkTile(
                        VkIcon.ASSET, stringResource(R.string.v3_assets_tile), { actions.onOpen("assets") },
                        Modifier.weight(1f).fillMaxHeight(), subtitle = stringResource(R.string.v3_assets_sub),
                        ornament = VkTileOrnament.WAVE, index = 6, testTag = "st_tile_assets",
                    )
                    VkTile(
                        VkIcon.ANIMAL, stringResource(R.string.v3_milk_title), { actions.onOpen("milk") },
                        Modifier.weight(1f).fillMaxHeight(), subtitle = stringResource(R.string.v3_milk_sub),
                        ornament = VkTileOrnament.MOON, index = 7, testTag = "st_tile_milk",
                    )
                }
                VkTileRow {
                    VkTile(
                        VkIcon.HELP, stringResource(R.string.st_set_help), { actions.onOpen(SettingsRoutes.HELP) },
                        Modifier.weight(1f).fillMaxHeight(), subtitle = stringResource(R.string.v2_help_sub),
                        ornament = VkTileOrnament.LEAF, index = 6, testTag = "st_tile_help",
                    )
                    VkTile(
                        VkIcon.INCOME, stringResource(R.string.v2_tile_update), {
                            if (!checking) {
                                checking = true
                                upToDate = false
                                scope.launch {
                                    val u = AppUpdater.check()
                                    checking = false
                                    if (u == null) upToDate = true else UpdateState.available.value = u
                                }
                            }
                        },
                        Modifier.weight(1f).fillMaxHeight(),
                        subtitle = when {
                            checking -> stringResource(R.string.v2_up_checking)
                            upToDate -> stringResource(R.string.v2_up_latest)
                            else -> stringResource(R.string.v2_version, BuildConfig.VERSION_NAME)
                        },
                        ornament = VkTileOrnament.SUN, index = 7, testTag = "st_tile_update",
                    )
                }
            }
            Text(
                stringResource(R.string.v2_version_line, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal data class PanchangTypeRow(val type: PanchangAlertType, val title: Int, val sub: Int, val icon: VkIcon)

internal val PanchangTypeRows = listOf(
    PanchangTypeRow(PanchangAlertType.BIJ, R.string.st_type_bij, R.string.st_pa_bij_sub, VkIcon.MOON),
    PanchangTypeRow(PanchangAlertType.AGIYARAS, R.string.st_type_agiyaras, R.string.st_pa_ag_sub, VkIcon.MOON),
    PanchangTypeRow(PanchangAlertType.POONAM, R.string.st_type_poonam, R.string.st_pa_poonam_sub, VkIcon.MOON),
    PanchangTypeRow(PanchangAlertType.AMAS, R.string.st_type_amas, R.string.st_pa_amas_sub, VkIcon.MOON),
    PanchangTypeRow(PanchangAlertType.FESTIVAL, R.string.st_type_festival, R.string.st_pa_fest_sub, VkIcon.DIYA),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EveningTimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
    StDialog(
        title = stringResource(R.string.st_pa_set_time),
        confirmLabel = stringResource(R.string.st_save),
        onConfirm = { onConfirm(state.hour, state.minute) },
        dismissLabel = stringResource(R.string.st_cancel),
        onDismiss = onDismiss,
    ) {
        TimePicker(state = state)
    }
}
