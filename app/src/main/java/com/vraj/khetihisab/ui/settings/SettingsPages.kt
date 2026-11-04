package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkThemePicker
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.components.vkAppear
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon

@Composable
private fun settingsVm(): SettingsViewModel = screenViewModel { deps, _ -> SettingsViewModel.create(deps) }

// ------------------------------------------------------------------ થીમ

@Composable
fun ThemeSettingsRoute(onBack: () -> Unit) {
    val vm = settingsVm()
    val s by vm.state.collectAsStateWithLifecycle()
    StScrollBody {
        VkTopHeader(title = stringResource(R.string.v2_tile_theme), onBack = onBack)
        StBodyColumn {
            VkCard(Modifier.fillMaxWidth().vkAppear(0, "theme_pick")) {
                VkThemePicker(selected = s.theme.themeId, onSelect = vm::setTheme)
                StNote(stringResource(R.string.st_theme_note))
            }
            VkCard(Modifier.fillMaxWidth().vkAppear(1, "theme_follow"), contentPadding = 0.dp) {
                StSwitchRow(
                    title = stringResource(R.string.st_theme_follow),
                    checked = s.theme.followSystemDark,
                    onCheckedChange = vm::setFollowSystem,
                    onLabel = stringResource(R.string.st_on),
                    offLabel = stringResource(R.string.st_off),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ પંચાંગ સૂચના (own page)

@Composable
fun PanchangAlertsRoute(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = settingsVm()
    val s by vm.state.collectAsStateWithLifecycle()
    var showTime by rememberSaveable { mutableStateOf(false) }
    val on = stringResource(R.string.st_on)
    val off = stringResource(R.string.st_off)
    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_panchang_alerts), onBack = onBack)
        StBodyColumn {
            VkCard(Modifier.fillMaxWidth().vkAppear(0, "pa_types").testTag("panchang_alerts"), contentPadding = 0.dp) {
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
                }
            }
            VkCard(Modifier.fillMaxWidth().vkAppear(1, "pa_more"), contentPadding = 0.dp) {
                Column {
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
                        onClick = { onOpen(SettingsRoutes.PANCHANG) },
                    )
                    VkRowDivider()
                    VkListRow(
                        title = stringResource(R.string.st_pa_calendar),
                        leadingIcon = VkIcon.PANCHANG,
                        showChevron = true,
                        onClick = { onOpen(SettingsRoutes.calendar()) },
                    )
                }
            }
            StNote(stringResource(R.string.st_pa_belief))
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

// ------------------------------------------------------------------ સામાન્ય સેટિંગ

@Composable
fun GeneralSettingsRoute(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = settingsVm()
    val s by vm.state.collectAsStateWithLifecycle()
    StScrollBody {
        VkTopHeader(title = stringResource(R.string.v2_tile_general), onBack = onBack)
        StBodyColumn {
            VkSectionTitle(stringResource(R.string.v2_home_screen))
            VkCard(Modifier.fillMaxWidth().vkAppear(0, "gen_home"), contentPadding = 0.dp) {
                StSwitchRow(
                    title = stringResource(R.string.v2_show_numbers),
                    meta = stringResource(R.string.v2_show_numbers_sub),
                    icon = VkIcon.REPORT,
                    checked = s.showHomeNumbers,
                    onCheckedChange = vm::setShowHomeNumbers,
                    onLabel = stringResource(R.string.st_on),
                    offLabel = stringResource(R.string.st_off),
                    modifier = Modifier.testTag("toggle_show_numbers"),
                )
            }
            VkSectionTitle(stringResource(R.string.v2_masters))
            VkCard(Modifier.fillMaxWidth().vkAppear(1, "gen_masters"), contentPadding = 0.dp) {
                Column {
                    Nav(R.string.st_set_profiles, VkIcon.PROFILE) { onOpen(SettingsRoutes.PROFILES) }
                    Nav(R.string.st_set_farms, VkIcon.FARM) { onOpen(SettingsRoutes.FARMS) }
                    Nav(R.string.st_set_crops, VkIcon.INCOME) { onOpen(SettingsRoutes.CROPS) }
                    Nav(R.string.st_set_categories, VkIcon.EXPENSE) { onOpen(SettingsRoutes.CATEGORIES) }
                    Nav(R.string.st_set_templates, VkIcon.MESSAGE, divider = false) { onOpen(SettingsRoutes.TEMPLATES) }
                }
            }
        }
    }
}

@Composable
private fun Nav(title: Int, icon: VkIcon, divider: Boolean = true, onClick: () -> Unit) {
    VkListRow(title = stringResource(title), leadingIcon = icon, showChevron = true, onClick = onClick)
    if (divider) VkRowDivider()
}
