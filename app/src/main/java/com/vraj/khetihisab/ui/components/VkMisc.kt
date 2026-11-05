package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.NightSwatchEnd
import com.vraj.khetihisab.ui.theme.NightSwatchStart
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import com.vraj.khetihisab.ui.theme.VkThemeId
import com.vraj.khetihisab.ui.theme.VkThemes
import java.io.File

/** Home bottom dock: tonal "− ખર્ચ / ઉપાડ" + gradient "+ આવક / વેચાણ" (doc 17 #1), on [VkActionBar]. */
@Composable
fun VkBottomDock(onExpense: () -> Unit, onIncome: () -> Unit, modifier: Modifier = Modifier) {
    VkActionBar(modifier) {
        VkTonalButton(stringResource(R.string.dock_expense), onExpense, Modifier.weight(1f), sign = "−")
        VkPrimaryButton(stringResource(R.string.dock_income), onIncome, Modifier.weight(1f), sign = "+")
    }
}

/** Photo slot (people, bills, borrowers). Shows the stored WebP or a silhouette + label. */
@Composable
fun VkPhotoSlot(
    path: String?,
    placeholderText: String,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    shape: Shape = CircleShape,
) {
    val x = LocalVkExtra.current
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(x.silhouetteBg)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onCapture),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AsyncImage(model = File(path), contentDescription = placeholderText, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VkIconImage(VkIcon.CAMERA, size = (size.value * 0.36f).dp)
                Text(
                    placeholderText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Short tile label ≤ 5 chars (doc 17 #9). Full names are in [themeFullNameRes]. */
fun themeShortNameRes(id: VkThemeId): Int = when (id) {
    VkThemeId.GREEN -> R.string.theme_green_short
    VkThemeId.MATI -> R.string.theme_mati_short
    VkThemeId.AKASH -> R.string.theme_akash_short
    VkThemeId.KESAR -> R.string.theme_kesar_short
    VkThemeId.NIGHT -> R.string.theme_night_short
}

fun themeFullNameRes(id: VkThemeId): Int = when (id) {
    VkThemeId.GREEN -> R.string.theme_green
    VkThemeId.MATI -> R.string.theme_mati
    VkThemeId.AKASH -> R.string.theme_akash
    VkThemeId.KESAR -> R.string.theme_kesar
    VkThemeId.NIGHT -> R.string.theme_night
}

/** Settings › થીમ picker: five swatch tiles, live switch (doc 01 §7). */
@Composable
fun VkThemePicker(selected: VkThemeId, onSelect: (VkThemeId) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VkThemeId.entries.forEach { id ->
                val on = id == selected
                val p = VkThemes.palette(id)
                val swatch = if (id == VkThemeId.NIGHT) Brush.linearGradient(listOf(NightSwatchStart, NightSwatchEnd)) else Brush.linearGradient(listOf(p.g1, p.g2))
                val shape = RoundedCornerShape(12.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 80.dp)
                        .clip(shape)
                        .background(if (on) cs.primaryContainer else cs.surfaceContainerLowest)
                        .border(if (on) 2.dp else 1.dp, if (on) cs.primary else cs.outlineVariant, shape)
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(id) })
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(swatch))
                    Text(
                        stringResource(themeShortNameRes(id)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) cs.onPrimaryContainer else cs.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            stringResource(themeFullNameRes(selected)),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}
