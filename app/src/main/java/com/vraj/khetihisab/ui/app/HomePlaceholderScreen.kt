package com.vraj.khetihisab.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import com.vraj.khetihisab.ui.components.AmountKind
import com.vraj.khetihisab.ui.components.VkAmount
import com.vraj.khetihisab.ui.components.VkBottomDock
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkHeaderChip
import com.vraj.khetihisab.ui.components.VkHeaderIconButton
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkPanchangCard
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkStatus
import com.vraj.khetihisab.ui.components.VkStatusBadge
import com.vraj.khetihisab.ui.components.VkThemePicker
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import com.vraj.khetihisab.ui.theme.VkTheme
import com.vraj.khetihisab.ui.theme.VkThemeId
import java.time.LocalDate
import java.time.ZoneId

/**
 * Wave 1 placeholder Home. Exercises VkTheme + the Vk* components and the live theme switch.
 * compose-screens replaces this with HomeScreen (doc 05). Figures are sample data only.
 */
@Composable
fun HomePlaceholderScreen(themeId: VkThemeId, onThemeChange: (VkThemeId) -> Unit) {
    val today = remember { LocalDate.now(ZoneId.of("Asia/Kolkata")) }
    val day = remember(today) { GujaratiPanchangHelper.day(today) }
    val banner = remember(today) { GujaratiPanchangHelper.bannerLabel(today) }
    val x = LocalVkExtra.current

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp), // dock height + 16dp (doc 17)
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                VkTopHeader(
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.home_greeting),
                    hero = true,
                    actions = { VkHeaderIconButton(VkIcon.SETTINGS, stringResource(R.string.settings_title), onClick = {}) },
                ) {
                    Text(banner, color = x.onGradient, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    VkHeaderChip(day.vsLabel)
                }
            }
            item { VkPanchangCard(day, onClick = {}, modifier = Modifier.padding(horizontal = 16.dp)) }
            item { VkSectionTitle(stringResource(R.string.home_sections), Modifier.padding(horizontal = 16.dp)) }
            item {
                val sections = listOf(
                    VkIcon.FARM to R.string.home_section_farm,
                    VkIcon.FAMILY to R.string.home_section_family,
                    VkIcon.PERSONAL to R.string.home_section_personal,
                    VkIcon.SHOPPING to R.string.home_section_shopping,
                    VkIcon.ASSET to R.string.home_section_assets,
                    VkIcon.REMINDER to R.string.home_section_reminder,
                )
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sections.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { (icon, label) ->
                                VkCard(Modifier.weight(1f), onClick = {}) {
                                    VkIconImage(icon, size = 40.dp)
                                    Text(stringResource(label), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(stringResource(R.string.home_this_month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            item { VkSectionTitle(stringResource(R.string.settings_theme), Modifier.padding(horizontal = 16.dp)) }
            item { VkThemePicker(themeId, onThemeChange, Modifier.padding(horizontal = 16.dp)) }
            item {
                VkSectionTitle(stringResource(R.string.home_recent), Modifier.padding(horizontal = 16.dp), action = stringResource(R.string.home_all), onAction = {})
            }
            item {
                VkCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentPadding = 0.dp) {
                    Column {
                        VkListRow(
                            title = stringResource(R.string.home_section_farm),
                            meta = GujaratiPanchangHelper.listLabel(today),
                            leadingIcon = VkIcon.INCOME,
                            trailing = { VkAmount(4_500_000, AmountKind.INCOME) },
                        )
                        VkRowDivider()
                        VkListRow(
                            title = stringResource(R.string.home_section_personal),
                            meta = GujaratiPanchangHelper.listLabel(today.minusDays(1)),
                            leadingIcon = VkIcon.EXPENSE,
                            trailing = { VkAmount(68_400, AmountKind.EXPENSE) },
                        )
                        VkRowDivider()
                        VkListRow(
                            title = stringResource(R.string.home_section_reminder),
                            meta = GujaratiPanchangHelper.listLabel(today.minusDays(2)),
                            leadingIcon = VkIcon.REMINDER,
                            trailing = { VkStatusBadge(VkStatus.Overdue(3)) },
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.home_wave1_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        }
        VkBottomDock(onExpense = {}, onIncome = {}, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Preview(name = "Home · green", widthDp = 360, heightDp = 1100)
@Composable
private fun HomeGreenPreview() = VkTheme(VkThemeId.GREEN) { HomePlaceholderScreen(VkThemeId.GREEN) {} }

@Preview(name = "Home · night", widthDp = 360, heightDp = 1100)
@Composable
private fun HomeNightPreview() = VkTheme(VkThemeId.NIGHT) { HomePlaceholderScreen(VkThemeId.NIGHT) {} }
