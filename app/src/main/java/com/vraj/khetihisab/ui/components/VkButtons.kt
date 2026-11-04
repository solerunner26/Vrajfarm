package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkDimens
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage

/*
 * Buttons (doc 01 §2, §8). Always one line: maxLines = 1, softWrap = false, ellipsis.
 * If a label does not fit, shorten the label (doc 01 §8.2 budgets). Do not put an icon with a long
 * label in a button narrower than 160dp (§8.4) — pass icon = null there.
 */

private enum class VkButtonKind { Filled, Tonal, Outlined, Text }

@Composable
private fun VkButtonBase(
    text: String,
    onClick: () -> Unit,
    kind: VkButtonKind,
    modifier: Modifier,
    icon: VkIcon?,
    sign: String?,
    enabled: Boolean,
    height: Dp,
) {
    val cs = MaterialTheme.colorScheme
    val x = LocalVkExtra.current
    // M3 disabled style: 12% onSurface container, 38% onSurface content (no gradient, no shadow).
    val disabledFill = kind == VkButtonKind.Filled || kind == VkButtonKind.Tonal
    val bg: Brush = when {
        !enabled && disabledFill -> SolidColor(cs.onSurface.copy(alpha = 0.12f))
        kind == VkButtonKind.Filled -> x.brandBrush
        kind == VkButtonKind.Tonal -> SolidColor(cs.primaryContainer)
        else -> SolidColor(Color.Transparent)
    }
    val fg = when {
        !enabled -> cs.onSurface.copy(alpha = 0.38f)
        kind == VkButtonKind.Filled -> x.onGradient
        kind == VkButtonKind.Tonal -> cs.onPrimaryContainer
        else -> cs.primary
    }
    var m = modifier
        .defaultMinSize(minWidth = 64.dp)
        .heightIn(min = height)
    if (kind == VkButtonKind.Filled && enabled) {
        m = m.shadow(elevation = 6.dp, shape = CircleShape, ambientColor = x.shadow, spotColor = x.shadow)
    }
    m = m.clip(CircleShape).background(bg)
    if (kind == VkButtonKind.Outlined) m = m.border(1.dp, cs.outline, CircleShape)
    m = m.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = if (icon != null) 12.dp else 20.dp)

    Box(m, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Box(Modifier.alpha(if (enabled) 1f else 0.38f)) { VkIconImage(icon, size = 28.dp) }
                Spacer(Modifier.width(8.dp))
            }
            if (sign != null) {
                Text(sign, color = fg, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1, softWrap = false)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = fg,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Primary CTA: brand-gradient pill, 60dp. */
@Composable
fun VkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: VkIcon? = null,
    sign: String? = null,
    enabled: Boolean = true,
) = VkButtonBase(text, onClick, VkButtonKind.Filled, modifier, icon, sign, enabled, VkDimens.buttonHeight)

/** Secondary: tonal `primaryContainer` pill, 60dp. */
@Composable
fun VkTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: VkIcon? = null,
    sign: String? = null,
    enabled: Boolean = true,
) = VkButtonBase(text, onClick, VkButtonKind.Tonal, modifier, icon, sign, enabled, VkDimens.buttonHeight)

/** Tertiary: outlined pill. */
@Composable
fun VkOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    VkButtonBase(text, onClick, VkButtonKind.Outlined, modifier, null, null, enabled, 56.dp)

/** Text action (dialogs, "બધું જુઓ"). 56dp touch target. */
@Composable
fun VkTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    VkButtonBase(text, onClick, VkButtonKind.Text, modifier, null, null, enabled, 56.dp)
