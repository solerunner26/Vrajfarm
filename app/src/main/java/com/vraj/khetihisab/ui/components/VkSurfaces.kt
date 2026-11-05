package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.decor.VkLeafDivider
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage

enum class VkTone { Neutral, Primary, Panchang, Error }

/** Card: 16dp radius, 16dp padding, 1dp outlineVariant border, no elevation (doc 01 §2). */
@Composable
fun VkCard(
    modifier: Modifier = Modifier,
    tone: VkTone = VkTone.Neutral,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val x = LocalVkExtra.current
    val (bg, border, fg) = when (tone) {
        VkTone.Neutral -> Triple(cs.surfaceContainerLowest, cs.outlineVariant, cs.onSurface)
        VkTone.Primary -> Triple(cs.secondaryContainer, cs.outlineVariant, cs.onSecondaryContainer)
        VkTone.Panchang -> Triple(cs.tertiaryContainer, x.panchangBorder, cs.onTertiaryContainer)
        VkTone.Error -> Triple(cs.errorContainer, cs.errorContainer, cs.onErrorContainer)
    }
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides fg,
        ) { content() }
    }
}

/** Section title with the leaf-sprig divider (doc 01 §4.2). */
@Composable
fun VkSectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        VkLeafDivider()
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(cs.outlineVariant))
        if (action != null && onAction != null) {
            VkTextButton(action, onAction)
        }
    }
}

/** List row ≥ 60dp (css .vk-row: 64dp). Title + meta single-line with ellipsis (doc 01 §8.3). */
@Composable
fun VkListRow(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    leadingIcon: VkIcon? = null,
    showChevron: Boolean = false,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (onClick != null) {
                    Modifier.vkPressScale(interaction, 0.98f)
                        .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            VkIconImage(leadingIcon, size = 40.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta != null) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.wrapContentWidth()) { trailing() }
        if (showChevron) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

/** Hairline between rows inside a flush card. */
@Composable
fun VkRowDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalVkExtra.current.divider))
}

enum class AmountKind { INCOME, EXPENSE, PENDING, NEUTRAL }

/** Money text: + primary (income), − onSurface (expense), error (pending/overdue). Never shrinks. */
@Composable
fun VkAmount(
    paise: Long,
    kind: AmountKind,
    modifier: Modifier = Modifier,
    style: TextStyle = com.vraj.khetihisab.ui.theme.VkCurrencyRow,
) {
    val cs = MaterialTheme.colorScheme
    val (sign, color) = when (kind) {
        AmountKind.INCOME -> "+" to cs.primary
        AmountKind.EXPENSE -> "−" to cs.onSurface
        AmountKind.PENDING -> "" to cs.error
        AmountKind.NEUTRAL -> (if (paise < 0) "−" else "") to cs.onSurface
    }
    Text(
        "$sign${VkFormat.rupees(paise)}",
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.wrapContentWidth(),
    )
}

sealed interface VkStatus {
    data object Ok : VkStatus
    data class DueSoon(val days: Int) : VkStatus
    data class Overdue(val days: Int) : VkStatus
}

@Composable
fun VkStatusBadge(status: VkStatus, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg, label) = when (status) {
        VkStatus.Ok -> Triple(cs.primaryContainer, cs.onPrimaryContainer, stringResource(R.string.status_ok))
        is VkStatus.DueSoon -> Triple(
            cs.surfaceContainer, cs.onSurface,
            if (status.days <= 0) stringResource(R.string.status_due_today) else stringResource(R.string.status_due_in, status.days),
        )
        is VkStatus.Overdue -> Triple(cs.errorContainer, cs.onErrorContainer, stringResource(R.string.status_overdue, status.days))
    }
    VkBadge(label, bg, fg, modifier)
}

@Composable
fun VkBadge(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        Text(text, color = content, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
    }
}
