package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.backup.BackupState
import com.vraj.khetihisab.backup.BackupTime
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.report.ReportType
import com.vraj.khetihisab.security.BiometricHelper
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkThemePicker
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import java.time.Instant
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, actions: SettingsActions) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bioAvailable = remember { BiometricHelper.isAvailable(context) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val on = stringResource(R.string.st_on)
    val off = stringResource(R.string.st_off)

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_title), onBack = actions.onBack)
        StBodyColumn {
            // ---- Theme ----
            VkSectionTitle(stringResource(R.string.st_theme))
            VkCard(Modifier.fillMaxWidth()) {
                VkThemePicker(selected = s.theme.themeId, onSelect = vm::setTheme)
                StNote(stringResource(R.string.st_theme_note))
            }
            VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                StSwitchRow(
                    title = stringResource(R.string.st_theme_follow),
                    checked = s.theme.followSystemDark,
                    onCheckedChange = vm::setFollowSystem,
                    onLabel = on, offLabel = off,
                )
            }

            // ---- Backup card ----
            BackupCard(s, onBackupNow = vm::backupNow, onConfigure = { actions.onOpen(SettingsRoutes.BACKUP) })

            // ---- Panchang alerts ----
            VkSectionTitle(stringResource(R.string.st_panchang_alerts))
            VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column {
                    PanchangTypeRows.forEach { row ->
                        StSwitchRow(
                            title = stringResource(row.title),
                            meta = stringResource(row.sub),
                            icon = row.icon,
                            checked = s.enabled(row.type),
                            onCheckedChange = { vm.setTypeEnabled(row.type, it) },
                            onLabel = on, offLabel = off,
                        )
                        if (row.type == PanchangAlertType.BIJ) {
                            StSwitchRow(
                                title = stringResource(R.string.st_pa_vad_bij),
                                meta = stringResource(R.string.st_pa_vad_bij_sub),
                                checked = s.alert.includeVadBij,
                                enabled = s.enabled(PanchangAlertType.BIJ),
                                onCheckedChange = vm::setVadBij,
                                modifier = Modifier.fillMaxWidth(),
                                onLabel = on, offLabel = off,
                            )
                        }
                        VkRowDivider()
                    }
                    StSwitchRow(
                        title = stringResource(R.string.st_pa_show_guidance),
                        meta = stringResource(R.string.st_pa_show_guidance_sub),
                        icon = VkIcon.PANCHANG,
                        checked = s.alert.showGuidance,
                        onCheckedChange = vm::setShowGuidance,
                        onLabel = on, offLabel = off,
                    )
                    VkRowDivider()
                    VkListRow(
                        title = stringResource(R.string.st_pa_evening),
                        meta = stringResource(R.string.st_pa_evening_sub),
                        leadingIcon = VkIcon.REMINDER,
                        onClick = { showTime = true },
                        trailing = {
                            VkBadge(
                                SettingsFormat.clock12(s.alert.eveningHour, s.alert.eveningMinute),
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        },
                    )
                    VkRowDivider()
                    VkListRow(
                        title = stringResource(R.string.st_pa_edit_guidance),
                        meta = stringResource(R.string.st_pa_edit_guidance_sub),
                        leadingIcon = VkIcon.BILL,
                        showChevron = true,
                        onClick = { actions.onOpen(SettingsRoutes.PANCHANG) },
                    )
                    VkRowDivider()
                    VkListRow(
                        title = stringResource(R.string.st_pa_calendar),
                        leadingIcon = VkIcon.PANCHANG,
                        showChevron = true,
                        onClick = { actions.onOpen(SettingsRoutes.calendar()) },
                    )
                }
            }

            // ---- General ----
            VkSectionTitle(stringResource(R.string.st_general))
            VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column {
                    NavRow(R.string.st_set_profiles, VkIcon.PROFILE) { actions.onOpen(SettingsRoutes.PROFILES) }
                    NavRow(R.string.st_set_farms, VkIcon.FARM) { actions.onOpen(SettingsRoutes.FARMS) }
                    NavRow(R.string.st_set_crops, VkIcon.INCOME) { actions.onOpen(SettingsRoutes.CROPS) }
                    NavRow(R.string.st_set_family, VkIcon.FAMILY) { actions.onOpen(SettingsRoutes.FAMILY) }
                    NavRow(R.string.st_set_categories, VkIcon.EXPENSE) { actions.onOpen(SettingsRoutes.CATEGORIES) }
                    NavRow(R.string.st_set_templates, VkIcon.MESSAGE) { actions.onOpen(SettingsRoutes.TEMPLATES) }
                    StSwitchRow(
                        title = stringResource(R.string.st_bio_toggle),
                        meta = if (bioAvailable) null else stringResource(R.string.st_bio_unavailable),
                        icon = VkIcon.LOCK,
                        checked = s.biometricEnabled && bioAvailable,
                        enabled = bioAvailable,
                        onCheckedChange = vm::setBiometric,
                        onLabel = on, offLabel = off,
                    )
                    VkRowDivider()
                    NavRow(R.string.st_pin_change, VkIcon.LOCK) { actions.onOpen(SettingsRoutes.SECURITY) }
                    NavRow(R.string.st_set_help, VkIcon.HELP, divider = false) { actions.onOpen(SettingsRoutes.HELP) }
                }
            }

            // ---- PDF reports ----
            VkSectionTitle(stringResource(R.string.st_reports))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportType.entries.forEach { t ->
                    VkChip(stringResource(t.titleRes), selected = false, onClick = { actions.onOpen(SettingsRoutes.reports(t)) })
                }
            }
        }
    }

    if (showTime) {
        EveningTimeDialog(
            hour = s.alert.eveningHour,
            minute = s.alert.eveningMinute,
            onDismiss = { showTime = false },
            onConfirm = { h, m ->
                vm.setEveningTime(h, m)
                showTime = false
            },
        )
    }
}

@Composable
private fun NavRow(title: Int, icon: VkIcon, divider: Boolean = true, onClick: () -> Unit) {
    VkListRow(title = stringResource(title), leadingIcon = icon, showChevron = true, onClick = onClick)
    if (divider) VkRowDivider()
}

internal data class PanchangTypeRow(val type: PanchangAlertType, val title: Int, val sub: Int, val icon: VkIcon)

internal val PanchangTypeRows = listOf(
    PanchangTypeRow(PanchangAlertType.BIJ, R.string.st_type_bij, R.string.st_pa_bij_sub, VkIcon.MOON),
    PanchangTypeRow(PanchangAlertType.AGIYARAS, R.string.st_type_agiyaras, R.string.st_pa_ag_sub, VkIcon.MOON),
    PanchangTypeRow(PanchangAlertType.POONAM, R.string.st_type_poonam, R.string.st_pa_poonam_sub, VkIcon.MOON),
    PanchangTypeRow(PanchangAlertType.AMAS, R.string.st_type_amas, R.string.st_pa_amas_sub, VkIcon.MOON),
    PanchangTypeRow(PanchangAlertType.FESTIVAL, R.string.st_type_festival, R.string.st_pa_fest_sub, VkIcon.DIYA),
)

@Composable
private fun BackupCard(s: SettingsUiState, onBackupNow: () -> Unit, onConfigure: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val now = System.currentTimeMillis()
    val state = s.backup.stateAt(now)
    val today = LocalDate.now(SettingsFormat.IST)
    val todayWord = stringResource(R.string.st_today)
    val tomorrowWord = stringResource(R.string.st_tomorrow)
    val nextAt = s.backup.nextRunAt ?: BackupTime.nextRun(Instant.ofEpochMilli(now)).toInstant().toEpochMilli()
    VkCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VkIconImage(VkIcon.BACKUP, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.st_backup_title), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.st_backup_sub), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val (bg, fg) = when (state) {
                BackupState.OK -> cs.primaryContainer to cs.onPrimaryContainer
                BackupState.FAILED -> cs.errorContainer to cs.onErrorContainer
                BackupState.STALE, BackupState.NEVER -> cs.surfaceContainer to cs.onSurface
            }
            VkBadge(stringResource(state.pillRes), bg, fg)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BackupStat(
                stringResource(R.string.st_backup_last),
                s.backup.lastSuccessAt?.let { SettingsFormat.backupWhen(it, today, todayWord, tomorrowWord) }
                    ?: stringResource(R.string.st_backup_never),
                Modifier.weight(1f),
            )
            BackupStat(stringResource(R.string.st_backup_next), SettingsFormat.backupWhen(nextAt, today, todayWord, tomorrowWord), Modifier.weight(1f))
        }
        if (state == BackupState.FAILED) {
            s.backup.lastFailure?.let { StError(stringResource(it.messageRes)) }
        }
        if (!s.backupConfigured) StNote(stringResource(R.string.bk_err_not_configured))
        VkTonalButton(stringResource(R.string.st_backup_now), onBackupNow, Modifier.fillMaxWidth())
        VkTextButton(stringResource(R.string.st_backup_configure), onConfigure, Modifier.fillMaxWidth())
    }
}

@Composable
private fun BackupStat(label: String, value: String, modifier: Modifier) {
    VkCard(modifier, contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EveningTimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
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
