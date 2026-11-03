package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import com.vraj.khetihisab.ui.theme.VkTheme
import com.vraj.khetihisab.ui.theme.VkThemeId
import java.time.LocalDate

/** Component gallery used by the light / dark previews (doc 02 acceptance). Not shipped in a screen. */
@Composable
internal fun VkComponentGallery() {
    val day = remember { GujaratiPanchangHelper.day(LocalDate.of(2026, 11, 3)) }
    var seg by remember { mutableStateOf(0) }
    var chip by remember { mutableStateOf(true) }
    Column(
        Modifier.background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VkTopHeader("વ્રજ ખેતી હિસાબ", subtitle = "03 નવેમ્બર 2026", onBack = {}, hero = true)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VkPanchangCard(day, onClick = {})
            VkSectionTitle("વિભાગો", action = "બધું જુઓ", onAction = {})
            VkPrimaryButton("આવક / વેચાણ", {}, Modifier.fillMaxWidth(), sign = "+")
            VkTonalButton("રિમાઇન્ડર બનાવો", {}, Modifier.fillMaxWidth(), icon = VkIcon.REMINDER)
            VkOutlinedButton("રદ કરો", {}, Modifier.fillMaxWidth())
            VkSegmented(listOf(0 to "રોકડ ચૂકવેલ", 1 to "ઉધાર બાકી"), seg, { seg = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VkChip("7", chip, { chip = !chip })
                VkChip("15", !chip, { chip = !chip })
            }
            VkCard {
                VkListRow("ખાતર · યુરિયા", meta = "02-11-2026 · આસો વદ આઠમ", leadingIcon = VkIcon.FARM, trailing = { VkAmount(245_000, AmountKind.EXPENSE) })
                VkListRow("કપાસ વેચાણ", meta = "01-11-2026", leadingIcon = VkIcon.INCOME, trailing = { VkAmount(12_500_000, AmountKind.INCOME) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VkStatusBadge(VkStatus.Ok)
                    VkStatusBadge(VkStatus.DueSoon(2))
                    VkStatusBadge(VkStatus.Overdue(3))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VkIcon.entries.take(8).forEach { VkIconImage(it, size = 32.dp) }
            }
            VkPhotoSlot(null, "ફોટો", {})
            VkNumericKeypad({}, {})
        }
        VkBottomDock({}, {})
    }
}

@Preview(name = "Components · green", widthDp = 360, heightDp = 1600)
@Composable
private fun GalleryGreen() = VkTheme(VkThemeId.GREEN) { VkComponentGallery() }

@Preview(name = "Components · night", widthDp = 360, heightDp = 1600)
@Composable
private fun GalleryNight() = VkTheme(VkThemeId.NIGHT) { VkComponentGallery() }

@Preview(name = "Components · kesar", widthDp = 360, heightDp = 1600)
@Composable
private fun GalleryKesar() = VkTheme(VkThemeId.KESAR) { VkComponentGallery() }
