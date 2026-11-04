package com.vraj.khetihisab.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.AmountKind
import com.vraj.khetihisab.ui.components.VkAmount
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkPhotoSlot
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import java.time.YearMonth

/** Bottom content padding: dock / FAB height + 16dp (doc 17 "Compose implication"). */
val DockBottomPadding: Dp = 120.dp
val PlainBottomPadding: Dp = 32.dp

/** Stat tile used on hero cards: small label + big money (≥ 26sp on hero, doc 01 §3). */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    big: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = if (big) {
                MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            } else {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            },
            color = valueColor,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Initial avatar on primaryContainer (owner "ગિ", family "A"), or the stored photo. */
@Composable
fun PersonAvatar(
    photoPath: String?,
    initial: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = initial,
) {
    if (photoPath != null) {
        VkPhotoSlot(photoPath, contentDescription, onClick, modifier, size = size)
    } else {
        val cs = MaterialTheme.colorScheme
        Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(cs.primaryContainer)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                color = cs.onPrimaryContainer,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = (size.value * 0.38f).sp),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** "પાવતી જુઓ" assist chip with the camera glyph (doc 05 §4). */
@Composable
fun BillChip(onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(R.string.scr_view_bill), maxLines = 1, softWrap = false) },
        leadingIcon = { VkIconImage(VkIcon.CAMERA, size = 18.dp) },
        colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.height(36.dp),
    )
}

/**
 * One ledger row (Home recent, Personal, Farm detail). Title/meta single-line; amount never shrinks.
 * The bill chip sits in the meta column so the amount column stays aligned.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TxnListRow(
    title: String,
    meta: String,
    icon: VkIcon,
    amountPaise: Long,
    kind: AmountKind,
    billPath: String?,
    onViewBill: (String) -> Unit,
    modifier: Modifier = Modifier,
    pendingBadge: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onLongClick != null) Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick) else Modifier),
    ) {
        VkListRow(
            title = title,
            meta = meta,
            leadingIcon = icon,
            trailing = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    VkAmount(amountPaise, kind)
                    if (pendingBadge) {
                        val cs = MaterialTheme.colorScheme
                        VkBadge(stringResource(R.string.scr_pending_badge), cs.errorContainer, cs.onErrorContainer)
                    }
                }
            },
        )
        if (billPath != null) {
            Row(Modifier.padding(start = 68.dp, bottom = 8.dp)) { BillChip { onViewBill(billPath) } }
        }
    }
}

/** Month-year selector: 12 chips + year arrows (doc 08 §3). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthPickerDialog(
    current: YearMonth,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember { mutableIntStateOf(current.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text(stringResource(R.string.scr_month_pick), maxLines = 1) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    VkTextButton("‹ ${year - 1}", { year -= 1 })
                    Text(
                        year.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    VkTextButton("${year + 1} ›", { year += 1 })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..12).forEach { m ->
                        val ym = YearMonth.of(year, m)
                        VkChip(
                            text = ScreensFormat.monthGu(ym),
                            selected = ym == current,
                            onClick = { onPick(ym) },
                        )
                    }
                }
            }
        },
        confirmButton = { VkTextButton(stringResource(R.string.scr_close), onDismiss) },
    )
}

/** Simple confirm dialog with stacked actions (doc 01 §8.5). */
@Composable
fun ConfirmDialog(
    title: String,
    body: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text(title) },
        text = body?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VkPrimaryButton(confirmLabel, onConfirm, Modifier.fillMaxWidth())
                VkTextButton(stringResource(R.string.scr_cancel), onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}

/**
 * Navigates to [route] if any destination in the graph matches it (other modules register settings,
 * profiles, reminders, panchang … later). Returns false, and does nothing, when no destination exists.
 */
fun NavController.navigateSafely(route: String): Boolean = runCatching { navigate(route) { launchSingleTop = true } }.isSuccess
